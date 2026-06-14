package com.guardsms.presentation.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.guardsms.databinding.ItemFlaggedDomainBinding
import com.guardsms.domain.model.FlaggedDomain

class FlaggedDomainAdapter : ListAdapter<FlaggedDomain, FlaggedDomainAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemFlaggedDomainBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemFlaggedDomainBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding
        b.tvDomain.text = item.domain
        b.tvThreatType.text = item.threatType.replace("_", " ").replaceFirstChar { it.uppercase() }
        b.tvReports.text = "${item.reportCount} report${if (item.reportCount != 1) "s" else ""}"
        b.chipVerified.isVisible = item.isVerified
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FlaggedDomain>() {
            override fun areItemsTheSame(a: FlaggedDomain, b: FlaggedDomain) = a.id == b.id
            override fun areContentsTheSame(a: FlaggedDomain, b: FlaggedDomain) = a == b
        }
    }
}
