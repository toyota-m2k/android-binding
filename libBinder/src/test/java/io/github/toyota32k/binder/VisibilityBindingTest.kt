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
class VisibilityBindingTest {
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
    fun hideByGoneTest() {
        val activity = createActivity()
        val data = MutableLiveData<Boolean>(false)
        val view = View(activity)
        view.visibility = View.VISIBLE
        var binding = VisibilityBinding.create(activity,view,data)

        assertEquals(View.GONE, view.visibility)
        data.value = true
        assertEquals(View.VISIBLE, view.visibility)
        data.value = false
        assertEquals(View.GONE, view.visibility)
        binding.dispose()

        data.value = true
        assertEquals(View.GONE, view.visibility)

        binding = VisibilityBinding.create(activity,view,data)
        assertEquals(View.VISIBLE, view.visibility)
        data.value = false
        assertEquals(View.GONE, view.visibility)
        data.value = true
        assertEquals(View.VISIBLE, view.visibility)

        binding.dispose()
    }

    @Test
    fun hideByGoneReverseTest() {
        val activity = createActivity()
        val data = MutableLiveData<Boolean>(false)
        val view = View(activity)
        view.visibility = View.VISIBLE
        var binding = VisibilityBinding.create(activity,view,data,boolConvert= BoolConvert.Inverse)
        assertEquals(View.VISIBLE, view.visibility)
        data.value = false
        assertEquals(View.VISIBLE, view.visibility)
        data.value = true
        assertEquals(View.GONE, view.visibility)
        data.value = false
        assertEquals(View.VISIBLE, view.visibility)
        data.value = true
        assertEquals(View.GONE, view.visibility)

        binding.dispose()
        data.value = true
        binding = VisibilityBinding.create(activity,view,data,boolConvert= BoolConvert.Inverse)
        assertEquals(View.GONE, view.visibility)

        data.value = false
        binding.dispose()
        assertEquals(View.VISIBLE, view.visibility)

        binding.dispose()
        data.value = false
        binding = VisibilityBinding.create(activity,view,data,boolConvert= BoolConvert.Inverse)
        assertEquals(View.VISIBLE, view.visibility)
        data.value = true
        assertEquals(View.GONE, view.visibility)

        binding.dispose()
        binding = VisibilityBinding.create(activity,view,data,boolConvert= BoolConvert.Inverse)
        assertEquals(View.GONE, view.visibility)

    }

    @Test
    fun hideByInvisibleTest() {
        val activity = createActivity()
        val data = MutableLiveData<Boolean>(false)
        val view = View(activity)
        view.visibility = View.VISIBLE
        var binding = VisibilityBinding.create(activity,view,data,BoolConvert.Straight,VisibilityBinding.HiddenMode.HideByInvisible)

        assertEquals(View.INVISIBLE, view.visibility)
        data.value = true
        assertEquals(View.VISIBLE, view.visibility)
        data.value = false
        assertEquals(View.INVISIBLE, view.visibility)
        binding.dispose()

        data.value = true
        assertEquals(View.INVISIBLE, view.visibility)

        binding = VisibilityBinding.create(activity,view,data,BoolConvert.Straight,VisibilityBinding.HiddenMode.HideByInvisible)
        assertEquals(View.VISIBLE, view.visibility)
        data.value = false
        assertEquals(View.INVISIBLE, view.visibility)
        data.value = true
        assertEquals(View.VISIBLE, view.visibility)

        binding.dispose()
    }
}