package com.foodiedev.useractivitydetection

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.foodiedev.useractivitydetection.receiver.ActivityTransitionReceiver
import com.foodiedev.useractivitydetection.util.ActivityTransitionsUtil
import com.foodiedev.useractivitydetection.util.Constants
import com.foodiedev.useractivitydetection.util.Constants.ACTIVITY_TRANSITION_STORAGE
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    lateinit var client: ActivityRecognitionClient
    lateinit var storage: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        client = ActivityRecognition.getClient(this)
        storage = PreferenceManager.getDefaultSharedPreferences(this)

        switchActivityTransition.isChecked = getRadioState()

        switchActivityTransition.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && !ActivityTransitionsUtil.hasActivityTransitionPermissions(this)
                ) {
                    switchActivityTransition.isChecked = false
                    requestActivityTransitionPermission()
                } else {
                    requestForUpdates()
                }
            } else {
                saveRadioState(false)
                deregisterForUpdates()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestActivityTransitionPermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        switchActivityTransition.isChecked = true
        saveRadioState(true)
        requestForUpdates()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun requestForUpdates() {
        client
            .requestActivityTransitionUpdates(
                ActivityTransitionsUtil.getActivityTransitionRequest(),
                getPendingIntent()
            )
            .addOnSuccessListener {
                showToast("successful registration")
            }
            .addOnFailureListener { e: Exception ->
                showToast("Unsuccessful registration")
            }
    }

    private fun deregisterForUpdates() {
        client
            .removeActivityTransitionUpdates(getPendingIntent())
            .addOnSuccessListener {
                getPendingIntent().cancel()
                showToast("successful deregistration")
            }
            .addOnFailureListener { e: Exception ->
                showToast("unsuccessful deregistration")
            }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            Constants.REQUEST_CODE_INTENT_ACTIVITY_TRANSITION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestActivityTransitionPermission() {
        EasyPermissions.requestPermissions(
            this,
            "You need to allow activity transition permissions in order to use this feature",
            Constants.REQUEST_CODE_ACTIVITY_TRANSITION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG)
            .show()
    }

    private fun saveRadioState(value: Boolean) {
        storage
            .edit()
            .putBoolean(ACTIVITY_TRANSITION_STORAGE, value)
            .apply()
    }

    private fun getRadioState() = storage.getBoolean(ACTIVITY_TRANSITION_STORAGE, false)
}