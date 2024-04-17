package com.hyphenate.chatdemo.ui.me

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.bean.LanguageType
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.databinding.DemoActivityCurrencyBinding
import com.hyphenate.easeui.base.EaseBaseActivity
import com.hyphenate.easeui.common.helper.EasePreferenceManager
import com.hyphenate.easeui.widget.EaseSwitchItemView

class CurrencyActivity:EaseBaseActivity<DemoActivityCurrencyBinding>(),
    EaseSwitchItemView.OnCheckedChangeListener, View.OnClickListener {
    private var targetLanguage:String? = ""
    override fun getViewBinding(inflater: LayoutInflater): DemoActivityCurrencyBinding {
        return DemoActivityCurrencyBinding.inflate(inflater)
    }
    companion object {
        private const val RESULT_CHOICE_LANGUAGE = 100
        private const val RESULT_TARGET_LANGUAGE = "target_language"
    }

    private val launcherToUpdateLanguage: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> onActivityResult(result, RESULT_CHOICE_LANGUAGE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initListener()
    }

    private fun initView(){
        initSwitch()
        initLanguage()
    }

    private fun initListener(){
        binding.let {
            it.switchItemDark.setOnCheckedChangeListener(this)
            it.arrowItemFeature.setOnClickListener(this)
            it.arrowItemLanguage.setOnClickListener(this)
            it.titleBar.setNavigationOnClickListener{
                mContext.onBackPressed()
            }
        }
    }

    private fun initSwitch(){
        val isBlack = DemoHelper.getInstance().getDataModel().getBoolean(DemoConstant.IS_BLACK_THEME)
        if (isBlack){
            binding.switchItemDark.setChecked(true)
        }else{
            binding.switchItemDark.setChecked(false)
        }
        binding.switchItemDark.setSwitchTarckDrawable(com.hyphenate.easeui.R.drawable.ease_switch_track_selector)
        binding.switchItemDark.setSwitchThumbDrawable(com.hyphenate.easeui.R.drawable.ease_switch_thumb_selector)
    }

    private fun initLanguage(){
        val tagLanguage = EasePreferenceManager.getInstance().getString(DemoConstant.TARGET_LANGUAGE)
        if (tagLanguage == LanguageType.ZH.value){
            targetLanguage = getString(R.string.currency_language_zh_cn)
        }else if (tagLanguage == LanguageType.EN.value){
            targetLanguage = getString(R.string.currency_language_en)
        }
        updateLanguage()
    }

    override fun onCheckedChanged(buttonView: EaseSwitchItemView?, isChecked: Boolean) {
        when(buttonView?.id){
            R.id.switch_item_dark ->{
                changeTheme(isChecked)
            }
            else -> {}
        }
    }

    private fun onActivityResult(result: ActivityResult, requestCode: Int) {
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            when (requestCode) {
                RESULT_CHOICE_LANGUAGE ->{
                    data?.let {
                        if (it.hasExtra(RESULT_TARGET_LANGUAGE)){
                            targetLanguage = it.getStringExtra(RESULT_TARGET_LANGUAGE)
                            updateLanguage()
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun changeTheme(isChecked: Boolean){
        AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        DemoHelper.getInstance().getDataModel().putBoolean(DemoConstant.IS_BLACK_THEME, isChecked)
    }

    private fun updateLanguage(){
        binding.let {
            if (targetLanguage.isNullOrEmpty()) return
            it.arrowItemLanguage.setContent(targetLanguage)
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.arrow_item_feature -> {
                startActivity(Intent(this@CurrencyActivity,FeaturesActivity::class.java))
            }
            R.id.arrow_item_language -> {
                launcherToUpdateLanguage.launch(Intent(
                    this@CurrencyActivity,
                    LanguageSettingActivity::class.java))
            }
            else -> {}
        }
    }


}