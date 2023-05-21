@file:Suppress("unused")

package io.github.toyota32k.binder

import android.annotation.SuppressLint
import androidx.lifecycle.*
import com.google.android.material.slider.Slider
import io.github.toyota32k.utils.asMutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.Float.max
import java.lang.Float.min

/**
 * Material Componentのスライダー
 * SeekBarより高機能だが、
 * 不正な値（範囲外とかStep位置からずれた値とか）を与えると死ぬので、それを回避するための補正を仕込んである。
 * そのため、EditTextと組み合わせたTwoWayバインドだと、なんか不自然な動きになる。
 *
 * min/max を変更するときに、valueが範囲外になると死ぬし、min==max になっても死ぬし、とにかく、そっと使うように。
 */
open class SliderBinding (
    override val data: LiveData<Float>,
    mode: BindingMode,
    private val min:LiveData<Float>? = null,
    private val max:LiveData<Float>? = null,
) : BaseBinding<Float>(mode), Slider.OnChangeListener {
    constructor(data:LiveData<Float>, min:LiveData<Float>?=null, max:LiveData<Float>?=null) : this(data, BindingMode.OneWay, min,max)

    private val slider: Slider?
        get() = view as? Slider

    private var minObserver: Observer<Float?>? = null
    private var maxObserver: Observer<Float?>? = null

    open fun connect(owner: LifecycleOwner, view:Slider) {
        super.connect(owner, view)
        if(max!=null) {
            maxObserver = Observer<Float?> { newValueTo ->
                if(newValueTo!=null) {
                    // max を変更した結果、value がレンジアウトする状態になると Slider::onDrawで死ぬので、valueをクリップしておく。
                    val v = clipByRange(view.valueFrom, newValueTo, view.value)
                    if(v!=view.value) {
                        view.value = v      // view.value を変えると、onValueChangedが呼ばれて、data.value 自動的にクリップされる。
                    }
                    view.valueTo = newValueTo
                }
            }.apply {
                max.observe(owner,this)
            }
        }
        if(min!=null) {
            minObserver = Observer<Float?> { newValueFrom->
                if(newValueFrom!=null) {
                    // min を変更した結果、value がレンジアウトする状態になると Slider::onDrawで死ぬので、valueをクリップしておく。
                    val v = clipByRange(newValueFrom, view.valueTo, view.value)
                    if(v!=view.value) {
                        view.value = v      // view.value を変えると、onValueChangedが呼ばれて、data.value 自動的にクリップされる。
                    }
                    view.valueFrom = newValueFrom
                }
            }.apply {
                min.observe(owner,this)
            }
        }
        if(mode!= BindingMode.OneWay) {
            view.addOnChangeListener(this)
            if(mode== BindingMode.OneWayToSource||data.value==null) {
                onValueChange(view, view.value, false)
            }
        }
    }

    override fun dispose() {
        minObserver?.let {
            min?.removeObserver(it)
            minObserver = null
        }
        maxObserver?.let {
            max?.removeObserver(it)
            maxObserver = null
        }
        if(mode!= BindingMode.OneWay) {
            slider?.removeOnChangeListener(this)
        }
        super.dispose()
    }

    private fun clipByRange(a:Float, b:Float, v:Float):Float {
        val min = min(a,b)
        val max = max(a,b)
        return min(max(min,v), max)
    }

    private fun fitToStep(v:Float, s:Float):Float {
        return if(s==0f) {
            v
        } else {
            @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
            s*Math.round(v/s)
        }
    }

    override fun onDataChanged(v: Float?) {
        val view = slider ?: return
        // SeekBar と違って、範囲外のValueを与えると描画時に例外が出て死ぬ。
        // stetSize!=0 の場合は、step位置からずれた値を入れるとやっぱり死ぬ。
        val t = fitToStep(clipByRange(view.valueFrom,view.valueTo, v ?: 0f),view.stepSize)
        if(view.value != t) {
            view.value = t
        }
    }
    // Slider.OnChangeListener
    @SuppressLint("RestrictedApi")
    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        if(data.value!=value) {
            mutableData?.value = value
        }
    }

    companion object {
        fun create(owner: LifecycleOwner, view: Slider, data:LiveData<Float>, min:LiveData<Float>?=null, max:LiveData<Float>?=null) : SliderBinding {
            return SliderBinding(data, BindingMode.OneWay, min, max).apply { connect(owner, view) }
        }
        fun create(owner: LifecycleOwner, view: Slider, data:MutableLiveData<Float>, mode: BindingMode = BindingMode.TwoWay, min:LiveData<Float>?=null, max:LiveData<Float>?=null) : SliderBinding {
            return SliderBinding(data, mode, min, max).apply { connect(owner, view) }
        }
    }
}

fun Binder.sliderBinding(owner: LifecycleOwner, view: Slider, data:LiveData<Float>, min:LiveData<Float>?=null, max:LiveData<Float>?=null): Binder
        = add(SliderBinding.create(owner, view, data, min, max))
fun Binder.sliderBinding(owner: LifecycleOwner, view: Slider, data: Flow<Float>, min:Flow<Float>?=null, max:Flow<Float>?=null): Binder
        = add(
    SliderBinding.create(
        owner,
        view,
        data.asLiveData(),
        min?.asLiveData(),
        max?.asLiveData()
    )
)
fun Binder.sliderBinding(view: Slider, data:LiveData<Float>, min:LiveData<Float>?=null, max:LiveData<Float>?=null): Binder
        = add(SliderBinding.create(requireOwner, view, data, min, max))
fun Binder.sliderBinding(view: Slider, data: Flow<Float>, min:Flow<Float>?=null, max:Flow<Float>?=null): Binder
        = add(
    SliderBinding.create(
        requireOwner,
        view,
        data.asLiveData(),
        min?.asLiveData(),
        max?.asLiveData()
    )
)

fun Binder.sliderBinding(owner: LifecycleOwner, view: Slider, data:MutableLiveData<Float>, mode: BindingMode = BindingMode.TwoWay, min:LiveData<Float>?=null, max:LiveData<Float>?=null): Binder
        = add(SliderBinding.create(owner, view, data, mode, min, max))
fun Binder.sliderBinding(owner: LifecycleOwner, view: Slider, data:MutableStateFlow<Float>, mode: BindingMode = BindingMode.TwoWay, min:Flow<Float>?=null, max:Flow<Float>?=null): Binder
        = add(
    SliderBinding.create(
        owner,
        view,
        data.asMutableLiveData(owner),
        mode,
        min?.asLiveData(),
        max?.asLiveData()
    )
)
fun Binder.sliderBinding(view: Slider, data:MutableLiveData<Float>, mode: BindingMode = BindingMode.TwoWay, min:LiveData<Float>?=null, max:LiveData<Float>?=null): Binder
        = add(SliderBinding.create(requireOwner, view, data, mode, min, max))
fun Binder.sliderBinding(view: Slider, data:MutableStateFlow<Float>, mode: BindingMode = BindingMode.TwoWay, min:Flow<Float>?=null, max:Flow<Float>?=null): Binder
        = add(
    SliderBinding.create(
        requireOwner,
        view,
        data.asMutableLiveData(requireOwner),
        mode,
        min?.asLiveData(),
        max?.asLiveData()
    )
)
