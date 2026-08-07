package io.github.toyota32k.binder

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import io.github.toyota32k.binder.command.ICommand
import io.github.toyota32k.binder.command.IUnitCommand
import io.github.toyota32k.utils.WeakReferenceDelegate

/**
 *　ビューに対するタッチ(ACTION_DOWN)と指を離す(ACTION_UP) 動作をコールバックまたはコマンドにバインドするクラス
 */
class ClickReleaseBinding(
    view:View?=null,
    handler: ((Boolean)->Unit)? = null
    ): IBinding {
    override val mode: BindingMode = BindingMode.OneWayToSource
    private var view: View? by WeakReferenceDelegate()
    private var callback: ((Boolean)->Unit)? = handler
    private var pushed: Boolean = false

    init {
        if(view!=null) {
            attachView(view)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attachView(view:View, handler: ((Boolean) -> Unit)?=null) {
        detachView()
        this.view = view
        if (handler != null) {
            this.callback = handler
        }
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    if (pushed) {
                        pushed = false
                        callback?.invoke(false)
                    }
                }

                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    pushed = true
                    callback?.invoke(true)
                }
                else -> {}
            }
            false
        }
    }

    private fun detachView() {
        val v = view ?: return
        if (pushed) {
            pushed = false
            callback?.invoke(false)
        }
        view?.setOnTouchListener(null)
        view = null
    }

    override fun dispose() {
        detachView()
        callback = null
    }
}

fun Binder.clickReleaseBinding(view:View, callback: (Boolean)->Unit):Binder
    = apply { ClickReleaseBinding(view, callback) }

fun Binder.bindClickReleaseCommand(clickCommand: IUnitCommand, releaseCommand: IUnitCommand, view:View): Binder
    = clickReleaseBinding(view) { if (it) { clickCommand } else { releaseCommand }.invoke() }

fun Binder.bindClickReleaseCommand(command: ICommand<Boolean>, view: View): Binder
    = clickReleaseBinding(view, command::invoke)