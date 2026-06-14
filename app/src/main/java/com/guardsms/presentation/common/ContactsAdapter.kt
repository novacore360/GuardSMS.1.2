package com.guardsms.presentation.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.guardsms.databinding.ItemContactBinding
import com.guardsms.domain.model.Contact

class ContactsAdapter : ListAdapter<Contact, ContactsAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val contact = getItem(position)
        val b = holder.binding
        b.tvName.text = contact.name
        b.tvPhone.text = contact.phone
        b.tvAvatar.text = contact.name.take(1).uppercase()
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
            override fun areContentsTheSame(a: Contact, b: Contact) = a == b
        }
    }
}
