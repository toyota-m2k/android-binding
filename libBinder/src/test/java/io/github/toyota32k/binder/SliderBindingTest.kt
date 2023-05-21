package io.github.toyota32k.binder

import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.android.material.slider.Slider
import junit.framework.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

// Material Components は、RobolectricTestRunnerを使ってもなんかうまくいかない。。。

@RunWith(RobolectricTestRunner::class)
@Config(application= JustTestApplication::class)
class SliderBindingTest {
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
        val activity = createActivity();
        val view = Slider(activity).apply { valueFrom=0f; valueTo=100f}
        val data = MutableLiveData<Float>(90f)

        view.value = 50f

        var binding = SliderBinding.create(activity,view,data,BindingMode.OneWay)
        assertEquals(90f, view.value)
        assertEquals(90f, data.value)

        data.value = 10f
        assertEquals(10f, view.value)
        assertEquals(data.value, view.value)

        view.value= 20f
        assertEquals(20f, view.value)
        assertNotEquals(data.value, view.value)
        assertEquals(10f, data.value)
    }

    @Test
    fun oneWayToSourceTest() {
        val activity = createActivity();
        val view = Slider(activity).apply { valueFrom=0f; valueTo=100f}
        val data = MutableLiveData<Float>(90f)
        view.value=50f

        var binding = SliderBinding.create(activity,view,data,BindingMode.OneWayToSource)
        assertEquals(50f, view.value)
        assertEquals(50f, data.value)

        view.value = 10f
        assertEquals(10f, view.value)
        assertEquals(data.value, view.value)

        data.value= 20f
        assertEquals(20f, data.value)
        assertNotEquals(data.value, view.value)
        assertEquals(10f, view.value)
    }

    @Test
    fun twoWayToSourceTest() {
        val activity = createActivity();
        val view = Slider(activity).apply { valueFrom=0f; valueTo=100f}
        val data = MutableLiveData<Float>(90f)
        view.value=50f

        var binding = SliderBinding.create(activity,view,data,BindingMode.TwoWay)
        assertEquals(90f, view.value)
        assertEquals(90f, data.value)

        view.value = 10f
        assertEquals(10f, view.value)
        assertEquals(data.value, view.value)

        data.value= 20f
        assertEquals(20f, data.value)
        assertEquals(20f, view.value)
    }

    @Test
    fun minMaxBindingTest() {
        val activity = createActivity();
        val view = Slider(activity).apply { valueFrom=0f; valueTo=100f}
        val data = MutableLiveData<Float>(50f)
        val min = MutableLiveData<Float>(10f)
        val max = MutableLiveData<Float>(90f)
        view.value=60f

        var binding = SliderBinding.create(activity,view,data,BindingMode.TwoWay, min, max)
        assertEquals(50f, view.value)
        assertEquals(50f, data.value)
        assertEquals(10f, view.valueFrom)
        assertEquals(90f, view.valueTo)

        view.value = 30f
        assertEquals(30f, view.value)
        assertEquals(data.value, view.value)

        data.value= 20f
        assertEquals(20f, data.value)
        assertEquals(20f, view.value)

        min.value = 40f
        assertEquals(40f, data.value)
        assertEquals(40f, view.value)
        assertEquals(40f, view.valueFrom)

        data.value = 0f
        assertEquals(0f, data.value)
        assertEquals(40f, view.value)
        assertEquals(40f, view.valueFrom)

        data.value = 80f
        assertEquals(80f, data.value)
        assertEquals(80f, view.value)
        assertEquals(40f, view.valueFrom)

        max.value = 60f
        assertEquals(60f, data.value)
        assertEquals(60f, view.value)
        assertEquals(60f, view.valueTo)
        assertEquals(40f, view.valueFrom)
    }
    @Test
    fun binderTest() {
        val activity = createActivity();
        val view = Slider(activity).apply { valueFrom=0f; valueTo=100f}
        val data = MutableLiveData<Float>(50f)
        val min = MutableLiveData<Float>(10f)
        val max = MutableLiveData<Float>(90f)
        view.value=60f

        val binder = Binder().owner(activity)
        binder.sliderBinding(view,data,BindingMode.TwoWay, min, max)
        assertEquals(50f, view.value)
        assertEquals(50f, data.value)
        assertEquals(10f, view.valueFrom)
        assertEquals(90f, view.valueTo)

        view.value = 30f
        assertEquals(30f, view.value)
        assertEquals(data.value, view.value)

        data.value= 20f
        assertEquals(20f, data.value)
        assertEquals(20f, view.value)

        min.value = 40f
        assertEquals(40f, data.value)
        assertEquals(40f, view.value)
        assertEquals(40f, view.valueFrom)

        data.value = 0f
        assertEquals(0f, data.value)
        assertEquals(40f, view.value)
        assertEquals(40f, view.valueFrom)

        data.value = 80f
        assertEquals(80f, data.value)
        assertEquals(80f, view.value)
        assertEquals(40f, view.valueFrom)

        max.value = 60f
        assertEquals(60f, data.value)
        assertEquals(60f, view.value)
        assertEquals(60f, view.valueTo)
        assertEquals(40f, view.valueFrom)
    }
}