@file:Suppress("unused")

package io.github.toyota32k.binder

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.utils.Disposer
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.disposableObserve
import kotlinx.coroutines.flow.Flow

open class HeadlessBinding<T>(val data: LiveData<T>, callback:((T?)->Unit)?=null) : IBinding {
    private val observed = Disposer()
    protected var onValueChanged:((T?)->Unit)? = callback

    fun setCallback(callback: ((T?) -> Unit)?) {
        onValueChanged = callback
    }

    override val mode: BindingMode = BindingMode.OneWay
    override fun dispose() {
        observed.dispose()
    }

    fun connect(owner:LifecycleOwner) {
        observed + data.disposableObserve(owner) { onValueChanged?.invoke(it) }
    }

    companion object {
        fun <T> create(owner:LifecycleOwner, data:LiveData<T>, callback:(T?)->Unit): HeadlessBinding<T> {
            return HeadlessBinding(data, callback).apply { connect(owner) }
        }
    }
}

fun <T> Binder.headlessBinding(owner:LifecycleOwner, data:LiveData<T>, callback:(T?)->Unit): Binder
        = add(HeadlessBinding.create(owner, data, callback))
fun <T> Binder.headlessNonnullBinding(owner:LifecycleOwner, data:LiveData<T>, callback:(T)->Unit): Binder
        = add(HeadlessBinding.create(owner, data) { if (it != null) callback(it) })
fun <T> Binder.headlessBinding(owner:LifecycleOwner, data: Flow<T>, callback:(T?)->Unit): Binder
        = add(HeadlessBinding.create(owner, data.asLiveData(), callback))
fun <T> Binder.headlessNonnullBinding(owner:LifecycleOwner, data:Flow<T>, callback:(T)->Unit): Binder
        = add(HeadlessBinding.create(owner, data.asLiveData()) { if (it != null) callback(it) })


fun <T> Binder.headlessBinding(data:LiveData<T>, callback:(T?)->Unit): Binder
        = add(HeadlessBinding.create(this.requireOwner, data, callback))
fun <T> Binder.headlessNonnullBinding(data:LiveData<T>, callback:(T)->Unit): Binder
        = add(HeadlessBinding.create(this.requireOwner, data) { if (it != null) callback(it) })
fun <T> Binder.headlessBinding(data:Flow<T>, callback:(T?)->Unit): Binder
        = add(HeadlessBinding.create(this.requireOwner, data.asLiveData(), callback))
fun <T> Binder.headlessNonnullBinding(data:Flow<T>, callback:(T)->Unit): Binder
        = add(
    HeadlessBinding.create(
        this.requireOwner,
        data.asLiveData()
    ) { if (it != null) callback(it) })
