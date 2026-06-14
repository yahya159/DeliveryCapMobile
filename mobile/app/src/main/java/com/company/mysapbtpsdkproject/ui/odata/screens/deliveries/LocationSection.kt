package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.company.mysapbtpsdkproject.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

/**
 * Reusable location block for a delivery.
 *
 * In [editable] mode it is collapsed to a single "Add current location" button until the user
 * captures a fix; afterwards a map preview is shown with Retake / Delete and external-app actions.
 * In read-only mode (detail screen) it shows the preview and external-app actions only, or a
 * placeholder when no location has been saved.
 */
@Composable
fun LocationSection(
    latitude: Double?,
    longitude: Double?,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onLocationCaptured: (Double, Double) -> Unit = { _, _ -> },
    onClear: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCapturing by remember { mutableStateOf(false) }
    val hasLocation = latitude != null && longitude != null

    fun captureLocation() {
        scope.launch {
            isCapturing = true
            val location = LocationUtils.fetchCurrentLocation(context)
            isCapturing = false
            if (location != null) {
                onLocationCaptured(location.latitude, location.longitude)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.capturing_location),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            captureLocation()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.location_permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestCapture() {
        if (LocationUtils.hasLocationPermission(context)) {
            captureLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(id = R.string.location_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            when {
                isCapturing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(text = stringResource(id = R.string.capturing_location))
                    }
                }

                hasLocation -> {
                    val position = LatLng(latitude!!, longitude!!)
                    val cameraPositionState = rememberCameraPositionState {
                        this.position = CameraPosition.fromLatLngZoom(position, 15f)
                    }
                    LaunchedEffect(position) {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(position, 15f)
                    }
                    val markerState = remember(latitude, longitude) {
                        MarkerState(position = position)
                    }

                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(mapType = MapType.NORMAL),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                            rotationGesturesEnabled = false,
                            tiltGesturesEnabled = false,
                            mapToolbarEnabled = false
                        )
                    ) {
                        Marker(state = markerState)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            id = R.string.location_coordinates,
                            latitude,
                            longitude
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    // External navigation apps.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                LocationUtils.openInGoogleMaps(context, latitude, longitude)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(id = R.string.open_google_maps))
                        }
                        OutlinedButton(
                            onClick = {
                                LocationUtils.openInWaze(context, latitude, longitude)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(id = R.string.open_waze))
                        }
                    }

                    if (editable) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { requestCapture() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(id = R.string.retake_location))
                            }
                            OutlinedButton(
                                onClick = onClear,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(id = R.string.delete_location))
                            }
                        }
                    }
                }

                editable -> {
                    Button(
                        onClick = { requestCapture() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(id = R.string.add_location))
                    }
                }

                else -> {
                    Text(
                        text = stringResource(id = R.string.no_location_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
