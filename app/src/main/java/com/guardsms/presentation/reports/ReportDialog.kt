package com.guardsms.presentation.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.guardsms.R
import com.guardsms.databinding.DialogReportBinding

class ReportDialog(
    private val onSubmitDomain: (String, String, String?) -> Unit,
    private val onSubmitUrl: (String, String, String?) -> Unit,
    private val onSubmitMessage: (String, String, String?) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogReportBinding? = null
    private val binding get() = _binding!!

    private val threatTypes = listOf("phishing", "fraud", "malware", "spam", "other")
    private var selectedType = "domain"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Threat type dropdown
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
            threatTypes.map { it.replaceFirstChar { c -> c.uppercase() } })
        binding.etThreatType.setAdapter(adapter)
        binding.etThreatType.setText(adapter.getItem(0), false)

        // Report type chips
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedType = when (checkedIds.firstOrNull()) {
                R.id.chip_url -> { binding.tilContent.hint = "Full URL (e.g. https://malicious.tk/claim)"; "url" }
                R.id.chip_message -> { binding.tilContent.hint = "Paste the suspicious message text"; "message" }
                else -> { binding.tilContent.hint = "Domain name (e.g. malicious-site.tk)"; "domain" }
            }
        }

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSubmit.setOnClickListener {
            val content = binding.etContent.text?.toString()?.trim() ?: ""
            val threat = threatTypes[
                threatTypes.indexOfFirst { it.lowercase() == binding.etThreatType.text.toString().lowercase() }
                    .takeIf { it >= 0 } ?: 0
            ]
            val desc = binding.etDescription.text?.toString()?.trim()?.ifBlank { null }

            if (content.isBlank()) {
                binding.tvError.text = "Please enter the ${selectedType} to report"
                binding.tvError.isVisible = true
                return@setOnClickListener
            }

            when (selectedType) {
                "domain" -> onSubmitDomain(content, threat, desc)
                "url" -> onSubmitUrl(content, threat, desc)
                "message" -> onSubmitMessage(content, threat, desc)
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
