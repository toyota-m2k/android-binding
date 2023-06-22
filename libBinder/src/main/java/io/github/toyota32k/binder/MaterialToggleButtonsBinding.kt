@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButtonToggleGroup

/**
 * MaterialButtonToggleGroupに支配される、複数のボタンと、その選択状態(LiveData<Boolean>)を個々にバインドするクラス。
 *
 * Usage:
 *
 * val binding = MaterialToggleButtonsBinding(owner,toggleGroup).apply {
 *               add(button1, viewModel.toggle1)
 *               add(button2, viewModel.toggle2)
 *               ...
 *            }
 */
class MaterialToggleButtonsBinding (
    override val mode: BindingMode = BindingMode.TwoWay
) : IBinding, MaterialButtonToggleGroup.OnButtonCheckedListener {

    var toggleGroup: MaterialButtonToggleGroup? = null


    private inner class DataObserver(owner: LifecycleOwner, val button: View, val data: MutableLiveData<Boolean>) : Observer<Boolean> {
        init {
            if (mode != BindingMode.OneWayToSource) {
                data.observe(owner,this)
            }
        }

        fun dispose() {
            data.removeObserver(this)
        }

        override fun onChanged(value: Boolean) {
            val view = toggleGroup ?: return
            val cur = view.checkedButtonIds.contains(button.id)
            if(value) {
                if(!cur) {
                    view.check(button.id)
                }
            } else {
                if(cur) {
                    view.uncheck(button.id)
                }
            }
        }
    }

    //    private val weakOwner = WeakReference(owner)
//    private val owner:LifecycleOwner?
//        get() = weakOwner.get()
    private val buttons = mutableMapOf<Int, DataObserver>()

    fun connect(view: MaterialButtonToggleGroup) {
        toggleGroup = view
        if(mode!= BindingMode.OneWay) {
            view.addOnButtonCheckedListener(this)
        }
    }

    data class ButtonAndData(val button: View, val data: MutableLiveData<Boolean>)

    fun add(owner: LifecycleOwner, button: View, data: MutableLiveData<Boolean>): MaterialToggleButtonsBinding {
        buttons[button.id] = DataObserver(owner,button,data)
        if(mode== BindingMode.OneWayToSource||(mode== BindingMode.TwoWay &&  data.value==null)) {
            data.value = toggleGroup?.checkedButtonIds?.find { it==button.id } != null
        }
        return this
    }

    fun add(owner: LifecycleOwner, vararg buttons: ButtonAndData): MaterialToggleButtonsBinding {
        for(b in buttons) {
            add(owner, b.button, b.data)
        }
        return this
    }

    class Builder(val owner: LifecycleOwner, val target: MaterialToggleButtonsBinding) {
        fun bind(button: View, data: MutableLiveData<Boolean>): Builder {
            target.add(owner, button, data)
            return this
        }
    }

    fun addViewsByBuilder(owner: LifecycleOwner, fn: Builder.()->Unit) {
        Builder(owner, this).apply {
            fn()
        }
    }

    private var disposed:Boolean = false
    override fun dispose() {
        if (mode != BindingMode.OneWayToSource) {
            buttons.forEach { (_, data) ->
                data.dispose()
            }
        }
        buttons.clear()
        if(mode!= BindingMode.OneWay) {
            toggleGroup?.removeOnButtonCheckedListener(this)
        }
        disposed = true
    }

    override fun onButtonChecked(
        group: MaterialButtonToggleGroup?,
        checkedId: Int,
        isChecked: Boolean
    ) {
        val v = buttons[checkedId] ?: return
        if(v.data.value != isChecked) {
            v.data.value = isChecked
        }
    }
    companion object {
        fun create(view: MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay) : MaterialToggleButtonsBinding {
            return MaterialToggleButtonsBinding(mode).apply { connect(view) }
        }
        fun create(owner: LifecycleOwner, view: MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay, vararg buttons: ButtonAndData) : MaterialToggleButtonsBinding {
            return create(view, mode).apply {
                add(owner, *buttons)
            }
        }
        fun create(owner: LifecycleOwner, view: MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay, fnBindViews: Builder.()->Unit): MaterialToggleButtonsBinding {
            return create(view, mode).apply {
                addViewsByBuilder(owner, fnBindViews)
            }
        }
    }
}

fun Binder.materialToggleButtonsBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay, fnBindViews: MaterialToggleButtonsBinding.Builder.()->Unit): Binder
        = add(MaterialToggleButtonsBinding.create(owner, view, mode, fnBindViews))
fun Binder.materialToggleButtonsBinding(view:MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay, fnBindViews: MaterialToggleButtonsBinding.Builder.()->Unit): Binder
        = add(MaterialToggleButtonsBinding.create(requireOwner, view, mode, fnBindViews))
