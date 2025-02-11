package io.github.toyota32k.binder

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.binder.databinding.ActivityCatalogBinding
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.utils.ActivityOrientation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date

class CatalogActivity : AppCompatActivity() {
    class CatalogViewModel : ViewModel() {
        val inputText = MutableStateFlow("")
        val inputNumber = MutableStateFlow<Float>(0f)

        enum class RadioSampleValue(@IdRes val id:Int, @IdRes val materialId:Int) {
            Radio1(R.id.radio1, R.id.mtRadio1),
            Radio2(R.id.radio2, R.id.mtRadio2),
            Radio3(R.id.radio3, R.id.mtRadio3);

            companion object {
                fun valueOf(@IdRes id: Int): RadioSampleValue? {
                    return entries.find { it.id == id }
                }
                fun materialValueOf(@IdRes id: Int): RadioSampleValue? {
                    return entries.find { it.materialId == id }
                }
            }
            object IDResolver:IIDValueResolver<RadioSampleValue> {
                override fun id2value(id: Int): RadioSampleValue? = valueOf(id)
                override fun value2id(v: RadioSampleValue): Int = v.id
            }
            object MaterialIDResolver:IIDValueResolver<RadioSampleValue> {
                override fun id2value(id: Int): RadioSampleValue? = materialValueOf(id)
                override fun value2id(v: RadioSampleValue): Int = v.materialId
            }
        }
        val radioSample = MutableStateFlow(RadioSampleValue.Radio1)

        enum class ToggleSampleValue(@IdRes val id:Int) {
            Toggle1(R.id.toggle1),
            Toggle2(R.id.toggle2),
            Toggle3(R.id.toggle3);

            companion object {
                fun valueOf(@IdRes id: Int): ToggleSampleValue? {
                    return entries.find { it.id == id }
                }
            }

            object IDResolver : IIDValueResolver<ToggleSampleValue> {
                override fun id2value(id: Int): ToggleSampleValue? = valueOf(id)
                override fun value2id(v: ToggleSampleValue): Int = v.id
            }
        }
        val toggleValue = MutableLiveData<List<ToggleSampleValue>>()


        val isVisible = MutableStateFlow(true)
        val isEnabled = MutableStateFlow(true)
        val visibleWithFadeEffect = MutableStateFlow(true)
        val fadeInOutCommand = LiteUnitCommand {
            visibleWithFadeEffect.value = !visibleWithFadeEffect.value
        }

        class ListItem(val title:String, val time: Date=Date()) {
            companion object {
                var nextId = 0
                fun create():ListItem {
                    return ListItem("Item-${++nextId}")
                }
            }
        }
        val list = ObservableList<ListItem>()
        val addCommand = LiteUnitCommand {
            list.add(ListItem.create())
        }

        val showActionBar = MutableStateFlow(true)
        val showStatusBar = MutableStateFlow(true)

        enum class Orientation(@IdRes val id:Int) {
            Auto(View.NO_ID),
            Portrait(R.id.radio_portrait),
            Landscape(R.id.radio_landscape);
            companion object {
                fun valueOf(@IdRes id: Int): Orientation? {
                    return Orientation.entries.find { it.id == id }
                }
            }

            object IDResolver : IIDValueResolver<Orientation> {
                override fun id2value(id: Int): Orientation? = valueOf(id)
                override fun value2id(v: Orientation): Int = v.id
            }
        }

        val orientation = MutableStateFlow<Orientation>(Orientation.Auto)
        val activityOrientation = orientation.map {
                when(it) {
                    Orientation.Portrait -> ActivityOrientation.PORTRAIT
                    Orientation.Landscape -> ActivityOrientation.LANDSCAPE
                    else -> ActivityOrientation.AUTO
                }
            }
        }


    private lateinit var controls: ActivityCatalogBinding
    private val binder = Binder()
    private val viewModel by viewModels<CatalogViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        controls = ActivityCatalogBinding.inflate(layoutInflater)
        setContentView(controls.root)
        ViewCompat.setOnApplyWindowInsetsListener(controls.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binder
            .owner(this)
            // text bibding
            .textBinding(controls.textView, viewModel.inputText)
            .editTextBinding(controls.editText, viewModel.inputText)
            // number binding
            .editFloatBinding(controls.editNumberText, viewModel.inputNumber)
            .sliderBinding(controls.slider, viewModel.inputNumber)
            // radio binding
            .radioGroupBinding(controls.radioGroup, viewModel.radioSample, CatalogViewModel.RadioSampleValue.IDResolver)
            .materialRadioButtonGroupBinding(controls.toggleGroupAsRadio, viewModel.radioSample, CatalogViewModel.RadioSampleValue.MaterialIDResolver)
            .textBinding(controls.radioValue, viewModel.radioSample.map { it.name })
            // toggle buttons binding
            .materialToggleButtonGroupBinding(controls.toggleGroup, viewModel.toggleValue, CatalogViewModel.ToggleSampleValue.IDResolver)
            .textBinding(controls.toggleValue, viewModel.toggleValue.map { it.joinToString { it.name } })
            // boolean binding
            .checkBinding(controls.checkBoxShow, viewModel.isVisible)
            .checkBinding(controls.checkBoxEnable, viewModel.isEnabled)
            .bindCommand(viewModel.fadeInOutCommand, controls.fadeInOutButton)
            .visibilityBinding(controls.sampleButton, viewModel.isVisible, hiddenMode = VisibilityBinding.HiddenMode.HideByInvisible)
            .multiEnableBinding(arrayOf(controls.sampleButton, controls.sampleEditText), viewModel.isEnabled)
            .fadeInOutBinding(controls.sampleContainer, viewModel.visibleWithFadeEffect)
            // RecyclerView binding
            .recyclerViewGestureBinding(controls.recyclerView, viewModel.list, R.layout.list_item,
                gestureParams = RecyclerViewBinding.GestureParams(dragToMove = true, swipeToDelete = true)) { _, view, item->
                    // この例は固定値なので binder は使わない
                    view.findViewById<TextView>(R.id.title).text = item.title
                    view.findViewById<TextView>(R.id.sub_title).text = item.time.toString()
                }
            .bindCommand(viewModel.addCommand, controls.addListItemButton)
            // Activity binding
            .checkBinding(controls.checkBoxActionBar, viewModel.showActionBar)
            .checkBinding(controls.checkBoxStatusBar, viewModel.showStatusBar)
            .activityActionBarBinding(viewModel.showActionBar)
            .activityStatusBarBinding(viewModel.showStatusBar)
            .materialRadioUnSelectableButtonGroupBinding(controls.orientationGroup, viewModel.orientation, CatalogViewModel.Orientation.IDResolver)
            .activityOrientationBinding(this, viewModel.activityOrientation)
    }
}