package com.guardsms.presentation.dashboard

import android.content.Intent
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.guardsms.R
import com.guardsms.databinding.FragmentDashboardBinding
import com.guardsms.presentation.auth.AuthActivity
import com.guardsms.presentation.common.FlaggedDomainAdapter
import com.guardsms.presentation.common.MessageMiniAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    private val messageAdapter = MessageMiniAdapter()
    private val domainAdapter = FlaggedDomainAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRecent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messageAdapter
        }
        binding.rvDomains.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = domainAdapter
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDashboard() }

        binding.btnSignOut.setOnClickListener {
            viewModel.signOut {
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
        }

        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.nav_reports)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    binding.tvTotal.text = state.totalMessages.toString()
                    binding.tvFlagged.text = state.flaggedCount.toString()
                    binding.tvSafe.text = state.safeCount.toString()
                    binding.tvBlocked.text = state.blockedCount.toString()

                    messageAdapter.submitList(state.recentMessages)
                    binding.tvEmptyRecent.isVisible = state.recentMessages.isEmpty()

                    domainAdapter.submitList(state.topFlaggedDomains)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
