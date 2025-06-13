@file:Suppress("unused", "PackageDirectoryMismatch")

package io.github.toyota32k.binder.command

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.utils.IDisposable
import java.lang.ref.WeakReference

/**
 * コマンドクラスのif定義
 */
interface ICommand<T>: IDisposable {
    @MainThread
    fun attachView(view: View, value: T) : IDisposable

    @MainThread
    fun bind(owner: LifecycleOwner, fn: (T) -> Unit): IDisposable

    @MainThread
    fun bindForever(fn:(T)->Unit): IDisposable

    @MainThread
    fun attachAndBind(owner: LifecycleOwner, view: View, value:T, fn:((T)->Unit)):IDisposable

    @MainThread
    fun reset()

    @MainThread
    fun invoke(value:T)
}

/**
 * attachViewに関する共通実装
 */
abstract class CommandBase<T> : ICommand<T> {
    class ClickListenerDisposer(v:View, var bind:IDisposable?=null) : IDisposable {
        var view: WeakReference<View>? = WeakReference<View>(v)

        override fun dispose() {
            bind?.dispose()
            view?.get()?.apply {
                if(this is EditText) {
                    setOnEditorActionListener(null)
                } else {
                    setOnClickListener(null)
                }
            }
            view = null
            bind = null
        }
    }

    protected open fun internalAttachView(view: View, value: T) {
        if(view is EditText) {
            view.setOnEditorActionListener {_,actionId,event->
                if (actionId == EditorInfo.IME_ACTION_DONE || event?.action == KeyEvent.ACTION_DOWN && (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                    invoke(value)
                    true
                } else false
            }
        } else {
            view.setOnClickListener {
                invoke(value)
            }
        }
    }

    @MainThread
    override fun attachView(view: View, value: T): IDisposable {
        internalAttachView(view, value)
        return ClickListenerDisposer(view)
    }

    @MainThread
    override fun attachAndBind(owner: LifecycleOwner, view: View, value:T, fn: (T) -> Unit): IDisposable {
        internalAttachView(view, value)
        return ClickListenerDisposer(view, bind(owner, fn))
    }
}

/**
 * コールバックに引数を取らないコマンドクラスのi/f
 * ICommand<Unit> そのものなのだが、invoke(Unit) などと書かないといけないのは、かっちょ悪すぎるので。
 */
interface IUnitCommand : IDisposable {
    @MainThread
    fun invoke()

    @MainThread
    fun attachAndBind(owner: LifecycleOwner, view: View, fn: () -> Unit): IDisposable

    @MainThread
    fun attachView(view: View):IDisposable

    @MainThread
    fun bind(owner: LifecycleOwner, fn: () -> Unit): IDisposable

    @MainThread
    fun bindForever(fn:()->Unit): IDisposable

    @MainThread
    fun reset()
}

open class UnitCommand(private val command:ICommand<Unit>) : IUnitCommand {
    override fun invoke()
        = command.invoke(Unit)

    override fun attachAndBind(owner: LifecycleOwner, view: View, fn: () -> Unit): IDisposable
        = command.attachAndBind(owner,view,Unit) { fn() }

    override fun attachView(view: View):IDisposable
        = command.attachView(view,Unit)

    override fun bind(owner: LifecycleOwner, fn: () -> Unit): IDisposable
        = command.bind(owner) { fn() }

    override fun bindForever(fn: () -> Unit): IDisposable
        = command.bindForever { fn() }

    override fun reset()
        = command.reset()

    override fun dispose()
        = command.dispose()
}

// region ICommand<T> binding

/**
 * コマンドにビューを１つずつアタッチする。
 */
fun <T> Binder.bindCommand(cmd:ICommand<T>, view:View, param:T): Binder
        = bindCommand(cmd, Pair(view,param))

/**
 * コマンドに１つ以上のビューをまとめてアッタッチする
 */
fun <T> Binder.bindCommand(cmd:ICommand<T>, vararg views:Pair<View,T>): Binder {
    views.forEach {  pair->
        add(cmd.attachView(pair.first, pair.second))
    }
    return this
}

/**
 * コマンドにハンドラ（コールバック）をバインドする
 */
fun <T> Binder.bindCommand(owner: LifecycleOwner, cmd:ICommand<T>, callback:(T)->Unit): Binder {
    add(cmd.bind(owner,callback))
    return this
}

/**
 * コマンドにビューをアタッチし、ハンドラ（コールバック）をバインドする
 */
fun <T> Binder.bindCommand(owner: LifecycleOwner, cmd:ICommand<T>, vararg views:Pair<View,T>, callback:(T)->Unit): Binder {
    bindCommand(cmd, *views)
    add(cmd.bind(owner,callback))
    return this
}

/**
 * コマンドにハンドラ（コールバック）をバインドする
 * Ownerなし版
 */
fun <T> Binder.bindCommand(cmd:ICommand<T>, callback:(T)->Unit): Binder
        = bindCommand(requireOwner, cmd, callback)

/**
 * コマンドにビューをアタッチし、ハンドラ（コールバック）をバインドする
 * Ownerなし版
 */
fun <T> Binder.bindCommand(cmd:ICommand<T>, vararg views:Pair<View,T>, callback:(T)->Unit): Binder
        = bindCommand(requireOwner, cmd, views=views, callback)

// endregion


// region IUnitCommand binding

/**
 * コマンドにビューをアッタッチする
 */
fun Binder.bindCommand(cmd:IUnitCommand, vararg views:View): Binder {
    views.forEach { view->
        add(cmd.attachView(view))
    }
    return this
}

/**
 * コマンドにハンドラ(callback)をバインドする
 */
fun Binder.bindCommand(owner: LifecycleOwner, cmd:IUnitCommand, callback:()->Unit): Binder
        = add(cmd.bind(owner,callback))

/**
 * コマンドにビューをアタッチし、ハンドラ (callback) をバインドする
 */
fun Binder.bindCommand(owner: LifecycleOwner, cmd:IUnitCommand, view:View, callback:()->Unit): Binder
        = bindCommand(cmd,view).add(cmd.bind(owner,callback))
fun Binder.bindCommand(owner: LifecycleOwner, cmd:IUnitCommand, vararg views:View, callback:()->Unit): Binder
        = bindCommand(cmd,*views).add(cmd.bind(owner,callback))

/**
 * コマンドにハンドラ(callback)をバインドする
 * ownerなし版
 */
fun Binder.bindCommand(cmd:IUnitCommand, callback:()->Unit): Binder
        = add(cmd.bind(requireOwner,callback))
/**
 * コマンドにビューをアタッチし、ハンドラ (callback) をバインドする
 * ownerなし版
 */
fun Binder.bindCommand(cmd:IUnitCommand, view:View, callback:()->Unit): Binder
        = bindCommand(requireOwner, cmd, view, callback)
fun Binder.bindCommand(cmd:IUnitCommand, vararg views:View, callback:()->Unit): Binder
        = bindCommand(requireOwner, cmd, views=views, callback)


// endregion