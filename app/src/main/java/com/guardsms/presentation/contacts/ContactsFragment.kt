package com.guardsms.presentation.contacts

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.guardsms.databinding.FragmentContactsBinding
import com.guardsms.presentation.common.ContactsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

@AndroidEntryPoint
class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactsViewModel by viewModels()

    private val adapter = ContactsAdapter()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) syncContacts()
        else Snackbar.make(binding.root, "Contacts permission required to sync", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ContactsFragment.adapter
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadContacts() }

        binding.btnSync.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
                syncContacts()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    binding.progressSync.isVisible = state.isSyncing
                    binding.btnSync.isEnabled = !state.isSyncing
                    binding.tvContactCount.text = "${state.totalContacts} contacts synced"
                    adapter.submitList(state.contacts)

                    state.errorMessage?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearFeedback()
                    }
                    state.successMessage?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearFeedback()
                    }
                }
            }
        }
    }

    private fun syncContacts() {
        viewModel.syncDeviceContacts(requireContext().contentResolver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
