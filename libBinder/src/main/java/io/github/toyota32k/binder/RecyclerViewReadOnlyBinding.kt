package io.github.toyota32k.binder

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.github.toyota32k.binder.list.RecyclerViewReadOnlyAdapter
import io.github.toyota32k.binder.list.RecyclerViewViewBindingReadOnlyAdapter
import io.github.toyota32k.utils.IDisposable

class RecyclerViewReadOnlyBinding<T> private constructor(
    val view: RecyclerView
) : IBinding {
    override val mode: BindingMode = BindingMode.OneWay
    override fun dispose() {
        (view.adapter as? IDisposable)?.dispose()
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: RecyclerView, listSource: LiveData<Collection<T>>, itemViewLayoutId:Int, bindView:(Binder, View, T)->Unit) : RecyclerViewReadOnlyBinding<T> {
            view.adapter = RecyclerViewReadOnlyAdapter(owner,listSource,itemViewLayoutId,bindView)
            return RecyclerViewReadOnlyBinding(view)
        }
        fun <T,B: ViewBinding> create(owner: LifecycleOwner, view: RecyclerView, listSource: LiveData<Collection<T>>, inflate:(parent:ViewGroup)->B, bindView:(controls:B, binder: Binder, view: View, item:T)->Unit) : RecyclerViewReadOnlyBinding<T> {
            view.adapter = RecyclerViewViewBindingReadOnlyAdapter(owner, listSource, inflate, bindView)
            return RecyclerViewReadOnlyBinding(view)
        }
    }
}

fun Binder.recyclerViewReadOnlyBinding(owner: LifecycleOwner, view: RecyclerView, listSource: LiveData<Collection<Any>>, itemViewLayoutId:Int, bindView:(Binder, View, Any)->Unit) : Binder {
    return add(RecyclerViewReadOnlyBinding.create(owner, view, listSource, itemViewLayoutId, bindView))
}