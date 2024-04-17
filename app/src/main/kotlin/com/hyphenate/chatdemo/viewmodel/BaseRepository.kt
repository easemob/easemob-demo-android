package com.hyphenate.chatdemo.viewmodel

import android.content.Context
import com.hyphenate.chatdemo.DemoApplication

open class BaseRepository {
    fun getContext(): Context {
        return DemoApplication.getInstance()
    }
}