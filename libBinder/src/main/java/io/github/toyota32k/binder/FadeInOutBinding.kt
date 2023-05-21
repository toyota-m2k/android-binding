@file:Suppress("unused")

package io.github.toyota32k.binder

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.disposableObserve
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

abstract class FadeInOutBase (
    data: LiveData<Boolean>,
    boolConvert: BoolConvert = BoolConvert.Straight,
    private val animDuration:Long = 500  // ms
) : BoolBinding(data, BindingMode.OneWay, boolConvert), Animator.AnimatorListener {

    private var showing = false
    private val animating get() = animator.isStarted

    private val animator = ValueAnimator.ofFloat(0f,1f)!!.also { a->
        a.duration = animDuration
        a.addUpdateListener(this::updateAlpha)
        a.addListener(this)
    }

    private val targetVisible:Boolean               // data.value
        get() = data.value == true

    protected abstract var alpha:Float                // view.alpha
    protected abstract var  visibility:  Int          // view.visibility

    private fun updateAlpha(a:ValueAnimator) {
        alpha = if(targetVisible) a.animatedValue as Float else 1f - a.animatedValue as Float
    }

    override fun onDataChanged(v: Boolean?) {
        if(v==true) {
            show()
        } else {
            hide()
        }
    }

    private fun show() {
        if(animating) {
            if(showing) return
            animator.cancel()
            animator.duration = calcRewindingDuration(alpha, showing)
        } else {
            if(visibility == View.VISIBLE) return
            alpha = 0f
            visibility = View.VISIBLE
            animator.duration = animDuration
        }
        showing = true
        animator.start()
    }

    private fun hide() {
        if(animating) {
            if(!showing) return
            animator.cancel()
            animator.duration = calcRewindingDuration(alpha, showing)
        } else {
            if(visibility != View.VISIBLE) return
            animator.duration = animDuration
        }
        showing = false
        animator.start()
    }

    // 巻き戻しに必要な時間
    private fun calcRewindingDuration(alpha:Float, showing:Boolean):Long {
        val ar = if(showing) alpha else 1f-alpha
        val d = (animDuration.toFloat()*ar).toLong()
        return max(100L, d)
    }

    override fun onAnimationStart(animation: Animator) {
    }

    override fun onAnimationEnd(animation: Animator) {
        if(targetVisible) {
            alpha = 1f
            visibility = View.VISIBLE
        } else {
            visibility = View.INVISIBLE
        }
    }

    override fun onAnimationCancel(animation: Animator) {
    }

    override fun onAnimationRepeat(animation: Animator) {
    }
}

class FadeInOutBinding(
    data: LiveData<Boolean>,
    boolConvert: BoolConvert = BoolConvert.Straight,
    animDuration:Long = 500  // ms
    ) : FadeInOutBase(data, boolConvert, animDuration), Animator.AnimatorListener {

    override var alpha: Float
        get() = view?.alpha ?: 0f
        set(value) { view?.alpha = value }

    override var visibility: Int
        get() = view?.visibility ?: View.INVISIBLE
        set(value) { view?.visibility = value }

    companion object {
        fun create(owner: LifecycleOwner, view: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): FadeInOutBinding {
            return FadeInOutBinding(data, boolConvert, duration).apply { connect(owner, view) }
        }
    }
}

class MultiFadeInOutBinding(
    data: LiveData<Boolean>,
    boolConvert: BoolConvert = BoolConvert.Straight,
    animDuration: Long = 500
) : FadeInOutBase(data, boolConvert, animDuration) {
    private val views = mutableListOf<View>()

    override var alpha: Float
        get() = views.firstOrNull()?.alpha ?: 0f
        set(value) = views.forEach { it.alpha = value }

    override var visibility: Int
        get() = views.firstOrNull()?.visibility ?: View.INVISIBLE
        set(value) = views.forEach { it.visibility = value }

    override fun connect(owner: LifecycleOwner, view:View) {
        UtLog.libLogger.assert( false,"use connectAll() method.")
    }

    fun connectAll(owner: LifecycleOwner, vararg targets:View) : MultiFadeInOutBinding {
        UtLog.libLogger.assert(mode== BindingMode.OneWay, "MultiVisibilityBinding ... support OneWay mode only.")
        if(observed==null) {
            observed = data.disposableObserve(owner, this::onDataChanged)
        }
        views.addAll(targets)
        if(data.value==null) {
            onDataChanged(data.value)
        }
        return this
    }

    override fun dispose() {
        views.clear()
        super.dispose()
    }
}

fun Binder.fadeInOutBinding(owner: LifecycleOwner, view: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): Binder
        = add(FadeInOutBinding.create(owner, view, data, boolConvert, duration))
fun Binder.fadeInOutBinding(owner: LifecycleOwner, view: View, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): Binder
        = add(FadeInOutBinding.create(owner, view, data.asLiveData(), boolConvert, duration))
fun Binder.fadeInOutBinding(view: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): Binder
        = add(FadeInOutBinding.create(requireOwner, view, data, boolConvert, duration))
fun Binder.fadeInOutBinding(view: View, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): Binder
        = add(FadeInOutBinding.create(requireOwner, view, data.asLiveData(), boolConvert, duration))


fun Binder.multiFadeInOutBinding(owner: LifecycleOwner, views: Array<View>, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): Binder
        = add(MultiFadeInOutBinding(data, boolConvert, duration).apply { connectAll(owner, *views) })
fun Binder.multiFadeInOutBinding(owner: LifecycleOwner, views: Array<View>, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): Binder
        = add(MultiFadeInOutBinding(data.asLiveData(), boolConvert, duration).apply { connectAll(owner, *views) })
fun Binder.multiFadeInOutBinding(views: Array<View>, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): Binder
        = add(MultiFadeInOutBinding(data, boolConvert, duration).apply { connectAll(requireOwner, *views) })
fun Binder.multiFadeInOutBinding(views: Array<View>, data: Flow<Boolean>, boolConvert: BoolConvert = BoolConvert.Straight, duration:Long=500): Binder
        = add(MultiFadeInOutBinding(data.asLiveData(), boolConvert, duration).apply { connectAll(requireOwner, *views) })
