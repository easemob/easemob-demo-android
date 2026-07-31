package com.hyphenate.chatdemo.common

import android.content.Context
import android.content.SharedPreferences

internal object PreferenceManager {

    private var mSharedPreferences: SharedPreferences? = null
    private const val PREF_NAME = "saveInfo"

    @Synchronized
    fun init(context: Context) {
        if (mSharedPreferences == null) {
            mSharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Save the value to the preference.
     * @param key The key of the preference.
     * @param value The value of the preference.
     */
    fun <T> putValue(key: String, value: T) {
        val editor = mSharedPreferences?.edit()
        when (value) {
            is String -> editor?.putString(key, value)
            is Int -> editor?.putInt(key, value)
            is Boolean -> editor?.putBoolean(key, value)
            is Float -> editor?.putFloat(key, value)
            is Long -> editor?.putLong(key, value)
            else -> editor?.putString(key, value.toString())
        }
        editor?.apply()
    }

    /**
     * Save multiple string values in one preferences transaction.
     */
    fun putStringValues(values: Map<String, String>) {
        val editor = mSharedPreferences?.edit() ?: return
        values.forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.apply()
    }

    /**
     * Remove multiple preference values in one preferences transaction.
     */
    fun removeValues(vararg keys: String) {
        val editor = mSharedPreferences?.edit() ?: return
        keys.forEach(editor::remove)
        editor.apply()
    }

    /**
     * Get the value from the preference.
     * @param key The key of the preference.
     * @param defValue The default value of the preference.
     */
    fun <T> getValue(key: String, defValue: T): T {
        val value = when (defValue) {
            is String -> mSharedPreferences?.getString(key, defValue)
            is Int -> mSharedPreferences?.getInt(key, defValue)
            is Boolean -> mSharedPreferences?.getBoolean(key, defValue)
            is Float -> mSharedPreferences?.getFloat(key, defValue)
            is Long -> mSharedPreferences?.getLong(key, defValue)
            else -> mSharedPreferences?.getString(key, defValue.toString())
        }
        return value as T
    }

}
