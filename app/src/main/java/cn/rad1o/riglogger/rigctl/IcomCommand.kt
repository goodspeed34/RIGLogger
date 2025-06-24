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

object IcomRigConstant {
    private const val TAG = "IcomRigConstant"

    /* LSB:0,USB:1,AM:2,CW:3,RTTY:4,FM:5,WFM:6,CW_R:7,RTTY_R:8,DV:17 */
    const val LSB: Int = 0
    const val USB: Int = 1
    const val AM: Int = 2
    const val CW: Int = 3
    const val RTTY: Int = 4
    const val FM: Int = 5
    const val WFM: Int = 6
    const val CW_R: Int = 7
    const val RTTY_R: Int = 8
    const val DV: Int = 0x17

    val SEND_FREQUENCY_DATA: ByteArray = byteArrayOf(0x00)
    const val CMD_SEND_FREQUENCY_DATA: Byte = 0x00

    val SEND_MODE_DATA: ByteArray = byteArrayOf(0x01)
    const val CMD_SEND_MODE_DATA: Byte = 0x01

    val READ_OPERATING_FREQUENCY: ByteArray = byteArrayOf(0x03)
    const val CMD_READ_OPERATING_FREQUENCY: Byte = 0x03

    val READ_OPERATING_MODE: ByteArray = byteArrayOf(0x04)
    const val CMD_READ_OPERATING_MODE: Byte = 0x04

    val SET_OPERATING_FREQUENCY: ByteArray = byteArrayOf(0x05)
    const val CMD_SET_OPERATING_FREQUENCY: Byte = 0x05

    val SET_OPERATING_MODE: ByteArray = byteArrayOf(0x06)
    const val CMD_SET_OPERATING_MODE: Byte = 0x06

    fun readOperationMode(ctrAddr: Int, rigAddr: Int): ByteArray {
        val data = ByteArray(6)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_READ_OPERATING_MODE
        data[5] = 0xfd.toByte()
        return data
    }

    fun sendOperationMode(ctrAddr: Int, rigAddr: Int, mode: Int): ByteArray {
        val data = ByteArray(8)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_SET_OPERATING_MODE
        data[5] = mode.toByte()
        data[6] = 0x01.toByte()
        data[7] = 0xfd.toByte()
        return data
    }

    fun readOperationFrequency(ctrAddr: Int, rigAddr: Int): ByteArray {
        val data = ByteArray(6)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_READ_OPERATING_FREQUENCY
        data[5] = 0xfd.toByte()
        return data
    }

    fun sendOperationFrequency(ctrAddr: Int, rigAddr: Int, freq: Long): ByteArray {
        val data = ByteArray(11)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_SET_OPERATING_FREQUENCY
        data[5] = (((freq % 100 / 10).toByte().toInt() shl 4) + (freq % 10).toByte()).toByte()
        data[6] =
            (((freq % 10000 / 1000).toByte().toInt() shl 4) + (freq % 1000 / 100).toByte()).toByte()
        data[7] = (((freq % 1000000 / 100000).toByte()
            .toInt() shl 4) + (freq % 100000 / 10000).toByte()).toByte()
        data[8] = (((freq % 100000000 / 10000000).toByte()
            .toInt() shl 4) + (freq % 10000000 / 1000000).toByte()).toByte()
        data[9] = (((freq / 1000000000).toByte()
            .toInt() shl 4) + (freq % 1000000000 / 100000000).toByte()).toByte()
        data[10] = 0xfd.toByte()

        Log.d(TAG, "setOperationFrequency: " + BaseRig.byteToStr(data))
        return data
    }
}

data class IcomCommand(val rawData: ByteArray) {

    val commandID: Byte
        get() = rawData.getOrNull(4) ?: -1

    val subCommand: Byte
        get() = rawData.getOrNull(5) ?: -1

    val subCommand2: Int
        get() = if (rawData.size >= 8) readShort(rawData, 6).toInt() else -1

    val subCommand3: Int
        get() = if (rawData.size >= 9) {
            (rawData[5].toInt() and 0xff) shl 16 or
                    (rawData[6].toInt() and 0xff) shl 8 or
                    (rawData[7].toInt() and 0xff)
        } else -1

    fun getData(hasSubCommand: Boolean): ByteArray {
        val pos = if (hasSubCommand) 6 else 5
        return if (rawData.size > pos) rawData.copyOfRange(pos, rawData.size) else byteArrayOf()
    }

    val data2Sub: ByteArray
        get() = if (rawData.size > 8) rawData.copyOfRange(8, rawData.size) else byteArrayOf()

    fun getFrequency(hasSubCommand: Boolean): Long {
        val data = getData(hasSubCommand)
        if (data.size < 5) return -1
        return ((data[0].toBCD(1)) +
                data[1].toBCD(100) +
                data[2].toBCD(10_000) +
                data[3].toBCD(1_000_000) +
                data[4].toBCD(100_000_000))
    }

    override fun toString(): String = rawData.joinToString(" ") { "%02X".format(it) }

    companion object {
        fun readShort(data: ByteArray, start: Int): Short {
            return if (data.size - start < 2) 0
            else (((data[start].toInt() and 0xff) shl 8) or (data[start + 1].toInt() and 0xff)).toShort()
        }

        private fun Byte.toBCD(unit: Int): Long {
            val b = this.toInt()
            return ((b shr 4) * 10 + (b and 0xF)) * unit.toLong()
        }
    }
}

class IcomCommandParser(
    private val ctrAddr: Int,
    private val rigAddr: Int,
    private val onCommand: (IcomCommand) -> Unit
) {
    private val buffer = mutableListOf<Byte>()

    fun feed(input: ByteArray) {
        buffer.addAll(input.toList())

        while (true) {
            val headIndex = getCommandStart(buffer) ?: break
            val endIndex = getCommandEnd(buffer, headIndex)
            if (endIndex == -1) {
                // 未找到帧尾，保留数据等待更多字节
                if (headIndex > 0) buffer.subList(0, headIndex).clear()
                break
            }

            val frame = buffer.subList(headIndex, endIndex + 1).toByteArray()
            buffer.subList(0, endIndex + 1).clear()

            parseFrame(frame)?.let { onCommand(it) }
        }
    }

    private fun getCommandStart(data: List<Byte>): Int? {
        for (i in 0 until data.size - 5) {
            if (data[i] == 0xFE.toByte() &&
                data[i + 1] == 0xFE.toByte() &&
                (data[i + 2] == ctrAddr.toByte() || data[i + 2] == 0x00.toByte()) &&
                data[i + 3] == rigAddr.toByte()
            ) {
                return i
            }
        }
        return null
    }

    private fun getCommandEnd(data: List<Byte>, start: Int): Int {
        for (i in start until data.size) {
            if (data[i] == 0xFD.toByte()) {
                return i
            }
        }
        return -1
    }

    private fun parseFrame(frame: ByteArray): IcomCommand? {
        if (frame.size < 6 || frame.last() != 0xFD.toByte()) return null
        val raw = frame.copyOfRange(0, frame.size - 1)
        return IcomCommand(raw)
    }
}