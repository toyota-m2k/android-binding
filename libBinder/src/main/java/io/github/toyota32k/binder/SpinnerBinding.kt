@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.binder.list.ListViewAdapter
import io.github.toyota32k.binder.list.MutableListViewAdapter
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

open class SpinnerBinding<T> (
    override val data: LiveData<T>,     // Spinnerの選択状態とバインディングされるデータ
    val list: List<T>,
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
        return list.indexOf(item)
//        val s = spinner ?:  return -1
//        for(i in (0 until s.count)) {
//            if(item == s.getItemAtPosition(i)) {
//                return i
//            }
//        }
//        return -1
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
        fun <T> createAdapter(list: List<T>, @LayoutRes itemLayout:Int, bindView:(Binder, View, T)->Unit): SpinnerAdapter {
            if(list is ObservableList<T>) {
                return MutableListViewAdapter(list, itemLayout, bindView)
            } else {
                return ListViewAdapter(list, itemLayout, bindView)
            }
        }

        /**
         * 一番シンプルなSpinnerAdapter
         * - TextView１つだけのアイテムビュー (android.R.layout.simple_spinner_item)を使用。
         * - item:T のtoString()の結果がTextViewに表示される。
         */
        fun <T> createSimpleAdapter(list: List<T>, itemToString:((T)->String)?):SpinnerAdapter {
            return createAdapter(list, android.R.layout.simple_spinner_item) { binder, view, item ->
                view.findViewById<TextView>(android.R.id.text1).text = itemToString?.invoke(item) ?: item.toString()
            }
        }


        /**
         * SpinnerBindingを作成する（LiveData版）
         *
         * @param owner LifecycleOwner
         * @param view Spinner
         * @param data Spinnerの選択値にバインディングするデータ
         * @param list Spinnerに表示するリスト
         * @param adapter Spinnerに表示するアダプタ。省略時はcreateSimpleAdapter()を使用。
         * @param mode BindingMode dataと選択値のバインディングモード
         */
        fun <T> create(owner: LifecycleOwner, view: Spinner, data: LiveData<T>, list: List<T>, adapter:SpinnerAdapter, mode: BindingMode = BindingMode.TwoWay) : SpinnerBinding<T> {
            view.adapter = adapter
            return SpinnerBinding(data, list, mode).apply { connect(owner,view) }
        }
        fun <T> create(owner: LifecycleOwner, view: Spinner, data: LiveData<T>, list: List<T>, itemToString:((T)->String)?=null, mode: BindingMode = BindingMode.TwoWay) : SpinnerBinding<T> {
            view.adapter = createSimpleAdapter(list, itemToString)
            return SpinnerBinding(data, list, mode).apply { connect(owner,view) }
        }
        /**
         * SpinnerBindingを作成する（Flow版）
         *
         * @param owner LifecycleOwner
         * @param view Spinner
         * @param data Spinnerの選択値にバインディングするデータ
         * @param list Spinnerに表示するリスト
         * @param adapter Spinnerに表示するアダプタ。省略時はcreateSimpleAdapter()を使用。
         * @param mode BindingMode dataと選択値のバインディングモード
         */
        fun <T> create(owner: LifecycleOwner, view: Spinner, data: Flow<T>, list: List<T>, adapter:SpinnerAdapter, mode: BindingMode = BindingMode.TwoWay) : SpinnerBinding<T> {
            view.adapter = adapter
            val flow = if(data is MutableStateFlow) data.asMutableLiveData(owner) else data.asLiveData()
            return SpinnerBinding(flow, list, mode).apply { connect(owner,view) }
        }
        fun <T> create(owner: LifecycleOwner, view: Spinner, data: Flow<T>, list: List<T>, itemToString:((T)->String)?=null, mode: BindingMode = BindingMode.TwoWay) : SpinnerBinding<T> {
            view.adapter = createSimpleAdapter(list, itemToString)
            val flow = if(data is MutableStateFlow) data.asMutableLiveData(owner) else data.asLiveData()
            return SpinnerBinding(flow, list, mode).apply { connect(owner,view) }
        }
    }
}

// spinnerBinding作成用ヘルパ関数たち
//  普通よく使うであろう、双方向バインディングのヘルパーを用意した。
//  createSimpleAdapterを使うなら、adapter引数は省略可能。
//  カスタマイズする場合は、SpinnerBinding.createAdapter()を使う。

fun <T> Binder.spinnerBinding(owner: LifecycleOwner, view: Spinner, data: MutableLiveData<T>, list: List<T>, adapter:SpinnerAdapter) : Binder
    = add(SpinnerBinding.create(owner, view, data, list, adapter))
fun <T> Binder.spinnerBinding(view: Spinner, data: MutableLiveData<T>, list: List<T>, adapter:SpinnerAdapter) : Binder
    = add(SpinnerBinding.create(requireOwner, view, data, list, adapter))

fun <T> Binder.spinnerBinding(owner: LifecycleOwner, view: Spinner, data: MutableStateFlow<T>, list: List<T>, itemToString:((T)->String)?=null) : Binder
    = add(SpinnerBinding.create(owner, view, data, list, itemToString))
fun <T> Binder.spinnerBinding(view: Spinner, data: MutableStateFlow<T>, list: List<T>, itemToString:((T)->String)?=null) : Binder
    = add(SpinnerBinding.create(requireOwner, view, data, list, itemToString))

fun <T> Binder.spinnerBinding(owner: LifecycleOwner, view: Spinner, data: MutableLiveData<T>, list: List<T>, itemToString:((T)->String)?=null) : Binder
        = add(SpinnerBinding.create(owner, view, data, list, itemToString))
fun <T> Binder.spinnerBinding(view: Spinner, data: MutableLiveData<T>, list: List<T>, itemToString:((T)->String)?=null) : Binder
        = add(SpinnerBinding.create(requireOwner, view, data, list, itemToString))
