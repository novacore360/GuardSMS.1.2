package com.guardsms.presentation.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.guardsms.R
import com.guardsms.databinding.DialogMessagePreviewBinding
import com.guardsms.domain.model.MessageStatus
import com.guardsms.domain.model.SmsMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessagePreviewDialog(
    private val message: SmsMessage,
    private val onRedFlag: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogMessagePreviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogMessagePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()

        binding.tvSender.text = message.senderName ?: message.sender
        binding.tvBody.text = message.body

        binding.tvTime.text = try {
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(message.receivedAt))
        } catch (e: Exception) { message.receivedAt }

        // Status chip
        val (label, textColor, bgColor) = if (message.isRedflagged)
            Triple("Flagged", ctx.getColor(R.color.danger), ctx.getColor(R.color.danger_bg))
        else
            Triple("Safe", ctx.getColor(R.color.safe), ctx.getColor(R.color.safe_bg))

        binding.chipStatus.text = label
        binding.chipStatus.setTextColor(textColor)
        binding.chipStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(bgColor)

        // Extracted links
        if (message.extractedLinks.isNotEmpty()) {
            binding.llLinksSection.isVisible = true
            message.extractedLinks.forEach { link ->
                val tv = TextView(ctx).apply {
                    text = "• $link"
                    textSize = 12f
                    setTextColor(ctx.getColor(R.color.text_link))
                    setPadding(0, 2, 0, 2)
                }
                binding.llLinks.addView(tv)
            }
        }

        // Threat detail
        if (!message.threatReason.isNullOrBlank()) {
            binding.cardThreat.isVisible = true
            binding.tvThreatDetail.text = message.threatReason
        }

        binding.btnRedflag.isVisible = !message.isRedflagged
        binding.btnRedflag.setOnClickListener {
            onRedFlag()
            dismiss()
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
