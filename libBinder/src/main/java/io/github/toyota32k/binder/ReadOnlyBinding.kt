@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow

class ReadOnlyBinding(
    rawData: LiveData<Boolean>,
    boolConvert: BoolConvert = BoolConvert.Straight)
    : BoolBinding(rawData, BindingMode.TwoWay, boolConvert) {
    override fun onDataChanged(v: Boolean?) {
        val view = this.view as? EditText ?: return
        val writable = v!=true
        view.isClickable = writable
        view.isCursorVisible = writable
        view.isFocusable = writable
        view.isFocusableInTouchMode = writable
        view.inputType =
        if(writable) inputType else EditorInfo.TYPE_NULL
    }

    var inputType:Int = EditorInfo.TYPE_NULL

    override fun connect(owner: LifecycleOwner, view: View) {
        inputType = (view as? EditText)?.inputType ?: EditorInfo.TYPE_NULL
        super.connect(owner, view)

    }

    companion object {
        fun create(owner: LifecycleOwner, view: EditText, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight) : ReadOnlyBinding {
            return ReadOnlyBinding(data, boolConvert).apply { connect(owner, view) }
        }
    }
}

fun Binder.readOnlyBinding(owner: LifecycleOwner, view: EditText, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): Binder
        = add(ReadOnlyBinding.create(owner, view, data, boolConvert))
fun Binder.readOnlyBinding(owner: LifecycleOwner, view: EditText, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): Binder
        = add(ReadOnlyBinding.create(owner, view, data.asLiveData(), boolConvert))
fun Binder.readOnlyBinding(view: EditText, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): Binder
        = add(ReadOnlyBinding.create(requireOwner, view, data, boolConvert))
fun Binder.readOnlyBinding(view: EditText, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight): Binder
        = add(ReadOnlyBinding.create(requireOwner, view, data.asLiveData(), boolConvert))
