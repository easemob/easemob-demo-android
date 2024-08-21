package com.hyphenate.chatdemo.ui.group

import androidx.core.content.ContextCompat
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.callkit.CallKitManager
import com.hyphenate.chatdemo.common.ReportHelper
import com.hyphenate.chatdemo.common.extensions.internal.parse
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatGroup
import com.hyphenate.easeui.feature.group.EaseGroupDetailActivity
import com.hyphenate.easeui.model.EaseMenuItem

class ChatGroupDetailActivity :EaseGroupDetailActivity(){

    override fun getDetailItem(): MutableList<EaseMenuItem>? {
        val list = super.getDetailItem()
        val videoItem = EaseMenuItem(
            title = getString(R.string.menu_video_call),
            resourceId = R.drawable.ease_video_camera,
            menuId = R.id.group_item_video_call,
            titleColor = ContextCompat.getColor(this, com.hyphenate.easeui.R.color.ease_color_primary),
            order = 2,
            resourceTintColor = ContextCompat.getColor(this, com.hyphenate.easeui.R.color.ease_color_primary)
        )
        list?.add(videoItem)
        return list
    }

    override fun onMenuItemClick(item: EaseMenuItem?, position: Int): Boolean {
        item?.let {menu->
            return when(menu.menuId){
                R.id.group_item_video_call -> {
                    CallKitManager.startConferenceCall(this, groupId)
                    true
                }

                else -> {
                    super.onMenuItemClick(item, position)
                }
            }
        }
        return false
    }

    override fun fetchGroupDetailSuccess(group: ChatGroup) {
        EaseIM.updateGroupInfo(listOf(group.parse()))
        super.fetchGroupDetailSuccess(group)
    }

    override fun getBottomSheetMenu(): MutableList<EaseMenuItem>? {
        val menu = super.getBottomSheetMenu()
        menu?.add( 0,EaseMenuItem(
            menuId = R.id.contact_complaint,
            title = getString(R.string.demo_report_title),
            titleColor = ContextCompat.getColor(this, com.hyphenate.easeui.R.color.ease_color_primary),
        ))
        return menu
    }

    override fun simpleMenuItemClickListener(position: Int, menu: EaseMenuItem) {
        super.simpleMenuItemClickListener(position, menu)
        if (menu.menuId == R.id.contact_complaint){
            ReportHelper.openEmailClient(this,groupId)
        }
    }
}