@file:Suppress("unused")

package io.github.toyota32k.binder

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.utils.lifecycle.asMutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

open class TextBinding protected constructor(
    override val data:LiveData<String>,
    mode: BindingMode
) : BaseBinding<String>(mode) {
    constructor(data:LiveData<String>):this(data, BindingMode.OneWay)
    // region Observer (LiveData-->View)

    protected val textView:TextView?
        get() = view as TextView?

    fun connect(owner: LifecycleOwner, view: TextView) {
        super.connect(owner,view)
    }

    override fun onDataChanged(v: String?) {
        val view = textView?:return
        if(v!=view.text.toString()) {
            view.text = v
        }
    }

    // endregion

    companion object {
        fun create(owner:LifecycleOwner, view:TextView, data:LiveData<String>) : TextBinding {
            return TextBinding(data, BindingMode.OneWay).apply { connect(owner,view) }
        }
        // for StateFlow
        fun create(owner: LifecycleOwner, view: TextView, data: Flow<String>): TextBinding {
            return create(owner, view, data.asLiveData())
        }
    }
}

open class EditTextBinding(
    data: MutableLiveData<String>,
    mode: BindingMode
) : TextBinding(data,mode), TextWatcher {

    private val editText:EditText?
        get() = view as EditText?


    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        onViewValueChanged(s?.toString())
    }

    open fun onViewValueChanged(tx:String?) {
        mutableData?.apply {
            if(tx!=value) {
                value = tx
            }
        }
    }

    fun connect(owner: LifecycleOwner, view:EditText) {
        super.connect(owner,view)
        if(mode!= BindingMode.OneWay) {
            view.addTextChangedListener(this)
            if(mode== BindingMode.OneWayToSource||data.value==null) {
                afterTextChanged(view.text)
            }
        }
    }

    override fun dispose() {
        editText?.removeTextChangedListener(this)
        super.dispose()
    }

    // endregion
    companion object {
        fun create(owner:LifecycleOwner, view:EditText, data:MutableLiveData<String>, mode: BindingMode = BindingMode.TwoWay): EditTextBinding {
            return EditTextBinding(data,mode).apply { connect(owner,view) }
        }
        // for StateFlow
        fun create(owner: LifecycleOwner, view: EditText, data: MutableStateFlow<String>, mode: BindingMode = BindingMode.TwoWay): EditTextBinding {
            return create(owner, view, data.asMutableLiveData(owner), mode)
        }
    }
}

fun Binder.textBinding(owner:LifecycleOwner, view:TextView, data:LiveData<String>): Binder
        = add(TextBinding.create(owner, view, data))
fun Binder.textBinding(owner: LifecycleOwner, view: TextView, data: Flow<String>): Binder
        = add(TextBinding.create(owner, view, data))
fun Binder.textBinding(view:TextView, data:LiveData<String>): Binder
        = add(TextBinding.create(requireOwner, view, data))
fun Binder.textBinding(view: TextView, data: Flow<String>): Binder
        = add(TextBinding.create(requireOwner, view, data))

fun Binder.editTextBinding(owner:LifecycleOwner, view:EditText, data:MutableLiveData<String>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditTextBinding.create(owner, view, data, mode))
fun Binder.editTextBinding(owner:LifecycleOwner, view:EditText, data:MutableStateFlow<String>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditTextBinding.create(owner, view, data, mode))
fun Binder.editTextBinding(view:EditText, data:MutableLiveData<String>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditTextBinding.create(requireOwner, view, data, mode))
fun Binder.editTextBinding(view:EditText, data:MutableStateFlow<String>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(EditTextBinding.create(requireOwner, view, data, mode))
