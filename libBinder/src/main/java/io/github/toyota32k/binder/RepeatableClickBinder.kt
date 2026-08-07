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
    view:View?=null,
    private val activationTime:Duration = 300.milliseconds,
    private val repeatInterval: Duration = 100.milliseconds,
    handler: ((EventType)->Unit)? = null
): IDisposable {
    private var view: View? by WeakReferenceDelegate()
    private var callback: ((EventType)->Unit)? = handler

//    private val logger = UtLog("ClickRepeater", UtLib.logger, UtClickRepeater::class.java)

    enum class EventType(val on:Boolean) {
        CLICKED(true),
        REPEATING(true),
        RELEASED(false),
    }

    private var repeating:Boolean = false
    init {
        if(view!=null) {
            attachView(view)
        }
    }

    private var job: Job? = null

    @MainThread
    fun run() {
        if(repeating) return
        repeating = true

        job = CoroutineScope(Dispatchers.Main).launch {
            delay(activationTime)
            while (isActive && repeating) {
                callback?.invoke(EventType.REPEATING)
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
    fun attachView(view:View, handler: ((EventType) -> Unit)?=null) {
        repeating = false
        this.view = view
        if (handler != null) {
            this.callback = handler
        }
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    stop()
                    callback?.invoke(EventType.RELEASED)
                }

                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    callback?.invoke(EventType.CLICKED)
                    run()
                }
                else -> {}
            }
            false
        }
    }

    override fun dispose() {
        view?.setOnTouchListener(null)
        view = null
        job?.cancel()
        job = null
        callback = null
    }
}

class RepeatableClickBinder<V>(
    owner:LifecycleOwner,
    view: V,
    activationTime:Duration,
    repeatInterval: Duration,
    onClick:((V)->Unit),
    onRelease:((V)->Unit)?,
): IBinding where V:View {

    constructor(
        owner:LifecycleOwner,
        view: V,
        activationTime:Duration,
        repeatInterval: Duration,
        onClick:((V)->Unit)): this(owner, view, activationTime, repeatInterval, onClick, null)

    override val mode: BindingMode = BindingMode.OneWayToSource
    private val callback = Callback<ClickRepeater.EventType,Unit>(owner) {
        if (it.on) {
            onClick(view)
        } else {
            onRelease?.invoke(view)
        }
    }
    private val repeater = ClickRepeater(view,activationTime, repeatInterval, callback::invoke)
    override fun dispose() {
        callback.dispose()
        repeater.dispose()
    }

    class Builder<V>(
        private val owner: LifecycleOwner,
        private val view: V,
    ) where V:View {

        private var mActivationTime:Duration = 500.milliseconds
        private var mRepeatInterval: Duration = 500.milliseconds
        private lateinit var mOnClick:((V)->Unit)
        private var mOnRelease:((V)->Unit)? = null

        fun activationTime(time:Duration): Builder<V> = apply {
            mActivationTime = time
        }
        fun repeatInterval(interval:Duration): Builder<V> = apply {
            mRepeatInterval = interval
        }
        fun onClick(handler:((V)->Unit)): Builder<V> = apply {
            mOnClick = handler
        }
        fun onRelease(handler:((V)->Unit)): Builder<V> = apply {
            mOnRelease = handler
        }
        fun build(): RepeatableClickBinder<V> {
            return RepeatableClickBinder<V>(owner, view, mActivationTime, mRepeatInterval, mOnClick, mOnRelease)
        }
    }
}

fun Binder.repeatableClickBinding(owner:LifecycleOwner, view:View, activationTime:Duration, repeatInterval: Duration, onClick:((View)->Unit)): Binder
        = add(RepeatableClickBinder(owner, view, activationTime, repeatInterval, onClick))

fun Binder.repeatableClickBinding(view:View, activationTime:Duration, repeatInterval: Duration, onClick:((View)->Unit))
        = add(RepeatableClickBinder(requireOwner, view, activationTime, repeatInterval, onClick))

fun Binder.repeatableClickBinding(owner:LifecycleOwner, view:View, customize:RepeatableClickBinder.Builder<View>.()->Unit): Binder
        = add(RepeatableClickBinder.Builder(owner, view).apply { customize() }.build())

fun Binder.repeatableClickBinding(view:View, customize:RepeatableClickBinder.Builder<View>.()->Unit): Binder
        = add(RepeatableClickBinder.Builder(requireOwner, view).apply { customize() }.build())

fun <T> Binder.bindRepeatableCommand(cmd: ICommand<T>, view:View, param:T, activationTime:Duration=500.milliseconds, repeatInterval: Duration=500.milliseconds): Binder
        = repeatableClickBinding(view, activationTime, repeatInterval) { cmd.invoke(param) }

fun <T> Binder.bindRepeatableCommand(owner:LifecycleOwner, cmd: ICommand<T>, view:View, param:T, activationTime:Duration=500.milliseconds, repeatInterval: Duration=500.milliseconds): Binder
        = repeatableClickBinding(owner, view, activationTime, repeatInterval) { cmd.invoke(param) }
