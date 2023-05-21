package io.github.toyota32k.binder

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.binder.list.ObservableList
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ObservableListTest {
    class DummyLifecycle(private val owner:LifecycleOwner) : Lifecycle() {
        private var state:State = State.INITIALIZED
            set(s) {
                if(s!=field) {
                    field = s
                    val ev = when(s) {
                        State.INITIALIZED-> Event.ON_ANY
                        State.CREATED->Event.ON_CREATE
                        State.STARTED->Event.ON_START
                        State.RESUMED->Event.ON_RESUME
                        State.DESTROYED->Event.ON_DESTROY
                    }
                    observers.toList().forEach { observer->
                        if(observer is LifecycleEventObserver) {
                            observer.onStateChanged(owner, ev)
                        }
                    }
                }
            }

        private val observers = mutableListOf<LifecycleObserver>()
        override val currentState: State
            get() = state

        override fun addObserver(observer: LifecycleObserver) {
            if(state==State.DESTROYED) {
                throw IllegalStateException("already destroyed")
            }
            observers.add(observer)
        }

        override fun removeObserver(observer: LifecycleObserver) {
            observers.remove(observer)
        }

//        override fun getCurrentState(): State {
//            return state
//        }

        fun next():Boolean {
            state = when(state) {
                State.DESTROYED -> return false
                State.INITIALIZED -> State.CREATED
                State.CREATED->State.STARTED
                State.STARTED->State.RESUMED
                State.RESUMED->State.DESTROYED
            }
            return true
        }
    }
    class DummyLifecycleOwner : LifecycleOwner {
        override val lifecycle = DummyLifecycle(this)
//        override fun getLifecycle(): Lifecycle {
//            return lifecycle
//        }

        fun onCreated(fn:()->Unit):Boolean {
            do {
                if(lifecycle.currentState== Lifecycle.State.CREATED) {
                    fn()
                    return true
                }
            } while(lifecycle.next())
            return false
        }

        fun onDestroy(fn:()->Unit):Boolean {
            do {
                if(lifecycle.currentState== Lifecycle.State.DESTROYED) {
                    fn()
                    return true
                }
            } while(lifecycle.next())
            return false
        }
    }

    private lateinit var list: ObservableList<Int>
    private lateinit var listMirror:MutableList<Int>

    private fun onListChanged(m: ObservableList.MutationEventData<*>) {
        when(m) {
            is ObservableList.RefreshEventData -> listMirror = list.toMutableList()
            is ObservableList.InsertEventData -> {
                for(i in 0 until m.range) {
                    listMirror.add(m.position+i, list[m.position+i])
                }
            }
            is ObservableList.RemoveEventData -> {
                for(i in 0 until m.range) {
                    listMirror.removeAt(m.position)
                }
            }
            is ObservableList.ChangedEventData -> {
                for(i in 0 until m.range) {
                    listMirror[m.position+i] = list[m.position]
                }
            }
            is ObservableList.MoveEventData -> {
                listMirror.add(m.to, listMirror.removeAt(m.from))
            }
        }
    }

    private fun assertEqualsList(list1:List<Int>, list2:List<Int>) {
        assertEquals(list1.size, list2.size)
        for (i in list1.indices) {
            assertEquals(list1[i],list2[i])
        }
    }
    private fun assertEqualsList() {
        assertEqualsList(list,listMirror)
    }

    @Before
    fun setup() {
        list = ObservableList()
        listMirror = mutableListOf()
    }


    @Test
    fun mutateListTest() {
        var owner = DummyLifecycleOwner()
        assertTrue(owner.onCreated {})
        var v = 1

        list.addListener(owner,this::onListChanged)

        assertEquals(list.size, listMirror.size)
        list.add(v++)
        assertEquals(1, list.size)
        assertEqualsList()
        list.add(v++)
        assertEqualsList()
        list.add(v++)
        assertEquals(3, list.size)
        assertEqualsList()
        list.add(1,v++)
        assertEquals(4, list.size)
        assertEqualsList()
        assertEquals(v-1, listMirror[1])
        list.remove(1)
        assertEqualsList()
        assertEquals(2, listMirror[1])

        list[2] = v++
        assertEqualsList()
        assertEquals(v-1, listMirror[2])

        list.clear()
        assertEqualsList()
        assertEquals(0, listMirror.size)

        assertTrue(owner.onDestroy {  })
        list.add(v++)
        assertEquals(1, list.size)
        assertEquals(v-1, list[0])
        assertEquals(0, listMirror.size)    // destroyでlistenerは無効化されているはず

        // 再接続
        owner = DummyLifecycleOwner()
        assertTrue(owner.onCreated {})
        list.addListener(owner,this::onListChanged)

    }

    @Test
    fun moveTest() {
        val owner = DummyLifecycleOwner()
        assertTrue(owner.onCreated {})
        list.addListener(owner,this::onListChanged)
        for(v in 0..9) {
           list.add(v)  // 0,1,2,...9
        }
        assertEqualsList()
        for(v in 0..9) {
            assertEquals(v, listMirror[v])
        }

        list.move(0,9)
        assertEqualsList()
        assertEquals(0, listMirror[9])
        for(v in 0..8) {
            assertEquals(v+1, listMirror[v])
        }

        list.move(9,0)
        assertEqualsList()
        for(v in 0..9) {
            assertEquals(v, listMirror[v])
        }

        list.move(4,5)
        assertEqualsList()
        assertEquals(4, listMirror[5])
        assertEquals(5, listMirror[4])

        list.move(5,4)
        assertEqualsList()
        for(v in 0..9) {
            assertEquals(v, listMirror[v])
        }

        list.move(3,5)
        assertEqualsList()
        assertEquals(4, listMirror[3])
        assertEquals(5, listMirror[4])
        assertEquals(3, listMirror[5])

        list.move(5,3)
        assertEqualsList()
        for(v in 0..9) {
            assertEquals(v, listMirror[v])
        }
    }

    @Test
    fun addAllTest() {
        val owner = DummyLifecycleOwner()
        assertTrue(owner.onCreated {})
        list.addListener(owner,this::onListChanged)

        list.addAll((0..9).toList())
        assertEqualsList()
        for(v in 0..9) {
            assertEquals(v,listMirror[v])
        }

        list.addAll(5, (100..109).toList())
        assertEqualsList()
        for(i in 0..4) {
            assertEquals(i,listMirror[i])
        }
        for(i in 5..5+9) {
            assertEquals(i-5+100, listMirror[i])
        }
        for(i in 5+10..10+9) {
            assertEquals(i-10,listMirror[i])
        }

        list.removeAt(5,10)
        assertEqualsList()
        for(v in 0..9) {
            assertEquals(v,listMirror[v])
        }

    }

    @Test
    fun removeTest() {
//        val ls = mutableListOf<Int>(1,2,3,1,2,3)
//        assertEquals(6,ls.size)
//        assertTrue(ls.remove(1))
//        assertEquals(5,ls.size)
//        assertEquals(2,ls[0])
//        assertTrue(ls.remove(1))
//        assertEquals(4,ls.size)
//        assertEquals(2,ls[0])
//        assertFalse(ls.remove(1))

        val owner = DummyLifecycleOwner()
        assertTrue(owner.onCreated {})
        list.addListener(owner,this::onListChanged)

        list.addAll(listOf(1,2,3,1,2,3))
        assertEquals(6,list.size)
        assertEqualsList()

        assertTrue(list.remove(1))
        assertEqualsList()
        assertEquals(5,list.size)
        assertEquals(2,list[0])

        assertTrue(list.remove(1))
        assertEqualsList()
        assertEquals(4,list.size)
        assertEquals(2,list[0])

        assertFalse(list.remove(1))
        assertEqualsList()

        list.clear()
        var reference = mutableListOf(1,2,3,1,2,3)
        list.addAll(reference)
        assertEqualsList(listMirror,reference)
        assertTrue(reference.removeAll(listOf(2,3,4)))
        assertTrue(list.removeAll(listOf(2,3,4)))
        assertEqualsList()
        assertEqualsList(reference,listMirror)

        reference = mutableListOf(1,2,3,1,2,3)
        list.replace(reference)
        assertEqualsList()
        assertEqualsList(reference,listMirror)

        assertTrue(reference.retainAll(listOf(1,2,4)))
        assertTrue(list.retainAll(listOf(1,2,4)))
        assertEqualsList()
        assertEqualsList(reference,listMirror)

        assertFalse(reference.retainAll(listOf(1,2,4)))
        assertFalse(list.retainAll(listOf(1,2,4)))
        assertEqualsList()
        assertEqualsList(reference,listMirror)
    }

    @Test
    fun iteratorTest() {
        val owner = DummyLifecycleOwner()
        assertTrue(owner.onCreated {})
        list.addListener(owner,this::onListChanged)

        val reference = (0..10).toMutableList()
        list.addAll(reference)

        var itrRef = reference.iterator()
        var itrEx  = list.iterator()

        while(itrRef.hasNext()) {
            assertTrue(itrEx.hasNext())
            assertEquals(itrRef.next(), itrEx.next())
        }

        itrRef = reference.iterator()
        itrEx  = list.iterator()

        for(v in 0..5) {
            assertEquals(itrRef.next(), itrEx.next())
        }
        itrRef.remove()
        itrEx.remove()
        assertEqualsList(reference,list)
        assertEqualsList()
        assertEquals(itrRef.next(), itrEx.next())
        assertEquals(itrRef.next(), itrEx.next())
        itrRef.remove()
        itrEx.remove()
        assertEqualsList(reference,list)
        assertEqualsList()
        assertEquals(itrRef.next(), itrEx.next())
    }

    @Test
    fun listIteratorTest() {
        val owner = DummyLifecycleOwner()
        assertTrue(owner.onCreated {})
        list.addListener(owner,this::onListChanged)

        var reference = (0..20).toMutableList()
        list.addAll(reference)

        var itrRef = reference.listIterator()
        var itrEx  = list.listIterator()

        while(itrRef.hasNext()) {
            assertTrue(itrEx.hasNext())
            assertEquals(itrRef.next(), itrEx.next())
        }

        while(itrRef.hasPrevious()) {
            assertTrue(itrEx.hasPrevious())
            assertEquals(itrRef.previous(), itrEx.previous())
        }

        itrRef = reference.listIterator(6)
        itrEx  = list.listIterator(6)
        assertEquals(itrRef.previous(),itrRef.next())
        assertEquals(itrEx.previous(),itrEx.next())

        itrRef = reference.listIterator(6)
        itrEx  = list.listIterator(6)
        assertEquals(itrRef.next(), itrEx.next())
        itrRef.remove()
        itrEx.remove()
        assertEqualsList(reference,list)
        assertEqualsList()
        assertEquals(itrRef.previous(), itrEx.previous())

        itrRef = reference.listIterator(6)
        itrEx  = list.listIterator(6)
        assertEquals(itrRef.previous(), itrEx.previous())
        assertEquals(itrRef.next(), itrEx.next())
        assertEquals(itrRef.previous(), itrEx.previous())
        itrRef.remove()
        itrEx.remove()
        assertEqualsList(reference,list)
        assertEqualsList()
        assertEquals(itrRef.next(), itrEx.next())

        itrRef = reference.listIterator(3)
        itrEx  = list.listIterator(3)

        for(v in 0..2) {
            assertEquals(itrRef.next(), itrEx.next())
        }
        itrRef.remove()
        itrEx.remove()
        assertEqualsList(reference,list)
        assertEqualsList()
        assertEquals(itrRef.next(), itrEx.next())
        assertEquals(itrRef.previous(), itrEx.previous())
        assertEquals(itrRef.previous(), itrEx.previous())
        assertEquals(itrRef.next(), itrEx.next())
        assertEquals(itrRef.previous(), itrEx.previous())
        itrRef.set(100)
        itrEx.set(100)
        assertEqualsList(reference,list)
        assertEqualsList()

        assertEquals(itrRef.previous(), itrEx.previous())
        itrRef.remove()
        itrEx.remove()
        assertEqualsList(reference,list)
        assertEqualsList()

        reference = (0..20).toMutableList()
        list.replace(reference)
        assertEqualsList(reference,list)

        itrRef = reference.listIterator(10)
        itrEx  = list.listIterator(10)
        itrRef.add(123)
        itrEx.add(123)
        assertEqualsList(reference,list)
        assertEquals(itrRef.next(), itrEx.next())
        assertEquals(itrRef.next(), itrEx.next())
        assertEquals(itrRef.previous(), itrEx.previous())
        assertEquals(itrRef.previous(), itrEx.previous())
        assertEquals(itrRef.previous(), itrEx.previous())
        assertEquals(itrRef.previous(), itrEx.previous())

        itrRef.remove()
        itrEx.remove()
        assertEqualsList(reference,list)
        assertEquals(itrRef.previous(), itrEx.previous())
//        itrRef.remove()
//        itrEx.remove()
//        assertEqualsList(reference,list)
//        itrRef.remove()
//        itrEx.remove()
//        assertEqualsList(reference,list)
//        itrRef.remove()
//        itrEx.remove()
//        assertEqualsList(reference,list)
    }
}