package io.github.toyota32k.binder.anim

import io.github.toyota32k.binder.anim.IReversibleAnimation.Companion.logger
import io.github.toyota32k.utils.reduce
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.IllegalStateException

@Suppress("unused")
class SequentialAnimation : IReversibleAnimation {
    override val reverse: Boolean get() = currentPointer?.reverse ?: false
    override val duration: Long
        get() = list.reduce(0L) { acc, e-> acc + e.duration }
    override val running: Boolean
        get() = currentPointer!=null

    private val list = mutableListOf<IReversibleAnimation>()
    private val mutex = Mutex()

    fun add(vararg animations: IReversibleAnimation): SequentialAnimation {
        list.addAll(animations)
        return  this
    }
    operator fun plus(anim: IReversibleAnimation): SequentialAnimation {
        list.add(anim)
        return  this
    }

    inner class NodePointer(private var currentIndex:Int, var reverse:Boolean) {
        constructor(prev: NodePointer?, reverse: Boolean) : this(prev?.currentIndex?:-1, reverse)
        private var nextIndex:Int = if(currentIndex<0) {
            if (!reverse) 0 else list.size - 1
        } else {
            currentIndex
        }

        var closed:Boolean = false
            private set

        fun close() {
            closed = true
        }

        val hasNext:Boolean get() = !closed && 0<=nextIndex && nextIndex<list.size

        fun next(): IReversibleAnimation? {
            if(!hasNext) {
                return null
            }
            currentIndex = nextIndex
            if(reverse) {
                nextIndex--
            } else {
                nextIndex++
            }
            return list[currentIndex]
        }
    }

    private var currentPointer: NodePointer? = null

    override suspend fun run(reverse: Boolean): Boolean {
        if(list.size==0) {
            logger.error("no animation")
            throw IllegalStateException("no animation")
        }
        val np = mutex.withLock {
            if(running&&reverse==this.reverse) {
                // already executing
                return false
            }
            currentPointer?.close()
            NodePointer(currentPointer,reverse).apply {
                currentPointer = this
            }
        }

        while(np.hasNext) {
            val node = np.next() ?: return false
            if(!node.run(reverse)) {
                return false
            }
        }
        mutex.withLock {
            if(np==currentPointer) {
                currentPointer = null
            }
        }
        return !np.closed
    }

    override fun invokeLastState(reverse: Boolean) {
        list.forEach {
            it.invokeLastState(reverse)
        }
    }
}