package com.guardsms.presentation.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.guardsms.R
import com.guardsms.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Setup nav for auth flow (just one fragment for now — login)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.auth_nav_host) as? NavHostFragment

        // If no nav host, just add LoginFragment directly
        if (navHostFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_nav_host, LoginFragment())
                .commit()
        }
    }

    fun onAuthSuccess() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
