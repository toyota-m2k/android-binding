@file:Suppress("unused")

package io.github.toyota32k.binder

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButtonToggleGroup
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * MaterialButtonToggleGroup を使ったトグルボタングループのバインディング
 * 各トグルボタンにT型のユニークキー（enumかR.id.xxxなど）が１：１に対応しているとして、そのListで選択状態をバインドする。
 * MaterialButtonToggleGroupを使う場合、トグルボタンとしてButtonを使うため、個々のボタンの選択状態の指定や選択イベントは使えないので。
 */
class MaterialToggleButtonGroupBinding<T>(
    data: MutableLiveData<List<T>>,
    mode: BindingMode = BindingMode.TwoWay
) : MaterialButtonGroupBindingBase<T, List<T>>(data,mode) {

    //    private val selected = mutableSetOf<T>()
    private var busy = false
    private fun inBusy(fn:()->Unit) {
        if(!busy) {
            busy = true
            try {
                fn()
            } finally {
                busy = false
            }
        }
    }

    override fun connect(owner: LifecycleOwner, view: MaterialButtonToggleGroup, idResolver: IIDValueResolver<T>) {
        super.connect(owner, view, idResolver)
        if(mode== BindingMode.OneWayToSource||(mode== BindingMode.TwoWay &&  data.value==null)) {
            for(c in toggleGroup?.checkedButtonIds ?:return) {
                onButtonChecked(toggleGroup, c, true)
            }
        }
    }

    override fun onDataChanged(v: List<T>?) {
        val view = toggleGroup ?: return
        inBusy {
            view.clearChecked()
            if (!v.isNullOrEmpty()) {
                v.forEach {
                    view.check(idResolver.value2id(it))
                }
            }
        }
    }

    override fun onButtonChecked(
        group: MaterialButtonToggleGroup?,
        checkedId: Int,
        isChecked: Boolean
    ) {
        inBusy {
//            val v = idResolver.id2value(checkedId) ?: return@inBusy
            data.value = group?.checkedButtonIds?.mapNotNull { idResolver.id2value(it) } ?: emptyList()
        }
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: MaterialButtonToggleGroup, data: MutableLiveData<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay) : MaterialToggleButtonGroupBinding<T> {
            return MaterialToggleButtonGroupBinding(data, mode).apply { connect(owner,view,idResolver) }
        }
    }
}

fun <T> Binder.materialToggleButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialToggleButtonGroupBinding.create(owner, view, data, idResolver, mode))
fun <T> Binder.materialToggleButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data: MutableStateFlow<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialToggleButtonGroupBinding.create(owner,view,data.asMutableLiveData(owner),idResolver,mode))
fun <T> Binder.materialToggleButtonGroupBinding(view:MaterialButtonToggleGroup, data:MutableLiveData<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialToggleButtonGroupBinding.create(requireOwner, view, data, idResolver, mode))
fun <T> Binder.materialToggleButtonGroupBinding(view:MaterialButtonToggleGroup, data: MutableStateFlow<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialToggleButtonGroupBinding.create(requireOwner,view,data.asMutableLiveData(requireOwner),idResolver,mode))

