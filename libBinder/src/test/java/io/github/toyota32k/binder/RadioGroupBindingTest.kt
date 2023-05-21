package io.github.toyota32k.binder

import android.content.Context
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class RadioGroupBindingTest {
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

    fun createRadioButton(context:Context):RadioGroup {
        val r0 = RadioButton(context).apply{ id = TestEnum.E0.id }
        val r1 = RadioButton(context).apply{ id = TestEnum.E1.id }
        val r2 = RadioButton(context).apply{ id = TestEnum.E2.id }
        val r3 = RadioButton(context).apply{ id = TestEnum.E3.id }
        return RadioGroup(context).apply {
            addView(r0)
            addView(r1)
            addView(r2)
            addView(r3)
        }
    }

    @Test
    fun oneWayCheckTest() {
        val activity = createActivity()
        val view = createRadioButton(activity)
        val data = MutableLiveData<TestEnum>(TestEnum.E1)
        view.check(TestEnum.E2.id)

        val binding = RadioGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.OneWay)
        Assert.assertEquals(TestEnum.E1.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        data.value = TestEnum.E2
        Assert.assertEquals(TestEnum.E2.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E2, data.value)

        view.check(TestEnum.E1.id)
        Assert.assertEquals(TestEnum.E1.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E2, data.value)
        finish()
    }
    @Test
    fun oneWayToSourceCheckTest() {
        val activity = createActivity()
        val view = createRadioButton(activity)
        val data = MutableLiveData<TestEnum>(TestEnum.E1)
        view.check(TestEnum.E2.id)

        val binding = RadioGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.OneWayToSource)
        Assert.assertEquals(TestEnum.E2.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E2, data.value)

        data.value = TestEnum.E1
        Assert.assertEquals(TestEnum.E2.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        view.check(TestEnum.E0.id)
        Assert.assertEquals(TestEnum.E0.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E0, data.value)
        finish()
    }
    @Test
    fun twoWayCheckTest() {
        val activity = createActivity()
        val view = createRadioButton(activity)
        val data = MutableLiveData<TestEnum>(TestEnum.E1)
        view.check(TestEnum.E2.id)

        val binding = RadioGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.TwoWay)
        Assert.assertEquals(TestEnum.E1.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        data.value = TestEnum.E1
        Assert.assertEquals(TestEnum.E1.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        view.check(TestEnum.E0.id)
        Assert.assertEquals(TestEnum.E0.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E0, data.value)
        finish()
    }

    @Test
    fun binderTest() {
        val activity = createActivity()
        val view = createRadioButton(activity)
        val data = MutableLiveData<TestEnum>(TestEnum.E1)
        view.check(TestEnum.E2.id)

        val binder = Binder().owner(activity)
        binder.radioGroupBinding(view, data, TestEnum.IDResolver)
        Assert.assertEquals(TestEnum.E1.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        data.value = TestEnum.E1
        Assert.assertEquals(TestEnum.E1.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        view.check(TestEnum.E0.id)
        Assert.assertEquals(TestEnum.E0.id, view.checkedRadioButtonId)
        Assert.assertEquals(TestEnum.E0, data.value)

        finish()
    }
}