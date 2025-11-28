package com.example.laseralarm.view.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.laseralarm.R
import com.example.laseralarm.utils.UserManager
import com.example.laseralarm.view.activities.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_profile, container, false)

        val fullName = UserManager.getFullName(requireContext())
        val phone = UserManager.getPhone(requireContext())
        val username = UserManager.getUsername(requireContext())
        val email = UserManager.getEmail(requireContext())

        view.findViewById<TextView>(R.id.tvFullName).text = "Full Name: $fullName"
        view.findViewById<TextView>(R.id.tvPhone).text = "Phone: $phone"
        view.findViewById<TextView>(R.id.tvUsername).text = "Username: $username"
        view.findViewById<TextView>(R.id.tvEmail).text = "Email: $email"

        setupClickListeners(view, fullName, phone, username, email)
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupClickListeners(
        view: View,
        currentFullName: String,
        currentPhone: String,
        currentUsername: String,
        currentEmail: String
    ) {
        view.findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog(currentFullName, currentPhone, currentUsername, currentEmail)
        }

        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            performLogout()
        }
    }

    private fun showEditProfileDialog(
        currentFullName: String,
        currentPhone: String,
        currentUsername: String,
        currentEmail: String
    ) {
        val editProfileFragment = EditProfileDialogFragment.newInstance(
            currentFullName,
            currentPhone,
            currentUsername,
            currentEmail
        )

        editProfileFragment.setProfileUpdateListener { newFullName, newPhone, newUsername, newEmail ->
            UserManager.updateUserData(requireContext().applicationContext, newFullName, newPhone, newUsername, newEmail)

            Toast.makeText(requireContext().applicationContext, "Profile updated!", Toast.LENGTH_SHORT).show()

            updateProfileData(newFullName, newPhone, newUsername, newEmail)
        }

        editProfileFragment.show(parentFragmentManager, "EditProfileDialog")
    }

    private fun updateProfileData(fullName: String, phone: String, username: String, email: String) {
        view?.findViewById<TextView>(R.id.tvFullName)?.text = "Full Name: $fullName"
        view?.findViewById<TextView>(R.id.tvPhone)?.text = "Phone: $phone"
        view?.findViewById<TextView>(R.id.tvUsername)?.text = "Username: $username"
        view?.findViewById<TextView>(R.id.tvEmail)?.text = "Email: $email"
    }

    private fun performLogout() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, which ->
                executeLogout()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun executeLogout() {
        try {
            FirebaseAuth.getInstance().signOut()

            UserManager.logout(requireContext().applicationContext)

            Toast.makeText(requireContext().applicationContext, "Logged out successfully", Toast.LENGTH_SHORT).show()

            dismiss()

            navigateToLogin()

        } catch (e: Exception) {
            Toast.makeText(requireContext().applicationContext, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        requireActivity().finish()
    }
}