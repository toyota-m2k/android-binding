@file:Suppress("unused")

package io.github.toyota32k.binder

import android.widget.SeekBar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

open class SeekBarBinding(
    data: LiveData<Int>,
    min: LiveData<Int>? = null,
    max: LiveData<Int>? = null,
    mode: BindingMode
) : ProgressBarBinding(data,min,max,mode), SeekBar.OnSeekBarChangeListener {
    private val seekBar:SeekBar?
        get() = view as? SeekBar

    fun connect(owner: LifecycleOwner, view: SeekBar) {
        super.connect(owner,view)
        if(mode!= BindingMode.OneWay) {
            view.setOnSeekBarChangeListener(this)
            if(mode== BindingMode.OneWayToSource||data.value==null) {
                onProgressChanged(seekBar, seekBar?.progress ?: 0, false)
            }
        }
    }

    override fun dispose() {
        if(mode!= BindingMode.OneWay) {
            seekBar?.setOnSeekBarChangeListener(null)
        }
        super.dispose()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        mutableData?.apply {
            if(value != progress) {
                value = progress
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    companion object {
        fun create(owner: LifecycleOwner, view:SeekBar, data:LiveData<Int>,min:LiveData<Int>?=null, max:LiveData<Int>?=null): SeekBarBinding {
            return SeekBarBinding(data,min,max, BindingMode.OneWay).apply { connect(owner,view) }
        }
        fun create(owner: LifecycleOwner, view:SeekBar, data:MutableLiveData<Int>,min:LiveData<Int>?=null, max:LiveData<Int>?=null,mode: BindingMode = BindingMode.TwoWay): SeekBarBinding {
            return SeekBarBinding(data,min,max,mode).apply { connect(owner,view) }
        }
    }
}

fun Binder.seekBarBinding(owner: LifecycleOwner, view:SeekBar, data:LiveData<Int>, min:LiveData<Int>?=null, max:LiveData<Int>?=null) : Binder
        = add(SeekBarBinding.create(owner, view, data, min, max))
fun Binder.seekBarBinding(owner: LifecycleOwner, view:SeekBar, data: Flow<Int>, min:LiveData<Int>?=null, max:LiveData<Int>?=null) : Binder
        = add(SeekBarBinding.create(owner, view, data.asLiveData(), min, max))
fun Binder.seekBarBinding(owner: LifecycleOwner, view:SeekBar, data:MutableLiveData<Int>, min:LiveData<Int>?=null, max:LiveData<Int>?=null, mode: BindingMode = BindingMode.TwoWay) : Binder
        = add(SeekBarBinding.create(owner, view, data, min, max, mode))
fun Binder.seekBarBinding(owner: LifecycleOwner, view:SeekBar, data:MutableStateFlow<Int>, min:LiveData<Int>?=null, max:LiveData<Int>?=null, mode: BindingMode = BindingMode.TwoWay) : Binder
        = add(SeekBarBinding.create(owner, view, data.asMutableLiveData(owner), min, max, mode))

fun Binder.seekBarBinding(view:SeekBar, data:LiveData<Int>, min:LiveData<Int>?=null, max:LiveData<Int>?=null) : Binder
        = add(SeekBarBinding.create(requireOwner, view, data, min, max))
fun Binder.seekBarBinding(view:SeekBar, data: Flow<Int>, min:LiveData<Int>?=null, max:LiveData<Int>?=null) : Binder
        = add(SeekBarBinding.create(requireOwner, view, data.asLiveData(), min, max))
fun Binder.seekBarBinding(view:SeekBar, data:MutableLiveData<Int>, min:LiveData<Int>?=null, max:LiveData<Int>?=null, mode: BindingMode = BindingMode.TwoWay) : Binder
        = add(SeekBarBinding.create(requireOwner, view, data, min, max, mode))
fun Binder.seekBarBinding(view:SeekBar, data:MutableStateFlow<Int>, min:LiveData<Int>?=null, max:LiveData<Int>?=null, mode: BindingMode = BindingMode.TwoWay) : Binder
        = add(
    SeekBarBinding.create(
        requireOwner,
        view,
        data.asMutableLiveData(requireOwner),
        min,
        max,
        mode
    )
)
