@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.SpinnerAdapter
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.toyota32k.binder.list.ListViewAdapter
import io.github.toyota32k.binder.list.MutableListViewAdapter
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow

class SpinnerBinding(val view: Spinner, adapter: SpinnerAdapter) : IBinding {
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
        fun <T> create(view:Spinner, list:List<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): SpinnerBinding {
            val adapter = ListViewAdapter(list, itemLayout, bindView)
            return SpinnerBinding(view, adapter)
        }

        fun <T> create(view:Spinner, list: ObservableList<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): SpinnerBinding {
            val adapter = MutableListViewAdapter(list, itemLayout, bindView)
            return SpinnerBinding(view, adapter)
        }
    }
}

open class SpinnerSelectionBinding<T>(
    override val data: LiveData<T>,
    mode: BindingMode,
): BaseBinding<T>(mode), AdapterView.OnItemSelectedListener {
    protected val spinner: Spinner?
        get() = view as Spinner?

    fun connect(owner: LifecycleOwner, view: Spinner) {
        super.connect(owner,view)
        if(mode!= BindingMode.OneWay) {
            view.onItemSelectedListener = this
            if(mode== BindingMode.OneWayToSource||data.value==null) {
                @Suppress("UNCHECKED_CAST")
                mutableData?.value = view.selectedItem as T?
            }
        }
    }

    private fun item2index(item:T):Int {
        val s = spinner ?:  return -1
        for(i in (0 until s.count)) {
            if(item == s.getItemAtPosition(i)) {
                return i
            }
        }
        return -1
    }

    override fun onDataChanged(v: T?) {
        val s = spinner ?: return
        if(v==null) {
            return
        }
        val i = item2index(v)
        if(i<=0) {
            return
        }
        s.setSelection(i)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        @Suppress("UNCHECKED_CAST")
        mutableData?.value = spinner?.getItemAtPosition(position) as T
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        mutableData?.value = null
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: Spinner, data: MutableLiveData<T>, mode: BindingMode = BindingMode.TwoWay) : SpinnerSelectionBinding<T> {
            return SpinnerSelectionBinding(data, mode).apply {connect(owner,view) }
        }
        fun <T> create(owner: LifecycleOwner, view: Spinner, data: MutableStateFlow<T>, mode: BindingMode = BindingMode.TwoWay) : SpinnerSelectionBinding<T> {
            return create(owner, view, data.asMutableLiveData(owner),mode)
        }
    }
}

fun <T> Binder.spinnerBinding(view: Spinner, list:List<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): Binder
        = add(SpinnerBinding.create(view, list, itemLayout, bindView))
fun <T> Binder.spinnerBinding(view: Spinner, list: ObservableList<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): Binder
        = add(SpinnerBinding.create(view, list, itemLayout, bindView))

fun <T> Binder.spinnerSelectionBinding(owner: LifecycleOwner, view: Spinner, data: MutableLiveData<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(SpinnerSelectionBinding.create(owner, view, data, mode))
fun <T> Binder.spinnerSelectionBinding(owner: LifecycleOwner, view: Spinner, data: MutableStateFlow<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(SpinnerSelectionBinding.create(owner, view, data, mode))

fun <T> Binder.spinnerSelectionBinding(view: Spinner, data: MutableLiveData<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(SpinnerSelectionBinding.create(requireOwner, view, data, mode))
fun <T> Binder.spinnerSelectionBinding(view: Spinner, data: MutableStateFlow<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(SpinnerSelectionBinding.create(requireOwner, view, data, mode))
