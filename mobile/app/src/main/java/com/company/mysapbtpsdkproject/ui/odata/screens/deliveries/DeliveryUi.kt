package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cloud.mobile.kotlin.odata.EntityValue

/* ----------------------------------------------------------------------------
 * Delivery design system — a lightweight, Grab-inspired green look that adapts
 * to light/dark mode and layers on top of the Fiori scaffold.
 * ------------------------------------------------------------------------- */

val DeliveryGreen = Color(0xFF00B14F)
val DeliveryGreenDark = Color(0xFF007A37)
val DeliverySyncOrange = Color(0xFFF59E0B)
val DeliverySyncRed = Color(0xFFEF4444)

/**
 * Offline sync state of a delivery, expressed as the avatar accent color:
 *  - orange = has unsynced local changes / pending upload (this SDK sets `isLocal` for both
 *             offline updates and offline creates; `isUpdated` stays false, so `isLocal` is
 *             the reliable "dirty" signal)
 *  - green  = downloaded / clean (already synced) — rows return to green after a sync
 *  - red    = the row is in an error state
 */
fun deliverySyncColor(entity: EntityValue): Color = when {
    entity.inErrorState -> DeliverySyncRed
    entity.isLocal -> DeliverySyncOrange
    else -> DeliveryGreen
}

/** Theme-adaptive neutral colors so cards/text read well in both light and dark. */
data class DeliveryPalette(
    val screenBg: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val trackInactive: Color,
)

@Composable
fun rememberDeliveryPalette(): DeliveryPalette =
    if (isSystemInDarkTheme()) {
        DeliveryPalette(
            screenBg = Color(0xFF101216),
            cardBg = Color(0xFF1B1E24),
            textPrimary = Color(0xFFF4F5F7),
            textSecondary = Color(0xFF9AA1AC),
            trackInactive = Color(0xFF343A42),
        )
    } else {
        DeliveryPalette(
            screenBg = Color(0xFFF3F5F7),
            cardBg = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xFF6B7280),
            trackInactive = Color(0xFFE5E7EB),
        )
    }

data class DeliveryStatusStyle(
    val label: String,
    val hint: String,
    val color: Color,
    /** Position in the happy-path tracker, or -1 for terminal/unknown states. */
    val stepIndex: Int,
)

/** Every status value the backend accepts (see srv/deliveryService.js validation). */
val deliveryStatusCodes = listOf(
    "CREATED",
    "ASSIGNED",
    "IN_PROGRESS",
    "ARRIVED",
    "DELIVERED",
    "FAILED",
    "CANCELLED",
)

private data class DeliveryStep(val code: String, val icon: ImageVector)

private val deliverySteps = listOf(
    DeliveryStep("CREATED", Icons.Filled.ShoppingCart),
    DeliveryStep("ASSIGNED", Icons.Filled.Person),
    DeliveryStep("IN_PROGRESS", Icons.Filled.LocationOn),
    DeliveryStep("ARRIVED", Icons.Filled.Home),
    DeliveryStep("DELIVERED", Icons.Filled.Done),
)

fun deliveryStatusStyle(status: String?): DeliveryStatusStyle = when (status?.uppercase()) {
    "CREATED" -> DeliveryStatusStyle("Order placed", "We've received the order.", Color(0xFF3B82F6), 0)
    "ASSIGNED" -> DeliveryStatusStyle("Driver assigned", "A driver is heading to pick up.", Color(0xFF6366F1), 1)
    "IN_PROGRESS" -> DeliveryStatusStyle("On the way", "Your delivery is on the move.", Color(0xFFF59E0B), 2)
    "ARRIVED" -> DeliveryStatusStyle("Arrived", "The driver has reached the address.", Color(0xFF14B8A6), 3)
    "DELIVERED" -> DeliveryStatusStyle("Delivered", "Delivered. Thank you!", DeliveryGreen, 4)
    "FAILED" -> DeliveryStatusStyle("Failed", "The delivery could not be completed.", Color(0xFFEF4444), -1)
    "CANCELLED" -> DeliveryStatusStyle("Cancelled", "This delivery was cancelled.", Color(0xFF9CA3AF), -1)
    else -> DeliveryStatusStyle(status ?: "Unknown", "", Color(0xFF9CA3AF), -1)
}

/**
 * Status picker for the edit screen: a read-only dropdown listing every allowed status,
 * each shown with the same color/label used by the tracker so the two always match.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDropdown(
    value: String,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = rememberDeliveryPalette()
    var expanded by remember { mutableStateOf(false) }
    val currentStyle = deliveryStatusStyle(value.ifEmpty { null })

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = if (value.isEmpty()) "" else currentStyle.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("status") },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (value.isEmpty()) palette.trackInactive else currentStyle.color)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeliveryGreen,
                focusedLabelColor = DeliveryGreen,
                cursorColor = DeliveryGreen,
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            deliveryStatusCodes.forEach { code ->
                val style = deliveryStatusStyle(code)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(style.color)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = style.label,
                                    color = palette.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(text = code, color = palette.textSecondary, fontSize = 11.sp)
                            }
                        }
                    },
                    onClick = {
                        onValueSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Small colored status chip used on the list cards. */
@Composable
fun StatusPill(status: String?) {
    val style = deliveryStatusStyle(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(style.color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = style.label,
            color = style.color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Horizontal order-progress tracker, inspired by the food-delivery reference UI. */
@Composable
fun DeliveryStatusTracker(status: String?, palette: DeliveryPalette) {
    val current = deliveryStatusStyle(status).stepIndex
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        deliverySteps.forEachIndexed { index, step ->
            val reached = current >= 0 && index <= current
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (reached) DeliveryGreen else palette.trackInactive),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = if (reached) Color.White else palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (index < deliverySteps.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (current > index) DeliveryGreen else palette.trackInactive)
                )
            }
        }
    }
}

/** Hero card: current status headline + hint + the tracker. */
@Composable
fun StatusTrackerCard(status: String?, palette: DeliveryPalette) {
    val style = deliveryStatusStyle(status)
    val headIcon =
        if (style.stepIndex >= 0) deliverySteps[style.stepIndex].icon else Icons.Filled.LocationOn
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.cardBg,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = style.label,
                        color = palette.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (style.hint.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(text = style.hint, color = palette.textSecondary, fontSize = 13.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DeliveryGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = headIcon,
                        contentDescription = null,
                        tint = DeliveryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            DeliveryStatusTracker(status, palette)
        }
    }
}

/** A delivery row in the list, rendered as a modern tappable card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeliveryListCard(
    name: String,
    note: String,
    status: String?,
    selected: Boolean,
    accentColor: Color,
    palette: DeliveryPalette,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = palette.cardBg,
        shape = RoundedCornerShape(18.dp),
        border = if (selected) BorderStroke(2.dp, DeliveryGreen) else null,
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selected) accentColor else accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Filled.Done, contentDescription = null, tint = Color.White)
                } else {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        color = accentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifEmpty { "Unnamed customer" },
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = note,
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                StatusPill(status)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }
}

/** Rounded grouped card used on the detail screen. */
@Composable
fun InfoCard(title: String, palette: DeliveryPalette, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.cardBg,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** A single labelled value with an optional leading icon. Hidden when value is blank. */
@Composable
fun InfoRow(
    label: String,
    value: String,
    palette: DeliveryPalette,
    icon: ImageVector? = null,
) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DeliveryGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = DeliveryGreen, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column {
            Text(text = label, color = palette.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(1.dp))
            Text(
                text = value,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
