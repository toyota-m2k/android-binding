package io.github.toyota32k.binder

import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.Listeners
import io.github.toyota32k.utils.UnitListeners

fun <T> Binder.addListener(owner: LifecycleOwner, listeners: Listeners<T>, fn:(T)->Unit) : Binder {
    return add(listeners.add(owner, fn))
}
fun <T> Binder.addListener(listeners: Listeners<T>, fn:(T)->Unit) : Binder {
    return add(listeners.add(requireOwner, fn))
}

fun <T> Binder.addListener(owner: LifecycleOwner, listeners: UnitListeners, fn:()->Unit) : Binder {
    return add(listeners.add(owner, fn))
}
fun <T> Binder.addListener(listeners: UnitListeners, fn:()->Unit) : Binder {
    return add(listeners.add(requireOwner, fn))
}
