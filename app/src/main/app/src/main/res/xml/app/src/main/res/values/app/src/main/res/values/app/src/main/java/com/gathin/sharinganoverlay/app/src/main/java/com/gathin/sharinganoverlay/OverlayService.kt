package com.gathin.sharinganoverlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawPath
import androidx.compose.ui.layout.ColumnScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.px
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, android.app.Notification.Builder(this, "overlay_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sharingan Active")
            .setContentText("Eyes tracking focus...")
            .build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        return START_STICKY
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        composeView = ComposeView(this).apply { setViewCompositionStrategy(ComposeView.ViewCompositionStrategy.DisposeOnDetachedFromWindow) }
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        composeView!!.setContent { SharinganOverlayUI() }
        windowManager!!.addView(composeView!!, params!!)
    }

    override fun onDestroy() {
        composeView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

// ----------------- COMPOSABLE UI -----------------
@Composable
fun SharinganOverlayUI() {
    // State from Accessibility Service (Injected via Global Object for simplicity)
    val focusX by remember { mutableStateOf(0f) }
    val focusY by remember { mutableStateOf(0f) }
    val focusW by remember { mutableStateOf(0f) }
    val focusH by remember { mutableStateOf(0f) }
    val hasFocus by remember { mutableStateOf(false) }

    // Animations
    val blinkAnim by remember { mutableStateOf(1f) }
    val spinAnim by remember { mutableStateOf(0f) }
    val timeAnim by remember { mutableStateOf(0f) }
    val glowRadius by remember { mutableStateOf(0f) }

    // Boot Spin Animation
    LaunchedEffect(true) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 3000) { spinAnim = (System.currentTimeMillis() - startTime) / 3000f * 360f; kotlinx.coroutines.delay(16) }
        spinAnim = 0f
    }

    // Blink Loop
    LaunchedEffect(true) {
        while (true) {
            kotlinx.coroutines.delay((3000..8000).random().toLong())
            // Blink down
            var t = 0f
            while (t < 1f) { t += 0.1f; blinkAnim = 1f - t; kotlinx.coroutines.delay(16) }
            // Blink up
            t = 0f
            while (t < 1f) { t += 0.1f; blinkAnim = t; kotlinx.coroutines.delay(16) }
        }
    }

    // Clock Second Hand
    LaunchedEffect(true) {
        while (true) {
            timeAnim = (System.currentTimeMillis() % 60000) / 60000f * 360f
            kotlinx.coroutines.delay(100)
        }
    }

    // Glow Pulse
    LaunchedEffect(hasFocus) {
        if (hasFocus) {
            var t = 0f
            while (t < 1f) { t += 0.02f; glowRadius = 100f + 50f * sin(t * 2 * PI); kotlinx.coroutines.delay(16) }
        } else { glowRadius = 0f }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. SPOTLIGHT GLOW (Behind focused item)
        if (hasFocus && glowRadius > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = focusX + focusW / 2
                val centerY = focusY + focusH / 2
                drawCircle(
                    color = Color(0xFFFFD700, 0.15f), // Gold glow
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    radius = glowRadius,
                    style = androidx.compose.ui.graphics.Stroke(width = 200f) // Large soft radius
                )
            }
        }

        // 2. ITACHI EYES (Top Center-ish, tracking focus)
        // We draw 2 eyes. Left Eye tracks focus. Right Eye mirrors.
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp.toPx()
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp.toPx()
        val eyeY = 150.dp.toPx()
        val eyeSeparation = 200.dp.toPx()
        val leftEyeCenterX = screenWidth / 2 - eyeSeparation / 2
        val rightEyeCenterX = screenWidth / 2 + eyeSeparation / 2

        // Calculate Look Vector
        val targetX = if (hasFocus) focusX + focusW/2 else screenWidth/2f
        val targetY = if (hasFocus) focusY + focusH/2 else eyeY
        
        SharinganEye(centerX = leftEyeCenterX, centerY = eyeY, targetX = targetX, targetY = targetY, blink = blinkAnim, spin = spinAnim)
        SharinganEye(centerX = rightEyeCenterX, centerY = eyeY, targetX = targetX, targetY = targetY, blink = blinkAnim, spin = spinAnim)

        // 3. CLOCK (Bottom Right)
        ClockDial(modifier = Modifier.padding(32.dp).align(Alignment.BottomEnd))
    }
}

@Composable
fun SharinganEye(centerX: Float, centerY: Float, targetX: Float, targetY: Float, blink: Float, spin: Float) {
    val eyeRadius = 80.dp.toPx()
    val pupilRadius = 25.dp.toPx()
    val tomoeRadius = 55.dp.toPx()
    val tomoeCount = 3

    // Calculate Pupil Offset (Track Focus)
    val dx = targetX - centerX
    val dy = targetY - centerY
    val dist = sqrt(dx*dx + dy*dy)
    val maxOffset = eyeRadius - pupilRadius - 10
    val offsetX = if (dist > 0) (dx / dist) * min(dist, maxOffset) else 0f
    val offsetY = if (dist > 0) (dy / dist) * min(dist, maxOffset) else 0f

    Canvas(modifier = Modifier.size(eyeRadius * 2, eyeRadius * 2).graphicsLayer { 
        translationX = centerX - eyeRadius; translationY = centerY - eyeRadius; scaleY = blink 
    }) {
        // 1. White Sclera
        drawCircle(Color.White, radius = eyeRadius, center = androidx.compose.ui.geometry.Offset(eyeRadius, eyeRadius))
        
        // 2. Red Iris Base
        drawCircle(Color(0xFFB22222), radius = eyeRadius - 5, center = androidx.compose.ui.geometry.Offset(eyeRadius, eyeRadius))
        
        // 3. Tomoe (3 Commas) - Rotating on Boot
        val paint = androidx.compose.ui.graphics.Paint().apply { color = Color(0xFF8B0000); style = androidx.compose.ui.graphics.Paint.Style.Fill }
        for (i in 0 until tomoeCount) {
            val angle = (spin + i * 120f) * PI / 180
            val cx = eyeRadius + tomoeRadius * cos(angle)
            val cy = eyeRadius + tomoeRadius * sin(angle)
            val path = Path().apply {
                addCircle(cx, cy, 15.dp.toPx(), Path.Direction.CW) // Simplified Tomoe shape
                // Real Tomoe needs custom Path (comma shape). Using circle for code brevity.
            }
            drawPath(path, paint)
        }

        // 4. Black Pupil (Tracking)
        drawCircle(
            Color.Black, 
            radius = pupilRadius, 
            center = androidx.compose.ui.geometry.Offset(eyeRadius + offsetX, eyeRadius + offsetY)
        )
        
        // 5. Highlight (Shine)
        drawCircle(Color.White.copy(alpha = 0.6f), radius = 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(eyeRadius + offsetX - 15, eyeRadius + offsetY - 15))
    }
}

@Composable
fun ClockDial(modifier: Modifier = Modifier) {
    val time = java.util.Calendar.getInstance()
    val hour = time.get(java.util.Calendar.HOUR) % 12
    val minute = time.get(java.util.Calendar.MINUTE)
    val second = time.get(java.util.Calendar.SECOND)
    val millis = time.get(java.util.Calendar.MILLISECOND)
    
    val radius = 80.dp.toPx()
    Box(modifier = modifier.size(radius * 2)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(radius, radius)
            // Dial Background
            drawCircle(Color.Black.copy(alpha = 0.5f), radius = radius, center = center)
            drawCircle(Color.White.copy(alpha = 0.1f), radius = radius - 5, center = center, style = androidx.compose.ui.graphics.Stroke(width = 2f))

            // Ticks
            for (i in 0..59) {
                val angle = (i * 6 - 90) * PI / 180
                val isHour = i % 5 == 0
                val len = if (isHour) 20.dp.toPx() else 10.dp.toPx()
                val w = if (isHour) 3f else 1.5f
                val c = if (isHour) Color.White else Color.White.copy(alpha = 0.5f)
                val x1 = center.x + (radius - len - 5) * cos(angle)
                val y1 = center.y + (radius - len - 5) * sin(angle)
                val x2 = center.x + (radius - 5) * cos(angle)
                val y2 = center.y + (radius - 5) * sin(angle)
                drawLine(c, androidx.compose.ui.geometry.Offset(x1, y1), androidx.compose.ui.geometry.Offset(x2, y2), strokeWidth = w)
            }

            // Hands
            // Hour
            drawHand(center, radius * 0.5f, (hour * 30 + minute * 0.5f - 90) * PI / 180, 6f, Color.White)
            // Minute
            drawHand(center, radius * 0.75f, (minute * 6 + second * 0.1f - 90) * PI / 180, 4f, Color.White)
            // Second (Rolling Dial Effect - Red)
            val secAngle = (second * 6 + (millis / 1000f) * 6 - 90) * PI / 180
            drawHand(center, radius * 0.9f, secAngle, 2f, Color.Red)
            
            // Center Dot
            drawCircle(Color.Red, radius = 8.dp.toPx(), center = center)
        }
        // Digital Time Text
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

private fun DrawScope.drawHand(center: androidx.compose.ui.geometry.Offset, length: Float, angleRad: Float, width: Float, color: Color) {
    val x = center.x + length * cos(angleRad)
    val y = center.y + length * sin(angleRad)
    drawLine(color, center, androidx.compose.ui.geometry.Offset(x, y), strokeWidth = width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
}
