package com.movielocal.server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.movielocal.server.server.MovieServerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("server_settings", Context.MODE_PRIVATE)
            val autoStartEnabled = prefs.getBoolean("auto_start_on_boot", false)
            
            if (autoStartEnabled) {
                val serviceIntent = Intent(context, MovieServerService::class.java)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
