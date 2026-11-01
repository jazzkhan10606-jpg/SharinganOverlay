package com.gathin.sharinganoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ColumnScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PermissionScreen() }
        // Auto-start services if perms granted
        if (checkOverlayPermission() && checkAccessibilityEnabled()) {
            startServices()
            finish()
        }
    }

    @Composable
    fun PermissionScreen() {
        val ctx = LocalContext.current
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SHARINGAN OVERLAY", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
            Text("Mi TV AXSO0 / Google TV\nEnable 2 Permissions Below", fontSize = 24.sp, textAlign = androidx.compose.ui.text.TextAlign.Center)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(32.dp))

            Button(onClick = { requestOverlayPermission(ctx) }, modifier = Modifier.size(600.dp, 80.dp)) {
                Text(if (checkOverlayPermission(ctx)) "✅ Overlay Permission Granted" else "🔴 GRANT OVERLAY PERMISSION", fontSize = 20.sp, color = Color.White)
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
            Button(onClick = { openAccessibilitySettings(ctx) }, modifier = Modifier.size(600.dp, 80.dp)) {
                Text(if (checkAccessibilityEnabled(ctx)) "✅ Accessibility Enabled" else "🔴 ENABLE ACCESSIBILITY", fontSize = 20.sp, color = Color.White)
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
            Button(onClick = { 
                if (checkOverlayPermission(ctx) && checkAccessibilityEnabled(ctx)) {
                    startServices()
                    Toast.makeText(ctx, "Services Started! Press Home Button.", Toast.LENGTH_LONG).show()
                    finish()
                } else { Toast.makeText(ctx, "Grant both first!", Toast.LENGTH_SHORT).show() }
            }, modifier = Modifier.size(600.dp, 80.dp)) {
                Text("START & HIDE", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }

    private fun checkOverlayPermission(ctx: android.content.Context = this): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(ctx) else true
    }
    private fun requestOverlayPermission(ctx: android.content.Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }
    private fun checkAccessibilityEnabled(ctx: android.content.Context = this): Boolean {
        val enabledServices = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabledServices.contains("com.gathin.sharinganoverlay.FocusTrackerService")
    }
    private fun openAccessibilitySettings(ctx: android.content.Context) {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    private fun startServices() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }
}
