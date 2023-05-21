package io.github.toyota32k.binder.anim

import android.animation.Animator
import android.animation.ValueAnimator
import io.github.toyota32k.binder.anim.IReversibleAnimation.Companion.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Long.max
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ValueAnimatorを使った、IReversibleAnimation の最も基本的な実装
 *
 */
@Suppress("unused")
class ReversibleValueAnimation(override val duration:Long) : IReversibleAnimation {
    private var mutex = Mutex()
    private var driver: Driver? = null

    override val reverse: Boolean = driver?.reverse ?: false
    override val running: Boolean
        get() = driver!=null

    private fun correctValue(v:Float, range:Float, reverse:Boolean) : Float {
        return if(!reverse) v else range - v
    }
    private inner class Driver private constructor(val reverse:Boolean, localDuration:Long, val range:Float, private val needStartEvent:Boolean) : Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
        constructor(reverse:Boolean) : this(reverse, duration, 1f, true)
        constructor(reverse:Boolean, localDuration:Long, range:Float) : this(reverse, localDuration, range, false)
//        constructor(prevDriver:Driver) : this(!prevDriver.reverse,  (duration.toFloat() * prevDriver.animValue).toLong(), prevDriver.animValue)

        private val animator = ValueAnimator.ofFloat(0f,range).also { it.duration = localDuration }
        val animValue:Float get() = animator.animatedValue as Float
        val correctedValue:Float
            get() = correctValue(animator.animatedValue as Float, range, reverse)

        var closed:Boolean = false
            private set
        private var continuation:Continuation<Boolean>? = null

        fun start(continuation: Continuation<Boolean>) {
            animator.addUpdateListener(this)
            animator.addListener(this)
            this.continuation = continuation
            if(needStartEvent) {
                startEvent?.invoke(reverse)
            }
            animator.start()
        }

        fun cancel() {
            close(false)
            animator.cancel()
        }

        private fun close(result:Boolean) {
            closed = true
            animator.removeUpdateListener(this)
            animator.removeListener(this)
            continuation?.resume(result)
            continuation = null
            CoroutineScope(Dispatchers.Default).launch {
                mutex.withLock {
                    if(this@Driver == driver) {
                        driver = null
                    }
                }
            }
        }

        override fun onAnimationStart(animation: Animator) {
            logger.debug()
        }

        override fun onAnimationEnd(animation: Animator) {
            logger.debug()
            if(!closed) {
                endEvent?.invoke(reverse)
                close(true)
            }
        }

        override fun onAnimationCancel(animation: Animator) {
            logger.debug()
        }

        override fun onAnimationRepeat(animation: Animator) {
            logger.debug()
        }

        override fun onAnimationUpdate(animation: ValueAnimator) {
            updateEvent?.invoke(correctedValue)
        }
    }

    override suspend fun run(reverse: Boolean): Boolean {
        val newDriver = mutex.withLock {
            val oldDriver = driver
            if (oldDriver != null && !oldDriver.closed) {
                if (oldDriver.reverse == reverse) {
                    return false    // already executing...
                }
                val animValue = oldDriver.animValue
                val partialDuration = max(100L, (duration.toFloat() * animValue).toLong())
                oldDriver.cancel()
                this.driver = Driver(reverse,  partialDuration, animValue)
            } else {
                this.driver = Driver(reverse)
            }
            this.driver!!
        }
        return suspendCoroutine { cont->
            newDriver.start(cont)
        }
    }

    override fun invokeLastState(reverse: Boolean) {
        val v = correctValue(1f, 1f, reverse = reverse)
        updateEvent?.invoke(v)
        endEvent?.invoke(reverse)
    }

    private var startEvent: ((reverse:Boolean)->Unit)? = null
    private var endEvent: ((reverse:Boolean)->Unit)? = null
    private var updateEvent: ((value:Float)->Unit)? = null

    fun onStart(fn:(reverse:Boolean)->Unit): ReversibleValueAnimation {
        startEvent = fn
        return this
    }
    fun onEnd(fn:(reverse:Boolean)->Unit): ReversibleValueAnimation {
        endEvent = fn
        return this
    }
    fun onUpdate(fn:(value:Float)->Unit): ReversibleValueAnimation {
        updateEvent = fn
        return this
    }

}