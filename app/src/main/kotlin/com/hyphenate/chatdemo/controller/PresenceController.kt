package com.hyphenate.chatdemo.controller

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.interfaces.IPresenceResultView
import com.hyphenate.chatdemo.utils.EasePresenceUtil
import com.hyphenate.chatdemo.viewmodel.PresenceViewModel
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatPresence
import com.hyphenate.easeui.common.dialog.CustomDialog
import com.hyphenate.easeui.common.dialog.SimpleListSheetDialog
import com.hyphenate.easeui.interfaces.SimpleListSheetItemClickListener
import com.hyphenate.easeui.model.ChatUIKitMenuItem

class PresenceController(
    private val context: Context,
    private val presenceVideModel:PresenceViewModel,
): IPresenceResultView {
    init {
        presenceVideModel.attachView(this)
    }
    private var presenceDialog: SimpleListSheetDialog? = null
    private var currentPresence:String? = null

    fun showPresenceStatusDialog(presence: ChatPresence?){
        val tag = EasePresenceUtil.getPresenceString(context,presence)
        if (
            tag == context.getString(R.string.uikit_presence_online) ||
            tag == context.getString(R.string.uikit_presence_busy) ||
            tag == context.getString(R.string.uikit_presence_do_not_disturb) ||
            tag == context.getString(R.string.ease_presence_away) ||
            tag == context.getString(R.string.uikit_presence_offline) || tag.isEmpty()
        ){ }else{
            currentPresence = tag
        }

        presenceDialog = SimpleListSheetDialog(
            context = context,
            itemList = defaultItems(),
            itemListener = object : SimpleListSheetItemClickListener {
                override fun onItemClickListener(position: Int, menu: ChatUIKitMenuItem) {
                    simpleMenuItemClickListener(position, menu)
                }
            })
        if (context is FragmentActivity){
            context.supportFragmentManager.let { presenceDialog?.show(it,"presence_status_dialog") }
        }else if (context is  Fragment){
            context.parentFragmentManager.let { presenceDialog?.show(it,"presence_status_dialog") }
        }
    }

    private fun simpleMenuItemClickListener(position: Int,menu: ChatUIKitMenuItem){
        when(menu.menuId){
            R.id.presence_status_online -> {
                presenceVideModel.publishPresence("")
                presenceDialog?.dismiss()
            }
            R.id.presence_status_busy -> {
                presenceVideModel.publishPresence(DemoConstant.PRESENCE_BUSY)
                presenceDialog?.dismiss()
            }
            R.id.presence_status_do_not_disturb -> {
                presenceVideModel.publishPresence(DemoConstant.PRESENCE_DO_NOT_DISTURB)
                presenceDialog?.dismiss()
            }
            R.id.presence_status_away -> {
                presenceVideModel.publishPresence(DemoConstant.PRESENCE_AWAY)
                presenceDialog?.dismiss()
            }
            R.id.presence_status_custom -> {
                presenceDialog?.dismiss()
                showCustomDialog()
            }
            else -> {}
        }
    }

    private fun defaultItems():MutableList<ChatUIKitMenuItem>{
        return mutableListOf(
            ChatUIKitMenuItem(
                menuId = R.id.presence_status_online,
                title = context.getString(R.string.uikit_presence_online),
                titleColor = ContextCompat.getColor(context, com.hyphenate.easeui.R.color.ease_color_primary)
            ),
            ChatUIKitMenuItem(
                menuId = R.id.presence_status_busy,
                title = context.getString(R.string.uikit_presence_busy),
                titleColor = ContextCompat.getColor(context, com.hyphenate.easeui.R.color.ease_color_primary)
            ),
            ChatUIKitMenuItem(
                menuId = R.id.presence_status_away,
                title = context.getString(R.string.ease_presence_away),
                titleColor = ContextCompat.getColor(context, com.hyphenate.easeui.R.color.ease_color_primary)
            ),
            ChatUIKitMenuItem(
                menuId = R.id.presence_status_do_not_disturb,
                title = context.getString(R.string.uikit_presence_do_not_disturb),
                titleColor = ContextCompat.getColor(context, com.hyphenate.easeui.R.color.ease_color_primary)
            ),
            ChatUIKitMenuItem(
                menuId = R.id.presence_status_custom,
                title = currentPresence?:context.getString(R.string.uikit_presence_custom),
                titleColor = ContextCompat.getColor(context, com.hyphenate.easeui.R.color.ease_color_primary)
            )
        )
    }

    private fun showCustomDialog(){
        val customDialog = CustomDialog(
            context = context,
            title = context.getString(R.string.presence_dialog_title),
            inputHint = context.getString(R.string.presence_dialog_input_hint),
            isEditTextMode = true,
            onInputModeConfirmListener = {
                presenceVideModel.publishPresence(it)
            }
        )
        customDialog.show()
    }

    override fun onPublishPresenceSuccess() {
        super.onPublishPresenceSuccess()
        ChatLog.e("ChatPresenceController","onPublishPresenceSuccess")
    }

    override fun onPublishPresenceFail(code: Int, message: String?) {
        super.onPublishPresenceFail(code, message)
        ChatLog.e("ChatPresenceController","onPublishPresenceFail $code $message")
    }

}