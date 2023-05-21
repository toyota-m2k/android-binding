@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.utils.ConvertLiveData
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.disposableObserve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

abstract class BoolBinding(
    rawData: LiveData<Boolean>,
    mode: BindingMode,
    boolConvert: BoolConvert
) : BaseBinding<Boolean>(mode) {
    override val data:LiveData<Boolean> = if(boolConvert== BoolConvert.Straight) rawData else ConvertLiveData<Boolean,Boolean>(rawData as MutableLiveData<Boolean>, { it!=true }, {it!=true})
}

open class GenericBoolBinding(
    rawData: LiveData<Boolean>,
    boolConvert: BoolConvert = BoolConvert.Straight,
    val applyValue:(View,Boolean)->Unit): BoolBinding(rawData, BindingMode.OneWay, boolConvert) {

    override fun onDataChanged(v: Boolean?) {
        if(v!=null) {
            view?.apply {
                applyValue(this, v)
            }
        }
    }

    companion object {
        fun create(owner: LifecycleOwner, view: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue: (View, Boolean) -> Unit) : GenericBoolBinding {
            return GenericBoolBinding(data, boolConvert,applyValue).apply { connect(owner, view) }
        }
        // for StateFlow
        fun create(owner: LifecycleOwner, view: View, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue: (View, Boolean) -> Unit): GenericBoolBinding {
            return create(owner, view, data.asLiveData(), boolConvert, applyValue)
        }
    }
}

open class GenericBoolMultiBinding(
    rawData: LiveData<Boolean>,
    boolConvert: BoolConvert = BoolConvert.Straight,
    val applyValue:(List<View>,Boolean)->Unit) :
    BoolBinding(rawData, BindingMode.OneWay, boolConvert) {
    private val views = mutableListOf<View>()

    override fun onDataChanged(v: Boolean?) {
        if(v!=null) {
            applyValue(views, v)
        }
    }

    override fun connect(owner: LifecycleOwner, view:View) {
        UtLog.libLogger.assert( false,"use connectAll() method.")
    }

    fun connectAll(owner:LifecycleOwner, vararg targets:View) {
        UtLog.libLogger.assert(mode== BindingMode.OneWay, "GenericBoolMultiBinding ... support OneWay mode only.")
        views.addAll(targets)
        if(observed==null) {
            observed = data.disposableObserve(owner, this::onDataChanged)
        }
        onDataChanged(data.value)
    }

    override fun dispose() {
        views.clear()
        super.dispose()
    }

    companion object {
        fun create(owner: LifecycleOwner, vararg targets: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue:(List<View>, Boolean)->Unit): GenericBoolMultiBinding {
            return GenericBoolMultiBinding(data, boolConvert, applyValue).apply {
                connectAll(owner, *targets)
            }
        }
        // for StateFlow
        fun create(owner: LifecycleOwner, vararg targets: View, data: StateFlow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue: (List<View>, Boolean) -> Unit): GenericBoolMultiBinding {
            return create(owner, targets = targets, data.asLiveData(), boolConvert, applyValue)
        }
    }
}

fun Binder.genericBoolBinding(owner: LifecycleOwner, view: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue: (View, Boolean) -> Unit): Binder
        = add(GenericBoolBinding.create(owner, view, data, boolConvert, applyValue))
fun Binder.genericBoolBinding(owner: LifecycleOwner, view: View, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue: (View, Boolean) -> Unit): Binder
        = add(GenericBoolBinding.create(owner, view, data, boolConvert, applyValue))
fun Binder.genericBoolBinding(view: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue: (View, Boolean) -> Unit): Binder
        = add(GenericBoolBinding.create(requireOwner, view, data, boolConvert, applyValue))
fun Binder.genericBoolBinding(view: View, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue: (View, Boolean) -> Unit): Binder
        = add(GenericBoolBinding.create(requireOwner, view, data, boolConvert, applyValue))

fun Binder.genericBoolMultiBinding(owner: LifecycleOwner, targets:Array<View>, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue:(List<View>, Boolean)->Unit): Binder
        = add(
    GenericBoolMultiBinding.create(
        owner,
        targets = targets,
        data,
        boolConvert,
        applyValue
    )
)
fun Binder.genericBoolMultiBinding(owner: LifecycleOwner, targets:Array<View>, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue:(List<View>, Boolean)->Unit): Binder
        = add(
    GenericBoolMultiBinding.create(
        owner,
        targets = targets,
        data.asLiveData(),
        boolConvert,
        applyValue
    )
)
fun Binder.genericBoolMultiBinding(targets:Array<View>, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue:(List<View>, Boolean)->Unit): Binder
        = add(
    GenericBoolMultiBinding.create(
        requireOwner,
        targets = targets,
        data,
        boolConvert,
        applyValue
    )
)
fun Binder.genericBoolMultiBinding(targets:Array<View>, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyValue:(List<View>, Boolean)->Unit): Binder
        = add(
    GenericBoolMultiBinding.create(
        requireOwner,
        targets = targets,
        data.asLiveData(),
        boolConvert,
        applyValue
    )
)
