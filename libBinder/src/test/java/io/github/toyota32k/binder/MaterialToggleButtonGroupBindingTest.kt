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
class MaterialToggleButtonGroupBindingTest {
    @Rule
    @JvmField
    val instantExecutorRule: InstantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var activityController: ActivityController<JustTestActivity>

    fun createActivity(): AppCompatActivity {
        activityController = Robolectric.buildActivity(JustTestActivity::class.java)
        return activityController.create().start().get() as AppCompatActivity
    }

    fun finish() {
        activityController.pause().destroy()
    }

    private fun createToggleButton(context: Context): MaterialButtonToggleGroup {
        val r0 = MaterialButton(context).apply { id = TestEnum.E0.id }
        val r1 = MaterialButton(context).apply { id = TestEnum.E1.id }
        val r2 = MaterialButton(context).apply { id = TestEnum.E2.id }
        val r3 = MaterialButton(context).apply { id = TestEnum.E3.id }
        val tg = MaterialButtonToggleGroup(context)
        tg.isSingleSelection = false
        tg.addView(r0)
        tg.addView(r1)
        tg.addView(r2)
        tg.addView(r3)
        return tg
    }

    @Test
    fun oneWayTest() {
        val activity = createActivity()
        val view = createToggleButton(activity)

        Assert.assertEquals(4, view.childCount)

        val data = MutableLiveData<List<TestEnum>>(listOf(TestEnum.E1))
        view.check(TestEnum.E2.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())


        val binding = MaterialToggleButtonGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.OneWay)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1), data.value?.toTypedArray())

        data.value = listOf(TestEnum.E2, TestEnum.E3)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())

        view.check(TestEnum.E1.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id, TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E2, TestEnum.E3), data.value?.toTypedArray())

        finish()
    }

    @Test
    fun oneWayToSourceCheckTest() {
        val activity = createActivity()
        val view = createToggleButton(activity)

        Assert.assertEquals(4, view.childCount)

        val data = MutableLiveData<List<TestEnum>>(listOf(TestEnum.E1))
        view.check(TestEnum.E2.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())


        val binding = MaterialToggleButtonGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.OneWayToSource)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E2), data.value?.toTypedArray())

        data.value = listOf(TestEnum.E2, TestEnum.E3)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())

        view.check(TestEnum.E1.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id, TestEnum.E2.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1, TestEnum.E2), data.value?.toTypedArray())

        finish()
    }

    @Test
    fun twoWayCheckTest() {
        val activity = createActivity()
        val view = createToggleButton(activity)

        Assert.assertEquals(4, view.childCount)

        val data = MutableLiveData<List<TestEnum>>(listOf(TestEnum.E1))
        view.check(TestEnum.E2.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())

        val binding = MaterialToggleButtonGroupBinding.create<TestEnum>(activity, view, data, TestEnum.IDResolver, BindingMode.TwoWay)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1), data.value?.toTypedArray())

        data.value = listOf(TestEnum.E2, TestEnum.E3)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E2, TestEnum.E3), data.value?.toTypedArray())

        view.check(TestEnum.E1.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id, TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1, TestEnum.E2, TestEnum.E3), data.value?.toTypedArray())

        finish()
    }

    @Test
    fun binderTest() {
        val activity = createActivity()
        val view = createToggleButton(activity)
        val data = MutableLiveData<List<TestEnum>>(listOf(TestEnum.E1))
        Assert.assertEquals(4, view.childCount)
        view.check(TestEnum.E2.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())

        val binder = Binder().owner(activity)
        binder.materialToggleButtonGroupBinding(view, data, TestEnum.IDResolver)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1), data.value?.toTypedArray())

        data.value = listOf(TestEnum.E2, TestEnum.E3)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E2, TestEnum.E3), data.value?.toTypedArray())

        view.check(TestEnum.E1.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id, TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1, TestEnum.E2, TestEnum.E3), data.value?.toTypedArray())

        finish()
    }

}