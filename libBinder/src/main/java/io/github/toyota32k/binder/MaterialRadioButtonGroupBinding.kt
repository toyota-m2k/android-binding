@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButtonToggleGroup
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * MaterialButtonToggleGroup を使ったラジオボタングループのバインディング
 * 考え方は RadioGroup と同じだが、i/fが異なるので、クラスは別になる。
 */
class MaterialRadioButtonGroupBinding<T>(
    data: MutableLiveData<T>,
    mode: BindingMode = BindingMode.TwoWay
) : MaterialButtonGroupBindingBase<T, T>(data,mode) {

    override fun connect(owner: LifecycleOwner, view: MaterialButtonToggleGroup, idResolver: IIDValueResolver<T>) {
        view.isSingleSelection = true
        super.connect(owner, view, idResolver)
        if(mode== BindingMode.OneWayToSource||(mode== BindingMode.TwoWay &&  data.value==null)) {
            onButtonChecked(toggleGroup, toggleGroup?.checkedButtonId?: View.NO_ID, true)
        }
    }

    // View --> Source
    override fun onButtonChecked(group: MaterialButtonToggleGroup?, @IdRes checkedId: Int, isChecked: Boolean) {
        if(checkedId== View.NO_ID) return
        if(isChecked) {
            val v = idResolver.id2value(checkedId) ?: return
            if(data.value!=v) {
                data.value = v
            }
        }
    }

    // Source --> View
    override fun onDataChanged(v: T?) {
        val view = toggleGroup?:return
        if(v!=null) {
            val id = idResolver.value2id(v)
            if(view.checkedButtonId != id) {
                view.clearChecked()
                view.check(id)
            }
        }
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: MaterialButtonToggleGroup, data: MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay) : MaterialRadioButtonGroupBinding<T> {
            return MaterialRadioButtonGroupBinding(data, mode).apply { connect(owner,view,idResolver) }
        }
    }
}

fun <T> Binder.materialRadioButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonGroupBinding.create(owner, view, data, idResolver, mode))
fun <T> Binder.materialRadioButtonGroupBinding(view:MaterialButtonToggleGroup, data:MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonGroupBinding.create(requireOwner, view, data, idResolver, mode))

fun <T> Binder.materialRadioButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data: MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonGroupBinding.create(owner,view,data.asMutableLiveData(owner),idResolver,mode))
fun <T> Binder.materialRadioButtonGroupBinding(view:MaterialButtonToggleGroup, data: MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonGroupBinding.create(requireOwner,view,data.asMutableLiveData(requireOwner),idResolver,mode))

