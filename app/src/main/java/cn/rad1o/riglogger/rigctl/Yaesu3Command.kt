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

data class Yaesu3Command(val rawData: ByteArray) {
    val commandID: String
        get() = rawData.decodeToString(0, 2)

    val data: String
        get() = rawData.decodeToString(2, rawData.size)

    fun getFrequency(): Long {
        if (!(commandID == "IF" && data.length >= 25)) return 0
        return data.substring(3, 12).toLong()
    }

    fun getMode(): OperationMode? {
        if (!(commandID == "IF" && data.length >= 25)) return null
        return OperationMode.fromYaesu3Def(data[19])
    }

    override fun toString(): String = "Yaesu3Command(commandID='$commandID', data='$data')"
}

class Yaesu3CommandParser(
    private val onCommand: (Yaesu3Command) -> Unit
) {
    private val buffer = mutableListOf<Byte>()

    fun feed(bytes: ByteArray) {
        buffer.addAll(bytes.toList())
        parseBuffer()
    }

    private fun parseBuffer() {
        while (true) {
            val endIndex = buffer.indexOf(';'.code.toByte())
            if (endIndex < 2) break

            val frame = buffer.subList(0, endIndex)
            val raw = frame.toByteArray()
            onCommand(Yaesu3Command(raw))

            repeat(endIndex + 1) { buffer.removeAt(0) }
        }
    }
}