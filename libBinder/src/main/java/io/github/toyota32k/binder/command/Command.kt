@file:Suppress("unused", "PackageDirectoryMismatch")

package io.github.toyota32k.binder.command

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.lifecycle.Listeners
import java.lang.ref.WeakReference

/**
 * Button の onClickイベントにバインドできるコマンドクラス。
 * コマンドハンドラに、タップされたビューが渡される。。。が、いままで、一度も、このビューを使ったことがない。
 * 今後は、LiteUnitCommandや ReliableUnitCommand を使うように意識改革していきたい。
 * ちなみに、この Command クラスのライフサイクル対応は、LiteUnitCommand 相当のため、サブスレッドから invoke()するような使い方は禁止。
 */
class Command() : View.OnClickListener, TextView.OnEditorActionListener, IDisposable {
    // 永続的ハンドラをbindするコンストラクタ
    // Command().apply { bindForever(fn) } と書いていたのをちょっと簡略化
    constructor(foreverFn:(View?)->Unit) :this() {
        bindForever(foreverFn)
    }

    constructor(view:View, foreverFn:(View?)->Unit):this() {
        bindForever(foreverFn)
        connectView(view)
    }

    private val listeners = Listeners<View?>()
    class ClickListenerDisposer(v:View, private var bind:IDisposable?=null) : IDisposable {
        var view:WeakReference<View>? = WeakReference<View>(v)

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

//        override fun isDisposed(): Boolean {
//            return view!=null
//        }

    }

    @MainThread
    fun connectView(view:View): Command {
        if(view is EditText) {
            view.setOnEditorActionListener(this)
        } else {
            view.setOnClickListener(this)
        }
        return this
    }

    @Deprecated("Use attachView", ReplaceWith("attachView"))
    @MainThread
    fun connectViewEx(view:View) : IDisposable {
        return attachView(view)
    }

    @MainThread
    fun attachView(view:View): IDisposable {
        if(view is EditText) {
            view.setOnEditorActionListener(this)
        } else {
            view.setOnClickListener(this)
        }
        return ClickListenerDisposer(view)
    }

    @MainThread
    fun bind(owner: LifecycleOwner, fn:((View?)->Unit)): IDisposable {
        return listeners.add(owner,fn)
    }

    @MainThread
    fun bindForever(fn:(View?)->Unit): IDisposable {
        return listeners.addForever(fn)
    }
//    @MainThread
//    fun bindForever(fn:()->Unit): IDisposable {
//        return listeners.addForever { fn() }
//    }

    @MainThread
    fun attachAndBind(owner: LifecycleOwner, view:View, fn:((View?)->Unit)):IDisposable {
        connectView(view)
        return ClickListenerDisposer(view, bind(owner,fn))
    }

    @MainThread
    fun connectAndBind(owner: LifecycleOwner, view:View, fn:((View?)->Unit)):IDisposable
        = attachAndBind(owner,view,fn)

    @MainThread
    fun reset() {
        listeners.clear()
    }

    override fun onClick(v: View?) {
        listeners.invoke(v)
    }

    /**
     * ボタンタップ以外からコマンドを実行するときに、onClick(null)と書くのはブサイクだから。
     */
    fun invoke() {
        onClick(null)
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        return if (actionId == EditorInfo.IME_ACTION_DONE || event?.action == KeyEvent.ACTION_DOWN && (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
            listeners.invoke(v)
            true
        } else false
    }

    override fun dispose() {
        reset()
    }
}