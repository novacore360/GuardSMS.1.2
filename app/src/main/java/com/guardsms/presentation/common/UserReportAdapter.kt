package com.guardsms.presentation.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.guardsms.databinding.ItemFlaggedDomainBinding
import com.guardsms.domain.model.UserReport

class UserReportAdapter : ListAdapter<UserReport, UserReportAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemFlaggedDomainBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemFlaggedDomainBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding
        b.tvDomain.text = item.content.take(50)
        b.tvThreatType.text = item.reportType.replaceFirstChar { it.uppercase() } + " — " +
            item.threatType.replace("_", " ").replaceFirstChar { it.uppercase() }
        b.tvReports.text = item.status.replaceFirstChar { it.uppercase() }
        b.chipVerified.text = "Submitted"
        b.chipVerified.visibility = android.view.View.VISIBLE
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<UserReport>() {
            override fun areItemsTheSame(a: UserReport, b: UserReport) = a.id == b.id
            override fun areContentsTheSame(a: UserReport, b: UserReport) = a == b
        }
    }
}
