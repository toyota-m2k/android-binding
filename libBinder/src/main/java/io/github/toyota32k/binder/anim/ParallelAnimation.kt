package io.github.toyota32k.binder.anim

import io.github.toyota32k.utils.reduce
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Long.max

@Suppress("unused")
class ParallelAnimation : IReversibleAnimation {
    override var reverse: Boolean = false
        private set
    override val duration: Long
        get() = list.reduce(0L) { acc,e-> max(acc, e.duration) }
    override var running: Boolean = false
        private set

    private val list = mutableListOf<IReversibleAnimation>()
    private val mutex = Mutex()

    fun add(vararg animations: IReversibleAnimation): ParallelAnimation {
        list.addAll(animations)
        return  this
    }
    operator fun plus(anim: IReversibleAnimation): ParallelAnimation {
        list.add(anim)
        return this
    }

    override suspend fun run(reverse: Boolean): Boolean {
        if(list.isEmpty()) {
            IReversibleAnimation.logger.error("no animation")
            throw IllegalStateException("no animation")
        }
        mutex.withLock {
            if(running&&reverse==this.reverse) {
                // already executing
                return false
            }
            this.reverse = reverse
        }
        coroutineScope {
            list.map { async {it.run(reverse)} }.awaitAll()
        }
        return reverse == this.reverse
    }

    override fun invokeLastState(reverse: Boolean) {
        list.forEach {
            it.invokeLastState(reverse)
        }
    }
}