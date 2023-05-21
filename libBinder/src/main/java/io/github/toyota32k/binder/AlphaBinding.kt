@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

class AlphaBinding(
    override val data:LiveData<Float>
) : BaseBinding<Float>(BindingMode.OneWay) {
    override fun onDataChanged(v: Float?) {
        if(v!=null) {
            view?.apply {
                alpha = v
            }
        }
    }
    companion object {
        fun create(owner: LifecycleOwner, view: View, data:LiveData<Float>): AlphaBinding {
            return AlphaBinding(data).apply { connect(owner,view) }
        }
    }
}

fun Binder.alphaBinding(owner: LifecycleOwner, view: View, data:LiveData<Float>): Binder
    = add(AlphaBinding.create(owner, view, data))
fun Binder.alphaBinding(view: View, data:LiveData<Float>): Binder
    = add(AlphaBinding.create(requireOwner, view, data))
