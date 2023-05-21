package io.github.toyota32k.binder

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import io.github.toyota32k.utils.disposableObserve
import kotlinx.coroutines.flow.Flow

fun <T> Binder.observe(owner: LifecycleOwner, data: LiveData<T>, fn:(value:T)->Unit):Binder
        = add(data.disposableObserve(owner,fn))
fun <T> Binder.observe(data:LiveData<T>,fn:(value:T)->Unit):Binder
        = observe(requireOwner, data, fn)

fun <T> Binder.observe(owner:LifecycleOwner, data: Flow<T>, callback:(value:T)->Unit):Binder
    = add(data.disposableObserve(owner,callback))

fun <T> Binder.observe(data:Flow<T>, callback:(value:T)->Unit):Binder
        = add(data.disposableObserve(requireOwner,callback))