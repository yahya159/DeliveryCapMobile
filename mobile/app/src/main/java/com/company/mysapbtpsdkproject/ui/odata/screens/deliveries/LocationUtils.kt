package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Helpers for capturing the device location and handing coordinates off to external map apps.
 */
object LocationUtils {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Fetch a single fresh fix from the fused location provider. Returns null on failure.
     * Caller must ensure permission has been granted before invoking.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { cont.resume(null) }
            cont.invokeOnCancellation { cts.cancel() }
        }

    /** Open Google Maps at the given coordinates, falling back to any map-capable app. */
    fun openInGoogleMaps(context: Context, latitude: Double, longitude: Double) {
        val label = Uri.encode("Delivery location")
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapsIntent)
        } else {
            // No Google Maps installed -> let the system offer any map handler / browser.
            val fallback = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            )
            context.startActivity(fallback)
        }
    }

    /** Start turn-by-turn navigation in Waze, falling back to its web URL. */
    fun openInWaze(context: Context, latitude: Double, longitude: Double) {
        val wazeUri = Uri.parse("waze://?ll=$latitude,$longitude&navigate=yes")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://waze.com/ul?ll=$latitude,$longitude&navigate=yes")
                )
            )
        }
    }
}
