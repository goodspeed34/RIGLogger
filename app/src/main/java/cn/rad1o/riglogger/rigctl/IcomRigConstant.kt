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

    //LSB:0,USB:1,AM:2,CW:3,RTTY:4,FM:5,WFM:6,CW_R:7,RTTY_R:8,DV:17
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
    val UNKNOWN: Int = -1


    const val swr_alert_max: Int = 120 //相当于3.0
    const val alc_alert_max: Int = 120 //超过，在表上显示红色

    //协谷6100的ALC值的原始值（0～255）在127±50都是最佳线性度范围，转换到0～120的线性范围就是36.17到83.19
    const val xiegu_alc_alert_max: Int = 84 //超过，在表上显示红色
    const val xiegu_alc_alert_min: Int = 36 //超过，在表上显示红色


    //PTT状态
    const val PTT_ON: Int = 1
    const val PTT_OFF: Int = 0

    //指令集
    val CMD_RESULT_OK: Byte = 0xfb.toByte() //
    val CMD_RESULT_FAILED: Byte = 0xfa.toByte() //

    val SEND_FREQUENCY_DATA: ByteArray = byteArrayOf(0x00) //发送频率数据
    const val CMD_SEND_FREQUENCY_DATA: Byte = 0x00 //发送频率数据

    val SEND_MODE_DATA: ByteArray = byteArrayOf(0x01) //发送模式数据
    const val CMD_SEND_MODE_DATA: Byte = 0x01 //发送模式数据

    val READ_BAND_EDGE_DATA: ByteArray = byteArrayOf(0x02) //读频率的波段边界
    const val CMD_READ_BAND_EDGE_DATA: Byte = 0x02 //读频率的波段边界

    val READ_OPERATING_FREQUENCY: ByteArray = byteArrayOf(0x03) //发送模式数据
    const val CMD_READ_OPERATING_FREQUENCY: Byte = 0x03 //发送模式数据

    val READ_OPERATING_MODE: ByteArray = byteArrayOf(0x04) //读取操作模式
    const val CMD_READ_OPERATING_MODE: Byte = 0x04 //读取操作模式

    val SET_OPERATING_FREQUENCY: ByteArray = byteArrayOf(0x05) //设置操作的频率
    const val CMD_SET_OPERATING_FREQUENCY: Byte = 0x05 //设置操作的频率

    val SET_OPERATING_MODE: ByteArray = byteArrayOf(0x06) //设置操作的模式
    const val CMD_SET_OPERATING_MODE: Byte = 0x06 //设置操作的模式

    const val CMD_READ_METER: Byte = 0x15 //读meter
    const val CMD_READ_METER_SWR: Byte = 0x12 //读meter子命令，驻波表
    const val CMD_READ_METER_ALC: Byte = 0x13 //读meter子命令，ALC表
    const val CMD_CONNECTORS: Byte = 0x1A //Connector设置，读取
    const val CMD_CONNECTORS_DATA_MODE: Byte = 0x05 //Connector设置，读取
    const val CMD_CONNECTORS_DATA_WLAN_LEVEL: Int = 0x050117 //Connector设置，读取


    const val CMD_COMMENT_1A: Byte = 0x1A //1A指令
    val SET_READ_PTT_STATE: ByteArray = byteArrayOf(0x1A, 0x00, 0x48) //读取或设置PTT状态,不建议使用

    val READ_TRANSCEIVER_STATE: ByteArray = byteArrayOf(0x1A, 0x00, 0x48) //读取电台发射状态
    val SET_TRANSCEIVER_STATE_ON: ByteArray = byteArrayOf(0x1C, 0x00, 0x01) //设置电台处于发射状态TX
    val SET_TRANSCEIVER_STATE_OFF: ByteArray = byteArrayOf(0x1C, 0x00, 0x00) //设置电台关闭发射状态RX
    val READ_TRANSMIT_FREQUENCY: ByteArray = byteArrayOf(0x1C, 0x03) //读取电台发射时的频率

    fun getModeStr(mode: Int): String {
        when (mode) {
            LSB -> return "LSB"
            USB -> return "USB"
            AM -> return "AM"
            CW -> return "CW"
            RTTY -> return "RTTY"
            FM -> return "FM"
            CW_R -> return "CW_R"
            RTTY_R -> return "RTTY_R"
            DV -> return "DV"
            else -> return "UNKNOWN"
        }
    }


    fun setPTTState(ctrAddr: Int, rigAddr: Int, state: Int): ByteArray {
        //1C指令，例如PTT ON：FE FE A1 E0 1C 00 01 FD
        val data = ByteArray(8)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = 0x1c.toByte() //主指令代码
        data[5] = 0x00.toByte() //子指令代码
        data[6] = state.toByte() //状态 01=tx 00=rx
        data[7] = 0xfd.toByte()
        return data
    }

    /**
     * 读驻波表
     *
     * @param ctrAddr 我的地址
     * @param rigAddr 电台地址
     * @return 指令数据包
     */
    fun getSWRState(ctrAddr: Int, rigAddr: Int): ByteArray {
        //1C指令，例如PTT ON：FE FE A1 E0 15 12 FD
        val data = ByteArray(7)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_READ_METER //主指令代码
        data[5] = CMD_READ_METER_SWR //子指令代码SWR
        data[6] = 0xfd.toByte()
        return data
    }

    /**
     * 读ALC表
     *
     * @param ctrAddr 我的地址
     * @param rigAddr 电台地址
     * @return 指令数据包
     */
    fun getALCState(ctrAddr: Int, rigAddr: Int): ByteArray {
        //1C指令，例如PTT ON：FE FE A1 E0 15 12 FD
        val data = ByteArray(7)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_READ_METER //主指令代码
        data[5] = CMD_READ_METER_ALC //子指令代码ALC
        data[6] = 0xfd.toByte()
        return data
    }

    fun getConnectorWLanLevel(ctrAddr: Int, rigAddr: Int): ByteArray {
        //1A指令，例如DATA MODE=WLAN：FE FE A1 E0 1A 05 01 17 FD
        val data = ByteArray(9)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_CONNECTORS //主指令代码1A
        data[5] = CMD_CONNECTORS_DATA_MODE //WLan level
        data[6] = 0x01.toByte()
        data[7] = 0x17.toByte()
        data[8] = 0xfd.toByte()
        return data
    }

    fun setConnectorWLanLevel(ctrAddr: Int, rigAddr: Int, level: Int): ByteArray {
        //1A指令，例如DATA MODE=WLAN：FE FE A1 E0 1A 05 01 17 FD
        val data = ByteArray(11)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_CONNECTORS //主指令代码1A
        data[5] = CMD_CONNECTORS_DATA_MODE //子指令代码ALC
        data[6] = 0x01.toByte()
        data[7] = 0x17.toByte()
        data[8] = (level shr 8 and 0xff).toByte()
        data[9] = (level and 0xff).toByte()
        data[10] = 0xfd.toByte()
        return data
    }

    //设置数据通讯方式
    fun setConnectorDataMode(ctrAddr: Int, rigAddr: Int, mode: Byte): ByteArray {
        //1A指令，例如DATA MODE=WLAN：FE FE A1 E0 1A 05 01 19 FD
        val data = ByteArray(10)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = CMD_CONNECTORS //主指令代码1A
        data[5] = CMD_CONNECTORS_DATA_MODE //子指令代码ALC
        data[6] = 0x01.toByte() //
        data[7] = 0x19.toByte() //
        data[8] = mode //数据连接的方式
        data[9] = 0xfd.toByte()
        return data
    }

    fun setOperationMode(ctrAddr: Int, rigAddr: Int, mode: Int): ByteArray {
        //06指令，例如USB=01：FE FE A1 E0 06 01 FD
        val data = ByteArray(8)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = 0x06.toByte() //指令代码
        data[5] = mode.toByte() //USB=01
        data[6] = 0x01.toByte() //fil1
        data[7] = 0xfd.toByte()
        return data
    }

    fun setOperationDataMode(ctrAddr: Int, rigAddr: Int, mode: Int): ByteArray {
        //26指令，例如USB-D=01：FE FE A1 E0 26 01 01 01 FD
        val data = ByteArray(10)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte() //70
        data[3] = ctrAddr.toByte() //E0
        data[4] = 0x26.toByte() //指令代码
        data[5] = 0x00.toByte() //指令代码
        data[6] = mode.toByte() //USB=01
        data[7] = 0x01.toByte() //data模式
        data[8] = 0x01.toByte() //fil1
        data[9] = 0xfd.toByte()
        return data
    }

    fun setReadFreq(ctrAddr: Int, rigAddr: Int): ByteArray {
        //06指令，例如USB=01：FE FE A1 E0 06 01 FD
        val data = ByteArray(6)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = 0x03.toByte() //指令代码
        data[5] = 0xfd.toByte()
        return data
    }


    fun setOperationFrequency(ctrAddr: Int, rigAddr: Int, freq: Long): ByteArray {
        //05指令，例如14.074M：FE FE A4 E0 05 00 40 07 14 00 FD
        val data = ByteArray(11)
        data[0] = 0xfe.toByte()
        data[1] = 0xfe.toByte()
        data[2] = rigAddr.toByte()
        data[3] = ctrAddr.toByte()
        data[4] = 0x05.toByte() //指令代码
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

    fun twoByteBcdToInt(data: ByteArray): Int {
        if (data.size < 2) return 0
        return ((data[1].toInt() and 0x0f) //取个位
                + ((data[1].toInt() shr 4) and 0xf) * 10 //取十位
                + (data[0].toInt() and 0x0f) * 100 //百位
                + ((data[0].toInt() shr 4) and 0xf) * 1000) //千位
    }

    fun twoByteBcdToIntBigEnd(data: ByteArray): Int {
        if (data.size < 2) return 0
        return ((data[0].toInt() and 0x0f) //取个位
                + ((data[0].toInt() shr 4) and 0xf) * 10 //取十位
                + (data[1].toInt() and 0x0f) * 100 //百位
                + ((data[1].toInt() shr 4) and 0xf) * 1000) //千位
    }
}