package io.github.toyota32k.binder

import android.os.Build
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class SeekBarTest {
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
        val view = SeekBar(activity).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                min = 0
            }
            max = 100
        }
        val data = MutableLiveData<Int>(40)
        view.progress=50

        var binding = SeekBarBinding.create(activity, view, data, null, null, BindingMode.OneWay)
        assertEquals(40, view.progress)
        assertEquals(data.value, view.progress)

        data.value = 10
        assertEquals(10, view.progress)
        assertEquals(data.value, view.progress)

        view.progress = 20
        assertEquals(20, view.progress)
        assertNotEquals(data.value, view.progress)
        assertEquals(10, data.value)
    }

    @Test
    fun oneWayToSourceTest() {
        val activity = createActivity()
        val view = SeekBar(activity).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                min = 0
            }
            max=100
        }
        val data = MutableLiveData<Int>(40)
        view.progress=50

        var binding = SeekBarBinding.create(activity, view, data, null, null, BindingMode.OneWayToSource)
        assertEquals(50, view.progress)
        assertEquals(data.value, view.progress)

        data.value = 10
        assertEquals(50, view.progress)
        assertEquals(10, data.value)
        assertNotEquals(data.value, view.progress)

        view.progress = 20
        assertEquals(20, view.progress)
        assertEquals(data.value, view.progress)
    }

    @Test
    fun twoWayTest() {
        val activity = createActivity()
        val view = SeekBar(activity).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                min = 0
            }
            max=100
        }
        val data = MutableLiveData<Int>(40)
        view.progress=50

        var binding = SeekBarBinding.create(activity, view, data, null, null, BindingMode.TwoWay)
        assertEquals(40, view.progress)     // Data側が優先される
        assertEquals(data.value, view.progress)

        data.value = 10
        assertEquals(10, view.progress)
        assertEquals(data.value, view.progress)

        view.progress = 20
        assertEquals(20, view.progress)
        assertEquals(data.value, view.progress)
    }

    @Test
    fun minMaxOneWayTest() {
        // APIレベルが26以上であることを前提とします
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        val activity = createActivity()
        val view = SeekBar(activity)
        val lmin = MutableLiveData<Int>(0)
        val lmax = MutableLiveData<Int>(100)
        val data = MutableLiveData<Int>(40)
        view.progress=50

        var binding = SeekBarBinding.create(activity, view, data, lmin, lmax, BindingMode.OneWay)
        assertEquals(40, view.progress)
        assertEquals(data.value, view.progress)

        data.value = 10
        assertEquals(10, view.progress)
        assertEquals(data.value, view.progress)

        lmax.value = 300
        lmin.value = 200
        assertEquals(200, view.progress)
        assertEquals(200, view.min)
        assertEquals(300, view.max)
        assertEquals(10, data.value)

        lmax.value = 50
        lmin.value = 0
        assertEquals(50, view.progress)
        assertEquals(0, view.min)
        assertEquals(50, view.max)
        assertEquals(10, data.value)
    }

    @Test
    fun minMaxOneWayToSourceTest() {
        // APIレベルが26以上であることを前提とします
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        val activity = createActivity()
        val view = SeekBar(activity).apply { min=0; max=100}
        val lmin = MutableLiveData<Int>(0)
        val lmax = MutableLiveData<Int>(100)
        val data = MutableLiveData<Int>(40)
        view.progress=50

        var binding = SeekBarBinding.create(activity, view, data, lmin, lmax, BindingMode.OneWayToSource)
        assertEquals(50, view.progress)
        assertEquals(data.value, view.progress)

        view.progress = 20
        assertEquals(20, view.progress)
        assertEquals(data.value, view.progress)

        lmax.value = 300
        lmin.value = 200
        assertEquals(200, view.progress)
        assertEquals(200, view.min)
        assertEquals(300, view.max)
        assertEquals(view.progress, data.value)

        lmax.value = 50
        lmin.value = 0
        assertEquals(50, view.progress)
        assertEquals(0, view.min)
        assertEquals(50, view.max)
        assertEquals(view.progress, data.value)
    }

    @Test
    fun minMaxTwoWayTest() {
        // APIレベルが26以上であることを前提とします
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        val activity = createActivity()
        val view = SeekBar(activity).apply { min=0; max=100}
        val lmin = MutableLiveData<Int>(0)
        val lmax = MutableLiveData<Int>(100)
        val data = MutableLiveData<Int>(40)
        view.progress=50

        var binding = SeekBarBinding.create(activity, view, data, lmin, lmax, BindingMode.TwoWay)
        assertEquals(40, view.progress)     // Dataが優先
        assertEquals(data.value, view.progress)

        view.progress = 20
        assertEquals(20, view.progress)
        assertEquals(data.value, view.progress)

        lmax.value = 300
        lmin.value = 200
        assertEquals(200, view.progress)
        assertEquals(200, view.min)
        assertEquals(300, view.max)
        assertEquals(view.progress, data.value)

        lmax.value = 50
        lmin.value = 0
        assertEquals(50, view.progress)
        assertEquals(0, view.min)
        assertEquals(50, view.max)
        assertEquals(view.progress, data.value)
    }

    @Test
    fun binderTest() {
        // APIレベルが26以上であることを前提とします
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        val activity = createActivity()
        val view = SeekBar(activity).apply { min=0; max=100}
        val lmin = MutableLiveData<Int>(0)
        val lmax = MutableLiveData<Int>(100)
        val data = MutableLiveData<Int>(40)
        view.progress=50

        val binder = Binder().owner(activity)
        binder.seekBarBinding(view, data, lmin, lmax)
        assertEquals(40, view.progress)     // Dataが優先
        assertEquals(data.value, view.progress)

        view.progress = 20
        assertEquals(20, view.progress)
        assertEquals(data.value, view.progress)

        lmax.value = 300
        lmin.value = 200
        assertEquals(200, view.progress)
        assertEquals(200, view.min)
        assertEquals(300, view.max)
        assertEquals(view.progress, data.value)

        lmax.value = 50
        lmin.value = 0
        assertEquals(50, view.progress)
        assertEquals(0, view.min)
        assertEquals(50, view.max)
        assertEquals(view.progress, data.value)
    }
}
