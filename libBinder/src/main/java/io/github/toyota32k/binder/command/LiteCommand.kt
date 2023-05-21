@file:Suppress("unused", "PackageDirectoryMismatch")

package io.github.toyota32k.binder.command

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.*

/**
 * invoke()から直接コールバックするライトウェイトコマンドクラス。
 *
 * LiteCommand, ReliableCommand ともに、コマンドハンドラにViewに対応した任意のパラメータを渡せるようにしているが、
 * 通常、ボタン毎にコマンドインスタンスを割付ければ、これにパラメータを渡す必要は、ほぼ皆無。
 * なので、普通は、LiteUnitCommand/ReliableUnitCommand を使えばよい。
 *
 * ちなみに、LiteUnitCommandは、ライフサイクルオーナー（通常はActivity）がdestroyされた状態でinvoke()すると、そのコマンドは捨てられ、
 * その後、再作成されたライフサイクルオーナーで再バインドしても、ハンドラは実行されない。これに対して、ReliableCommandは、
 * ライフサイクルオーナーが死んでいる間にinvokeされたコマンドは、新しいライフサイクルオーナーにバインドされたときに、ハンドラが実行される。
 * したがって、ボタンクリックに対するハンドラとしては、LiteCommandで十分だが、ボタンクリック後、サブスレッドでごにょごにょしたあと、
 * コマンドを実行する、というような場合、つまり、サブスレッドでの処理が終わった時、デバイス回転などの操作により、Activityがdestroyされている可能性がある場合には、
 * ReliableCommandを使用する。ただし、ReliableCommandには、コマンドハンドラを１つしかバインドできない点に注意（複数回バインドすると例外が発生）。
 * 尚、ICommand#bindForever()は、ライフサイクルを無視してハンドラを指定でき、ライフサイクルオーナーが生きていようが死んでいようが、invoke()すると、必ず、ハンドラが呼びだされる。
 * つまり、bindForever を利用する限り、LiteCommand と ReliableCommand の挙動に違いはないので、その場合は、より軽量なReliableCommand で十分のはず。
 */
open class LiteCommand<T>() : CommandBase<T>() {
    constructor(fn:(T)->Unit):this() {
        bindForever(fn)
    }

    private val listeners = Listeners<T>()

    override fun bind(owner: LifecycleOwner, fn: (T) -> Unit): IDisposable {
        return listeners.add(owner,fn)
    }

    final override fun bindForever(fn: (T) -> Unit): IDisposable {
        return listeners.addForever(fn)
    }

    override fun reset() {
        listeners.clear()
    }

    override fun dispose() {
        reset()
    }

    override fun invoke(value: T) {
        listeners.invoke(value)
    }
}

/**
 * コールバックに引数を取らないコマンドクラス
 */
open class LiteUnitCommand private constructor(rc:LiteCommand<Unit>): UnitCommand(rc) {
    constructor():this(LiteCommand<Unit>())
    constructor(fn:()->Unit):this(LiteCommand { fn() })
}

//fun <T> Binder.bindCommand(callback:(T)->Unit, attachViews:LiteCommand<T>.()->Unit):Binder
//    = add(LiteCommand(callback).apply{ attachViews() })