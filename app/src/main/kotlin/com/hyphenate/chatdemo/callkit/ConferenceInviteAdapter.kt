package com.hyphenate.chatdemo.callkit

import android.view.LayoutInflater
import android.view.ViewGroup
import com.hyphenate.easeui.base.EaseBaseRecyclerViewAdapter
import com.hyphenate.easeui.databinding.EaseLayoutGroupSelectContactBinding
import com.hyphenate.easeui.feature.search.interfaces.OnContactSelectListener
import com.hyphenate.easeui.model.EaseUser

class ConferenceInviteAdapter(private val groupId: String?): EaseBaseRecyclerViewAdapter<EaseUser>() {
    private var selectedListener: OnContactSelectListener? = null
    private var existMembers:MutableList<String> = mutableListOf()
    private val checkedList:MutableList<String> = mutableListOf()

    override fun getViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<EaseUser> {
        return ConferenceMemberSelectViewHolder(groupId, checkedList,
                EaseLayoutGroupSelectContactBinding.inflate(LayoutInflater.from(parent.context),
                    parent, false)
            )
    }

    override fun onBindViewHolder(holder: ViewHolder<EaseUser>, position: Int) {
        if (holder is ConferenceMemberSelectViewHolder){
            holder.setSelectedMembers(existMembers)
            holder.setCheckBoxSelectListener(selectedListener)
        }
        super.onBindViewHolder(holder, position)
    }

    fun setExistMembers(existMembers:MutableList<String>){
        this.existMembers = existMembers
        notifyDataSetChanged()
    }

    /**
     * Set the listener for the checkbox selection.
     */
    fun setCheckBoxSelectListener(listener: OnContactSelectListener){
        this.selectedListener = listener
        notifyDataSetChanged()
    }

}