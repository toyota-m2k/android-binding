@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import io.github.toyota32k.binder.anim.IReversibleAnimation
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.lifecycle.disposableObserve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnimationBinding(
    data: LiveData<Boolean>,
    boolConvert: BoolConvert = BoolConvert.Straight
): BoolBinding(data.distinctUntilChanged(), BindingMode.OneWay, boolConvert) {
    private var animation: IReversibleAnimation? = null

    fun setAnimation(owner:LifecycleOwner, animation: IReversibleAnimation, applyLastState:Boolean) {
        if(observed==null) {
            this.animation = null
            observed = data.disposableObserve(owner, this::onDataChanged)
        }
        this.animation = animation

        if(applyLastState) {
            val v = data.value
            if(v!=null) {
                animation.invokeLastState(v)
            }
        }
    }

    override fun connect(owner: LifecycleOwner, view: View) {
        UtLog.libLogger.assert( false,"use setAnimation().")
    }

    override fun onDataChanged(v: Boolean?) {
        val anim = animation
        if(v!=null&&anim!=null) {
            CoroutineScope(Dispatchers.Main).launch {
                anim.run(v)
            }
        }
    }

    override fun dispose() {
        super.dispose()
        animation = null
    }
}

fun Binder.animationBinding(owner:LifecycleOwner, animation: IReversibleAnimation, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyLastState:Boolean): Binder
    = add(AnimationBinding(data,boolConvert).apply { setAnimation(owner, animation, applyLastState) })
fun Binder.animationBinding(animation: IReversibleAnimation, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, applyLastState:Boolean): Binder
    = add(AnimationBinding(data,boolConvert).apply { setAnimation(requireOwner, animation, applyLastState) })
