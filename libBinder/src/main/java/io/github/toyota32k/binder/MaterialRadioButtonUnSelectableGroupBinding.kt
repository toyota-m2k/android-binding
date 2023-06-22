@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButtonToggleGroup
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * 「何も選択されていない状態」を許容する RadioButton バインディング
 * 何も選択されていない状態は、View.NO_ID として扱うので、
 * IIDValueResolverには、View.NO_ID と、非選択時の値（nullや NONE的なenum値）とのマップを定義しておくこと。
 * 例）
 */
class MaterialRadioButtonUnSelectableGroupBinding<T>(
    data: MutableLiveData<T>,
    mode: BindingMode = BindingMode.TwoWay

) : MaterialButtonGroupBindingBase<T, T>(data,mode) {

    override fun connect(owner: LifecycleOwner, view: MaterialButtonToggleGroup, idResolver: IIDValueResolver<T>) {
        view.isSingleSelection = false
        view.isSelectionRequired = false
        super.connect(owner, view, idResolver)
        if(mode== BindingMode.OneWayToSource||(mode== BindingMode.TwoWay &&  data.value==null)) {
            onButtonChecked(toggleGroup, toggleGroup?.checkedButtonId?: View.NO_ID, true)
        }
    }

    override fun onDataChanged(v: T?) {
        val view = toggleGroup?:return
        if(v!=null) {
            val id = idResolver.value2id(v)
            // onButtonChecked() のコールスタックから、check/uncheckを呼び出すと表示が更新されないので、ちょっとだけ遅延する。
            CoroutineScope(Dispatchers.Main).launch {
                delay(50)
                val checked = view.checkedButtonIds
                if (id != View.NO_ID && !checked.contains(id)) {
                    view.check(id)
                }
                checked.forEach {
                    if (it != id) {
                        view.uncheck(it)
                    }
                }
            }
        } else {
            view.clearChecked()
        }
    }

    override fun onButtonChecked(group: MaterialButtonToggleGroup?, @IdRes checkedId: Int, isChecked: Boolean) {
        if(checkedId== View.NO_ID) return
        val sel = idResolver.id2value(checkedId) ?: return
        val newVal = if(isChecked) {
            // ボタンが選択状態になった
            sel
        } else if (sel == data.value) {
            // 選択されていたボタンが非選択状態になった
            idResolver.id2value(View.NO_ID)
        } else {
            // 選択されていないボタンが非選択状態になった --> 処置不要
            return
        }
        if(newVal != data.value) {
            data.value = newVal
        }
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: MaterialButtonToggleGroup, data: MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay) : MaterialRadioButtonUnSelectableGroupBinding<T> {
            return MaterialRadioButtonUnSelectableGroupBinding(data, mode).apply { connect(owner,view,idResolver) }
        }
    }
}


fun <T> Binder.materialRadioUnSelectableButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonUnSelectableGroupBinding.create(owner, view, data, idResolver, mode))
fun <T> Binder.materialRadioUnSelectableButtonGroupBinding(view:MaterialButtonToggleGroup, data:MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonUnSelectableGroupBinding.create(requireOwner, view, data, idResolver, mode))

fun <T> Binder.materialRadioUnSelectableButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data: MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonUnSelectableGroupBinding.create(owner,view,data.asMutableLiveData(owner),idResolver,mode))
fun <T> Binder.materialRadioUnSelectableButtonGroupBinding(view:MaterialButtonToggleGroup, data: MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonUnSelectableGroupBinding.create(requireOwner,view,data.asMutableLiveData(requireOwner),idResolver,mode))
