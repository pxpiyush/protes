package com.example.core.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Centralized motion specifications for Protes, maintaining a calm, elegant, and
 * highly consistent premium feel across the entire app.
 */
object Motion {
    object Duration {
        const val VeryFast = 100
        const val Fast = 180
        const val Normal = 250
        const val Slow = 350
        const val Long = 500
    }

    object Easing {
        // Material Design 3 Emphasized / Calm Easing Curves
        val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        val Accelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
        val Sharp = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
    }

    // Springs for tactile physical movement (snapping, dragging, spring back)
    object SpringSpec {
        val Tactile = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
        val Soft = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
        val BouncyCard = spring<Float>(
            dampingRatio = 0.75f,
            stiffness = 300f
        )
    }

    /**
     * Determines if system animations should be reduced or disabled entirely.
     */
    @Composable
    fun rememberReduceMotion(): Boolean {
        val context = LocalContext.current
        return remember(context) {
            try {
                val scale = Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
                )
                scale == 0f
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Helper to get custom-tailored animation specification with reduce-motion awareness.
     */
    @Composable
    fun <T> tweenSpec(
        durationMillis: Int = Duration.Normal,
        delayMillis: Int = 0,
        easing: androidx.compose.animation.core.Easing = Easing.Standard
    ): FiniteAnimationSpec<T> {
        return if (rememberReduceMotion()) {
            snap(delayMillis = 0)
        } else {
            tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = easing
            )
        }
    }

    /**
     * Helper to trigger consistent and refined tactile haptic feedback.
     */
    fun performHaptic(context: Context, style: HapticStyle) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (style) {
                HapticStyle.Light -> {
                    // Quick light tick for tiny confirmations (Copy, Favorite, Pin)
                    VibrationEffect.createOneShot(12, 40)
                }
                HapticStyle.Medium -> {
                    // Medium pop (Delete, Drop on canvas, Long press)
                    VibrationEffect.createOneShot(28, 90)
                }
                HapticStyle.Heavy -> {
                    // Heavy thud (error, or complex long action)
                    VibrationEffect.createOneShot(45, 140)
                }
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val duration = when (style) {
                HapticStyle.Light -> 12L
                HapticStyle.Medium -> 28L
                HapticStyle.Heavy -> 45L
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    enum class HapticStyle {
        Light, Medium, Heavy
    }
}
