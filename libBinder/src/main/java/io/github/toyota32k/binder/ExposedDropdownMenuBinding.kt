package io.github.toyota32k.binder

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.toyota32k.utils.lifecycle.asMutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow

interface ILabelResolverCreator<T> {
    fun toLabel(fn:(T)->String)
    fun toItem(fn:(String)->T)
}

/**
 * 非選択状態を許容しないドロップダウンメニューのバインディングクラス
 */
open class ExposedDropdownMenuBinding<T>(
    override val data: LiveData<T>,     // Spinnerの選択状態とバインディングされるデータ
    val list: List<T>,
    mode: BindingMode,
): BaseBinding<T>(mode), AdapterView.OnItemClickListener, ILabelResolverCreator<T> {
    protected var itemToLabel: (T)->String = { it.toString() }
    protected var labelToItem: (String)->T = { label-> list.first { itemToLabel(it)==label } ?: throw IllegalArgumentException("Invalid label: $label") }

    override fun toLabel(fn:(T)->String) {
        itemToLabel = fn
    }
    override fun toItem(fn:(String)->T) {
        labelToItem = fn
    }
    protected val autoCompleteTextView: AutoCompleteTextView?
        get() = view as AutoCompleteTextView?

    lateinit var adapter: ArrayAdapter<String>
    fun connect(owner: LifecycleOwner, view: AutoCompleteTextView, fn:(ILabelResolverCreator<T>.()->Unit)?=null) {
        super.connect(owner,view)
        fn?.invoke(this)
        view.isSaveEnabled = false  // restoreInstanceStateによる復元を無効化 ... こうしておかないと、回転すると adapter の設定が消えてしまう
        adapter = ArrayAdapter<String>(view.context, com.google.android.material.R.layout.support_simple_spinner_dropdown_item, list.map(itemToLabel))
        view.setAdapter(adapter)

        if(mode!= BindingMode.OneWay) {
            view.onItemClickListener = this
            if(mode== BindingMode.OneWayToSource||data.value==null) {
                @Suppress("UNCHECKED_CAST")
                mutableData?.value = labelToItem(view.text.toString())
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val label = adapter.getItem(position) ?: return
        mutableData?.value = labelToItem(label)
    }

    override fun onDataChanged(v: T?) {
        if(v==null) return
        val s = autoCompleteTextView ?: return
        val tx = itemToLabel(v)
        if (tx!=s.text.toString()) {
            s.setText(tx,false)
        }
    }
//    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        val v = list.getOrNull(position) ?: return
//        if(mutableData?.value!=v) {
//            mutableData?.value = v
//        }
//    }
//
//    override fun onNothingSelected(parent: AdapterView<*>?) {
//        mutableData?.value = list[0]
//    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: AutoCompleteTextView, data: LiveData<T>, list: List<T>, mode: BindingMode, fn:(ILabelResolverCreator<T>.()->Unit)?=null) : ExposedDropdownMenuBinding<T> {
            return ExposedDropdownMenuBinding(data, list, mode).apply { connect(owner,view,fn) }
        }
    }
}

fun <T> Binder.exposedDropdownMenuBinding(owner: LifecycleOwner, view: AutoCompleteTextView, data: MutableLiveData<T>, list: List<T>, mode: BindingMode = BindingMode.TwoWay, resolver:(ILabelResolverCreator<T>.()->Unit)?=null)
        = add(ExposedDropdownMenuBinding.create(owner, view, data, list, mode, resolver))
fun <T> Binder.exposedDropdownMenuBinding(view: AutoCompleteTextView, data: MutableLiveData<T>, list: List<T>, mode: BindingMode = BindingMode.TwoWay, resolver:(ILabelResolverCreator<T>.()->Unit)?=null): Binder
        = add(ExposedDropdownMenuBinding.create(requireOwner, view, data, list, mode, resolver))

fun <T> Binder.exposedDropdownMenuBinding(owner: LifecycleOwner, view: AutoCompleteTextView, data: MutableStateFlow<T>, list: List<T>, mode: BindingMode = BindingMode.TwoWay, resolver:(ILabelResolverCreator<T>.()->Unit)?=null): Binder
        = add(ExposedDropdownMenuBinding.create(owner, view, data.asMutableLiveData(owner), list, mode, resolver))
fun <T> Binder.exposedDropdownMenuBinding(view: AutoCompleteTextView, data: MutableStateFlow<T>, list: List<T>, mode: BindingMode = BindingMode.TwoWay, resolver:(ILabelResolverCreator<T>.()->Unit)?=null): Binder
        = add(ExposedDropdownMenuBinding.create(requireOwner, view, data.asMutableLiveData(requireOwner), list, mode, resolver))
