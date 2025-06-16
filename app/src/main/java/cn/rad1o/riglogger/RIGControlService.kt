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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import cn.rad1o.riglogger.rigport.CableSerialPort
import cn.rad1o.riglogger.rigport.OnConnectorStateChanged
import kotlinx.coroutines.*

class RIGControlService : Service() {
    companion object {
        const val TAG = "CableSerialPort"
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var snackMessage = MutableLiveData<String>()

    inner class LocalBinder : Binder() {
        fun getService(): RIGControlService = this@RIGControlService
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun startForegroundService() {
        val chanId = "RIGControlService"
        val chan = NotificationChannel(chanId, "RIG Control Service",
            NotificationManager.IMPORTANCE_HIGH)

        getSystemService(NotificationManager::class.java).createNotificationChannel(chan)

        val notification = NotificationCompat.Builder(this, chanId)
            .setContentTitle("RIG Control Service Running")
            .setContentText("Current frequency at 1 MHz")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        Log.e("RIGControlService", "Service is starting...")
        startForeground(1, notification)
    }

    private fun initRigConn() {
        val ports = CableSerialPort.listSerialPorts(applicationContext)
        if (!(ports.size >= 0)) {
            Log.e(TAG, "Failed to find a serial port to connect")
            stopSelf()
        }

        val serialPort = ports[0]
        val cableSerialPort = CableSerialPort(applicationContext, serialPort, 19200, 8, 1, 0,
            object : OnConnectorStateChanged {
                override fun onDisconnected() {
                    Log.e(TAG, "Serial port disconnected, bye")
                    stopSelf()
                }

                override fun onConnected() {
                    Log.i(TAG, "Serial port connected")
                }

                override fun onRunError(message: String?) {
                    Log.e(TAG, "Failed to connect the serial port: $message")
                }
            })

        if (!cableSerialPort.connect()) {
            Log.e(TAG, "Failed to connect to the serial port: timed out")
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        initRigConn()
    }
}