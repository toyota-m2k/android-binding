@file:Suppress("PackageDirectoryMismatch", "unused")

package io.github.toyota32k.binder.command

import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.*

/**
 * LifecycleOwnerが死んでいる間にinvoke()されても、次に bind()した時点で、ちゃんとコールバックすることができるCommandクラス。
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
 * 尚、ICommand#bindForever()は、ライフサイクルを無視してハンドラを指定でき、ライフサイクルオーナーの生死にかかわらず、invoke()すると、必ず、ハンドラが呼びだされる。
 * つまり、bindForever を利用する限り、LiteCommand と ReliableCommand の挙動に違いはないので、その場合は、より軽量なReliableCommand で十分のはず。
 */
class ReliableCommand<T:Any>() : CommandBase<T>() {
    constructor(fn:(T)->Unit):this() {
        bindForever(fn)
    }
    private val subject = SingleLiveData<T>()
    private var disposable:IDisposable? = null

    override fun bind(owner: LifecycleOwner, fn: (T) -> Unit): IDisposable {
        disposable?.dispose()
        return subject.disposableObserve(owner, fn).apply { disposable = this }
    }

    override fun bindForever(fn: (T) -> Unit): IDisposable {
        disposable?.dispose()
        return subject.disposableObserveForever(fn).apply { disposable = this }
    }

    override fun reset() {
        dispose()
    }

    override fun dispose() {
        disposable?.dispose()
        disposable = null
    }

    override fun invoke(value: T) {
        subject.value = value
    }
}

/**
 * コールバックに引数を取らないコマンドクラス
 */
class ReliableUnitCommand private constructor(rc: ReliableCommand<Unit>): UnitCommand(rc) {
    constructor():this(ReliableCommand<Unit>())
    constructor(fn:()->Unit):this(ReliableCommand{ fn() })
}
