package io.github.toyota32k.binder

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButtonToggleGroup

abstract class MaterialButtonGroupBindingBase<T,DataType> (
    override val data: MutableLiveData<DataType>,
    mode: BindingMode = BindingMode.TwoWay
)  : BaseBinding<DataType>(mode), MaterialButtonToggleGroup.OnButtonCheckedListener {
    private var btnListener: MaterialButtonToggleGroup.OnButtonCheckedListener? = null

    lateinit var idResolver: IIDValueResolver<T>

    val toggleGroup: MaterialButtonToggleGroup?
        get() = view as? MaterialButtonToggleGroup

    open fun connect(owner: LifecycleOwner, view: MaterialButtonToggleGroup, idResolver: IIDValueResolver<T>) {
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
