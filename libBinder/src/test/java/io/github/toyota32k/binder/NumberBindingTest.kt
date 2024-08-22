package io.github.toyota32k.binder

import android.widget.EditText
import android.widget.TextView
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
class NumberBindingTest {
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
    fun oneWayNumberTest() {
        val activity = createActivity()
        val view = TextView(activity)
        val data = MutableLiveData(111)
        view.text="222"

        val binding = NumberBinding.create(activity, view, data)
        assertEquals("111", view.text)
        assertEquals(data.value, view.text.toString().toInt())

        data.value=333
        assertEquals("333", view.text)
        assertEquals(data.value, view.text.toString().toInt())

        view.text="444"
        assertEquals("444", view.text)
        assertEquals(333, data.value)

    }

    @Test
    fun oneWayToSourceNumberTest() {
        val activity = createActivity()
        val view = EditText(activity)
        val data = MutableLiveData(111)
        view.setText("222")

        val binding = EditNumberBinding.create(activity, view, data, BindingMode.OneWayToSource)
        assertEquals("222", view.text.toString())
        assertEquals(222, data.value)

        view.setText("444")
        assertEquals("444", view.text.toString())
        assertEquals(444, data.value)

        data.value=333
        assertEquals("444", view.text.toString())
        assertEquals(333, data.value)


    }
    @Test
    fun twoWayNumberTest() {
        val activity = createActivity()
        val view = EditText(activity)
        val data = MutableLiveData(111)
        view.setText("222")

        val binding = EditNumberBinding.create(activity, view, data, BindingMode.TwoWay)
        assertEquals("111", view.text.toString())
        assertEquals(111, data.value)

        view.setText("444")
        assertEquals("444", view.text.toString())
        assertEquals(444, data.value)

        data.value=333
        assertEquals("333", view.text.toString())
        assertEquals(333, data.value)
    }

    @Test
    fun binderTest() {
        val activity = createActivity()
        val view = EditText(activity)
        val data = MutableLiveData(111)
        view.setText("222")

        val binder = Binder().owner(activity)
        binder.editIntBinding(activity, view, data)
        assertEquals("111", view.text.toString())
        assertEquals(111, data.value)

        view.setText("444")
        assertEquals("444", view.text.toString())
        assertEquals(444, data.value)

        data.value=333
        assertEquals("333", view.text.toString())
        assertEquals(333, data.value)
    }
}