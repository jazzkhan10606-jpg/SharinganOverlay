package com.gathin.sharinganoverlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FocusTrackerService : AccessibilityService() {
    companion object {
        // Static holder for UI to read (Hack for no-DI simplicity)
        var latestFocusX: Float = 0f
        var latestFocusY: Float = 0f
        var latestFocusW: Float = 0f
        var latestFocusH: Float = 0f
        var hasFocus: Boolean = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            // We only care about Focus changes (DPad navigation)
            if (it.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED || 
                it.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ||
                it.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED) {
                
                val source = it.source ?: return
                val bounds = android.graphics.Rect()
                source.getBoundsInScreen(bounds)
                source.recycle()

                latestFocusX = bounds.left.toFloat()
                latestFocusY = bounds.top.toFloat()
                latestFocusW = bounds.width().toFloat()
                latestFocusH = bounds.height().toFloat()
                hasFocus = true
            }
        }
    }

    override fun onInterrupt() { hasFocus = false }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        serviceInfo.eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED or AccessibilityEvent.TYPE_VIEW_SELECTED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        serviceInfo.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo.notificationTimeout = 100
    }
}
