package com.guardsms.presentation.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.guardsms.R
import com.guardsms.databinding.ItemMessageBinding
import com.guardsms.domain.model.SmsMessage

// Lightweight mini list reusing same item layout but simplified
class MessageMiniAdapter : ListAdapter<SmsMessage, MessageMiniAdapter.VH>(DIFF) {

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

        val (label, textColor, bgColor) = if (msg.isRedflagged)
            Triple("Flagged", ctx.getColor(R.color.danger), ctx.getColor(R.color.danger_bg))
        else
            Triple("Safe", ctx.getColor(R.color.safe), ctx.getColor(R.color.safe_bg))

        b.chipStatus.text = label
        b.chipStatus.setTextColor(textColor)
        b.chipStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(bgColor)
        b.chipNotContact.visibility = android.view.View.GONE
        b.scrollLinks.visibility = android.view.View.GONE
        b.tvThreatReason.visibility = android.view.View.GONE
        b.btnRedflag.visibility = android.view.View.GONE
        b.btnPreview.visibility = android.view.View.GONE
        b.tvTime.text = try {
            val inst = java.time.Instant.parse(msg.receivedAt)
            java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")
                .withZone(java.time.ZoneId.systemDefault()).format(inst)
        } catch (e: Exception) { "" }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SmsMessage>() {
            override fun areItemsTheSame(a: SmsMessage, b: SmsMessage) = a.id == b.id
            override fun areContentsTheSame(a: SmsMessage, b: SmsMessage) = a == b
        }
    }
}
