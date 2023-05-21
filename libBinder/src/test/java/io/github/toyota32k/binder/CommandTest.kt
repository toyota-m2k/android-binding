package io.github.toyota32k.binder

import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.toyota32k.binder.command.LiteCommand
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.ReliableCommand
import io.github.toyota32k.binder.command.ReliableUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class CommandTest {
    @Rule
    @JvmField
    val instantExecutorRule : InstantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var activityController : ActivityController<JustTestActivity>

    fun createActivity(): AppCompatActivity {
        activityController = Robolectric.buildActivity(JustTestActivity::class.java)
        return activityController.create().start().get() as AppCompatActivity
    }
    fun finish() {
        activityController.pause().destroy()
    }

    @Test
    fun liteCommandTest() {
        var activity = createActivity()
        var value:Int = -1
        val command = LiteCommand<Int>()

        var d = command.bind(activity) {
            value = it
        }

        assertEquals(-1, value)
        command.invoke(2)
        assertEquals(2, value)
        command.invoke(4)
        assertEquals(4, value)

        finish()
        command.invoke(6)
        assertEquals(4, value)

        activity = createActivity()
        d = command.bind(activity) {
            value = it*2
        }
        // LiteCommand は、activityが死んでいる間のイベントは捨てられる
        assertEquals(4, value)

        command.invoke(8)
        assertEquals(16, value)

        // unbindするとイベントは来ない
        d.dispose()
        command.invoke(10)
        assertEquals(16, value)

        d = command.bind(activity) {
            value = it*10
        }
        command.invoke(12)
        assertEquals(120, value)


        // command を　dispose()したら、その後はイベントは来ない
        command.dispose()
        command.invoke(14)
        assertEquals(120, value)

        finish()
    }

    @Test
    fun reliableCommandTest() {
        var activity = createActivity()
        var value:Int = -1
        val command = ReliableCommand<Int>()

        var d = command.bind(activity) {
            value = it
        }

        assertEquals(-1, value)
        command.invoke(2)
        assertEquals(2, value)
        command.invoke(4)
        assertEquals(4, value)

        finish()
        command.invoke(6)
        assertEquals(4, value)

        activity = createActivity()
        d = command.bind(activity) {
            value = it*2
        }
        // ReliableCommandは、Activityが死んでいる間のイベントも、再バインドしたときに受け取れる
        assertEquals(12, value)

        command.invoke(8)
        assertEquals(16, value)

        // unbindするとイベントは来ない
        d.dispose()
        command.invoke(10)
        assertEquals(16, value)

        d = command.bind(activity) {
            value = it*10
        }
        // 再バインドすると、その時点で値を受け取れる
        assertEquals(100, value)

        command.invoke(12)
        assertEquals(120, value)


        // command を　dispose()したら、その後はイベントは来ない
        command.dispose()
        command.invoke(14)
        assertEquals(120, value)

        finish()
    }

    @Test
    fun unitCommandTest() {
        var activity = createActivity()
        var liteValue:Int = 0
        var reliableValue:Int = 0
        val reliableCommand = ReliableUnitCommand()
        val liteCommand = LiteUnitCommand()

        reliableCommand.bind(activity) {
            reliableValue++
        }
        liteCommand.bind(activity) {
            liteValue++
        }

        assertEquals(0,liteValue)
        assertEquals(0, reliableValue)

        reliableCommand.invoke()
        liteCommand.invoke()

        assertEquals(1, liteValue)
        assertEquals(1, reliableValue)

        finish()

        assertEquals(1, liteValue)
        assertEquals(1, reliableValue)

        reliableCommand.invoke()
        liteCommand.invoke()

        assertEquals(1, liteValue)
        assertEquals(1, reliableValue)

        activity = createActivity()
        reliableCommand.bind(activity) {
            reliableValue++
        }
        liteCommand.bind(activity) {
            liteValue++
        }
        assertEquals(1, liteValue)
        assertEquals(2, reliableValue)

        finish()
    }

    @Test
    fun foreverBindTest() {
        var activity = createActivity()
        var liteValue:Int = 0
        var foreverLiteValue:Int = 0
        var foreverReliableValue:Int = 0
        val reliableCommand = ReliableUnitCommand() {
            foreverReliableValue++
        }
        val liteCommand = LiteUnitCommand() {
            foreverLiteValue++
        }
        liteCommand.bind(activity) {
            liteValue++
        }

        assertEquals(0,liteValue)
        assertEquals(0, foreverReliableValue)
        assertEquals(0, foreverLiteValue)

        reliableCommand.invoke()
        liteCommand.invoke()

        assertEquals(1, liteValue)
        assertEquals(1, foreverReliableValue)
        assertEquals(1, foreverLiteValue)

        finish()

        assertEquals(1, liteValue)
        assertEquals(1, foreverReliableValue)
        assertEquals(1, foreverLiteValue)

        reliableCommand.invoke()
        liteCommand.invoke()

        assertEquals(1, liteValue)
        assertEquals(2, foreverReliableValue)
        assertEquals(2, foreverLiteValue)

        activity = createActivity()
        liteCommand.bind(activity) {
            liteValue++
        }
        assertEquals(1, liteValue)
        assertEquals(2, foreverReliableValue)
        assertEquals(2, foreverLiteValue)

        liteCommand.reset()
        reliableCommand.reset()

        reliableCommand.invoke()
        liteCommand.invoke()
        assertEquals(1, liteValue)
        assertEquals(2, foreverReliableValue)
        assertEquals(2, foreverLiteValue)

        finish()
    }

    @Test
    fun binderTest() {
        var activity = createActivity()
        var liteValue:Int = 0
        var reliableValue:Int = 0
        val reliableCommand = ReliableUnitCommand()
        val liteCommand = LiteUnitCommand()
        val binder = Binder()
        binder
            .owner(activity)
            .bindCommand(liteCommand) {
                liteValue++
            }
            .bindCommand(reliableCommand) {
                reliableValue++
            }

        assertEquals(2, binder.count)
        assertEquals(0,liteValue)
        assertEquals(0, reliableValue)

        reliableCommand.invoke()
        liteCommand.invoke()

        assertEquals(1, liteValue)
        assertEquals(1, reliableValue)

        finish()
        assertEquals(0, binder.count)

        assertEquals(1, liteValue)
        assertEquals(1, reliableValue)

        reliableCommand.invoke()
        liteCommand.invoke()

        assertEquals(1, liteValue)
        assertEquals(1, reliableValue)

        activity = createActivity()
        binder
            .owner(activity)
            .bindCommand(liteCommand) {
                liteValue++
            }
            .bindCommand(reliableCommand) {
                reliableValue++
            }

        assertEquals(1, liteValue)
        assertEquals(2, reliableValue)

        finish()

    }
}