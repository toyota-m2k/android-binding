package io.github.toyota32k.binder

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application= JustTestApplication::class)
class MaterialRadioButtonGroupBindingTest {
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

    private fun createRadioButton(context: Context): MaterialButtonToggleGroup {
        val r0 = MaterialButton(context).apply{ id = TestEnum.E0.id }
        val r1 = MaterialButton(context).apply{ id = TestEnum.E1.id }
        val r2 = MaterialButton(context).apply{ id = TestEnum.E2.id }
        val r3 = MaterialButton(context).apply{ id = TestEnum.E3.id }
        val tg = MaterialButtonToggleGroup(context)
        tg.isSingleSelection = true
        tg.addView(r0)
        tg.addView(r1)
        tg.addView(r2)
        tg.addView(r3)
        return tg
    }


    @Test
    fun oneWayTest() {
        val activity = createActivity()
        val view = createRadioButton(activity)

        Assert.assertEquals(4, view.childCount)

        val data = MutableLiveData<TestEnum>(TestEnum.E1)
        view.check(TestEnum.E2.id)

        val binding = MaterialRadioButtonGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.OneWay)
        Assert.assertEquals(TestEnum.E1.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        data.value = TestEnum.E2
        Assert.assertEquals(TestEnum.E2.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E2, data.value)

        view.check(TestEnum.E1.id)
        Assert.assertEquals(TestEnum.E1.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E2, data.value)
    }

    @Test
    fun oneWayToSourceCheckTest() {
        val activity = createActivity()
        val view = createRadioButton(activity)
        val data = MutableLiveData<TestEnum>(TestEnum.E1)
        view.check(TestEnum.E2.id)

        val binding = MaterialRadioButtonGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.OneWayToSource)
        Assert.assertEquals(TestEnum.E2.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E2, data.value)

        data.value = TestEnum.E1
        Assert.assertEquals(TestEnum.E2.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        view.check(TestEnum.E0.id)
        Assert.assertEquals(TestEnum.E0.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E0, data.value)
    }

    @Test
    fun twoWayCheckTest() {
        val activity = createActivity()
        val view = createRadioButton(activity)
        val data = MutableLiveData<TestEnum>(TestEnum.E1)
        view.check(TestEnum.E2.id)

        val binding = MaterialRadioButtonGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.TwoWay)
        Assert.assertEquals(TestEnum.E1.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        data.value = TestEnum.E1
        Assert.assertEquals(TestEnum.E1.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        view.check(TestEnum.E0.id)
        Assert.assertEquals(TestEnum.E0.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E0, data.value)
    }

    @Test
    fun binderTest() {
        val activity = createActivity()
        val view = createRadioButton(activity)
        val data = MutableLiveData<TestEnum>(TestEnum.E1)
        val binder = Binder().owner(activity)
        view.check(TestEnum.E2.id)

        binder.materialRadioButtonGroupBinding(view, data, TestEnum.IDResolver)
        Assert.assertEquals(TestEnum.E1.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        data.value = TestEnum.E1
        Assert.assertEquals(TestEnum.E1.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E1, data.value)

        view.check(TestEnum.E0.id)
        Assert.assertEquals(TestEnum.E0.id, view.checkedButtonId)
        Assert.assertEquals(TestEnum.E0, data.value)

        finish()
    }

}