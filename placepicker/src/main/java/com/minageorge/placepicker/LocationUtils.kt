package com.minageorge.placepicker

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

object LocationUtils {

     const val GPS_REQUEST = 5034
    
    fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun turnGPSOn(context: Context,onGpsListener: OnGpsListener?) {
        LocationServices.getSettingsClient(context)
            .checkLocationSettings(LocationSettingsRequest.Builder().addLocationRequest( LocationRequest.create()).build())
            .addOnSuccessListener {   onGpsListener?.gpsStatus(true) }
            .addOnFailureListener {
                when ((it as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val rae = it as ResolvableApiException
                        rae.startResolutionForResult(context as Activity?, GPS_REQUEST)
                    } catch (sie: SendIntentException) {
                        Log.i(ContentValues.TAG, "PendingIntent unable to execute request.")
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                        Log.e(ContentValues.TAG, errorMessage)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }
    
    public interface OnGpsListener {
        fun gpsStatus(isGPSEnable: Boolean)
    }
}