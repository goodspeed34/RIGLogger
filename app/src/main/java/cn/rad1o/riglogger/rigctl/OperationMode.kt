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

enum class OperationMode(i: Int) {
    AM(0),
    FM(1),
    DV(3),
    USB(4),
    LSB(5),
    CWL(6),
    CWR(7),
    RTTYL(8),
    RTTYR(9);

    companion object {
        /**
         * Convert the mode constants defined in Icom CI-V protocol to the enum.
         */
        fun fromIcomDef(value: Int): OperationMode? {
            when (value) {
                IcomRigConstant.LSB -> return LSB
                IcomRigConstant.USB -> return USB
                IcomRigConstant.AM -> return AM
                IcomRigConstant.CW -> return CWL
                IcomRigConstant.RTTY -> return RTTYL
                IcomRigConstant.FM -> return FM
                IcomRigConstant.CW_R -> return CWR
                IcomRigConstant.RTTY_R -> return RTTYR
                IcomRigConstant.DV -> return DV
                else -> return null
            }
        }

        /**
         * Convert the enum into a human readable string.
         */
        fun toHumanReadable(value: OperationMode?): String {
            return when (value) {
                AM -> "AM"
                FM -> "FM"
                DV -> "DV"
                USB -> "USB"
                LSB -> "LSB"
                CWL -> "CW"
                CWR -> "CWR"
                RTTYL -> "RTTY"
                RTTYR -> "RTTYR"
                else -> "Unknown"
            }
        }

        /**
         * Convert the enum into a mode string used in Cloudlog API.
         */
        fun toCloudlogMode(value: OperationMode): String {
            return when (value) {
                AM -> "AM"
                FM -> "FM"
                DV -> "DIGITALVOICE"
                USB -> "USB"
                LSB -> "LSB"
                CWL -> "CW"
                CWR -> "CW"
                RTTYL -> "RTTY"
                RTTYR -> "RTTY"
            }
        }
    }
}