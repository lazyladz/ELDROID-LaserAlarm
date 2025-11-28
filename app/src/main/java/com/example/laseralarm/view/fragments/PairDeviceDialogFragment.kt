package com.example.laseralarm.view.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.laseralarm.R

class PairDeviceDialogFragment : DialogFragment() {

    private var onDevicePairedListener: ((String, String) -> Unit)? = null

    fun setOnDevicePairedListener(listener: (String, String) -> Unit) {
        onDevicePairedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_pair_device, null)

        val etDeviceId = view.findViewById<EditText>(R.id.etDeviceId)
        val etDeviceName = view.findViewById<EditText>(R.id.etDeviceName)

        builder.setView(view)
            .setTitle("🔗 Add New Alarm Device")
            .setPositiveButton("Pair Device") { dialog, which ->
                val deviceId = etDeviceId.text.toString().trim()
                val deviceName = etDeviceName.text.toString().trim()

                if (deviceId.isEmpty() || deviceName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                } else {
                    onDevicePairedListener?.invoke(deviceId, deviceName)
                }
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        return builder.create()
    }
}