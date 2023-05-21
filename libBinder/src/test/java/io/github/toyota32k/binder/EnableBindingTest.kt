package io.github.toyota32k.binder

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class EnableBindingTest {
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
    fun enableTest() {
        val activity = createActivity()
        val view = View(activity)
        val data = MutableLiveData<Boolean>(false)

        view.isEnabled = true
        var binding = EnableBinding.create(activity, view, data)
        assertEquals(view.isEnabled, data.value)
        data.value = true
        assertEquals(view.isEnabled, data.value)
        assertTrue(view.isEnabled)

        data.value = false
        assertEquals(view.isEnabled, data.value)
        assertFalse(view.isEnabled)

    }
}