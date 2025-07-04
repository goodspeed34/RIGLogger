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

import androidx.lifecycle.MutableLiveData
import cn.rad1o.riglogger.rigport.BaseRigConnector
import cn.rad1o.riglogger.rigport.SerialParameter

interface OnConnectReceiveData {
    fun onData(data: ByteArray)
}

interface OnRigStateChanged {
    fun onDisconnected()
    fun onConnected()
    fun onRunError(message: String?)
}

abstract class BaseRig {
    companion object {
        fun byteToStr(data: ByteArray): String {
            val s = StringBuilder()
            for (i in data.indices) {
                s.append(String.format("%02x ", data[i].toInt() and 0xff))
            }
            return s.toString()
        }
    }

    private var freq: Long = 439950000 /* What is this? ;-) */
    var mutFreq: MutableLiveData<Long> = MutableLiveData()

    private var mode: OperationMode = OperationMode.FM /* What is this? ;-) */
    var mutMode: MutableLiveData<OperationMode> = MutableLiveData()

    private var connector: BaseRigConnector? = null
    private var onRigStateChanged: OnRigStateChanged? = null

    abstract fun getName(): String
    abstract fun isConnected(): Boolean

    abstract fun readStatus()
    abstract fun writeFrequency()

    open val defaultSerialParameter: SerialParameter? = null

    abstract fun onRecv(data: ByteArray)

    fun getFreq(): Long { return freq }
    fun setFreq(freq: Long) {
        if (this.freq == freq) return
        if (freq.toInt() == 0) return
        if (freq.toInt() == -1) return

        mutFreq.postValue(freq)
        this.freq = freq
    }

    fun getMode(): OperationMode { return mode }
    fun setMode(mode: OperationMode) {
        if (this.mode == mode) return

        mutMode.postValue(mode)
        this.mode = mode
    }

    fun getOnRigStateChanged(): OnRigStateChanged? { return onRigStateChanged }
    fun setOnRigStateChanged(onRigStateChanged: OnRigStateChanged?)
    { this.onRigStateChanged = onRigStateChanged }

    fun getConnector(): BaseRigConnector? { return connector }
    fun setConnector(connector: BaseRigConnector) {
        this.connector = connector

        this.connector!!.setOnRigStateChanged(onRigStateChanged)
        this.connector!!.setOnConnectReceiveData(object : OnConnectReceiveData {
            override fun onData(data: ByteArray) {
                onRecv(data)
            }
        })
    }

    fun onDisconnecting() { }
}