package io.github.toyota32k.binder

import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController


@RunWith(RobolectricTestRunner::class)
class TextBindingTest {
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
    fun onwWayTextBindingTest() {
        val activity = createActivity()
        val view = TextView(activity)
        val data = MutableLiveData<String>()

        view.text="123"
        val textBinding = TextBinding.create(activity, view, data)

        assertTrue(view.text.isNullOrEmpty())
        assertTrue(data.value.isNullOrEmpty())
        data.value = "abc"
        assertEquals("abc", data.value)
        assertEquals("abc", view.text)

        view.text = "xyz"
        assertEquals("abc", data.value)
        assertEquals("xyz", view.text)

        data.value = "abc"
        assertEquals("abc", data.value)
        assertEquals("abc", view.text)

        data.value = null
        assertTrue(view.text.isNullOrEmpty())
        assertTrue(data.value.isNullOrEmpty())
    }

    @Test
    fun oneWayToSourceBindingTest() {
        val activity = createActivity()
        val view = EditText(activity)
        val data = MutableLiveData<String>()
        view.setText("123")
        val textBinding = EditTextBinding.create(activity, view, data, mode = BindingMode.OneWayToSource)

        assertEquals("123", view.text.toString())
        assertEquals(view.text.toString(), data.value)
        data.value = "abc"
        assertEquals("abc", data.value)
        assertEquals("123", view.text.toString())

        view.setText("xyz")
        assertEquals("xyz", data.value)
        assertEquals("xyz", view.text.toString())
    }

    @Test
    fun twoWayBindingTest() {
        val activity = createActivity()
        val view = EditText(activity)
        val data = MutableLiveData<String>("data")
        view.setText("view")
        val textBinding = EditTextBinding.create(activity, view, data, mode = BindingMode.TwoWay)

        assertEquals("data", data.value)
        assertEquals("data", view.text.toString())

        data.value = "abc"
        assertEquals("abc", data.value)
        assertEquals("abc", view.text.toString())

        view.setText("xyz")
        assertEquals("xyz", data.value)
        assertEquals("xyz", view.text.toString())
    }

    @Test
    fun binderTest() {
        val activity = createActivity()
        val view = EditText(activity)
        val data = MutableLiveData<String>("data")
        view.setText("view")
        val binder = Binder().owner(activity)
        binder.editTextBinding(view, data)

        assertEquals("data", data.value)
        assertEquals("data", view.text.toString())

        data.value = "abc"
        assertEquals("abc", data.value)
        assertEquals("abc", view.text.toString())

        view.setText("xyz")
        assertEquals("xyz", data.value)
        assertEquals("xyz", view.text.toString())
    }
}