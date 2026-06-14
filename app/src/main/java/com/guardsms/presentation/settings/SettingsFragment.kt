package com.guardsms.presentation.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.guardsms.BuildConfig
import com.guardsms.R
import com.guardsms.databinding.FragmentSettingsBinding
import com.guardsms.presentation.auth.AuthActivity
import com.guardsms.presentation.dashboard.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvVersion.text = BuildConfig.VERSION_NAME
        binding.tvEmail.text = com.guardsms.data.remote.SupabaseClientProvider.client
            .auth.currentUserOrNull()?.email ?: "—"

        binding.itemSignOut.setOnClickListener {
            viewModel.signOut {
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
        }

        binding.itemPrivacy.setOnClickListener {
            PrivacyPolicyDialog().show(parentFragmentManager, "privacy")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
