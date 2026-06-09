package com.example.veegtrackerpro.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsHelper(context: Context) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logEvent(name: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(name, params)
    }

    fun logRoleSelected(role: String) {
        val bundle = Bundle().apply {
            putString("selected_role", role)
        }
        logEvent("role_selection", bundle)
    }

    fun logSignIn(method: String, success: Boolean) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
            putBoolean("success", success)
        }
        logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    fun logPoiMarked(type: String) {
        val bundle = Bundle().apply {
            putString("poi_type", type)
        }
        logEvent("poi_marked", bundle)
    }
    
    fun logRouteStarted(routeName: String) {
        val bundle = Bundle().apply {
            putString("route_name", routeName)
        }
        logEvent("route_started", bundle)
    }
}
