# Reference

## 1. Binding Modes and Bindable Data Types

There are three binding modes in android-binding.

### BindingMode.OneWay

One-way binding from an Observable field in the ViewModel to a property in the View.
Methods like visibilityBinding, enableBinding, textBinding, and progressBarBinding support only OneWay mode.

For one-way binding, use `LiveData` or `Flow` as the Observable type.

### BindingMode.TwoWay

**Two-way** binding between an Observable field in the ViewModel and a property in the View.
Methods like editTextBinding, checkBinding, and seekBarBinding support TwoWay mode. Bindings that support TwoWay mode also support OneWay and OneWayToSource modes, and their behavior can be changed with the bindingMode argument.

For two-way binding, use `MutableStateFlow` or `MutableLiveData` as the Observable type.

### BindingMode.OneWayToSource

One-way binding from a property in the View to an Observable field in the ViewModel. This is seldom used because TwoWay can generally substitute it.

For binding, use `MutableStateFlow` or `MutableLiveData` as the Observable type.

## 2. Binding Classes

### Boolean Binding Classes


|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
| CheckBinding | CompoundButtons (CheckBox, Switch, ToggleButton, ...) | isChecked |Boolean| TwoWay |
| EnableBinding | View | isEnabled | Boolean | OneWay      |
| MultiEnableBinding     | View(s) | isEnabled |Boolean| OneWay      |
| VisibilityBinding      | View | visibility |Boolean| OneWay      |
| MultiVisibilityBinding | View(s) | visibility |Boolean| OneWay      |
| ReadOnlyBinding | EditText | read only behavior |Boolean| OneWay      |

- Bind the Boolean Observable field of the ViewModel to the Boolean property (isChecked, isEnabled) of the View.
- In VisibilityBinding, the visibility property can be bound to a Boolean value by specifying View.GONE/View.INVISIBLE in HiddenMode.
- For each Boolean binding, the BoolConvert argument can be used to specify whether to invert true/false values.
- EnableBinding and MultiEnableBinding can specify the transparency when disabled with the `alphaOnDisabled` Float argument. This provides a simple disabled display for Views that do not automatically show as disabled when `isEnabled = false`, such as icon buttons.

----
### Text Binding Classes

| Binding Class | View | View Property | ViewModel Type | Binding Mode |
|---|---|---|---|---|
| TextBinding | View | text | String | OneWay |
| IntBinding | View | text | Int | OneWay |
| LongBinding | View | text | Long | OneWay |
| FloatBinding | View | text | Float | OneWay |
| EditTextBinding | EditText | text | String | TwoWay |
| EditIntBinding | EditText | text | Int | TwoWay |
| EditLongBinding | EditText | text | Long | TwoWay |
| EditFloatBinding | EditText | text | Float | TwoWay |

- `TextBinding` binds a String-type Observable field in the ViewModel to the `text` property of a View (such as a Button or TextView) in a one-way direction.
- `IntBinding`/`LongBinding`/`FloatBinding` bind the corresponding numeric-type Observable fields to the `text` property of a View in a one-way direction.
- `EditTextBinding` binds a String-type Observable field in the ViewModel to the `text` property of an `EditText` in a two-way direction.
- `EditIntBinding`/`EditLongBinding`/`EditFloatBinding` bind the corresponding numeric-type Observable fields to the `text` property of an `EditText` in a two-way direction.

----
### ProgressBar/Slider Binding Classes

| Binding Class | View | View Property | ViewModel Type | Binding Mode |
|---|---|---|---|---|
| ProgressBarBinding | ProgressBar | progress | Int | OneWay |
|  |  | min | Int | OneWay |
|  |  | max | Int | OneWay |
| SeekBarBinding | SeekBar | value | Int | TwoWay |
|  |  | min | Int | OneWay |
|  |  | max | Int | OneWay |
| SliderBinding | Slider (Material Components) | value | Float | TwoWay |
|  |  | valueFrom | Float | OneWay |
|  |  | valueTo | Float | OneWay |

- `ProgressBarBinding` binds an Int-type Observable field in the ViewModel to the `progress` property of a `ProgressBar` in a one-way direction. Optionally, min/max properties can also be bound in a one-way direction.
- `SeekBarBinding` binds an Int-type Observable field in the ViewModel to the `value` property of a `SeekBar` in a two-way direction. Optionally, `min`/`max` properties can also be bound in a one-way direction.
- `SliderBinding` binds a Float-type Observable field in the ViewModel to the `value` property of a Material Component `Slider` in a two-way direction. Optionally, `valueFrom`/`valueTo` properties can also be bound in a one-way direction.

----
### Radio Button Binding Classes

| Binding Class | View | View Property | ViewModel Type | Binding Mode |
|---|---|---|---|---|
| RadioGroupBinding | RadioGroup | checkedRadioButtonId | T:Any (with IIDValueResolver&lt;T>) | TwoWay |
| MaterialRadioButtonGroupBinding | MaterialButtonToggleGroup (isSingleSelection=true, isSelectionRequired=true) | checkedButtonId | T:Any (with IIDValueResolver&lt;T>) | TwoWay |
| MaterialRadioButtonUnSelectableGroupBinding | MaterialButtonToggleGroup (isSingleSelection=true, isSelectionRequired=true) | checkedButtonId | T:Any (with IIDValueResolver&lt;T>) | TwoWay |

- `RadioGroupBinding` and `MaterialRadioButtonGroupBinding` bind the `checkedRadioButtonId` property of `RadioGroup` and the `checkedButtonId` property of `MaterialButtonToggleGroup` respectively, with an Observable&lt;T> field in the ViewModel (usually T is an enum value), in a two-way manner.
- `MaterialRadioUnSelectableButtonGroupBinding` is the same as `MaterialRadioButtonGroupBinding` except that it allows an "unselected" state.
- To convert between button IDs (android:id) and an `enum class` type, implement the `IIDValueResolver` interface.
    ```kotlin
    interface IIDValueResolver<T> {
        fun id2value(@IdRes id:Int) : T?
        fun value2id(v:T): Int
    }
    ```
    Example:
    ```kotlin
    enum class RGB(@IdRes val id:Int) {
        RED(R.id.red_button),
        GREEN(R.id.green_button),
        BLUE(R.id.blue_button);

        companion object {
            fun valueOf(@IdRes id: Int): RGB? {
                return entries.find { it.id == id }
            }
        }
        object IDResolver:IIDValueResolver<RGB> {
            override fun id2value(id: Int): RGB? = valueOf(id)
            override fun value2id(v: RGB): Int = v.id
        }
    }

    ...
    class MainViewModel : ViewModel() {
        val rgb = MutableStateFlow<RGB?>(null)
    }

    class MainActivity: AppCompatActivity() {
        private val viewModel by viewModels<MainViewModel>()
        private val binder = Binder()
        override fun onCreate(savedInstanceState: Bundle?) {
            ...
            binder
                .owner(this)
                .materialRadioUnSelectableButtonGroupBinding(
                    buttonToggleGroup, viewModel.rgb, RGB.IDResolver)
    ```

----
### Toggle Button Group Binding Classes

| Binding Class | View | View Property | ViewModel Type | Binding Mode |
|---|---|---|---|---|
| MaterialToggleButtonGroupBinding | MaterialButtonToggleGroup | checkedButtonIds | List&lt;T:Any> (using IIDValueResolver&lt;T>) | TwoWay |
| MaterialToggleButtonsBinding | MaterialButtonToggleGroup | checkedButtonIds | Boolean(s) | TwoWay |

- The MaterialToggleButtonGroup in Material Components groups multiple toggle buttons. There are two approaches to model the values of these toggle buttons:
    - **MaterialToggleButtonGroupBinding**: Treats the toggled-on buttons as a list of enum values converted using the [IIDValueResolver](#ラジオボタンバインディングクラス) and binds them bidirectionally to an `Observable<List<T>>` field in the ViewModel.
    - **MaterialToggleButtonsBinding**: Treats each button's on/off state individually and binds each bidirectionally to an `Observable<Boolean>` field in the ViewModel.

    Example of using MaterialToggleButtonsBinding:
    ```kotlin
    class MainViewModel : ViewModel() {
        val red = MutableStateFlow<Boolean>(false)
        val green = MutableStateFlow<Boolean>(false)
        val blue = MutableStateFlow<Boolean>(false)
    }

    class MainActivity : AppCompatActivity() {
        private val viewModel by viewModels<MainViewModel>()
        private val binder = Binder()
        override fun onCreate(savedInstanceState: Bundle?) {
            ...
            binder
                .owner(this)
                .materialToggleButtonsBinding(group, BindingMode.TwoWay) {
                    bind(controls.redButton, viewModel.red)
                    bind(controls.greenButton, viewModel.green)
                    bind(controls.blueButton, viewModel.blue)
                }
    ```

----
### RecyclerView Binding Classes

| Binding Class | View | View Property | ViewModel Type | Binding Mode |
|---|---|---|---|---|
| RecyclerViewBinding | RecyclerView | adapter | ObservableList | TwoWay |

- To bind `RecyclerView` and its element list, use `ObservableList`, an implementation class of the MutableList interface that can monitor element changes. In the ViewModel, implement the list data source using `ObservableList` instead of `MutableList`.
- `RecyclerViewBinding` binds `RecyclerView` and the ViewModel's `ObservableList` in a two-way manner. It also supports drag-and-drop (D&D) reordering of the list, and its enable/disable state can be bound to a Boolean Observable field for dynamic toggling.
- `RecyclerViewBinding` supports element deletion via right swipe gestures. The enable/disable state of this gesture can also be dynamically toggled by binding it to a Boolean Observable field.
- Several Binder extension functions are provided depending on the presence of D&D support and gesture support. Choose the appropriate extension function based on your needs.
- Individual elements and the Item View used to display them are connected via the `bindView:(Binder, View, T)->Unit)` argument. A Binder instance that operates according to the Item View's lifecycle in the RecyclerView is passed as the first argument, allowing you to define binding within the bindView.

    Example:
    ```kotlin
    class Item {
        val name: String
        val selected = MutableStateFlow<Boolean>(false)
    }

    class MainViewModel : ViewModel() {
        val list = ObservableList<Item>()
    }

    class MainActivity : AppCompatActivity() {
        private val viewModel by viewModels<MainViewModel>()
        private val binder = Binder()
        override fun onCreate(savedInstanceState: Bundle?) {
            ...
            binder
            .owner(this)
            .recyclerViewGestureBinding(
                findViewById<RecyclerView>(R.id.list_view), 
                viewModel.list, 
                itemViewLayoutId = R.layout.list_item,
                gestureParams = RecyclerViewBinding.GestureParams(
                    dragToMove = true,
                    swipeToDelete = true)) { itemBinder, view, item ->
                    view.findViewById<TextView>(R.id.title).text = item.name
                    itemBinder
                    .checkBinding(view.findViewById<TextView>(R.id.checkbox), item.selected)
                }
            ...
    ```
----
### Animation Binding

| Binding Class | View | View Property | ViewModel Type | Binding Mode |
|---|---|---|---|---|
| FadeInOutBinding | View | visibility with fade in/out effect | Boolean | OneWay |
| MultiFadeInOutBinding | View(s) | visibility with fade in/out effect | Boolean | OneWay |
| AnimationBinding | View | any property with [reversible animation](https://github.com/toyota-m2k/android-binding/blob/main/libBinder/src/main/java/io/github/toyota32k/binder/anim/IReversibleAnimation.kt) effect | Boolean | OneWay |

- `FadeInOutBinding` and `MultiFadeInOutBinding` bind a Boolean Observable field in the ViewModel to the visibility property of a View, similar to VisibilityBinding, but with fade-in/out visual effects when visibility is toggled.
- `AnimationBinding` triggers multiple animations either simultaneously or sequentially based on changes in a Boolean Observable field in the ViewModel. It assumes that reversible animations will be provided as implementation classes of the `IReversibleAnimation` interface. Here are some implementations:
  - **ReversibleValueAnimation**: A class that performs basic reversible value changes.
  - **VisibilityAnimation**: An animation class that changes visibility and alpha values using ReversibleValueAnimation. To implement animations like slide-in/out, refer to this class to create animations that change properties like view offsets.
  - **SequentialAnimation**: Holds a list of `IReversibleAnimation` instances and triggers them sequentially. Useful for animations with an order, such as hiding one side panel before showing another.
  - **ParallelAnimation**: Holds a list of `IReversibleAnimation` instances and triggers them simultaneously. Useful for animations that should occur at the same time, such as hiding both a side panel and a header view simultaneously.

----
### Other Binding Classes

| Binding Class | View | View Property | ViewModel Type | Binding Mode |
|---|---|---|---|---|
| GenericBinding | View | Any | action callback | - |
| GenericBoolBinding | View | Any | action callback | OneWay |
| HeadlessBinding | - | - | action callback | - |
| ActivityBinding | (Activity) | StatusBar visibility | Boolean | OneWay |
|  |  | ActionBar visibility | Boolean | OneWay |
|  |  | Orientation | [ActivityOrientation](https://github.com/toyota-m2k/android-utilities/blob/cf408fb4aee6e45763f6970ddccdb071b781125b/libUtils/src/main/java/io/github/toyota32k/utils/ActivityExt.kt#L49) | OneWay |

- `GenericBinding` and `GenericBooleanBinding` allow specifying a callback function that is called with the View when the bound ViewModel value changes. This means you can define any arbitrary processing for the View that occurs when an Observable value changes, without implementing new binding classes.
- `HeadlessBinding` extends the concept of GenericBinding further, allowing you to define any arbitrary processing that occurs when an Observable value changes, independently of any View. This is essentially the same as directly observing the values of Flow or LiveData but allows you to describe all observations in the ViewModel using the same binding method and automatically unsubscribe according to the lifecycle.
- `ActivityBinding` provides binding functionality between Activity properties (StatusBar/ActionBar visibility, Orientation) and the ViewModel. Since an Activity is not a View, HeadlessBinding is used in this implementation.

### 3. Command Classes

### LiteCommand / LiteUnitCommand

`LiteCommand<T>` / `LiteUnitCommand` are classes that trigger registered actions (handlers) based on click events (setOnClickListener) for Views (such as Buttons) or Return key press events for EditTexts.

`LiteCommand<T>` takes one argument in the handler.
For example, when registering a single handler for both an OK button and a Cancel button, `LiteCommand<Boolean>` can be used to handle OK/Cancel with a single command instance and a single handler.

```kotlin
val completeCommand = LiteCommand<Boolean> { ok ->
    if (ok) {
        // OK button tapped
    } else {
        // Cancel button tapped
    }
}

val binder = Binder()
override fun onCreate(savedInstanceState: Bundle?) {
    binder
    .owner(this)
    .bindCommand(completeCommand, findViewById<Button>(R.id.ok_button).to(true))
    .bindCommand(completeCommand, findViewById<Button>(R.id.cancel_button).to(false))
}
```

`LiteUnitCommand` uses a handler without arguments.
The above code can be rewritten using `LiteUnitCommand` as follows:

```kotlin
val okCommand = LiteUnitCommand { 
   // OK button tapped
}
val cancelCommand = LiteUnitCommand { 
   // Cancel button tapped
}

val binder = Binder()
override fun onCreate(savedInstanceState: Bundle?) {
    binder
    .owner(this)
    .bindCommand(okCommand, findViewById<Button>(R.id.ok_button))
    .bindCommand(cancelCommand, findViewById<Button>(R.id.cancel_button))
}
```

### ReliableCommand / ReliableUnitCommand

The usage of `ReliableCommand<T>` / `ReliableUnitCommand` is exactly the same as `LiteCommand<T>` / `LiteUnitCommand`, with the only difference being their behavior when invoked while the `LifecycleOwner` is destroyed. For UI event handlers like button taps, `LiteCommand` / `LiteUnitCommand` work perfectly well, as it is certain that the `LifecycleOwner` is "alive" when invoked.

However, consider commands that are invoked from background thread processing, such as displaying a message box when a file download is complete.

When the download completes in a background thread, the screen might be off, or the user might have switched to another app. In such cases, the `LifecycleOwner`'s Activity might be destroyed when invoked. Commands implemented with `LiteCommand` / `LiteUnitCommand` will not work as expected:
- Handlers bound with `bind()` will not be called.
- Handlers bound with `bindForever()` will be executed immediately, but the message box display request will fail as the app is in the background.

For such commands, use `ReliableCommand` / `ReliableUnitCommand`. When these commands are invoked while the `LifecycleOwner` is destroyed, they will hold off executing the handler until the `LifecycleOwner` is reconstructed and available. This ensures that the handler is executed while the `LifecycleOwner` is "alive."