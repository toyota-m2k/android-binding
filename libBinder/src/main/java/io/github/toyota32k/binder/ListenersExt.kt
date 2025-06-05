package io.github.toyota32k.binder

import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.lifecycle.Listeners
import io.github.toyota32k.utils.lifecycle.UnitListeners

fun <T> Binder.addListener(owner: LifecycleOwner, listeners: Listeners<T>, fn:(T)->Unit) : Binder {
    return add(listeners.add(owner, fn))
}
fun <T> Binder.addListener(listeners: Listeners<T>, fn:(T)->Unit) : Binder {
    return add(listeners.add(requireOwner, fn))
}

fun Binder.addListener(owner: LifecycleOwner, listeners: UnitListeners, fn:()->Unit) : Binder {
    return add(listeners.add(owner, fn))
}
fun Binder.addListener(listeners: UnitListeners, fn:()->Unit) : Binder {
    return add(listeners.add(requireOwner, fn))
}
