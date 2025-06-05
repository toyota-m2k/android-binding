package io.github.toyota32k.binder

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.lifecycle.disposableObserve


//abstract class DisposableImpl : IBinding {
//    protected abstract fun cleanup()
//
//    private var alive:Boolean = true
//
//    override fun dispose() {
//        if(alive) {
//            alive = false
//            cleanup()
//        }
//    }
//
//    override fun isDisposed(): Boolean {
//        return !alive
//    }
//}

abstract class BaseBinding<T>(override val mode: BindingMode) : IBinding {
    abstract val data: LiveData<T>
    open val mutableData : MutableLiveData<T>?
        get() = data as? MutableLiveData<T>

    open var view: View? = null
    protected var observed: IDisposable? = null

    open fun connect(owner:LifecycleOwner, view:View) {
        this.view = view
        if(mode!= BindingMode.OneWayToSource) {
            observed = data.disposableObserve(owner,this::onDataChanged)
            // data.value==null のときobserveのタイミングでonDataChanged()が呼ばれないような現象があったので明示的に呼び出しておく。
            onDataChanged(data.value)
        }
    }

    protected abstract fun onDataChanged(v:T?)

    override fun dispose() {
        view = null
        observed?.dispose()
        observed = null
    }
}