package io.github.toyota32k.binder

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.toyota32k.binder.command.ICommand
import io.github.toyota32k.logger.Chronos
import io.github.toyota32k.logger.UtLog
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.UtLib
import io.github.toyota32k.utils.WeakReferenceDelegate
import io.github.toyota32k.utils.gesture.UtClickRepeater
import io.github.toyota32k.utils.lifecycle.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * ビューを押し続けたときに、一定間隔でイベントを発行し続けるクラス
 * 単体で使ってもよいが、RepeatableClickBinderと組み合わせて使うことを想定している。
 * @param handler タップイベントのハンドラ。falseを返すと、その時点で反復終了
 */
class ClickRepeater (
    val owner: LifecycleOwner,
    view:View?=null,
    private val activationTime:Duration = 300.milliseconds,
    private val repeatInterval: Duration = 100.milliseconds,
    handler: (suspend (View)->Boolean)? = null
): IDisposable {
    private var view: View? by WeakReferenceDelegate()
    private var callback: (suspend (View)->Boolean)? = handler

    private val logger = UtLog("ClickRepeater", UtLib.logger, UtClickRepeater::class.java)
    private var chronos = Chronos(logger)

    private var repeating:Boolean = false
    init {
        if(view!=null) {
            attachView(view)
        }
    }

    private var job: Job? = null

    private suspend fun invokeCallback():Boolean {
        val v = view ?: return false
        return callback?.invoke(v) == true
    }

    @MainThread
    private fun start() {
        if(repeating) return
        repeating = true

        job = owner.lifecycleScope.launch {
            invokeCallback()
            delay(activationTime)
            while (isActive && repeating) {
                if (!invokeCallback()) {
                    break
                }
                delay(repeatInterval)
            }
        }
    }

    @MainThread
    fun stop() {
        repeating = false
        job?.cancel()
        job = null
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attachView(view:View, handler: (suspend (View) -> Boolean)?=null) {
        repeating = false
        this.view = view
        if (handler != null) {
            this.callback = handler
        }
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    chronos.lap("Touch - UP")
                    stop()
                }

                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    chronos.lap("Touch - DOWN")
                    start()
                }
                else -> {}
            }
            false
        }
    }

    override fun dispose() {
        callback = null
        view?.setOnTouchListener(null)
        view = null
        job?.cancel()
        job = null
    }
}

/**
 * ビューを押し続けたときに、一定間隔でイベントを発行し続けるバインダークラス
 */
class RepeatableClickBinder(
    val repeater: ClickRepeater
): IBinding {
    constructor(
        owner:LifecycleOwner,
        view: View,
        activationTime:Duration,
        repeatInterval: Duration,
        onClick:suspend (View)->Boolean
    ) : this(ClickRepeater(owner, view, activationTime, repeatInterval, onClick))

    override val mode: BindingMode = BindingMode.OneWayToSource
    override fun dispose() {
        repeater.dispose()
    }

    class Builder(private val owner: LifecycleOwner, private val view: View,) {
        private var mActivationTime:Duration = 500.milliseconds
        private var mRepeatInterval: Duration = 500.milliseconds
        private lateinit var mOnClick:(suspend (View)->Boolean)
        private var mOnRelease:((View)->Unit)? = null

        fun activationTime(time:Duration): Builder = apply {
            mActivationTime = time
        }
        fun repeatInterval(interval:Duration): Builder = apply {
            mRepeatInterval = interval
        }
        fun onClick(handler:(suspend (View)->Boolean)): Builder = apply {
            mOnClick = { handler(view) }
        }
        fun onRelease(handler:((View)->Unit)): Builder = apply {
            mOnRelease = handler
        }
        fun build(): RepeatableClickBinder {
            return RepeatableClickBinder (owner, view, mActivationTime, mRepeatInterval, mOnClick)
        }
    }
}

fun Binder.repeatableClickBinding(owner:LifecycleOwner, view:View, activationTime:Duration, repeatInterval: Duration, onClick:(suspend (View)->Boolean)): Binder
        = add(RepeatableClickBinder(owner, view, activationTime, repeatInterval, onClick))

fun Binder.repeatableClickBinding(view:View, activationTime:Duration, repeatInterval: Duration, onClick:(suspend (View)->Boolean))
        = add(RepeatableClickBinder(requireOwner, view, activationTime, repeatInterval, onClick))

fun Binder.repeatableClickBinding(owner:LifecycleOwner, view:View, customize:RepeatableClickBinder.Builder.()->Unit): Binder
        = add(RepeatableClickBinder.Builder(owner, view).apply { customize() }.build())

fun Binder.repeatableClickBinding(view:View, customize:RepeatableClickBinder.Builder.()->Unit): Binder
        = add(RepeatableClickBinder.Builder(requireOwner, view).apply { customize() }.build())

fun <T> Binder.bindRepeatableCommand(cmd: ICommand<T>, view:View, param:T, activationTime:Duration=500.milliseconds, repeatInterval: Duration=500.milliseconds): Binder
        = repeatableClickBinding(view, activationTime, repeatInterval) {
            cmd.invoke(param)
            true
        }

fun <T> Binder.bindRepeatableCommand(owner:LifecycleOwner, cmd: ICommand<T>, view:View, param:T, activationTime:Duration=500.milliseconds, repeatInterval: Duration=500.milliseconds): Binder
        = repeatableClickBinding(owner, view, activationTime, repeatInterval) {
            cmd.invoke(param)
            true
        }
