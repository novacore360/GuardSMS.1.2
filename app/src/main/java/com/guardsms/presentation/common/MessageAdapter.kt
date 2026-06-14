package com.guardsms.presentation.common

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.guardsms.R
import com.guardsms.databinding.ItemMessageBinding
import com.guardsms.domain.model.MessageStatus
import com.guardsms.domain.model.SmsMessage
import com.guardsms.domain.model.ThreatLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessageAdapter(
    private val onPreview: (SmsMessage) -> Unit,
    private val onRedFlag: (SmsMessage) -> Unit
) : ListAdapter<SmsMessage, MessageAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = getItem(position)
        val b = holder.binding
        val ctx = holder.itemView.context

        b.tvSender.text = msg.senderName ?: msg.sender
        b.tvPreview.text = msg.body
        b.chipNotContact.isVisible = !msg.isContact

        // Status chip
        val (label, textColor, bgColor) = when {
            msg.status == MessageStatus.BLOCKED.name -> Triple("Blocked", ctx.getColor(R.color.blocked), ctx.getColor(R.color.blocked_bg))
            msg.isRedflagged && msg.threatLevel == ThreatLevel.CRITICAL.name -> Triple("Critical", ctx.getColor(R.color.critical), ctx.getColor(R.color.critical_bg))
            msg.isRedflagged && msg.threatLevel == ThreatLevel.HIGH.name -> Triple("High Risk", ctx.getColor(R.color.danger), ctx.getColor(R.color.danger_bg))
            msg.isRedflagged -> Triple("Flagged", ctx.getColor(R.color.warning), ctx.getColor(R.color.warning_bg))
            else -> Triple("Safe", ctx.getColor(R.color.safe), ctx.getColor(R.color.safe_bg))
        }
        b.chipStatus.text = label
        b.chipStatus.setTextColor(textColor)
        b.chipStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(bgColor)

        // Threat reason
        if (!msg.threatReason.isNullOrBlank() && msg.isRedflagged) {
            b.tvThreatReason.text = "⚠ ${msg.threatReason?.take(80)}"
            b.tvThreatReason.isVisible = true
        } else {
            b.tvThreatReason.isVisible = false
        }

        // Links
        if (msg.extractedLinks.isNotEmpty()) {
            b.scrollLinks.isVisible = true
            b.llLinks.removeAllViews()
            msg.extractedLinks.take(3).forEach { link ->
                val chip = Chip(ctx).apply {
                    text = link.take(30)
                    textSize = 10f
                    chipMinHeight = 24f
                    setChipBackgroundColorResource(R.color.surface_variant)
                    setTextColor(ctx.getColor(R.color.text_secondary))
                    isClickable = false
                }
                b.llLinks.addView(chip)
            }
        } else {
            b.scrollLinks.isVisible = false
        }

        // Time
        b.tvTime.text = try {
            val instant = Instant.parse(msg.receivedAt)
            DateTimeFormatter.ofPattern("MMM d, h:mm a")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) { msg.receivedAt.take(16) }

        // Red flag button visibility
        b.btnRedflag.isVisible = !msg.isRedflagged
        b.btnRedflag.setOnClickListener { onRedFlag(msg) }
        b.btnPreview.setOnClickListener { onPreview(msg) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SmsMessage>() {
            override fun areItemsTheSame(a: SmsMessage, b: SmsMessage) = a.id == b.id
            override fun areContentsTheSame(a: SmsMessage, b: SmsMessage) = a == b
        }
    }
}
