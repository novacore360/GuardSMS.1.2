package com.guardsms.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.guardsms.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var isSignUp = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSubmit.setOnClickListener { handleSubmit() }
        binding.tvToggle.setOnClickListener { toggleMode() }
        binding.tvForgot.setOnClickListener { handleForgotPassword() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    binding.btnSubmit.isEnabled = !state.isLoading

                    if (state.isSuccess) {
                        (requireActivity() as AuthActivity).onAuthSuccess()
                        return@collect
                    }

                    if (state.errorMessage != null) {
                        binding.tvError.text = state.errorMessage
                        binding.tvError.isVisible = true
                        viewModel.clearError()
                    } else {
                        binding.tvError.isVisible = false
                    }
                }
            }
        }
    }

    private fun handleSubmit() {
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        if (isSignUp) {
            val confirm = binding.etConfirmPassword.text?.toString() ?: ""
            viewModel.signUp(email, password, confirm)
        } else {
            viewModel.signIn(email, password)
        }
    }

    private fun handleForgotPassword() {
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        viewModel.resetPassword(email)
    }

    private fun toggleMode() {
        isSignUp = !isSignUp
        if (isSignUp) {
            binding.tvTitle.text = "Create Account"
            binding.btnSubmit.text = "Create Account"
            binding.tvToggle.text = "Already have an account? Sign in"
            binding.tilConfirmPassword.isVisible = true
            binding.tvForgot.isVisible = false
        } else {
            binding.tvTitle.text = "Sign In"
            binding.btnSubmit.text = "Sign In"
            binding.tvToggle.text = "Don't have an account? Sign up"
            binding.tilConfirmPassword.isVisible = false
            binding.tvForgot.isVisible = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
