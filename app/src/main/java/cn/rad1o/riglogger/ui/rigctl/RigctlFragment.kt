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

package cn.rad1o.riglogger.ui.rigctl

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.edit
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import cn.rad1o.riglogger.MainActivity
import cn.rad1o.riglogger.R
import cn.rad1o.riglogger.RigType
import cn.rad1o.riglogger.databinding.FragmentRigctlBinding
import cn.rad1o.riglogger.rigport.CableSerialPort
import cn.rad1o.riglogger.rigport.SerialParameter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText

class RigctlFragment : Fragment() {

    private var _binding: FragmentRigctlBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var availablePorts: ArrayList<String>
    private lateinit var portAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRigctlBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    fun restoreConfiguration() {
        val dropdown = view?.findViewById<MaterialAutoCompleteTextView>(R.id.rigTypeInput)
        val prefs = requireContext().getSharedPreferences("rigsvc_prefs", Context.MODE_PRIVATE)

        val savedRigType = prefs.getInt("rig_type", -1)
        if (savedRigType > 0) {
            dropdown?.setText(RigType.fromValue(savedRigType)?.label, false)
        }
    }

    fun restoreSerialConfiguration(param: SerialParameter) {
        val baudRate = view?.findViewById<TextInputEditText>(R.id.baudRateInput)
        baudRate?.setText(param.baudRate.toString())

        val dataBits = view?.findViewById<TextInputEditText>(R.id.dataBitsInput)
        dataBits?.setText(param.dataBits.toString())

        val stopBits = view?.findViewById<TextInputEditText>(R.id.stopBitsInput)
        stopBits?.setText(param.stopBits.toString())

        val parity = view?.findViewById<TextInputEditText>(R.id.parityInput)
        parity?.setText(param.parity.toString())
    }

    fun refreshSerialPorts() {
        val prefs = requireContext().getSharedPreferences("rigsvc_prefs", Context.MODE_PRIVATE)
        val dropdown = view?.findViewById<MaterialAutoCompleteTextView>(R.id.rigPortInput)
        val ports = CableSerialPort.listSerialPorts(requireContext())
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            ports.map { it.toString() }
        )

        availablePorts = ports.map { it.toString() } as ArrayList<String>

        dropdown?.setAdapter(adapter)
        portAdapter = adapter

        if (prefs.getString("ser_desc", "") in availablePorts) {
            dropdown?.setText(prefs.getString("ser_desc", ""), false)
        } else { dropdown?.text?.clear() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if ((activity as MainActivity).viewModel.isServiceRunning) {
            view.findViewById<View>(R.id.formView).visibility = View.GONE
            view.findViewById<View>(R.id.runningView).visibility = View.VISIBLE
            return
        }

        val prefs = requireContext().getSharedPreferences("rigsvc_prefs", Context.MODE_PRIVATE)
        val rigDropdown = view.findViewById<MaterialAutoCompleteTextView>(R.id.rigTypeInput)
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_1,
            RigType.all().map { it.label }
        )

        rigDropdown.setAdapter(adapter)
        rigDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedLabel = adapter.getItem(position) ?: return@setOnItemClickListener
            val rigType = RigType.fromLabel(selectedLabel)
            if (rigType == null) return@setOnItemClickListener

            prefs.edit { putInt("rig_type", rigType.value) }

            val rig = rigType.getRigObj()
            rig?.defaultSerialParameter?.let { restoreSerialConfiguration(it) }
        }

        val baudRate = view.findViewById<TextInputEditText>(R.id.baudRateInput)
        val dataBits = view.findViewById<TextInputEditText>(R.id.dataBitsInput)
        val stopBits = view.findViewById<TextInputEditText>(R.id.stopBitsInput)
        val parity = view.findViewById<TextInputEditText>(R.id.parityInput)

        baudRate.setText(prefs.getInt("ser_baudrate", 0).toString())
        dataBits.setText(prefs.getInt("ser_databits", 0).toString())
        stopBits.setText(prefs.getInt("ser_stopbits", 0).toString())
        parity.setText(prefs.getInt("ser_parity", 0).toString())

        baudRate.doAfterTextChanged { text ->
            if (text.toString() == "") return@doAfterTextChanged
            prefs.edit { putInt("ser_baudrate", text.toString().toInt()) }
        }
        dataBits.doAfterTextChanged { text ->
            if (text.toString() == "") return@doAfterTextChanged
            if (text.toString().toInt() > 8) return@doAfterTextChanged
            prefs.edit { putInt("ser_databits", text.toString().toInt()) }
        }
        stopBits.doAfterTextChanged { text ->
            if (text.toString() == "") return@doAfterTextChanged
            if (text.toString().toInt() > 2) return@doAfterTextChanged
            prefs.edit { putInt("ser_stopbits", text.toString().toInt()) }
        }
        parity.doAfterTextChanged { text ->
            if (text.toString() == "") return@doAfterTextChanged
            if (text.toString().toInt() > 2) return@doAfterTextChanged
            prefs.edit { putInt("ser_parity", text.toString().toInt()) }
        }

        restoreConfiguration()
        refreshSerialPorts()

        val portDropdown = view.findViewById<MaterialAutoCompleteTextView>(R.id.rigPortInput)
        portDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedLabel = portAdapter.getItem(position) ?: return@setOnItemClickListener
            prefs.edit { putString("ser_desc", selectedLabel) }
            refreshSerialPorts()
        }

        val refreshBtn = view.findViewById<MaterialButton>(R.id.refreshBtn)
        refreshBtn.setOnClickListener { _ ->
            refreshSerialPorts()
        }

        val onairBtn = view.findViewById<MaterialButton>(R.id.startButton)
        onairBtn.setOnClickListener { _ ->
            (activity as MainActivity).startService()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}