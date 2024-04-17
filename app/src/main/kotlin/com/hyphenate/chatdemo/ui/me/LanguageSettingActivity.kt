package com.hyphenate.chatdemo.ui.me

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.bean.Language
import com.hyphenate.chatdemo.bean.LanguageType
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.common.LanguageUtil
import com.hyphenate.chatdemo.databinding.DemoActivityLanguageBinding
import com.hyphenate.chatdemo.interfaces.LanguageListItemSelectListener
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.base.EaseBaseActivity
import com.hyphenate.easeui.common.helper.EasePreferenceManager
import java.util.Locale

class LanguageSettingActivity:EaseBaseActivity<DemoActivityLanguageBinding>() {
    private var tagList:MutableList<Language> = mutableListOf()
    private var languageAdapter:LanguageAdapter? = null
    private var currentTag:String = ""
    private var languageCode:String = ""
    private var selectedPosition: Int = -1

    override fun getViewBinding(inflater: LayoutInflater): DemoActivityLanguageBinding {
        return DemoActivityLanguageBinding.inflate(inflater)
    }

    companion object {
        private const val RESULT_TARGET_LANGUAGE = "target_language"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initListener()
    }

    fun initView(){
        defaultLanguage()
        binding.let {
            languageAdapter = LanguageAdapter(tagList)
            val layoutManager = LinearLayoutManager(mContext)
            it.rlSheetList.layoutManager = layoutManager
            it.rlSheetList.adapter = this.languageAdapter
            val tagLanguage = EasePreferenceManager.getInstance().getString(DemoConstant.TARGET_LANGUAGE)
            if (tagLanguage.isNullOrEmpty()){
                val index = tagList.indexOfFirst { language-> language.type.value == Locale.getDefault().language }
                languageAdapter?.setSelectPosition(index)
            }else{
                tagLanguage.let { tag->
                    val index = tagList.indexOfFirst { language-> language.type.value == tag }
                    languageCode = tag
                    selectedPosition = index
                    languageAdapter?.setSelectPosition(index)
                }
            }
            updateConfirm()
        }
    }

    fun initListener(){
        languageAdapter?.setLanguageListItemClickListener(object : LanguageListItemSelectListener{
            override fun onSelectListener(position: Int,language:Language) {
                currentTag = language.tag
                languageCode = language.type.value
                selectedPosition = position
                updateConfirm()
            }
        })
        binding.titleBar.setOnMenuItemClickListener { item->
            when (item.itemId){
                R.id.action_language_confirm -> {
                    chengApplicationLanguage()
                }
                else -> {}
            }
            true
        }
        binding.titleBar.setNavigationOnClickListener{
            mContext.onBackPressed()
        }

    }

    fun updateConfirm(){
        binding.let {
            it.titleBar.getToolBar().let { tb ->
                tb.menu.findItem(R.id.action_language_confirm)?.let { menuItem->
                    menuItem.isVisible = true
                    menuItem.title?.let { tl ->
                        val spannable = SpannableString(menuItem.title)
                        spannable.setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(mContext,
                                if (selectedPosition != -1){
                                    com.hyphenate.easeui.R.color.ease_color_primary
                                }else{
                                    com.hyphenate.easeui.R.color.ease_color_on_background_high
                                })
                            )
                            , 0, tl.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        menuItem.title = spannable
                    }
                }
            }
        }
    }

    private fun chengApplicationLanguage(){
        LanguageUtil.changeLanguage(languageCode)
        EaseIM.getConfig()?.chatConfig?.targetTranslationLanguage = languageCode
        EasePreferenceManager.getInstance().putString(DemoConstant.TARGET_LANGUAGE, languageCode)
        val resultIntent = Intent()
        resultIntent.putExtra(RESULT_TARGET_LANGUAGE,currentTag)
        setResult(RESULT_OK,resultIntent)
    }

    private fun defaultLanguage(){
        tagList = mutableListOf(
            Language(LanguageType.ZH,getString(R.string.currency_language_zh_cn)),
            Language(LanguageType.EN,getString(R.string.currency_language_en))
        )
    }

    class LanguageAdapter(
        private val languageList: MutableList<Language>?,
    ) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>(){
        private lateinit var listener: LanguageListItemSelectListener
        private var selectPosition:Int = -1

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.language_tag)
            val tagCb: CheckBox = itemView.findViewById(R.id.language_cb)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.ease_layout_language_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return languageList?.size ?: 0
        }

        override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
            languageList?.let {
                holder.textView.text = it[position].tag
                holder.tagCb.isChecked = selectPosition == position

                holder.tagCb.isClickable = false

                holder.itemView.setOnClickListener {
                    if (position == selectPosition) {
                        holder.tagCb.isChecked = false
                        selectPosition = -1
                    }else{
                        holder.tagCb.isChecked = true
                        selectPosition = position
                    }
                    notifyDataSetChanged()

                    if (holder.tagCb.isChecked){
                        listener.onSelectListener(position,languageList[selectPosition])
                    }
                }
            }
        }

        fun setSelectPosition(selectPosition:Int){
            this.selectPosition = selectPosition
            notifyDataSetChanged()
        }

        fun setLanguageListItemClickListener(listener: LanguageListItemSelectListener){
            this.listener = listener
        }

    }
}