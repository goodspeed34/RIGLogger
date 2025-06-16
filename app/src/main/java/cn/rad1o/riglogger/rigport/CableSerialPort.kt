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

/* Portions of this file are adapted from work originally authored by BG7YOZ.
 * The original source is licensed under the MIT License:
 *
 * Copyright (c) 2023 BG7YOZ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cn.rad1o.riglogger.rigport

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.core.content.ContextCompat
import cn.rad1o.riglogger.BuildConfig
import cn.rad1o.riglogger.R
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException


class CableSerialPort  {
    companion object {
        const val TAG = "CableSerialPort"
        const val SEND_TIMEOUT = 2000
        const val INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID

        fun listSerialPorts(context: Context): ArrayList<SerialPort> {
            val serialPorts = ArrayList<SerialPort>()
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            for (device in usbManager.deviceList.values) {
                val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
                    ?: continue
                for (i in driver.ports.indices) {
                    serialPorts.add(
                        SerialPort(
                            device.deviceId, device.vendorId,
                            device.productId, i
                        )
                    )
                }
            }
            return serialPorts
        }
    }

    private var usbPermission = UsbPermission.Unknown
    enum class UsbPermission {
        Unknown, Requested, Granted, Denied
    }
    private lateinit var onStateChanged: OnConnectorStateChanged
    private lateinit var broadcastReceiver: BroadcastReceiver
    private var context: Context

    private var vendorId = 0x0c26
    private var baudRate = 19200
    private var dataBits = 8
    private var stopBits = 1
    private var parity = 0
    private var portNum = 0

    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null
    var ioListener: SerialInputOutputManager.Listener? = null

    private var usbManager: UsbManager? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var driver: UsbSerialDriver? = null

    private var connected = false

    constructor(
        mContext: Context, serialPort: SerialPort?,
        baudRate: Int, dataBits: Int, stopBits: Int, parity: Int,
        connectorStateChanged: OnConnectorStateChanged
    ) {
        vendorId = serialPort!!.vendorId
        portNum = serialPort!!.portNum

        this@CableSerialPort.baudRate = baudRate
        this@CableSerialPort.dataBits = dataBits
        this@CableSerialPort.stopBits = stopBits
        this@CableSerialPort.parity = parity

        context = mContext
        this.onStateChanged = connectorStateChanged

        doBroadcast()
    }

    constructor(mContext: Context) {
        context = mContext
        doBroadcast()
    }

    private fun doBroadcast() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.action)) {
                    usbPermission =
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                            UsbPermission.Granted
                        else
                            UsbPermission.Denied
                    connect()
                }
            }
        }
    }

    private fun prepare(): Boolean {
        registerRigSerialPort(context)
        var device: UsbDevice? = null
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        usbConnection = null
        if (usbManager == null) {
            return false
        }

        for (v in usbManager!!.deviceList.values) {
            if (v.vendorId == vendorId) {
                device = v
            }
        }

        if (device == null) {
            Log.e(TAG, String.format("Failed to open the serial port: E_NODEV %04x", vendorId))
            return false
        }

        driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) { driver = CdcAcmSerialDriver(device) }
        if (driver!!.ports.size < portNum) {
            Log.e(TAG, "Serial port NUM doesn't exist, unable to open")
            return false
        }

        Log.d(TAG, "connect: port size:" + driver!!.ports.size.toString())
        usbSerialPort = driver!!.ports[portNum]
        usbConnection = usbManager!!.openDevice(driver!!.device)

        return true
    }

    //@RequiresApi(api = Build.VERSION_CODES.S)
    fun connect(): Boolean {
        connected = false
        if (!prepare()) {}

        if (driver == null) {
            onStateChanged.onRunError(context.getString(R.string.serial_no_driver))
            return false
        }

        if (usbConnection == null && usbPermission == UsbPermission.Unknown
            && !usbManager!!.hasPermission(
                driver!!.getDevice()
            )
        ) {
            usbPermission = UsbPermission.Requested

            var usbPermissionIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(INTENT_ACTION_GRANT_USB), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager?.requestPermission(driver!!.device, usbPermissionIntent)
            prepare()
        }

        if (usbConnection == null) {
            onStateChanged.onRunError(context.getString(R.string.serial_connect_no_access))
            return false
        }

        try {
            usbSerialPort?.open(usbConnection)

            Log.d(
                TAG, java.lang.String.format(
                    "serial:baud rate：%d,data bits:%d,stop bits:%d,parity bit:%d",
                    baudRate, dataBits, stopBits, parity
                )
            )

            usbSerialPort?.setParameters(baudRate, dataBits, stopBits, parity)
            usbIoManager =
                SerialInputOutputManager(usbSerialPort, object : SerialInputOutputManager.Listener {
                    override fun onNewData(data: ByteArray) {
                        ioListener?.onNewData(data)
                    }

                    override fun onRunError(e: Exception) {
                        ioListener?.onRunError(e)
                        disconnect()
                    }
                })

            usbIoManager!!.start()
            Log.d(TAG, "Successfully opened the serial port")
            connected = true

            onStateChanged.onConnected()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open the serial port: " + e.message)
            onStateChanged.onRunError(
                context.getString(R.string.serial_connect_failed) + e.message
            )
            disconnect()
            return false
        }
        return true
    }

    fun sendData(src: ByteArray?): Boolean {
        if (usbSerialPort != null) {
            try {
                usbSerialPort!!.write(src, SEND_TIMEOUT)
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to write to the serial port: " + e.message)
                return false
            }
            return true
        } else {
            Log.e(TAG, "Failed to write to the serial port: the port is yet to be opened")
            return false
        }
    }

    fun disconnect() {
        connected = false
        onStateChanged.onDisconnected()

        usbIoManager!!.listener = null
        usbIoManager!!.stop()

        usbIoManager = null
        try {
            usbSerialPort!!.close()
        } catch (ignored: IOException) { }
        usbSerialPort = null
    }

    fun registerRigSerialPort(context: Context) {
        Log.d(TAG, "registerRigSerialPort: registered!")
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            IntentFilter(INTENT_ACTION_GRANT_USB),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregisterRigSerialPort(activity: Activity) {
        Log.d(TAG, "unregisterRigSerialPort: unregistered!")
        activity.unregisterReceiver(broadcastReceiver)
    }

    fun setRTS_On(rts_on: Boolean) {
        try {
            val controlLines = usbSerialPort!!.supportedControlLines
            if (controlLines.contains(ControlLine.RTS)) {
                usbSerialPort!!.rts = rts_on
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setDTR_On(dtr_on: Boolean) {
        try {
            val controlLines = usbSerialPort!!.supportedControlLines
            if (controlLines.contains(ControlLine.DTR)) {
                usbSerialPort!!.dtr = dtr_on
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "setDTR_On: " + e.message)
        }
    }

    fun getOnStateChanged(): OnConnectorStateChanged {
        return onStateChanged
    }

    fun setOnStateChanged(onStateChanged: OnConnectorStateChanged?) {
        this.onStateChanged = onStateChanged!!
    }

    fun getVendorId(): Int {
        return vendorId
    }

    fun setVendorId(deviceId: Int) {
        this.vendorId = deviceId
    }

    fun getPortNum(): Int {
        return portNum
    }

    fun setPortNum(portNum: Int) {
        this.portNum = portNum
    }

    fun getBaudRate(): Int {
        return baudRate
    }

    fun setBaudRate(baudRate: Int) {
        this.baudRate = baudRate
    }

    fun isConnected(): Boolean {
        return connected
    }

    class SerialPort(deviceId: Int, vendorId: Int, productId: Int, portNum: Int) {
        var deviceId: Int = 0
        var vendorId: Int = 0x0c26
        var productId: Int = 0
        var portNum: Int = 0

        init {
            this.deviceId = deviceId
            this.vendorId = vendorId
            this.productId = productId
            this.portNum = portNum
        }

        @SuppressLint("DefaultLocale")
        override fun toString(): String {
            return String.format(
                "SerialPort:deviceId=0x%04X, vendorId=0x%04X, portNum=%d",
                deviceId, vendorId, portNum
            )
        }

        @SuppressLint("DefaultLocale")
        fun information(): String {
            return String.format(
                "\\0x%04X\\0x%04X\\0x%04X\\0x%d",
                deviceId,  vendorId,  productId, portNum
            )
        }
    }
}