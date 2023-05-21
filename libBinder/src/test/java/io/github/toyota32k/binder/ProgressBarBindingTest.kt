package io.github.toyota32k.binder

import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Assert.*
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class ProgressBarBindingTest {
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
    fun oneWayTest() {
        val activity = createActivity()
        activity.setTheme(android.R.style.Theme)

        val view = ProgressBar(activity, null, android.R.style.Widget_DeviceDefault_ProgressBar_Horizontal).apply { min = 0; max = 100; isIndeterminate=false }
        val data = MutableLiveData<Int>(40)
        view.progress=50

        var binding = ProgressBarBinding.create(activity, view, data, null, null)
        assertEquals(40, view.progress)
        assertEquals(data.value, view.progress)

        data.value = 10
        assertEquals(10, view.progress)
        assertEquals(data.value, view.progress)

        view.progress = 20
        assertEquals(20, view.progress)
        assertNotEquals(data.value, view.progress)
        assertEquals(10, data.value)

        // dataの値が、Min/Max の範囲を超えると、progressの値は、min/maxでクリップされる。
        data.value = 200
        assertEquals(100, view.progress)
        assertNotEquals(data.value, view.progress)
    }
    @Test
    fun minMaxTest() {
        val activity = createActivity()
        activity.setTheme(android.R.style.Theme)

        val lmin = MutableLiveData<Int>(100)
        val lmax = MutableLiveData<Int>(200)
        val view = ProgressBar(activity, null, android.R.style.Widget_DeviceDefault_ProgressBar_Horizontal).apply { min = 0; max = 100; isIndeterminate=false }
        val data = MutableLiveData<Int>(150)
        view.progress=130

        var binding = ProgressBarBinding.create(activity, view, data, lmin, lmax)
        assertEquals(data.value, view.progress)
        assertEquals(150, view.progress)
        assertEquals(100, view.min)
        assertEquals(200, view.max)

        lmin.value = 0
        lmax.value = 100
        assertEquals(150, data.value)
        assertEquals(100, view.progress)
        assertEquals(0, view.min)
        assertEquals(100, view.max)
    }

    @Test
    fun binderTest() {
        val activity = createActivity()
        activity.setTheme(android.R.style.Theme)

        val lmin = MutableLiveData<Int>(100)
        val lmax = MutableLiveData<Int>(200)
        val view = ProgressBar(activity, null, android.R.style.Widget_DeviceDefault_ProgressBar_Horizontal).apply { min = 0; max = 100; isIndeterminate=false }
        val data = MutableLiveData<Int>(150)
        view.progress=130

        val binder = Binder().owner(activity)
        binder.progressBarBinding(view, data, lmin, lmax)
        assertEquals(data.value, view.progress)
        assertEquals(150, view.progress)
        assertEquals(100, view.min)
        assertEquals(200, view.max)

        lmin.value = 0
        lmax.value = 100
        assertEquals(150, data.value)
        assertEquals(100, view.progress)
        assertEquals(0, view.min)
        assertEquals(100, view.max)

        finish()
    }

}
