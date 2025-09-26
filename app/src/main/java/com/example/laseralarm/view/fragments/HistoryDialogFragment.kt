package com.example.laseralarm.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.laseralarm.R

class HistoryDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_history, container, false)

        val historyContainer = view.findViewById<LinearLayout>(R.id.historyContainer)

        // ✅ Static sample logs (can later load dynamically)
        val logs = listOf(
            "09/24/2025\nTriggered at 2025-09-24 17:09",
            "09/24/2025\nSafe Since 2025-09-23 19:00",
            "09/23/2025\nTriggered at 2025-09-23 11:45"
        )

        logs.forEach { log ->
            val tv = TextView(requireContext())
            tv.text = log
            tv.setTextColor(resources.getColor(android.R.color.white, null))
            tv.textSize = 14f
            tv.setPadding(0, 8, 0, 8)
            historyContainer.addView(tv)
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
