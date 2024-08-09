package com.hyphenate.chatdemo.common.extensions.internal

import com.hyphenate.easeui.widget.EaseSwitchItemView

internal fun EaseSwitchItemView.setSwitchDefaultStyle(){
    setSwitchTarckDrawable(com.hyphenate.easeui.R.drawable.ease_switch_track_selector)
    setSwitchThumbDrawable(com.hyphenate.easeui.R.drawable.ease_switch_thumb_selector)
}