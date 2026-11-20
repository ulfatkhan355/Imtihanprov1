package com.imtihanpro.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Imtihan Pro v2.0 8dp-based Design Tokens
 */
object ImtihanSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object ImtihanRadii {
    val chip = 10.dp
    val button = 12.dp
    val card = 16.dp
    val panel = 20.dp
    val sheet = 24.dp
}

object ImtihanMotion {
    const val fastMs = 150
    const val standardMs = 220
    const val emphasisMs = 300
}

object ImtihanDimensions {
    val minTouchTarget = 48.dp
    val iconSmall = 18.dp
    val iconMedium = 22.dp
    val iconLarge = 28.dp
    val hairlineBorder = 1.dp
}

val ImtihanShapes = Shapes(
    extraSmall = RoundedCornerShape(ImtihanRadii.chip),
    small = RoundedCornerShape(ImtihanRadii.button),
    medium = RoundedCornerShape(ImtihanRadii.card),
    large = RoundedCornerShape(ImtihanRadii.panel),
    extraLarge = RoundedCornerShape(ImtihanRadii.sheet)
)
