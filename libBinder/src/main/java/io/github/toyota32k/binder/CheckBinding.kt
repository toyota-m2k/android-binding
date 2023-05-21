@file:Suppress("unused")

package io.github.toyota32k.binder

import android.widget.CompoundButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

open class CheckBinding protected constructor(
    rawData: LiveData<Boolean>,
    mode: BindingMode,
    boolConvert: BoolConvert = BoolConvert.Straight
) : BoolBinding(rawData,mode,boolConvert), CompoundButton.OnCheckedChangeListener {
    constructor(data:LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight) : this(data,
        BindingMode.OneWay,boolConvert)
    private val compoundButton:CompoundButton?
        get() = view as? CompoundButton

    fun connect(owner: LifecycleOwner, view: CompoundButton) {
        super.connect(owner, view)
        if(mode!= BindingMode.OneWay) {
            view.setOnCheckedChangeListener(this)
            if(mode== BindingMode.OneWayToSource ||data.value==null) {
                // view --> data
                onCheckedChanged(view, view.isChecked)
            }
        }
    }

    override fun dispose() {
        if(mode!= BindingMode.OneWay) {
            compoundButton?.setOnCheckedChangeListener(null)
        }
        super.dispose()
    }

    // region OnCheckedChangeListener

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        mutableData?.apply {
            if(value!=isChecked) {
                value = isChecked
            }
        }
    }

    override fun onDataChanged(v: Boolean?) {
        val chk = v == true
        compoundButton?.apply {
            if(isChecked!=chk) {
                isChecked = chk
            }
        }
    }

    companion object {
        fun create(owner: LifecycleOwner, view: CompoundButton, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): CheckBinding {
            return CheckBinding(data, BindingMode.OneWay, boolConvert).apply { connect(owner, view) }
        }
        fun create(owner: LifecycleOwner, view:CompoundButton, data: MutableLiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, mode: BindingMode = BindingMode.TwoWay): CheckBinding {
            return CheckBinding(data, mode, boolConvert).apply { connect(owner, view) }
        }
    }
}

fun Binder.checkBinding(owner: LifecycleOwner, view: CompoundButton, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): Binder
        = add(CheckBinding.create(owner, view, data, boolConvert))
fun Binder.checkBinding(owner: LifecycleOwner, view: CompoundButton, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): Binder
        = add(CheckBinding.create(owner, view, data.asLiveData(), boolConvert))
fun Binder.checkBinding(owner: LifecycleOwner, view:CompoundButton, data: MutableLiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(CheckBinding.create(owner, view, data, boolConvert, mode))
fun Binder.checkBinding(owner: LifecycleOwner, view:CompoundButton, data: MutableStateFlow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(CheckBinding.create(owner, view, data.asMutableLiveData(owner), boolConvert, mode))

fun Binder.checkBinding(view: CompoundButton, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): Binder
        = add(CheckBinding.create(requireOwner, view, data, boolConvert))
fun Binder.checkBinding(view: CompoundButton, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): Binder
        = add(CheckBinding.create(requireOwner, view, data.asLiveData(), boolConvert))
fun Binder.checkBinding(view:CompoundButton, data: MutableLiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(CheckBinding.create(requireOwner, view, data, boolConvert, mode))
fun Binder.checkBinding(view:CompoundButton, data: MutableStateFlow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(
    CheckBinding.create(
        requireOwner,
        view,
        data.asMutableLiveData(requireOwner),
        boolConvert,
        mode
    )
)
