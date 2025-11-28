package com.example.laseralarm.service

import android.util.Log
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailService {

    companion object {
        // Gmail SMTP Configuration
        private const val EMAIL_HOST = "smtp.gmail.com"
        private const val EMAIL_PORT = "587"

        // Replace with your actual credentials
        private const val EMAIL_USERNAME = "lazylad65@gmail.com"
        private const val EMAIL_PASSWORD = "giwu fbar wjam gral"

        // Store verification codes temporarily
        private val verificationCodes = mutableMapOf<String, Pair<String, Long>>()
    }

    fun sendVerificationCode(toEmail: String, verificationCode: String): Boolean {
        return try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", EMAIL_HOST)
                put("mail.smtp.port", EMAIL_PORT)
                put("mail.smtp.ssl.trust", EMAIL_HOST)
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EMAIL_USERNAME, "LaserAlarm App"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "LaserAlarm - Password Reset Verification Code"
                setText("""
                    Password Reset Request
                    
                    Your verification code is: $verificationCode
                    
                    Enter this code in the app to reset your password.
                    
                    This code will expire in 10 minutes.
                    
                    If you didn't request this reset, please ignore this email.
                    
                    Best regards,
                    LaserAlarm Security Team
                """.trimIndent())
            }

            Transport.send(message)

            // Store the code with timestamp
            verificationCodes[toEmail] = Pair(verificationCode, System.currentTimeMillis())
            Log.d("EmailService", "✓ Verification code sent to $toEmail: $verificationCode")
            true
        } catch (e: Exception) {
            Log.e("EmailService", "✗ Failed to send email to $toEmail: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Method to verify codes
    fun verifyCode(email: String, code: String): Boolean {
        val stored = verificationCodes[email]
        if (stored != null) {
            val (storedCode, timestamp) = stored
            // Check if code is expired (10 minutes)
            if (System.currentTimeMillis() - timestamp > 10 * 60 * 1000) {
                verificationCodes.remove(email)
                return false
            }
            return storedCode == code
        }
        return false
    }

    // Method to resend code
    fun resendCode(email: String): String? {
        val newCode = String.format("%06d", Random().nextInt(999999))
        val success = sendVerificationCode(email, newCode)
        return if (success) newCode else null
    }
}