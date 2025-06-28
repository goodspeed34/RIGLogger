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

package cn.rad1o.riglogger

import cn.rad1o.riglogger.rigctl.BaseRig
import cn.rad1o.riglogger.rigctl.XieGuRig
import cn.rad1o.riglogger.rigctl.Yaesu39Rig

data class RigType(val label: String, val value: Int) {
    companion object {
        private val values = listOf<RigType>(
            RigType("Xiegu G90 / 6100 Series", 1),
            RigType("Yaesu FT-891", 2),
        )

        fun fromLabel(label: String): RigType? {
            return values.find { it.label == label }
        }

        fun fromValue(value: Int): RigType? {
            return values.find { it.value == value }
        }

        fun all(): List<RigType> = values
    }

    fun getRigObj(): BaseRig? {
        return when (value) {
            1 -> XieGuRig(40)
            2 -> Yaesu39Rig()
            else -> null
        }
    }
}
