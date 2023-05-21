@file:Suppress("unused")

package io.github.toyota32k.binder.command

import android.view.View

open class LongClickCommand<T>() : LiteCommand<T>() {
    constructor(fn:(T)->Unit):this() {
        bindForever(fn)
    }
    constructor(outerCommand: ICommand<T>):this() {
        bindForever { outerCommand.invoke(it) }
    }
    override fun internalAttachView(view: View, value: T) {
        view.setOnLongClickListener {
            invoke(value)
            true
        }
    }
}

open class LongClickUnitCommand private constructor(rc: LongClickCommand<Unit>): UnitCommand(rc) {
    constructor():this(LongClickCommand<Unit>())
    constructor(fn:()->Unit):this(LongClickCommand { fn() })
    constructor(outerCommand: UnitCommand):this(LongClickCommand { outerCommand.invoke() })
}
