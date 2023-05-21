package io.github.toyota32k.binder

import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class HeadlessBindingTest {
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
    fun headlessTest() {
        val activity = createActivity()
        val boolData = MutableLiveData<Boolean>(false)
        var sync:Boolean = true

        val binder = Binder()
            .owner(activity)
            .headlessNonnullBinding<Boolean>(boolData) {
                sync = it
            }

        assertEquals(false, sync)
        boolData.value = true
        assertEquals(true, sync)

    }
}
