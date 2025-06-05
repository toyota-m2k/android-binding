@file:Suppress("unused")

package io.github.toyota32k.binder

import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.lifecycle.LifecycleDisposer

@Suppress("MemberVisibilityCanBePrivate")
open class Binder : LifecycleDisposer() {
    val bindings get() = disposables

    override operator fun plus(disposable:IDisposable): Binder {
        return add(disposable)
    }
    override operator fun minus(disposable:IDisposable): Binder {
        return remove(disposable)
    }

    val hasOwner : Boolean
        get() = lifecycleOwner != null

    val requireOwner: LifecycleOwner
        get() = lifecycleOwner ?: throw IllegalStateException("lifecycleOwner has not be set, call owner() at first.")

    fun owner(owner:LifecycleOwner): Binder {
        lifecycleOwner = owner
        return this
    }

    fun add(vararg bindings:IDisposable): Binder {
        register(*bindings)
        return this
    }

    fun add(fn:()->IDisposable?): Binder {
        fn()?.apply {
            register(this)
        }
        return this
    }

    fun remove(vararg bindings:IDisposable): Binder {
        unregister(*bindings)
        return this
    }

    fun conditional(condition: Boolean, fn: Binder.()->Unit): Binder {
        if(condition) {
            fn()
        }
        return this
    }

    fun conditionalAdd(condition:Boolean, fn:()->IDisposable): Binder {
        return conditional(condition) {
            add(fn())
        }
    }
}