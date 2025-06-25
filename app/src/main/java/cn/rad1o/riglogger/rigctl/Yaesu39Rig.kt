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

package cn.rad1o.riglogger.rigctl

import android.util.Log
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class Yaesu39Rig() : BaseRig() {
    private var readStatusTimer: Timer? = Timer()

    private val parser = Yaesu3CommandParser { command ->
        when {
            command.commandID == "IF" -> {
                val freqTemp: Long = command.getFrequency()
                if (freqTemp >= 500000 && freqTemp <= 250000000) {
                    setFreq(freqTemp)
                }

                val mode = command.getMode()
                if (mode != null) {
                    setMode(mode)
                }
            }
        }
    }

    private fun readTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {
                try {
                    if (!isConnected()) {
                        readStatusTimer!!.cancel()
                        readStatusTimer!!.purge()
                        readStatusTimer = null
                        return
                    }

                    readStatus()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update RIG information:" + e.message)
                }
            }
        }
    }

    override fun isConnected(): Boolean {
        if (getConnector() == null) {
            return false
        }
        return getConnector()!!.isConnected()
    }

    override fun readStatus() {
        if (getConnector() != null) {
            getConnector()?.sendData("IF;".toByteArray())
        }
    }

    override fun writeFrequency() {
        if (getConnector() != null) {
            getConnector()?.sendData(
                ("FA" + String.format(Locale.US, "%09d", getFreq()) + ";").toByteArray()
            );
        }
    }

    override fun onRecv(data: ByteArray) { parser.feed(data) }

    override fun getName(): String { return "Yaesu FT-891" }

    init {
        Log.d(TAG, "Yaesu FT-891: Create.")
        readStatusTimer?.schedule(readTask(), 1000, 1000)
    }

    companion object { private const val TAG = "Yaesu39Rig" }
}