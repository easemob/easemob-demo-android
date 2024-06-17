package com.hyphenate.chatdemo.callkit

import android.text.TextUtils
import android.view.View
import androidx.viewbinding.ViewBinding
import com.hyphenate.easeui.common.extensions.toProfile
import com.hyphenate.easeui.databinding.EaseLayoutGroupSelectContactBinding
import com.hyphenate.easeui.feature.group.adapter.EaseGroupMemberListAdapter
import com.hyphenate.easeui.feature.group.viewholder.EaseSelectContactViewHolder
import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.model.EaseUser

class ConferenceMemberSelectViewHolder(
    private val groupId: String?,
    private val checkedMemberList: MutableList<String>,
    viewBinding: EaseLayoutGroupSelectContactBinding
): EaseSelectContactViewHolder(viewBinding) {
    private var isShowInitLetter:Boolean = false

    fun setShowInitialLetter(isShow:Boolean){
        this.isShowInitLetter = isShow
    }

    override fun initView(viewBinding: ViewBinding?) {
        if (viewBinding is EaseLayoutGroupSelectContactBinding) {
            viewBinding.cbSelect.isClickable = false
        }
    }

    override fun setData(item: EaseUser?, position: Int) {
        item?.let { user->
            with(viewBinding) {
                itemLayout.setOnClickListener {
                    val isChecked = cbSelect.isChecked
                    cbSelect.isChecked = !isChecked
                    if (!isChecked) {
                        if (!checkedMemberList.contains(user.userId)) {
                            checkedMemberList.add(user.userId)
                        }
                    }else {
                        if (checkedMemberList.contains(user.userId)) {
                            checkedMemberList.remove(user.userId)
                        }
                    }
                    selectedListener?.onContactSelectedChanged(it, user.userId, cbSelect.isChecked)
                }
                cbSelect.isChecked = checkedMemberList.contains(user.userId)
                cbSelect.isSelected = false
                itemLayout.isEnabled = true
                if (checkedList.isNotEmpty() && isContains(checkedList,item.userId)) {
                    cbSelect.isSelected = true
                    itemLayout.isEnabled = false
                }
                val header = user.initialLetter
                letterHeader.visibility = View.GONE
                emPresence.setUserAvatarData(user.toProfile())
                tvName.text = user.nickname ?: user.userId

                groupId?.let { id ->
                    EaseProfile.getGroupMember(id, user.userId)?.let { profile ->
                        emPresence.setUserAvatarData(profile)
                        tvName.text = profile.getRemarkOrName()
                    }
                }

                if (position == 0 || header != null && adapter is EaseGroupMemberListAdapter
                    && header != (adapter as EaseGroupMemberListAdapter).getItem(position - 1)?.initialLetter) {
                    if (!TextUtils.isEmpty(header) && isShowInitLetter) {
                        letterHeader.visibility = View.VISIBLE
                        letterHeader.text = header
                    }
                }
            }
        }
    }
}