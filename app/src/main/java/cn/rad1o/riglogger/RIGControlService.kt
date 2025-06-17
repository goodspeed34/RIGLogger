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
import cn.rad1o.riglogger.rigctl.BaseRig
import cn.rad1o.riglogger.rigctl.OperationMode
import cn.rad1o.riglogger.rigctl.XieGuRig
import cn.rad1o.riglogger.rigport.CableConnector
import cn.rad1o.riglogger.rigport.CableSerialPort
import java.util.Locale

class RIGControlService : LifecycleService() {
    companion object {
        const val TAG = "RIGControlService"
        const val CHANID = "RIGControlService"
    }

    private val binder = LocalBinder()
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification

    private var rig: BaseRig? = null
    inner class LocalBinder : Binder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun shutdown() {
        notificationManager.cancel(1)
        stopForeground(true)
        stopSelf()
    }

    private fun updateNotification() {
        val text = String.format(Locale.US,
            "FREQ %,.1f kHz MODE %s",
            rig!!.getFreq() / 1000.0,
            OperationMode.toHumanReadable(rig!!.getMode())
        )

        notification = NotificationCompat.Builder(this, CHANID)
            .setContentTitle(getString(R.string.rig_connected))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_logger_black_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(1, notification)
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

            if (rig?.isConnected() == true) {
                Log.i(TAG, "RIG connected")
            } else {
                Log.i(TAG, "RIG not connected")
                shutdown()
            }

            rig!!.mutFreq.observe(this) { freq ->
                Log.d(TAG, "Observed frequency change: $freq")
                updateNotification()
            }

            rig!!.mutMode.observe(this) { mode ->
                Log.d(TAG, "Observed mode change: ${OperationMode.toHumanReadable(mode)}")
                updateNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        initRigConn()
    }
}