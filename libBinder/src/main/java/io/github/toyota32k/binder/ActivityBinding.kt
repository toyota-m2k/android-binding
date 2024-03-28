package io.github.toyota32k.binder

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import io.github.toyota32k.utils.ActivityOptions
import io.github.toyota32k.utils.ActivityOrientation
import io.github.toyota32k.utils.setOrientation
import io.github.toyota32k.utils.showActionBar
import io.github.toyota32k.utils.showStatusBar
import kotlinx.coroutines.flow.Flow

/**
 * Boolean値とStatusBar表示状態のバインディング
 */
fun Binder.activityStatusBarBinding(owner: LifecycleOwner, show: LiveData<Boolean>)
        = headlessNonnullBinding(owner, show) { (owner as FragmentActivity).showStatusBar(it) }
fun Binder.activityStatusBarBinding(owner: LifecycleOwner, show: Flow<Boolean>)
        = headlessNonnullBinding(owner, show) { (owner as FragmentActivity).showStatusBar(it) }
fun Binder.activityStatusBarBinding(show: LiveData<Boolean>)
        = activityStatusBarBinding(requireOwner, show)
fun Binder.activityStatusBarBinding(show: Flow<Boolean>)
        = activityStatusBarBinding(requireOwner, show)


/**
 * Boolean値とActionBar表示状態のバインディング
 */
fun Binder.activityActionBarBinding(owner: LifecycleOwner, show: LiveData<Boolean>)
        = headlessNonnullBinding(owner, show) { (owner as AppCompatActivity).showActionBar(it) }
fun Binder.activityActionBarBinding(owner: LifecycleOwner, show: Flow<Boolean>)
        = headlessNonnullBinding(owner, show) { (owner as AppCompatActivity).showActionBar(it) }
fun Binder.activityActionBarBinding(show: LiveData<Boolean>)
        = activityActionBarBinding(requireOwner, show)
fun Binder.activityActionBarBinding(show: Flow<Boolean>)
        = activityActionBarBinding(requireOwner, show)

/**
 * Orientationのバインディング
 */
fun Binder.activityOrientationBinding(owner: LifecycleOwner, show: LiveData<ActivityOrientation>)
        = headlessNonnullBinding(owner, show) { (owner as FragmentActivity).setOrientation(it) }
fun Binder.activityOrientationBinding(owner: LifecycleOwner, show: Flow<ActivityOrientation>)
        = headlessNonnullBinding(owner, show) { (owner as FragmentActivity).setOrientation(it) }
fun Binder.activityOrientationBinding(show: LiveData<ActivityOrientation>)
        = activityOrientationBinding(requireOwner, show)
fun Binder.activityOrientationBinding(show: Flow<ActivityOrientation>)
        = activityOrientationBinding(requireOwner, show)

/**
 * ActivityOption のバインディング
 */
fun Binder.activityOptionsBinding(owner: LifecycleOwner, options: LiveData<ActivityOptions>)
        = headlessNonnullBinding(owner, options) { it.apply(owner as FragmentActivity) }

fun Binder.activityOptionsBinding(owner: LifecycleOwner, options: Flow<ActivityOptions>)
        = headlessNonnullBinding(owner, options) { it.apply(owner as FragmentActivity) }

fun Binder.activityOptionsBinding(options: LiveData<ActivityOptions>)
        = activityOptionsBinding(requireOwner, options)

fun Binder.activityOptionsBinding(options: Flow<ActivityOptions>)
        = activityOptionsBinding(requireOwner, options)