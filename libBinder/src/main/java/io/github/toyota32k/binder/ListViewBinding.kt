@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import android.widget.ListAdapter
import android.widget.ListView
import androidx.annotation.LayoutRes
import io.github.toyota32k.binder.list.ListViewAdapter
import io.github.toyota32k.binder.list.MutableListViewAdapter
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.utils.IDisposable

class ListViewBinding(val view: ListView, adapter:ListAdapter) : IBinding {
    override val mode: BindingMode = BindingMode.OneWay

    init {
        view.adapter = adapter
    }

    override fun dispose() {
        val adapter = view.adapter as? IDisposable ?: return
        view.adapter = null
        adapter.dispose()
    }

    companion object {
        fun <T> create(view:ListView, list:List<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): ListViewBinding {
            val adapter = ListViewAdapter(list, itemLayout, bindView)
            return ListViewBinding(view, adapter)
        }

        fun <T> create(view:ListView, list: ObservableList<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): ListViewBinding {
            val adapter = MutableListViewAdapter(list, itemLayout, bindView)
            return ListViewBinding(view, adapter)
        }
    }
}

fun <T> Binder.listViewBinding(view:ListView, list:List<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): Binder
    = add(ListViewBinding.create(view, list, itemLayout, bindView))
fun <T> Binder.listViewBinding(view:ListView, list: ObservableList<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): Binder
    = add(ListViewBinding.create(view, list, itemLayout, bindView))

