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

import android.R
import android.util.Log
import java.util.Timer
import java.util.TimerTask


class XieGuRig(civAddress: Int) : BaseRig() {
    private val ctrAddress = 0xE0
    private var dataBuffer = ByteArray(0)
    private var readFreqTimer: Timer? = Timer()

    private fun readTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {
                try {
                    if (!isConnected()) {
                        readFreqTimer!!.cancel()
                        readFreqTimer!!.purge()
                        readFreqTimer = null
                        return
                    }

                    readFreqFromRig()
                } catch (e: Exception) {
                    Log.e(TAG, "readFreq or meter error:" + e.message)
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

    override fun readFrequency() {
        if (getConnector() != null) {
            getConnector()?.sendData(IcomRigConstant.setReadFreq(ctrAddress, getCivAddress()));
        }
    }

    override fun writeFrequency() {
        if (getConnector() != null) {
            getConnector()?.sendData(
                IcomRigConstant.setOperationFrequency(
                    ctrAddress, getCivAddress(), getFreq()
                )
            );
        }
    }

    private fun getCommandEnd(data: ByteArray): Int {
        for (i in data.indices) {
            if (data[i] == 0xFD.toByte()) {
                return i
            }
        }
        return -1
    }

    private fun getCommandHead(data: ByteArray): Int {
        if (data.size < 2) return -1
        for (i in 0..<data.size - 1) {
            if (data[i] == 0xFE.toByte() && data[i + 1] == 0xFE.toByte()) {
                return i
            }
        }
        return -1
    }

    private fun analysisCommand(data: ByteArray) {
        val headIndex = getCommandHead(data)
        if (headIndex == -1) {
            return
        }
        val icomCommand: IcomCommand?
        if (headIndex == 0) {
            icomCommand = IcomCommand.getCommand(ctrAddress, getCivAddress(), data)
        } else {
            val temp = ByteArray(data.size - headIndex)
            System.arraycopy(data, headIndex, temp, 0, temp.size)
            icomCommand = IcomCommand.getCommand(ctrAddress, getCivAddress(), temp)
        }
        if (icomCommand == null) {
            return
        }

        when (icomCommand.getCommandID()) {
            IcomRigConstant.CMD_SEND_FREQUENCY_DATA, IcomRigConstant.CMD_READ_OPERATING_FREQUENCY -> {
                val freqTemp: Long = icomCommand.getFrequency(false)
                if (freqTemp >= 500000 && freqTemp <= 250000000) {
                    setFreq(freqTemp)
                }
            }

            IcomRigConstant.CMD_SEND_MODE_DATA, IcomRigConstant.CMD_READ_OPERATING_MODE -> {}
        }
    }

    override fun onRecv(data: ByteArray) {
        val commandEnd = getCommandEnd(data)
        if (commandEnd <= -1) {
            val temp = ByteArray(dataBuffer.size + data.size)
            System.arraycopy(dataBuffer, 0, temp, 0, dataBuffer.size)
            System.arraycopy(data, 0, temp, dataBuffer.size, data.size)
            dataBuffer = temp
        } else {
            val temp = ByteArray(dataBuffer.size + commandEnd + 1)
            System.arraycopy(dataBuffer, 0, temp, 0, dataBuffer.size)
            dataBuffer = temp
            System.arraycopy(data, 0, dataBuffer, dataBuffer.size - commandEnd - 1, commandEnd + 1)
        }
        if (commandEnd != -1) {
            analysisCommand(dataBuffer)
        }
        dataBuffer = ByteArray(0)
        if (commandEnd <= -1 || commandEnd < data.size) {
            val temp = ByteArray(data.size - commandEnd + 1)
            for (i in 0..<data.size - commandEnd - 1) {
                temp[i] = data[commandEnd + i + 1]
            }
            dataBuffer = temp
        }
    }

    fun readFreqFromRig() {
        if (getConnector() != null) {
            getConnector()!!.sendData(IcomRigConstant.setReadFreq(ctrAddress, getCivAddress()))
        }
    }

    override fun getName(): String {
        return "XIEGU 6100 series"
    }

    init {
        Log.d(TAG, "XieGuRig 6100: Create.")
        setCivAddress(civAddress)

        readFreqTimer?.schedule(readTask(), 1000, 1000)
    }

    companion object {
        private const val TAG = "XieGu6100Rig"
    }
}