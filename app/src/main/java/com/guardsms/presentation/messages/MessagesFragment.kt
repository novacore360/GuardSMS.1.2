package com.guardsms.presentation.messages

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.guardsms.databinding.FragmentMessagesBinding
import com.guardsms.domain.model.SmsMessage
import com.guardsms.presentation.common.MessageAdapter
import com.guardsms.presentation.common.MessagePreviewDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MessagesViewModel by viewModels()

    private val adapter = MessageAdapter(
        onPreview = { msg -> showPreview(msg) },
        onRedFlag = { msg -> viewModel.redFlagMessage(msg) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MessagesFragment.adapter
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadMessages() }
        binding.btnRefresh.setOnClickListener { viewModel.loadMessages() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.setFilter(
                    when (tab.position) {
                        1 -> FilterTab.FLAGGED
                        2 -> FilterTab.SAFE
                        else -> FilterTab.ALL
                    }
                )
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    adapter.submitList(state.messages)
                    binding.emptyState.isVisible = state.messages.isEmpty() && !state.isLoading
                    binding.rvMessages.isVisible = state.messages.isNotEmpty()

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

    private fun showPreview(message: SmsMessage) {
        MessagePreviewDialog(
            message = message,
            onRedFlag = { viewModel.redFlagMessage(message) }
        ).show(parentFragmentManager, "preview")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
