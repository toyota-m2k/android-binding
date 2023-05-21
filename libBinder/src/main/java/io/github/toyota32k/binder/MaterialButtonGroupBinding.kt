@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButtonToggleGroup
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow

abstract class MaterialButtonGroupBindingBase<T,DataType> (
    override val data:MutableLiveData<DataType>,
    mode: BindingMode = BindingMode.TwoWay
)  : BaseBinding<DataType>(mode), MaterialButtonToggleGroup.OnButtonCheckedListener {
    private var btnListener: MaterialButtonToggleGroup.OnButtonCheckedListener? = null

     lateinit var idResolver: IIDValueResolver<T>

     val toggleGroup:MaterialButtonToggleGroup?
        get() = view as? MaterialButtonToggleGroup

    open fun connect(owner:LifecycleOwner, view:MaterialButtonToggleGroup, idResolver: IIDValueResolver<T>) {
        this.idResolver = idResolver
        super.connect(owner,view)
        if(mode!= BindingMode.OneWay) {
            view.addOnButtonCheckedListener(this)
        }
    }

    override fun dispose() {
        if(mode!= BindingMode.OneWay) {
            toggleGroup?.removeOnButtonCheckedListener(this)
        }
        super.dispose()
    }
}

/**
 * MaterialButtonToggleGroup を使ったラジオボタングループのバインディング
 * 考え方は RadioGroup と同じだが、i/fが異なるので、クラスは別になる。
 */
class MaterialRadioButtonGroupBinding<T>(
    data:MutableLiveData<T>,
    mode: BindingMode = BindingMode.TwoWay
) : MaterialButtonGroupBindingBase<T, T>(data,mode) {

    override fun connect(owner: LifecycleOwner, view: MaterialButtonToggleGroup, idResolver: IIDValueResolver<T>) {
        view.isSingleSelection = true
        super.connect(owner, view, idResolver)
        if(mode== BindingMode.OneWayToSource||(mode== BindingMode.TwoWay &&  data.value==null)) {
            onButtonChecked(toggleGroup, toggleGroup?.checkedButtonId?:View.NO_ID, true)
        }
    }

    // View --> Source
    override fun onButtonChecked(group: MaterialButtonToggleGroup?, @IdRes checkedId: Int, isChecked: Boolean) {
        if(checkedId==View.NO_ID) return
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
        fun <T> create(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay) : MaterialRadioButtonGroupBinding<T> {
            return MaterialRadioButtonGroupBinding(data, mode).apply { connect(owner,view,idResolver) }
        }
    }
}

/**
 * MaterialButtonToggleGroup を使ったトグルボタングループのバインディング
 * 各トグルボタンにT型のユニークキー（enumかR.id.xxxなど）が１：１に対応しているとして、そのListで選択状態をバインドする。
 * MaterialButtonToggleGroupを使う場合、トグルボタンとしてButtonを使うため、個々のボタンの選択状態の指定や選択イベントは使えないので。
 */
class MaterialToggleButtonGroupBinding<T>(
    data:MutableLiveData<List<T>>,
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

    override fun connect(owner: LifecycleOwner,view: MaterialButtonToggleGroup,idResolver: IIDValueResolver<T>) {
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
        fun <T> create(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay) : MaterialToggleButtonGroupBinding<T> {
            return MaterialToggleButtonGroupBinding(data, mode).apply { connect(owner,view,idResolver) }
        }
    }
}

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

    var toggleGroup:MaterialButtonToggleGroup? = null


    private inner class DataObserver(owner:LifecycleOwner, val button: View, val data:MutableLiveData<Boolean>) : Observer<Boolean> {
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

    data class ButtonAndData(val button:View, val data:MutableLiveData<Boolean>)

    fun add(owner:LifecycleOwner, button:View, data:MutableLiveData<Boolean>): MaterialToggleButtonsBinding {
        buttons[button.id] = DataObserver(owner,button,data)
        if(mode== BindingMode.OneWayToSource||(mode== BindingMode.TwoWay &&  data.value==null)) {
            data.value = toggleGroup?.checkedButtonIds?.find { it==button.id } != null
        }
        return this
    }

    fun add(owner:LifecycleOwner, vararg buttons: ButtonAndData): MaterialToggleButtonsBinding {
        for(b in buttons) {
            add(owner, b.button, b.data)
        }
        return this
    }

    class Builder(val owner:LifecycleOwner,val target: MaterialToggleButtonsBinding) {
        fun bind(button:View, data:MutableLiveData<Boolean>): Builder {
            target.add(owner, button, data)
            return this
        }
    }

    fun addViewsByBuilder(owner:LifecycleOwner, fn: Builder.()->Unit) {
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
        fun create(view:MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay) : MaterialToggleButtonsBinding {
            return MaterialToggleButtonsBinding(mode).apply { connect(view) }
        }
        fun create(owner: LifecycleOwner, view:MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay, vararg buttons: ButtonAndData) : MaterialToggleButtonsBinding {
            return create(view, mode).apply {
                add(owner, *buttons)
            }
        }
        fun create(owner: LifecycleOwner, view:MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay, fnBindViews: Builder.()->Unit): MaterialToggleButtonsBinding {
            return create(view, mode).apply {
                addViewsByBuilder(owner, fnBindViews)
            }
        }
    }
}

fun <T> Binder.materialRadioButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonGroupBinding.create(owner, view, data, idResolver, mode))
fun <T> Binder.materialRadioButtonGroupBinding(view:MaterialButtonToggleGroup, data:MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialRadioButtonGroupBinding.create(requireOwner, view, data, idResolver, mode))

fun <T> Binder.materialRadioButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(
    MaterialRadioButtonGroupBinding.create(
        owner,
        view,
        data.asMutableLiveData(owner),
        idResolver,
        mode
    )
)
fun <T> Binder.materialRadioButtonGroupBinding(view:MaterialButtonToggleGroup, data:MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(
    MaterialRadioButtonGroupBinding.create(
        requireOwner,
        view,
        data.asMutableLiveData(requireOwner),
        idResolver,
        mode
    )
)

fun <T> Binder.materialToggleButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialToggleButtonGroupBinding.create(owner, view, data, idResolver, mode))
fun <T> Binder.materialToggleButtonGroupBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableStateFlow<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(
    MaterialToggleButtonGroupBinding.create(
        owner,
        view,
        data.asMutableLiveData(owner),
        idResolver,
        mode
    )
)
fun <T> Binder.materialToggleButtonGroupBinding(view:MaterialButtonToggleGroup, data:MutableLiveData<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(MaterialToggleButtonGroupBinding.create(requireOwner, view, data, idResolver, mode))
fun <T> Binder.materialToggleButtonGroupBinding(view:MaterialButtonToggleGroup, data:MutableStateFlow<List<T>>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(
    MaterialToggleButtonGroupBinding.create(
        requireOwner,
        view,
        data.asMutableLiveData(requireOwner),
        idResolver,
        mode
    )
)


fun Binder.materialToggleButtonsBinding(owner: LifecycleOwner, view:MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay, fnBindViews: MaterialToggleButtonsBinding.Builder.()->Unit): Binder
        = add(MaterialToggleButtonsBinding.create(owner, view, mode, fnBindViews))
fun Binder.materialToggleButtonsBinding(view:MaterialButtonToggleGroup, mode: BindingMode = BindingMode.TwoWay, fnBindViews: MaterialToggleButtonsBinding.Builder.()->Unit): Binder
        = add(MaterialToggleButtonsBinding.create(requireOwner, view, mode, fnBindViews))
