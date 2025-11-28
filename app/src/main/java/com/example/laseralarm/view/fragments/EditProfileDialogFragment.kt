package com.example.laseralarm.view.fragments

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.laseralarm.R
import com.example.laseralarm.model.UserRepository
import com.example.laseralarm.presenter.ProfilePresenter
import com.example.laseralarm.utils.UserManager
import com.google.firebase.auth.FirebaseAuth

class EditProfileDialogFragment : DialogFragment() {

    private var profileUpdateListener: ((String, String, String, String) -> Unit)? = null
    private lateinit var profilePresenter: ProfilePresenter

    companion object {
        private const val ARG_FULL_NAME = "full_name"
        private const val ARG_PHONE = "phone"
        private const val ARG_USERNAME = "username"
        private const val ARG_EMAIL = "email"

        fun newInstance(
            fullName: String,
            phone: String,
            username: String,
            email: String
        ): EditProfileDialogFragment {
            val fragment = EditProfileDialogFragment()
            val args = Bundle()
            args.putString(ARG_FULL_NAME, fullName)
            args.putString(ARG_PHONE, phone)
            args.putString(ARG_USERNAME, username)
            args.putString(ARG_EMAIL, email)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_edit_profile, container, false)

        profilePresenter = ProfilePresenter(UserRepository())

        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        val currentFullName = arguments?.getString(ARG_FULL_NAME) ?: ""
        val currentPhone = arguments?.getString(ARG_PHONE) ?: ""
        val currentUsername = arguments?.getString(ARG_USERNAME) ?: ""
        val currentEmail = arguments?.getString(ARG_EMAIL) ?: ""

        etFullName.setText(currentFullName)
        etPhone.setText(currentPhone)
        etUsername.setText(currentUsername)
        etEmail.setText(currentEmail)

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (validateInput(fullName, phone, username, email)) {
                updateProfile(fullName, phone, username, email)
            }
        }

        return view
    }

    private fun validateInput(
        fullName: String,
        phone: String,
        username: String,
        email: String
    ): Boolean {
        if (fullName.isEmpty()) {
            showError("Full name cannot be empty")
            return false
        }

        if (phone.isEmpty()) {
            showError("Phone number cannot be empty")
            return false
        }

        if (username.isEmpty()) {
            showError("Username cannot be empty")
            return false
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Enter a valid email address")
            return false
        }

        return true
    }

    private fun updateProfile(
        fullName: String,
        phone: String,
        username: String,
        email: String
    ) {

        profilePresenter.updateUserProfile(
            fullName = fullName,
            phone = phone,
            username = username,
            email = email,
            onSuccess = { message ->
                UserManager.updateUserData(
                    requireContext().applicationContext,
                    fullName,
                    phone,
                    username,
                    email
                )

                profileUpdateListener?.invoke(fullName, phone, username, email)

                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                dismiss()
            },
            onError = { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun setProfileUpdateListener(listener: (String, String, String, String) -> Unit) {
        this.profileUpdateListener = listener
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}