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

import cn.rad1o.riglogger.rigctl.OnConnectReceiveData
import cn.rad1o.riglogger.rigctl.OnRigStateChanged


open class BaseRigConnector {
    private var connected: Boolean = false
    private var onConnectReceiveData: OnConnectReceiveData? = null
    private var onRigStateChanged: OnRigStateChanged? = null

    private var onConnectorStateChanged: OnConnectorStateChanged =
        object : OnConnectorStateChanged {
            override fun onDisconnected() {
                if (onRigStateChanged != null) {
                    onRigStateChanged!!.onDisconnected()
                }
                connected = false
            }

            override fun onConnected() {
                if (onRigStateChanged != null) {
                    onRigStateChanged!!.onConnected()
                }
                connected = true
            }

            override fun onRunError(message: String?) {
                if (onRigStateChanged != null) {
                    onRigStateChanged!!.onRunError(message)
                }
                connected = false
            }
        }

    @Synchronized
    open fun sendData(data: ByteArray?) { }

    fun getOnConnectReceiveData(): OnConnectReceiveData { return onConnectReceiveData!! }
    fun setOnConnectReceiveData(receiveData: OnConnectReceiveData) {
        onConnectReceiveData = receiveData
    }

    open fun connect() {}
    open fun disconnect() {}

    fun getOnRigStateChanged(): OnRigStateChanged { return onRigStateChanged!! }
    fun setOnRigStateChanged(onRigStateChanged: OnRigStateChanged?) {
        this.onRigStateChanged = onRigStateChanged
    }

    fun getOnConnectorStateChanged(): OnConnectorStateChanged { return onConnectorStateChanged }
    fun setOnConnectorStateChanged(onConnectorStateChanged: OnConnectorStateChanged) {
        this.onConnectorStateChanged = onConnectorStateChanged
    }

    fun isConnected(): Boolean { return connected }

    fun readShortBigEndianData(data: ByteArray, start: Int): Short {
        if (data.size - start < 2) return 0
        return (data[start].toShort().toInt() and 0xff
                or ((data[start + 1].toShort().toInt() and 0xff) shl 8)).toShort()
    }
}