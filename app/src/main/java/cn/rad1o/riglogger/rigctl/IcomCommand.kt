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

class IcomCommand {
    private lateinit var rawData: ByteArray

    fun getCommandID(): Byte { //获取主命令
        if (rawData.size < 5) {
            return -1
        }
        return rawData[4]
    }

    val commandID: Int
        /**
         * 获取主命令
         *
         * @return 主命令值
         */
        get() { //获取主命令
            if (rawData.size < 5) {
                return -1
            }
            return rawData[4].toInt()
        }

    val subCommand: Int
        /**
         * 获取子命令，有的指令没有子命令，要注意。
         *
         * @return 子命令
         */
        get() { //获取子命令
            if (rawData.size < 7) {
                return -1
            }
            return rawData[5].toInt()
        }

    val subCommand2: Int
        /**
         * 获取带2字节的子命令，有的指令没有子命令，有的指令只有1个字节，要注意。
         * @return 子指令
         */
        get() { //获取子命令
            if (rawData.size < 8) {
                return -1
            }
            return readShortData(rawData, 6).toInt()
        }
    val subCommand3: Int
        /**
         * 获取带3字节的子命令，有的指令没有子命令，有的指令只有1个字节，要注意。
         * @return 子指令
         */
        get() { //获取子命令
            if (rawData.size < 9) {
                return -1
            }
            return ((rawData[7].toInt() and 0xff)
                    or ((rawData[6].toInt() and 0xff) shl 8
                    ) or ((rawData[5].toInt() and 0xff) shl 16))
        }

    /**
     * 获取数据区，有的指令有子命令，有的没有子命令，所以要区分出来。子命令占一个字节
     *
     * @param hasSubCommand 是否有子命令
     * @return 返回数据区
     */
    fun getData(hasSubCommand: Boolean): ByteArray {
        val pos: Int

        if (hasSubCommand) {
            pos = 6
        } else {
            pos = 5
        }
        if (rawData.size < pos + 1) { //没有数据区了
            return ByteArray(0)
        }

        val data = ByteArray(rawData.size - pos)

        for (i in 0..<rawData.size - pos) {
            data[i] = rawData[pos + i]
        }
        return data
    }

    val data2Sub: ByteArray
        get() {
            if (rawData.size < 9) { //没有数据区了
                return ByteArray(0)
            }

            val data = ByteArray(rawData.size - 8)

            System.arraycopy(rawData, 8, data, 0, rawData.size - 8)
            return data
        }

    /**
     * 从数据区中计算频率BCD码
     *
     * @param hasSubCommand 是否含有子命令
     * @return 返回频率值
     */
    fun getFrequency(hasSubCommand: Boolean): Long {
        val data = getData(hasSubCommand)
        if (data.size < 5) {
            return -1
        }
        return ((data[0].toInt() and 0x0f) //取个位 1hz
                + ((data[0].toInt() shr 4) and 0xf) * 10 //取十位 10hz
                + (data[1].toInt() and 0x0f) * 100 //百位 100hz
                + ((data[1].toInt() shr 4) and 0xf) * 1000 //千位  1khz
                + (data[2].toInt() and 0x0f) * 10000 //万位 10khz
                + ((data[2].toInt() shr 4) and 0xf) * 100000 //十万位 100khz
                + (data[3].toInt() and 0x0f) * 1000000 //百万位 1Mhz
                + ((data[3].toInt() shr 4) and 0xf) * 10000000 //千万位 10Mhz
                + (data[4].toInt() and 0x0f) * 100000000 //亿位 100Mhz
                + ((data[4].toInt() shr 4) and 0xf) * 100000000).toLong() //十亿位 1Ghz
    }


    companion object {
        private const val TAG = "RigCommand"
        //解析接收的指令
        /**
         * 从串口中接到的数据解析出指令的数据:FE FE E0 A4 Cn Sc data FD
         *
         * @param ctrAddr 控制者地址，默认E0或00
         * @param rigAddr 电台地址，705默认是A4
         * @param buffer  从串口接收到的数据
         * @return 返回电台指令对象，如果不符合指令的格式，返回null。
         */
        fun getCommand(ctrAddr: Int, rigAddr: Int, buffer: ByteArray): IcomCommand? {
            Log.d(TAG, "getCommand: " + BaseRig.byteToStr(buffer))
            if (buffer.size <= 5) { //指令的长度不可能小于等5
                return null
            }
            var position = -1 //指令的位置
            for (i in buffer.indices) {
                if (i + 6 > buffer.size) { //说明没找到指令
                    return null
                }
                if (buffer[i] == 0xfe.toByte() && buffer[i + 1] == 0xfe.toByte() //命令头0xfe 0xfe
                    && (buffer[i + 2] == ctrAddr.toByte() || (buffer[i + 2] == 0x00.toByte())) //控制者地址默认E0或00
                    && buffer[i + 3] == rigAddr.toByte()
                ) { //电台地址，705的默认值是A4，协谷是70
                    position = i
                    break
                }
            }
            //说明没找到
            if (position == -1) {
                return null
            }

            var dataEnd = -1
            //从命令头之后查起。所以i=position
            for (i in position..<buffer.size) {
                if (buffer[i] == 0xfd.toByte()) { //是否到结尾了
                    dataEnd = i
                    break
                }
            }
            if (dataEnd == -1) { //说明没找到结尾
                return null
            }

            val icomCommand = IcomCommand()
            icomCommand.rawData = ByteArray(dataEnd - position)
            var pos = 0
            for (i in position..<dataEnd) { //把指令数据搬到rawData中
                //icomCommand.rawData[i] = buffer[i];
                icomCommand.rawData[pos] = buffer[i] //定位错误
                pos++
            }
            return icomCommand
        }


        /**
         * 把字节转换成short，不做小端转换！！
         *
         * @param data 字节数据
         * @return short
         */
        fun readShortData(data: ByteArray, start: Int): Short {
            if (data.size - start < 2) return 0
            return (data[start + 1].toShort().toInt() and 0xff
                    or ((data[start].toShort().toInt() and 0xff) shl 8)).toShort()
        }
    }
}