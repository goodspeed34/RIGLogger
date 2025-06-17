/*
 *     RIGLogger - A Ham Radio Logging Solution for Android with Cloudlog
 *     Copyright (C) 2025 Gong Zhile
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.rad1o.riglogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import cn.rad1o.riglogger.rigctl.BaseRig
import cn.rad1o.riglogger.rigctl.OnRigStateChanged
import cn.rad1o.riglogger.rigctl.OperationMode
import cn.rad1o.riglogger.rigctl.XieGuRig
import cn.rad1o.riglogger.rigport.CableConnector
import cn.rad1o.riglogger.rigport.CableSerialPort
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

class RIGControlService : LifecycleService() {
    companion object {
        const val TAG = "RIGControlService"
        const val CHANID = "RIGControlService"
        const val DEFAULT_RETRIES = 3
    }

    private val binder = LocalBinder()
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification
    val snackbarMessage = MutableLiveData<String>()

    private var rig: BaseRig? = null
    private var retries = DEFAULT_RETRIES

    private var clConfigured = false
    private var clApiEndpoint = ""
    private var clApiKey = ""

    inner class LocalBinder : Binder() {
        fun getService(): RIGControlService = this@RIGControlService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun shutdown() {
        notificationManager.cancel(1)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun configureCloudlog(endpoint: String, apiKey: String) {
        clApiEndpoint = endpoint
        clApiKey = apiKey
        clConfigured = true
    }

    private fun updateNotification() {
        var flagText = ""

        if (!clConfigured) {
            flagText = "(NOLOG)"
        } else if (retries < DEFAULT_RETRIES) {
            flagText = "(RETRY)"
        }

        val text = String.format(Locale.US,
            "FREQ %,.1f kHz MODE %s %s",
            rig!!.getFreq() / 1000.0,
            OperationMode.toHumanReadable(rig!!.getMode()),
            flagText
        )

        notification = NotificationCompat.Builder(this, CHANID)
            .setContentTitle(getString(R.string.rig_connected))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_logger_black_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun updateCloudlogRadioInfo() {
        if (!clConfigured) return
        val data = mapOf(
            "key" to clApiKey,
            "radio" to "${rig!!.getName()} (RIGLogger)",
            "frequency" to rig!!.getFreq(),
            "mode" to OperationMode.toCloudlogMode(rig!!.getMode())
        )

        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = Gson().toJson(data).toString()

            val request = Request.Builder()
                .url(clApiEndpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(body.toRequestBody(mediaType))
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.code != 200) {
                    Log.e(TAG, "HTTP ${response.code}: ${response.body?.string()}")
                    if (--retries <= 0) {
                        Log.e(TAG, "Networking error, exceeded max number of retries")
                        snackbarMessage.postValue(
                            getString(R.string.rig_control_failed_cloudlog_returned, response.code)
                        )
                        shutdown()
                    }
                } else {
                    retries = DEFAULT_RETRIES
                    Log.d(TAG, "HTTP ${response.code}: ${response.body?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (--retries <= 0) {
                    Log.e(TAG, "Networking error, exceeded max number of retries")
                    snackbarMessage.postValue(
                        getString(R.string.rig_control_failed_due_to_network_problems)
                    )
                    shutdown()
                }
            }
        }
    }

    private fun startForegroundService() {
        notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val chan = NotificationChannel(CHANID, "RIG Control Service",
            NotificationManager.IMPORTANCE_MIN)

        chan.setSound(null, null)
        chan.enableVibration(false)
        chan.enableLights(false)

        getSystemService(NotificationManager::class.java).createNotificationChannel(chan)

        notification = NotificationCompat.Builder(this, CHANID)
            .setContentTitle("RIG Control Service Running")
            .setContentText("Starting...")
            .setSmallIcon(R.drawable.ic_logger_black_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        Log.e("RIGControlService", "Service is starting...")
        startForeground(1, notification)
    }

    private fun initRigConn() {
        val ports = CableSerialPort.listSerialPorts(applicationContext)
        if (ports.isEmpty()) {
            Log.e(TAG, "Failed to find a serial port to connect")
            snackbarMessage.postValue(getString(R.string.rig_control_failed_port_not_found))
            shutdown()
            return
        }

        rig = XieGuRig(40)
        if (rig != null) {
            val cableConnector = CableConnector(
                applicationContext,
                ports[0],
                19200,
                rig
            )

            cableConnector.connect()
            rig!!.setConnector(cableConnector)

            cableConnector.setOnRigStateChanged(object : OnRigStateChanged {
                override fun onDisconnected() {
                    snackbarMessage.postValue(
                        getString(R.string.disconnected, rig!!.getName())
                    )
                    shutdown()
                }

                override fun onConnected() {
                    snackbarMessage.postValue(
                        getString(R.string.successfully_connected_to, rig!!.getName())
                    )
                }

                override fun onRunError(message: String?) {
                    var actualMessage = ""
                    if (message == null)
                        actualMessage = "?"
                    else actualMessage = message
                    snackbarMessage.postValue(
                        getString(R.string.error_occurred_in, rig!!.getName(), actualMessage)
                    )
                    shutdown()
                }
            })

            if (rig?.isConnected() == true) {
                Log.i(TAG, "RIG connected")
            } else {
                Log.i(TAG, "RIG not connected")
                snackbarMessage.postValue(getString(R.string.failed_to_connect_to, rig!!.getName()))
                shutdown()
            }

            rig!!.mutFreq.observe(this) { freq ->
                Log.d(TAG, "Observed frequency change: $freq")
                updateNotification()
                updateCloudlogRadioInfo()
            }

            rig!!.mutMode.observe(this) { mode ->
                Log.d(TAG, "Observed mode change: ${OperationMode.toHumanReadable(mode)}")
                updateNotification()
                updateCloudlogRadioInfo()
            }
        }

        snackbarMessage.postValue(getString(R.string.successfully_connected_to, rig!!.getName()))
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        initRigConn()
    }
}