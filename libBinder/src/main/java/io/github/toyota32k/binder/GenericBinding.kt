@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow

open class GenericBinding<V,T>(
    override val data: LiveData<T>,
    val action: (V,T?)->Unit
) : BaseBinding<T>(BindingMode.OneWay) where V:View {

    @Suppress("UNCHECKED_CAST")
    override fun onDataChanged(v: T?) {
        val typedView:V = view as? V ?: return
        action(typedView, v)
    }

    companion object {
        fun <V,T> create(owner: LifecycleOwner, view:V, data:LiveData<T>, action:(V,T?)->Unit) : GenericBinding<V, T> where V:View {
            return GenericBinding(data, action).apply { connect(owner,view) }
        }
    }
}

fun <V:View,T> Binder.genericBinding(owner: LifecycleOwner, view:V, data:LiveData<T>, action:(V, T?)->Unit): Binder
        = add(GenericBinding.create(owner, view, data, action))
fun <V:View,T> Binder.genericBinding(view:V, data:LiveData<T>, action:(V, T?)->Unit): Binder
        = add(GenericBinding.create(requireOwner, view, data, action))
fun <V:View,T> Binder.genericBinding(owner: LifecycleOwner, view:V, data: Flow<T>, action:(V, T?)->Unit): Binder
        = add(GenericBinding.create(owner, view, data.asLiveData(), action))
fun <V:View,T> Binder.genericBinding(view:V, data:Flow<T>, action:(V, T?)->Unit): Binder
        = add(GenericBinding.create(requireOwner, view, data.asLiveData(), action))