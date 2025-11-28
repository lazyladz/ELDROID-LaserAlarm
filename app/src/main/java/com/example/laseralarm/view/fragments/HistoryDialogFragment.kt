package com.example.laseralarm.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.laseralarm.R
import com.example.laseralarm.presenter.HistoryPresenter
import com.example.laseralarm.view.HistoryView

class HistoryDialogFragment : DialogFragment(), HistoryView {

    private lateinit var presenter: HistoryPresenter
    private lateinit var historyContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_history, container, false)

        historyContainer = view.findViewById(R.id.historyContainer)
        progressBar = ProgressBar(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Initialize presenter
        presenter = HistoryPresenter(this, requireContext())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load history when dialog is created
        presenter.loadHistory()
    }

    override fun showLoading(show: Boolean) {
        if (show) {
            historyContainer.removeAllViews()
            historyContainer.addView(progressBar)
        } else {
            historyContainer.removeView(progressBar)
        }
    }

    override fun showHistory(events: List<String>) {
        historyContainer.removeAllViews()

        if (events.isEmpty()) {
            val tv = TextView(requireContext())
            tv.text = "No alarm events yet"
            tv.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            tv.textSize = 14f
            tv.setPadding(0, 16, 0, 16)
            tv.gravity = android.view.Gravity.CENTER
            historyContainer.addView(tv)
        } else {
            events.forEach { event ->
                val tv = TextView(requireContext())
                tv.text = event
                tv.setTextColor(resources.getColor(android.R.color.white, null))
                tv.textSize = 14f
                tv.setPadding(32, 16, 32, 16)
                tv.background = resources.getDrawable(R.drawable.bg_event_card, null)
                tv.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
                historyContainer.addView(tv)
            }
        }
    }

    override fun showError(message: String) {
        historyContainer.removeAllViews()
        val tv = TextView(requireContext())
        tv.text = "Error: $message"
        tv.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        tv.textSize = 14f
        tv.setPadding(0, 16, 0, 16)
        tv.gravity = android.view.Gravity.CENTER
        historyContainer.addView(tv)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            (resources.displayMetrics.heightPixels * 0.70).toInt()
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}