package io.github.toyota32k.binder

import android.widget.ToggleButton
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
class CheckBindingTest {
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
    fun oneWayCheckTest() {
        val activity = createActivity()
        val view = ToggleButton(activity)
        val data = MutableLiveData<Boolean>(false)

        val binding = CheckBinding.create(activity, view, data, BoolConvert.Straight, BindingMode.OneWay)
        assertEquals(data.value, view.isChecked)
        assertFalse(view.isChecked)
        data.value = true
        assertEquals(data.value, view.isChecked)
        assertTrue(view.isChecked)

        view.isChecked = false
        assertTrue(data.value!!)
    }

    @Test
    fun oneWayToSourceCheckTest() {
        val activity = createActivity()
        val view = ToggleButton(activity)
        val data = MutableLiveData<Boolean>(false)

        view.isChecked = true
        val binding = CheckBinding.create(activity, view, data, BoolConvert.Straight, BindingMode.OneWayToSource)
        assertEquals(data.value, view.isChecked)
        assertTrue(data.value!!)

        view.isChecked = false
        assertEquals(data.value, view.isChecked)
        assertFalse(data.value!!)

        data.value = true
        assertNotEquals(data.value, view.isChecked)
        assertFalse(view.isChecked)
    }

    @Test
    fun twoWayCheckTest() {
        val activity = createActivity()
        val view = ToggleButton(activity)
        val data = MutableLiveData<Boolean>(false)

        view.isChecked = true
        val binding = CheckBinding.create(activity, view, data, BoolConvert.Straight, BindingMode.TwoWay)
        assertEquals(data.value, view.isChecked)
        assertFalse(view.isChecked)

        view.isChecked = false
        assertEquals(data.value, view.isChecked)
        assertFalse(data.value!!)

        data.value = true
        assertEquals(data.value, view.isChecked)
        assertTrue(view.isChecked)
    }
}