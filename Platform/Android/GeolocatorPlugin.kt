package com.dotnative.plugins

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

class GeolocatorPlugin(private val activity: Activity) {

    private val locationManager = activity.getSystemService(LocationManager::class.java)
    private var pendingReply: PluginReply? = null
    private var listener: LocationListener? = null

    init {

        val channel = NativeChannels.channel("dotnative.geolocator")
        channel.onReset = {
            stop()
        }
        channel.handle("getCurrentPosition") { args, reply ->
            request(args, reply)
        }
    }

    private fun request(args: Any?, reply: PluginReply) {

        if (pendingReply != null) {

            reply.failure("busy", "A position request is already pending")
            return
        }
        val highAccuracy = (args as? Map<*, *>)?.get("highAccuracy") as? Boolean ?: false
        val fine =
            activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse =
            activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {

            reply.failure(
                "permission_denied",
                "Request location permission before reading a position",
            )
            return
        }
        if (highAccuracy && !fine) {

            reply.failure(
                "permission_denied",
                "Fine location permission is required for high accuracy",
            )
            return
        }
        try {

            val providers = locationManager.getProviders(true)
            val provider =
                if (highAccuracy && providers.contains(LocationManager.GPS_PROVIDER))
                    LocationManager.GPS_PROVIDER
                else if (providers.contains(LocationManager.NETWORK_PROVIDER))
                    LocationManager.NETWORK_PROVIDER
                else providers.firstOrNull() ?: error("No location provider is enabled")
            val last = locationManager.getLastKnownLocation(provider)
            if (last != null && System.currentTimeMillis() - last.time < 30_000) {

                reply.success(value(last))
                return
            }
            val current =
                object : LocationListener {

                    override fun onLocationChanged(location: Location) {

                        stop()
                        reply.success(value(location))
                    }

                    @Deprecated("Deprecated by Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) =
                        Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) {

                        if (pendingReply === reply) {

                            stop()
                            reply.failure("provider_disabled", "Location provider was disabled")
                        }
                    }
                }
            listener = current
            pendingReply = reply
            reply.onCancel = {
                stop()
            }
            locationManager.requestLocationUpdates(
                provider,
                0L,
                0f,
                current,
                Looper.getMainLooper(),
            )
        } catch (error: SecurityException) {

            reply.failure("permission_denied", error.message ?: "Location permission was denied")
        } catch (error: Exception) {

            stop()
            reply.failure("location_unavailable", error.message ?: "Could not read a location")
        }
    }

    private fun value(location: Location): Map<String, Any?> =
        mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy.toDouble(),
            "altitude" to location.altitude,
            "timestamp" to location.time,
        )

    private fun stop() {

        listener?.let {
            runCatching {
                locationManager.removeUpdates(it)
            }
        }
        listener = null
        pendingReply = null
    }
}
