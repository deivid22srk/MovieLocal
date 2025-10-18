package com.movielocal.client.data.api

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class ServerDiscovery(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    suspend fun findServer(): String? = withContext(Dispatchers.IO) {
        try {
            val localIp = getLocalIpAddress() ?: return@withContext null
            val baseIp = localIp.substringBeforeLast(".")
            
            Log.d("ServerDiscovery", "Scanning network: $baseIp.x")
            
            val jobs = (1..254).map { lastOctet ->
                async {
                    val ip = "$baseIp.$lastOctet"
                    if (checkServer(ip)) {
                        ip
                    } else {
                        null
                    }
                }
            }
            
            val results = jobs.awaitAll()
            results.filterNotNull().firstOrNull()
        } catch (e: Exception) {
            Log.e("ServerDiscovery", "Error during server discovery", e)
            null
        }
    }

    private suspend fun checkServer(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "http://$ip:8080/api/health"
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            
            if (success) {
                Log.d("ServerDiscovery", "Server found at: $ip:8080")
            }
            
            success
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipInt = wifiInfo.ipAddress
            
            if (ipInt == 0) return null
            
            return String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            Log.e("ServerDiscovery", "Error getting local IP", e)
            return null
        }
    }
}
