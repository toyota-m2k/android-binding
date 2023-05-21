package io.github.toyota32k.binder

import android.view.View
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class BinderTest {
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
    fun oldStyleTest() {
        val activity = createActivity()
        val textView = TextView(activity)
        val textData = MutableLiveData<String>()
        val boolData = MutableLiveData<Boolean>(false)

        val binder = Binder().register(
            TextBinding.create(activity, textView, textData),
            EnableBinding.create(activity, textView, boolData)
        )
        assertTrue(textView.text.isNullOrEmpty())
        assertFalse(textView.isEnabled)
        textData.value = "abc"
        assertEquals("abc", textView.text)
        assertFalse(textView.isEnabled)

        boolData.value = true
        assertTrue(textView.isEnabled)

        finish()

        textData.value = "xyz"
        assertEquals(2, binder.count)       // finish()しても、Bindingデータは残っている。LiveDataのobserverは解除されるので、バインド自体は切れている。
        assertEquals("abc", textView.text)

        binder.dispose()
        assertEquals(0, binder.count)
        assertTrue(binder.disposed)

    }

    @Test
    fun withoutBinderOwnerTest() {
        val activity = createActivity()
        val textView = TextView(activity)
        val textData = MutableLiveData<String>()
        val boolData = MutableLiveData<Boolean>(false)

        val binder = Binder()
            .textBinding(activity, textView, textData)
            .enableBinding(activity, textView, boolData)

        // Binderの書き方が変わるだけで、挙動は oldStyleと同じ
        assertTrue(textView.text.isNullOrEmpty())
        assertFalse(textView.isEnabled)
        textData.value = "abc"
        assertEquals("abc", textView.text)
        assertFalse(textView.isEnabled)

        boolData.value = true
        assertTrue(textView.isEnabled)

        finish()

        textData.value = "xyz"
        assertEquals(2, binder.count)       // finish()しても、Bindingデータは残っている。LiveDataのobserverは解除されるので、バインド自体は切れている。
        assertEquals("abc", textView.text)

        binder.dispose()
        assertEquals(0, binder.count)
        assertTrue(binder.disposed)
    }

    @Test
    fun withOwnerTest() {
        val activity = createActivity()
        val textView = TextView(activity)
        val textData = MutableLiveData<String>()
        val boolData = MutableLiveData<Boolean>(false)

        val binder = Binder()
            .owner(activity)
            .textBinding(textView, textData)
            .enableBinding(textView, boolData)

        // Binderの書き方が変わるだけで、挙動は oldStyleと同じ
        assertTrue(textView.text.isNullOrEmpty())
        assertFalse(textView.isEnabled)
        textData.value = "abc"
        assertEquals("abc", textView.text)
        assertFalse(textView.isEnabled)

        boolData.value = true
        assertTrue(textView.isEnabled)

        finish()

        textData.value = "xyz"
        assertEquals(0, binder.count)       // finish()すると、reset()が呼ばれるはず
        assertFalse(binder.disposed)
        assertEquals("abc", textView.text)

        binder.dispose()
        assertEquals(0, binder.count)
        assertTrue(binder.disposed)
    }

    @Test
    fun genericBoolBindingTest() {
        var activity = createActivity()
        val view:View = ToggleButton(activity)
        val data = MutableLiveData<Boolean>(false)
        val flow = MutableStateFlow(false)
        val binder = Binder().owner(activity)

        var v1: Boolean = false
        var v2: Boolean = false
        var v3: Boolean = false
        var v4: Boolean = false
        binder
            .genericBoolBinding(activity, view, data, BoolConvert.Straight) { v,b -> v1=b }
            .genericBoolBinding(activity, view, flow, BoolConvert.Straight) { v,b -> v2=b }
            .genericBoolBinding(view, data, BoolConvert.Straight) {v,b -> v3=b }
            .genericBoolBinding(view, flow, BoolConvert.Straight) {v,b -> v4=b }

        assertEquals(4, binder.count)
        assertFalse(v1)
        assertFalse(v2)
        assertFalse(v3)
        assertFalse(v4)

        data.value = true
        assertTrue(v1)
        assertFalse(v2)
        assertTrue(v3)
        assertFalse(v4)

        flow.value = true
        assertTrue(v1)
        assertTrue(v2)
        assertTrue(v3)
        assertTrue(v4)

        finish()
        assertEquals(0, binder.count)
        data.value = false
        flow.value = false
        assertTrue(v1)
        assertTrue(v2)
        assertTrue(v3)
        assertTrue(v4)

        activity = createActivity()
        binder
            .owner(activity)
            .genericBoolMultiBinding(activity, arrayOf(view), data) { v,b -> assertEquals(view,v[0]); v1=b }
            .genericBoolMultiBinding(activity, arrayOf(view), flow) { v, b -> assertEquals(view,v[0]); v2=b }
            .genericBoolMultiBinding(arrayOf(view), data) {v,b -> assertEquals(view,v[0]); v3=b }
            .genericBoolMultiBinding(arrayOf(view), flow) {v,b -> assertEquals(view,v[0]); v4=b }

        assertEquals(4, binder.count)
        // bind時にハンドラが１回呼ばれるので、data/flow の値が反映される。
        assertFalse(v1)
        assertFalse(v2)
        assertFalse(v3)
        assertFalse(v4)

        data.value = true
        assertTrue(v1)
        assertFalse(v2)
        assertTrue(v3)
        assertFalse(v4)

        flow.value = true
        assertTrue(v1)
        assertTrue(v2)
        assertTrue(v3)
        assertTrue(v4)

        finish()
        assertEquals(0, binder.count)
    }

    @Test
    fun checkBindingTest() {
        val activity = createActivity()
        val view = ToggleButton(activity)
        val data = MutableLiveData<Boolean>(false)
        val flow = MutableStateFlow(false)

        val binder = Binder().owner(activity)
        binder
            .checkBinding(view, data as LiveData<Boolean>, BoolConvert.Straight)
            .checkBinding(view, data.asFlow(), BoolConvert.Straight)
            .checkBinding(view, data, BoolConvert.Straight, BindingMode.TwoWay)
            .checkBinding(view, flow, BoolConvert.Straight, BindingMode.TwoWay)
            .checkBinding(activity, view, data as LiveData<Boolean>, BoolConvert.Straight)
            .checkBinding(activity, view, data.asFlow(), BoolConvert.Straight)
            .checkBinding(activity, view, data, BoolConvert.Straight, BindingMode.TwoWay)
            .checkBinding(activity, view, flow, BoolConvert.Straight, BindingMode.TwoWay)
        assertEquals(8, binder.count)
        assertFalse(flow.value)
        data.value = true
        assertTrue(flow.value)
        flow.value = false
        assertFalse(data.value==false)
        finish()

        assertEquals(0, binder.count)
        data.value = true
        assertFalse(flow.value)
    }

    @Test
    fun enableBindingTest() {
        val activity = createActivity()
        val view1 = TextView(activity)
        val view2 = TextView(activity)
        val data = MutableLiveData<Boolean>(false)
        val flow = MutableStateFlow(false)

        val binder = Binder().owner(activity)
        binder
            .enableBinding(view1, data, BoolConvert.Straight)
            .enableBinding(view2, flow, BoolConvert.Straight)

        assertEquals(2, binder.count)
        assertFalse(view1.isEnabled)
        assertFalse(view2.isEnabled)

        data.value = true
        assertTrue(view1.isEnabled)
        assertFalse(view2.isEnabled)
        flow.value = true
        assertTrue(view1.isEnabled)
        assertTrue(view2.isEnabled)

        binder.reset()
        assertEquals(0, binder.count)

        data.value = false
        binder
            .multiEnableBinding(arrayOf(view1,view2), data, BoolConvert.Straight)
        assertEquals(1, binder.count)
        assertFalse(view1.isEnabled)
        assertFalse(view2.isEnabled)
        data.value = true
        assertTrue(view1.isEnabled)
        assertTrue(view2.isEnabled)

        binder.reset()
        assertEquals(0, binder.count)
        flow.value = false
        binder
            .multiEnableBinding(arrayOf(view1,view2), flow, BoolConvert.Straight)
        assertEquals(1, binder.count)
        assertFalse(view1.isEnabled)
        assertFalse(view2.isEnabled)
        data.value = true
        assertFalse(view1.isEnabled)
        assertFalse(view2.isEnabled)
        flow.value = true
        assertTrue(view1.isEnabled)
        assertTrue(view2.isEnabled)

        finish()
        assertEquals(0, binder.count)
    }

    @Test
    fun visibilityBindingTest() {
        val activity = createActivity()
        val view1 = TextView(activity)
        val view2 = TextView(activity)
        val data = MutableLiveData<Boolean>(false)
        val flow = MutableStateFlow(false)

        val binder = Binder().owner(activity)
        binder
            .visibilityBinding(view1, data, BoolConvert.Straight)
            .visibilityBinding(view2, flow, BoolConvert.Straight)

        assertEquals(2, binder.count)
        assertFalse(view1.isVisible)
        assertFalse(view2.isVisible)

        data.value = true
        assertTrue(view1.isVisible)
        assertFalse(view2.isVisible)
        flow.value = true
        assertTrue(view1.isVisible)
        assertTrue(view2.isVisible)

        binder.reset()
        assertEquals(0, binder.count)

        data.value = false
        binder
            .multiVisibilityBinding(arrayOf(view1,view2), data, BoolConvert.Straight)
        assertEquals(1, binder.count)
        assertFalse(view1.isVisible)
        assertFalse(view2.isVisible)
        data.value = true
        assertTrue(view1.isVisible)
        assertTrue(view2.isVisible)

        binder.reset()
        assertEquals(0, binder.count)
        flow.value = false
        binder
            .multiVisibilityBinding(arrayOf(view1,view2), flow, BoolConvert.Straight)
        assertEquals(1, binder.count)
        assertFalse(view1.isVisible)
        assertFalse(view2.isVisible)
        data.value = true
        assertFalse(view1.isVisible)
        assertFalse(view2.isVisible)
        flow.value = true
        assertTrue(view1.isVisible)
        assertTrue(view2.isVisible)

        finish()
        assertEquals(0, binder.count)
    }

    @Test
    fun combinatorialVisibilityBindingTest() {
        val activity = createActivity()
        val view1 = TextView(activity)
        val view2 = TextView(activity)
        val view3 = TextView(activity)
        val view4 = TextView(activity)
        val data = MutableLiveData<Boolean>(false)

        val binder = Binder().owner(activity)

        binder.combinatorialVisibilityBinding(data) {
            straightGone(view1)
            straightInvisible(view2)
            inverseGone(view3)
            inverseInvisible(view4)
        }

        assertEquals(View.GONE, view1.visibility)
        assertEquals(View.INVISIBLE, view2.visibility)
        assertEquals(View.VISIBLE, view3.visibility)
        assertEquals(View.VISIBLE, view4.visibility)

        data.value = true

        assertEquals(View.VISIBLE, view1.visibility)
        assertEquals(View.VISIBLE, view2.visibility)
        assertEquals(View.GONE, view3.visibility)
        assertEquals(View.INVISIBLE, view4.visibility)

        finish()
    }
}