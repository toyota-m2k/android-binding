@file:Suppress("unused")

package io.github.toyota32k.binder

import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.*
import io.github.toyota32k.utils.ConvertLiveData
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

open class NumberBinding<N> (
    data: LiveData<N>
) : TextBinding(data.map{it.toString()}) where N : Number {
    companion object {
        fun create(owner: LifecycleOwner, view: TextView, data: LiveData<Int>): IntBinding {
            return IntBinding(data).apply { connect(owner, view) }
        }

        fun create(owner: LifecycleOwner, view: TextView, data: LiveData<Long>): LongBinding {
            return LongBinding(data).apply { connect(owner, view) }
        }
        fun create(owner: LifecycleOwner, view: TextView, data: LiveData<Float>): FloatBinding {
            return FloatBinding(data).apply { connect(owner, view) }
        }
    }
}

open class EditNumberBinding<N> (
    data: MutableLiveData<N>,
    mode: BindingMode,
    private val revert: ((String?)->N?)?
) : EditTextBinding(ConvertLiveData<N,String>(data, {it.toString()}, {revert?.invoke(it)}),mode) where N : Number {

    override fun onDataChanged(v: String?) {
        val rev = revert ?: return super.onDataChanged(v)
        val view = textView ?: return
        if (rev(v)!=null && rev(v) != rev(view.text.toString())) {
            view.text = v
        }
    }

    override fun onViewValueChanged(tx:String?) {
        val rev = revert ?: return super.onViewValueChanged(tx)
        mutableData?.apply {
            if(rev(tx)!=rev(value)) {
                value = tx
            }
        }
    }

    companion object {
        fun create(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Int>, mode: BindingMode = BindingMode.TwoWay): EditIntBinding {
            return EditIntBinding(data, mode).apply { connect(owner, view) }
        }

        fun create(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Long>, mode: BindingMode = BindingMode.TwoWay): EditLongBinding {
            return EditLongBinding(data, mode).apply { connect(owner, view) }
        }
        fun create(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Float>, mode: BindingMode = BindingMode.TwoWay): EditFloatBinding {
            return EditFloatBinding(data, mode).apply { connect(owner, view) }
        }
    }
}

class IntBinding(data: LiveData<Int>) : NumberBinding<Int>(data)
class EditIntBinding(data: MutableLiveData<Int>, mode: BindingMode = BindingMode.TwoWay) : EditNumberBinding<Int>(data,mode,{it?.toIntOrNull()})

class LongBinding(data: LiveData<Long>) : NumberBinding<Long>(data)
class EditLongBinding(data: MutableLiveData<Long>, mode: BindingMode = BindingMode.TwoWay) : EditNumberBinding<Long>(data,mode,{it?.toLongOrNull()})

class FloatBinding(data: LiveData<Float>) : NumberBinding<Float>(data)
class EditFloatBinding(data: MutableLiveData<Float>, mode: BindingMode = BindingMode.TwoWay) : EditNumberBinding<Float>(data,mode,{it?.toFloatOrNull()})

fun Binder.intBinding(owner: LifecycleOwner, view: TextView, data: LiveData<Int>): Binder
        = add(NumberBinding.create(owner, view, data))
fun Binder.intBinding(owner: LifecycleOwner, view: TextView, data: Flow<Int>): Binder
        = add(NumberBinding.create(owner, view, data.asLiveData()))
fun Binder.intBinding(view: TextView, data: LiveData<Int>): Binder
        = add(NumberBinding.create(requireOwner, view, data))
fun Binder.intBinding(view: TextView, data: Flow<Int>): Binder
        = add(NumberBinding.create(requireOwner, view, data.asLiveData()))

fun Binder.longBinding(owner: LifecycleOwner, view: TextView, data: LiveData<Long>): Binder
        = add(NumberBinding.create(owner, view, data))
fun Binder.longBinding(owner: LifecycleOwner, view: TextView, data: Flow<Long>): Binder
        = add(NumberBinding.create(owner, view, data.asLiveData()))
fun Binder.longBinding(view: TextView, data: LiveData<Long>): Binder
        = add(NumberBinding.create(requireOwner, view, data))
fun Binder.longBinding(view: TextView, data: Flow<Long>): Binder
        = add(NumberBinding.create(requireOwner, view, data.asLiveData()))

fun Binder.floatBinding(owner: LifecycleOwner, view: TextView, data: LiveData<Float>): Binder
        = add(NumberBinding.create(owner, view, data))
fun Binder.floatBinding(owner: LifecycleOwner, view: TextView, data: Flow<Float>): Binder
        = add(NumberBinding.create(owner, view, data.asLiveData()))
fun Binder.floatBinding(view: TextView, data: LiveData<Float>): Binder
        = add(NumberBinding.create(requireOwner, view, data))
fun Binder.floatBinding(view: TextView, data: Flow<Float>): Binder
        = add(NumberBinding.create(requireOwner, view, data.asLiveData()))

fun Binder.editIntBinding(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Int>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(owner, view, data, mode))
fun Binder.editIntBinding(owner: LifecycleOwner, view: EditText, data: MutableStateFlow<Int>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(owner, view, data.asMutableLiveData(owner), mode))
fun Binder.editIntBinding(view: EditText, data: MutableLiveData<Int>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(requireOwner, view, data, mode))
fun Binder.editIntBinding(view: EditText, data: MutableStateFlow<Int>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(
    EditNumberBinding.create(
        requireOwner,
        view,
        data.asMutableLiveData(requireOwner),
        mode
    )
)

fun Binder.editLongBinding(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Long>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(owner, view, data, mode))
fun Binder.editLongBinding(owner: LifecycleOwner, view: EditText, data: MutableStateFlow<Long>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(owner, view, data.asMutableLiveData(owner), mode))
fun Binder.editLongBinding(view: EditText, data: MutableLiveData<Long>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(requireOwner, view, data, mode))
fun Binder.editLongBinding(view: EditText, data: MutableStateFlow<Long>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(
    EditNumberBinding.create(
        requireOwner,
        view,
        data.asMutableLiveData(requireOwner),
        mode
    )
)

fun Binder.editFloatBinding(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Float>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(owner, view, data, mode))
fun Binder.editFloatBinding(owner: LifecycleOwner, view: EditText, data: MutableStateFlow<Float>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(owner, view, data.asMutableLiveData(owner), mode))
fun Binder.editFloatBinding(view: EditText, data: MutableLiveData<Float>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditNumberBinding.create(requireOwner, view, data, mode))
fun Binder.editFloatBinding(view: EditText, data: MutableStateFlow<Float>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(
    EditNumberBinding.create(
        requireOwner,
        view,
        data.asMutableLiveData(requireOwner),
        mode
    )
)
