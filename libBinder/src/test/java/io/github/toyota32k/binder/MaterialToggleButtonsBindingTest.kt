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
class MaterialToggleButtonsBindingTest {
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
        val dataList = arrayOf(
            MutableLiveData<Boolean>(false),
            MutableLiveData<Boolean>(true),
            MutableLiveData<Boolean>(false),
            MutableLiveData<Boolean>(false),
        )

        Assert.assertEquals(4, view.childCount)

        view.check(TestEnum.E2.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())


        val binding = MaterialToggleButtonsBinding.create(activity, view, BindingMode.OneWay) {
            bind(view.findViewById(TestEnum.E0.id), dataList[0])
            bind(view.findViewById(TestEnum.E1.id), dataList[1])
            bind(view.findViewById(TestEnum.E2.id), dataList[2])
            bind(view.findViewById(TestEnum.E3.id), dataList[3])
        }
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(false,true,false,false), dataList.map {it.value}.toTypedArray())

        dataList[2].value = true
        Assert.assertArrayEquals(arrayOf(false,true,true,false), dataList.map {it.value}.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id, TestEnum.E2.id), view.checkedButtonIds.toTypedArray())

        view.check(TestEnum.E3.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id, TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(false,true,true,false), dataList.map {it.value}.toTypedArray())
        finish()
    }

    @Test
    fun oneWayToSourceTest() {
        val activity = createActivity()
        val view = createToggleButton(activity)
        val dataList = arrayOf(
            MutableLiveData<Boolean>(false),
            MutableLiveData<Boolean>(true),
            MutableLiveData<Boolean>(false),
            MutableLiveData<Boolean>(false),
        )

        Assert.assertEquals(4, view.childCount)

        view.check(TestEnum.E2.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())


        val binding = MaterialToggleButtonsBinding.create(activity, view, BindingMode.OneWayToSource) {
            bind(view.findViewById(TestEnum.E0.id), dataList[0])
            bind(view.findViewById(TestEnum.E1.id), dataList[1])
            bind(view.findViewById(TestEnum.E2.id), dataList[2])
            bind(view.findViewById(TestEnum.E3.id), dataList[3])
        }
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(false,false,true,false), dataList.map {it.value}.toTypedArray())

        dataList[1].value = true
        Assert.assertArrayEquals(arrayOf(false,true,true,false), dataList.map {it.value}.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())

        view.check(TestEnum.E3.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(false,true,true,true), dataList.map {it.value}.toTypedArray())
        //                                     ^^^^
        //                                     操作されたボタン以外の値には影響しない点に注意！
        //                  MaterialToggleButtonGroupBindingは、リストを取り替えるので、他のボタンの値にも影響するが。。。
        finish()
    }

    @Test
    fun twoWayTest() {
        val activity = createActivity()
        val view = createToggleButton(activity)
        val dataList = arrayOf(
            MutableLiveData<Boolean>(false),
            MutableLiveData<Boolean>(true),
            MutableLiveData<Boolean>(false),
            MutableLiveData<Boolean>(false),
        )

        Assert.assertEquals(4, view.childCount)

        view.check(TestEnum.E2.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())


        val binding = MaterialToggleButtonsBinding.create(activity, view, BindingMode.TwoWay) {
            bind(view.findViewById(TestEnum.E0.id), dataList[0])
            bind(view.findViewById(TestEnum.E1.id), dataList[1])
            bind(view.findViewById(TestEnum.E2.id), dataList[2])
            bind(view.findViewById(TestEnum.E3.id), dataList[3])
        }
        // Bind時の動作は OneWay と同じ（Source-->View)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(false,true,false,false), dataList.map {it.value}.toTypedArray())

        dataList[2].value = true
        Assert.assertArrayEquals(arrayOf(false,true,true,false), dataList.map {it.value}.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id,TestEnum.E2.id), view.checkedButtonIds.toTypedArray())

        view.check(TestEnum.E3.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id, TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(false,true,true,true), dataList.map {it.value}.toTypedArray())
        finish()
    }

    @Test
    fun binderTest() {
        val activity = createActivity()
        val view = createToggleButton(activity)
        val dataList = arrayOf(
            MutableLiveData<Boolean>(false),
            MutableLiveData<Boolean>(true),
            MutableLiveData<Boolean>(false),
            MutableLiveData<Boolean>(false),
        )
        val binder = Binder().owner(activity)

        Assert.assertEquals(4, view.childCount)
        view.check(TestEnum.E2.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E2.id), view.checkedButtonIds.toTypedArray())

        binder.materialToggleButtonsBinding(view) {
            bind(view.findViewById(TestEnum.E0.id), dataList[0])
            bind(view.findViewById(TestEnum.E1.id), dataList[1])
            bind(view.findViewById(TestEnum.E2.id), dataList[2])
            bind(view.findViewById(TestEnum.E3.id), dataList[3])
        }
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(false,true,false,false), dataList.map {it.value}.toTypedArray())

        dataList[2].value = true
        Assert.assertArrayEquals(arrayOf(false,true,true,false), dataList.map {it.value}.toTypedArray())
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id,TestEnum.E2.id), view.checkedButtonIds.toTypedArray())

        view.check(TestEnum.E3.id)
        Assert.assertArrayEquals(arrayOf(TestEnum.E1.id, TestEnum.E2.id, TestEnum.E3.id), view.checkedButtonIds.toTypedArray())
        Assert.assertArrayEquals(arrayOf(false,true,true,true), dataList.map {it.value}.toTypedArray())

        finish()
    }

}