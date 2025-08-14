package com.hyphenate.chatdemo.callkit

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.hyphenate.chatdemo.MainActivity
import com.hyphenate.chatdemo.base.ActivityState
import com.hyphenate.chatdemo.callkit.extensions.isTargetActivity
import com.hyphenate.chatdemo.common.extensions.internal.makeTaskToFront
import com.hyphenate.chatdemo.ui.login.SplashActivity
import com.hyphenate.callkit.manager.FloatWindow

class CallKitActivityLifecycleCallback: Application.ActivityLifecycleCallbacks, ActivityState {
    private val resumeActivity = mutableListOf<Activity>()
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityList.add(0, activity)
    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        if (!resumeActivity.contains(activity)) {
            resumeActivity.add(activity)
            restartSingleInstanceActivity(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {
        resumeActivity.remove(activity)
        if (resumeActivity.isEmpty()) {
            val a = getOtherTaskSingleInstanceActivity(activity.taskId)
//            if (a != null && a.isTargetActivity() && !FloatWindow.i) {
//                a.makeTaskToFront()
//            }
            Log.e("ActivityLifecycle", "在后台了")
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        activityList.remove(activity)
    }

    override fun current(): Activity? {
        return if (activityList.size > 0) activityList[0] else null
    }

    override val activityList: MutableList<Activity>
        get() = mutableListOf()

    override fun count(): Int {
        return activityList.size
    }

    override val isFront: Boolean
        get() = resumeActivity.size > 0

    private fun getOtherTaskSingleInstanceActivity(taskId: Int): Activity? {
        if (taskId != 0 && activityList.size > 1) {
            for (activity in activityList) {
                if (activity.taskId != taskId) {
                    if (activity.isTargetActivity()) {
                        return activity
                    }
                }
            }
        }
        return null
    }

    /**
     * 用于按下home键，点击图标，检查启动模式是singleInstance，且在activity列表中首位的Activity
     * 下面的方法，专用于解决启动模式是singleInstance, 为开启悬浮框的情况
     * @param activity
     */
    private fun restartSingleInstanceActivity(activity: Activity) {
        val isClickByFloat = activity.intent.getBooleanExtra("isClickByFloat", false)
        if (isClickByFloat) {
            return
        }
        //刚启动，或者从桌面返回app
        if (resumeActivity.size == 1 && resumeActivity[0] is SplashActivity) {
            return
        }
        //至少需要activityList中至少两个activity
        if (resumeActivity.size >= 1 && activityList.size > 1) {
            val a = getOtherTaskSingleInstanceActivity(resumeActivity[0].taskId)
            if (//当前activity和列表中首个activity不相同
                a != null && !a.isFinishing && a !== activity && a.taskId != activity.taskId ) {
                Log.e("ActivityLifecycle", "启动了activity = " + a.javaClass.name)
                activity.startActivity(Intent(activity, a.javaClass))
            }
        }
    }
}