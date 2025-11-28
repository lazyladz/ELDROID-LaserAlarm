package com.example.laseralarm.contract

import com.example.laseralarm.model.User

interface ProfileContract {
    interface View {
        fun showUserInfo(user: User)
        fun showError(message: String)
    }

    interface Presenter {
        fun loadUserInfo()
    }
}
