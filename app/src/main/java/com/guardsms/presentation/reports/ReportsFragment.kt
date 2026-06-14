package com.guardsms.presentation.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.guardsms.databinding.FragmentReportsBinding
import com.guardsms.presentation.common.FlaggedDomainAdapter
import com.guardsms.presentation.common.UserReportAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()

    private val domainAdapter = FlaggedDomainAdapter()
    private val userReportAdapter = UserReportAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = domainAdapter
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }

        binding.btnAddReport.setOnClickListener { showReportDialog() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val tab_enum = if (tab.position == 0) ReportTab.COMMUNITY else ReportTab.MY_REPORTS
                viewModel.setTab(tab_enum)
                binding.rvReports.adapter = if (tab.position == 0) domainAdapter else userReportAdapter
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    domainAdapter.submitList(state.flaggedDomains)
                    userReportAdapter.submitList(state.userReports)

                    state.errorMessage?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearFeedback()
                    }
                    state.successMessage?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearFeedback()
                    }
                }
            }
        }
    }

    private fun showReportDialog() {
        ReportDialog(
            onSubmitDomain = { domain, threat, desc -> viewModel.reportDomain(domain, threat, desc) },
            onSubmitUrl = { url, threat, desc -> viewModel.reportUrl(url, threat, desc) },
            onSubmitMessage = { msg, threat, desc -> viewModel.reportMessage(msg, threat, desc) }
        ).show(parentFragmentManager, "report_dialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
