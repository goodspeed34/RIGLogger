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

package cn.rad1o.riglogger.rigctl

import android.util.Log
import java.util.Timer
import java.util.TimerTask


class XieGuRig(private val civAddress: Int) : BaseRig() {
    private val ctrAddress = 0xE0
    private var readStatusTimer: Timer? = Timer()

    private val parser = IcomCommandParser(ctrAddress, civAddress) { command ->
        when (command.commandID) {
            IcomRigConstant.CMD_SEND_FREQUENCY_DATA, IcomRigConstant.CMD_READ_OPERATING_FREQUENCY -> {
                val freqTemp: Long = command.getFrequency(false)
                if (freqTemp >= 500000 && freqTemp <= 250000000) {
                    setFreq(freqTemp)
                }
            }

            IcomRigConstant.CMD_SEND_MODE_DATA, IcomRigConstant.CMD_READ_OPERATING_MODE -> {
                val retData = command.getData(false)
                val mode = OperationMode.fromIcomDef(retData[0].toInt())

                if (mode == null) {
                    Log.w(TAG, "Invalid mode ${retData[0]} received from RIG, ignored")
                } else setMode(mode)
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
            getConnector()?.sendData(IcomRigConstant.readOperationFrequency(
                ctrAddress, civAddress
            ))
            getConnector()?.sendData(IcomRigConstant.readOperationMode(
                ctrAddress, civAddress
            ))
        }
    }

    override fun writeFrequency() {
        if (getConnector() != null) {
            getConnector()?.sendData(
                IcomRigConstant.sendOperationFrequency(
                    ctrAddress, civAddress, getFreq()
                )
            );
        }
    }

    override fun onRecv(data: ByteArray) { parser.feed(data) }

    override fun getName(): String { return "XIEGU 6100 series" }

    init {
        Log.d(TAG, "XieGuRig 6100: Create.")
        readStatusTimer?.schedule(readTask(), 1000, 1000)
    }

    companion object { private const val TAG = "XieGu6100Rig" }
}