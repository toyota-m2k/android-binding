package io.github.toyota32k.binder

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.coroutineScope
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.lifecycle.disposableObserve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class BaseFlowBinding<T>(override val mode: BindingMode) : IBinding {
    abstract val data : StateFlow<T>
    open val mutableData : MutableStateFlow<T>?
        get() = data as MutableStateFlow<T>

    open var view: View? = null
    private var scope: CoroutineScope? = null

    open fun connect(owner:LifecycleOwner, view:View) {
        this.view = view
        if(mode!= BindingMode.OneWayToSource) {
            val sc = owner.lifecycle.coroutineScope.apply { scope = this }
            data.onEach { onDataChanged(it) }.launchIn(sc)
            // data.value==null のときobserveのタイミングでonDataChanged()が呼ばれないような現象があったので明示的に呼び出しておく。
            onDataChanged(data.value)
        }
    }

    protected abstract fun onDataChanged(v:T?)

    override fun dispose() {
        view = null
        scope?.cancel("disposed")
        scope = null
    }

}