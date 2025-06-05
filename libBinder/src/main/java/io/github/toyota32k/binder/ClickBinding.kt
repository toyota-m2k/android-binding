@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.lifecycle.Callback
import io.github.toyota32k.utils.lifecycle.Listeners

/**
 * 通常（クリックイベントを直接扱いたい場合以外は）、Command系クラス(LiteCommandなど）を利用する。
 */
class ClickBinding<V> (
    owner:LifecycleOwner,
    val view: V,
    private val listeners :Listeners<V>,
    fn:((V)->Unit),
) : IBinding, View.OnClickListener where V:View {
    constructor(owner:LifecycleOwner, view:V, fn:((V)->Unit)) : this(owner,view,Listeners<V>(), fn)
    override val mode: BindingMode = BindingMode.OneWayToSource
    private var key: IDisposable? = null
    init {
        view.setOnClickListener(this)
        key = listeners.add(owner,fn)
    }

    override fun onClick(v: View?) {
        listeners.invoke(view)
    }

    override fun dispose() {
        key?.dispose()
        key = null
    }

    companion object {
        fun <V> create(owner: LifecycleOwner, view:V, fn:(View)->Unit): ClickBinding<V> where V:View{
            return ClickBinding(owner, view, fn)
        }
    }
}

class LongClickBinding<V>(
    owner: LifecycleOwner,
    val view: V,
    fn: (V)->Boolean
) : IBinding, View.OnLongClickListener where V:View {
    override val mode: BindingMode = BindingMode.OneWayToSource
    private var callback : Callback<V,Boolean>? = Callback(owner,fn)
    init {
        view.setOnLongClickListener(this)
    }

    override fun onLongClick(v: View?): Boolean {
        return callback?.invoke(view) == true
    }
    override fun dispose() {
        callback?.dispose()
        callback = null
    }
}

fun <V:View> Binder.clickBinding(owner:LifecycleOwner, view:V, fn:(View)->Unit): Binder
        = add(ClickBinding(owner, view, fn))
fun <V:View> Binder.clickBinding(view:V, fn:(View)->Unit): Binder
        = add(ClickBinding(requireOwner, view, fn))

fun <V:View> Binder.longClickBinding(owner: LifecycleOwner, view:V, fn:(V)->Boolean) : Binder
        = add(LongClickBinding(owner,view,fn))

fun <V:View> Binder.longClickBinding(view:V, fn:(V)->Boolean) : Binder
        = add(LongClickBinding(requireOwner,view,fn))