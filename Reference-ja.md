# リファレンス

## 1. バインディングモードとバインド可能なデータ型

android-binding には、３つのバインディングモードがあります。

### BindingMode.OneWay

ViewModelの Ovservable型フィールドから、ビューのプロパティへの単方向バインディング。<br>
visibilityBinding, enableBinding, textBinding, progressBarBinding などは、OneWayモードだけをサポートします。

単方向バインド可能なObservable型として、`LiveData` または、`Flow` を使用します。

### BindingMode.TwoWay

ViewModelの Observable型フィールドと、ビューのプロパティとの**双方向**バインディング。<br>
editTextBinding, checkBinding, seekBarBinding などは、TwoWayモードをサポートします。TwoWayモードをサポートするバインディングは、OneWay, OneWayToSourceモードもサポートしており、bindingMode引数で動作を変更できます。

双方向バインド可能なObservable型として、`MutableStateFlow` または `MutableLiveData` を使用します。

### BindingMode.OneWayToSource

Viewのプロパティから、ViewModelのObservable型フィールドへの単方向バインディング<br>。ほとんどの場合、TwoWay で代用できるため、あまり使いません。

バインド可能なObservable型として、`MutableStateFlow` または `MutableLiveData` を使用します。


## 2. バインディングクラス

### Boolean型バインディングクラス

|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
| CheckBinding | CompoundButtons (CheckBox, Switch, ToggleButton, ...) | isChecked |Boolean| TwoWay |
| EnableBinding | View | isEnabled | Boolean | OneWay      |
| MultiEnableBinding     | View(s) | isEnabled |Boolean| OneWay      |
| VisibilityBinding      | View | visibility |Boolean| OneWay      |
| MultiVisibilityBinding | View(s) | visibility |Boolean| OneWay      |
| ReadOnlyBinding | EditText | read only behavior |Boolean| OneWay      |

- ViewModelのBoolean型Observableフィールドを、ビューのBoolean型プロパティ(isChecked, isEnabled)にバインドします。
- VisibilityBindingでは、HiddenMode で、View.GONE/View.INVISIBLE を指定することにより、visibility プロパティをBoolean値にバインドします。
- 各Boolean型バインディングは、true/false を反転するかどうかを、`BoolConvert` 型の引数で指定できます。
- EnableBinding, MultiEnableBinding は、Float型の `alphaOnDisabled` 引数で、disable時の透過度を指定することができます。アイコンボタンなど、isEnabled = false にしても、自動的に無効表示にならないViewの場合に、簡易的な無効表示を提供します。

----
### Text型バインディングクラス

|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
|TextBinding|View|text|String|OneWay|
|IntBinding|View|text|Int|OneWay|
|LongBinding|View|text|Long|OneWay|
|FloatBinding|View|text|Float|OneWay|
|EditTextBinding|EditText|text|String|TwoWay|
|EditIntBinding|EditText|text|Int|TwoWay|
|EditLongBinding|EditText|text|Long|TwoWay|
|EditFloatBinding|EditText|text|Float|TwoWay|

- TextBinding は、ViewModelの String型Observableフィールドを、View（ButtonやTextViewなど）の `text` プロパティに単方向バインドします。
- IntBinding/LongBinding/FloatBinding は、対応する数値型Observableフィールドを Viewの `text` プロパティに単方向バインドします。
- EditTextBinding は、ViewModelの String型Observableフィールドと `EditText` の `text` プロパティとを双方向にバインドします。
- EditIntBinding/EditLongBinding/EditFloatBinding は、対応する数値型Observableフィールドを、`EditText `の `text` プロパティとを双方向にバインドします。

----
### ProgressBar/Sliderバインディングクラス

|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
|ProgressBarBinding|ProgressBar|progress|Int|OneWay|
|||min|Int|OneWay|
|||max|Int|OneWay|
|SeekBarBinding|SeekBar|value|Int|TwoWay|
|||min|Int|OneWay|
|||max|Int|OneWay|
|SliderBinding|Slider (Material Components)|value|Float|TwoWay|
|||valueFrom|Float|OneWay|
|||valueTo|Float|OneWay|


- `ProgressBarBinding` は、ViewModelのInt型Observableフィールドを、`ProgressBar` の `progress` プロパティに単方向バインドします。オプションで min/max プロパティも単方向バインドが可能です。
- `SeekBarBinding` は、ViewModelのInt型Observableフィールドを、`SeekBar`の `value` プロパティと双方向バインドします。オプションで `min`/`max` プロパティも単方向バインドが可能です。
- `SliderBinding` は、ViewModelのFloat型Observableフィールドを、Material Component の `Slider` の `value` プロパティと双方向バインドします。オプションで `valueFrom`/`valueTo` プロパティも単方向バインドが可能です。

----
### ラジオボタンバインディングクラス

|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
|RadioGroupBinding|RadioGroup|checkedRadioButtonId|T:Any (with IIDValueResolver&lt;T>)|TwoWay|
|MaterialRadioButtonGroupBinding|MaterialButtonToggleGroup (isSingleSelection=true, isSelectionRequired=true)|checkedButtonId|T:Any (with IIDValueResolver&lt;T>)|TwoWay|
|MaterialRadioButtonUnSelectableGroupBinding|MaterialButtonToggleGroup (isSingleSelection=true, isSelectionRequired=true)|checkedButtonId|T:Any (with IIDValueResolver&lt;T>)|TwoWay|

- `RadioGroupBinding`と`MaterialRadioButtonGroupBinding`は、それぞれ`RadioGroup` の `checkedRadioButtonId` プロパティ、`MaterialButtonToggleGroup` の `checkedButtonId` プロパティとViewModelが持つ、Observable&lt;T>型フィールド（通常 T はenum 値）とを双方向バインドします。
- `MaterialRadioUnSelectableButtonGroupBinding`は、「選択なし」という状態を許容する以外は、materialRadioButtonGroupBinding と同じです。
- ボタンのID(android:id) と `enum class`型を相互変換するには、`IIDValueResolver` を実装してください。
    ```kotlin
    interface IIDValueResolver<T> {
        fun id2value(@IdRes id:Int) : T?
        fun value2id(v:T): Int
    }
    ```
    例：
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
### トグルボタングループ用バインディングクラス

|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
|MaterialToggleButtonGroupBinding|MaterialButtonToggleGroup|checkedButtonIds|List&lt;T:Any> (using IIDValueResolver&lt;t>)|TwoWay|
|MaterialToggleButtonsBinding|MaterialButtonToggleGroup|checkedButtonIds|Boolean(s)|TwoWay|

- Material Component の MaterialToggleButtonGroup は、複数のトグルボタンをグループ化します。この各トグルボタンの値をモデル化するため、２つのアプローチを用意しました。
    - **MaterialToggleButtonGroupBinding**:<br>onになったトグルボタンに対して [IIDValueResolver](#ラジオボタンバインディングクラス)を使って変換される enum値のListとして扱い、ViewModel の `List<T>`型 Observableフィールドに双方向バインドします。
    - **MaterialToggleButtonsBinding**:<br>各ボタンのon/off を個別に扱い、ViewModel の `Boolean`型 Observableフィールドに１つずつ双方向にバインドします。

    MaterialToggleButtonsBindingの使用例：
    ```kotlin
    class MainViewModel:ViewModel() {
        val red = MutableStateFlow<Boolean>(false)
        val green = MutableStateFlow<Boolean>(false)
        val blue = MutableStateFlow<Boolean>(false)
    }

    class MainActivity: AppCompatActivity() {
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
### RecyclerView用バインディングクラス

|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
|RecyclerViewBinding|RecyclerView|adapter|ObservableList|TwoWay|

- `RecyclerView` と、その要素リストをバインドするため、要素の変更を監視可能な、MutableList i/f の実装クラス `ObservableList`を使用します。ビューモデルで、リストのデータソースを、MutableList の代わりに ObservableList クラスを使って実装します。
- RecyclerViewBindingは、RecyclerView と ViewModel の ObservableList とを双方向にバインドします。D&Dによるリストの並べ替えもサポートしており、その有効・無効も Boolean型のObservableフィールドにバインドして、動的に切り替えることも可能です。
- RecyclerViewBindingは、リスト要素の右スワイプジェスチャーによる要素削除をサポートします。こちらもジェスチャーの有効・無効を Boolean型のObservableフィールドにバインドして、動的に切り替えることも可能です。
- D&Dサポート、ジェスチャーサポートの有無などによって、いくつかのBinder拡張関数が用意されています。目的に応じて適切な拡張関数を選択してください。
- 個々の要素と、それを表示するためのItem View は、引数 `bindView:(Binder, View, T)->Unit)` で接続します。その第一引数で RecyclerView の Item View のライフサイクルに合わせて動作するBinderインスタンスが渡されるので、 bindView内で、Item View に対するバインディングを定義できます。

    例：
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
        override fun onCreate(saveInstanceState:Bundle?) {
            ...
            binder
            .owner(this)
            .recyclerViewGestureBinding(
                findViewById<RecyclerView>(R.id.list_view), 
                viewModel.list, 
                itemViewLayoutId = R.layout.list_item,
                gestureParams = RecyclerViewBinding.GestureParams(
                    dragToMove = true,
                    swipeToDelete= true)) { itemBinder, view, item->
                    view.findViewById<TextView>(R.id.title).text = item.name
                    itemBinder
                    .checkBinding(view.findViewById<TextView>(R.id.checkbox), item.selected)
                }
            ...
    ```

----
### アニメーションバインディング
|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
| FadeInOutBinding       | View | visibility with fade in/out effect |Boolean| OneWay      |
| MultiFadeInOutBinding   | View(s)   | visibility with fade in/out effect |Boolean| OneWay|
| AnimationBinding       | View | any property with [reversible animation](https://github.com/toyota-m2k/android-binding/blob/main/libBinder/src/main/java/io/github/toyota32k/binder/anim/IReversibleAnimation.kt) effect | Boolean |OneWay      |

- `FadeInOutBinding`, `MultiFadeInOutBinding` は、VisibilityBinding と同様に ViewModelのBoolean型Observableフィールドを View の Visibility に単方向バインドしますが、表示・非表示が切り替わるときに、フェードイン・アウトの視覚効果を付与します。
- `AnimationBinding` は、ViewModelのBoolean型Observableフィールドの変化をトリガーにして、複数のアニメーションを同時またはシーケンシャルに実行します。可逆的なアニメーションは、IReversibleAnimation i/f の実装クラスとして利用する側で用意する想定ですが、次の実装が利用できます。
  - ReversibleValueAnimation<br>
  基本的な値の可逆変化を行うクラス
  - VisibilityAnimation<br>
  ReversibleValueAnimationを利用して、visibilityとalpha値を変化させるアニメーションクラス。
  スライドイン、アウトなどのアニメーションを実装する場合は、このクラスを参考に、viewのオフセットなどを変化させるアニメーションクラスを実装してください。
  - SequencialAnimation<br>
  IReversibleAnimationインスタンスのListを持ち、それらをシーケンシャルに発動します。例えば、サイドパネルを非表示にしたあと、別のサイドパネルを表示する、というような順序性を持った処理のアニメーション化に利用します。
  - ParallelAnimation<br>
  IReversibleAnimationインスタンスのListを持ち、それらを同時に発動します。
  例えば、サイドパネルとヘッダービューを同時に非表示にする、というような同時処理のアニメーション化に利用します。


----
### その他のバインディングクラス

|Binding Class| View | View Property | ViewModel Type | Binding Mode|
|---|---|---|---|---|
|GenericBinding|View|Any|action callback|-|
|GenericBoolBinding|View|Any|action callback|OneWay|
|HeadlessBinding|-|-|action callback|-|
|ActivityBinding|(Activity)|StatusBar visibility|Boolean|OneWay|
|||ActionBar visibility|Boolean|OneWay|
|||Orientation|[ActivityOrientation](https://github.com/toyota-m2k/android-utilities/blob/cf408fb4aee6e45763f6970ddccdb071b781125b/libUtils/src/main/java/io/github/toyota32k/utils/ActivityExt.kt#L49)|OneWay
- `GenericBinding`, `GenericBooleanBinding` は、バインドしたViewModelの値が変化したときに、ビューとともに呼び出されるコールバック関数を指定します。つまり「Ovservableな値が変化したときに実行される、Viewに対する任意の処理」が定義できるので、新たにバインディングクラスを実装することなく、任意のプロパティへのバインディングを記述することが可能となります。
- `HeadlessBinding` は、GenericBinding の考え方をさらに進めて、Viewとは無関係に、「Observableな値が変化したときに実行される任意の処理」を定義できるようにするものです。これは、もはや、FlowやLiveData の値を直接 observe するのと同じなのですが、ViewModelに対する Observeをすべて、Binding と同じ手法で記述でき、ライフサイクルに応じて自動的にunsubscribeできる利点があります。
- `ActivityBinding` は、Activityのプロパティ（StatusBar/ActionBarの表示・非表示、Orientation）と ViewModelとのバインド機能を提供します。ActivityはViewではないので、この実装に HeadlessBinding を利用しています。

## 3. コマンドクラス

### LiteCommand / LiteUnitCommand

`LiteCommand<T>` / `LiteUnitCommand` は、View（Buttonなど）のクリックイベント(setOnClickListener)、または、EditTextに対するReturnキー押下イベントをトリガーとして、登録されたアクション（ハンドラ）を呼び出します。

LiteCommand&lt;T> は、ハンドラに１個引数を取ります。
例えば、okボタンとキャンセルボタンに１つのハンドラを登録するような場合に、LiteCommand&lt;Boolean>を使えば、１つのcommandインスタンス＋１つのハンドラで、ok/cancel を処理できます。

```kotlin
val completeCommand = LiteCommand<Boolean> { ok->
    if(ok) {
        // ok button tapped
    } else {
        // cancel button tapped
    }
}

val binder = Binder()
override fun onCreate(savedInstanceState:Bundle?) {
    binder
    .owner(this)
    .bindCommand(completeCommand, findViewById<Button>(R.id.ok_button).to(true))
    .bindCommand(completeCommand, findViewById<Button>(R.id.cancel_button).to(false))
}
```

LiteUnitCommand は、引数のないハンドラを使います。
上記のコードは、LiteUnitCommandを使って次のように書き換えられます。

```kotlin
val okCommand = LiteUnitCommand { 
   // ok button tapped
}
val cancelCommand = LiteUnitCommand { 
   // cancel button tapped
}

val binder = Binder()
override fun onCreate(savedInstanceState:Bundle?) {
    binder
    .owner(this)
    .bindCommand(okCommand, findViewById<Button>(R.id.ok_button))
    .bindCommand(cancelCommand, findViewById<Button>(R.id.cancel_button))
}
```

### ReliableCommand / ReliableUnitCommand

使い方は、LiteCommand / LiteUnitCommand とまったく同じですが、コマンドが invoke() されたときに、LifecycleOwner が destroy() されていた場合の挙動だけが異なります。ButtonタップなどのUIイベントのハンドラとして利用する場合は、LiteCommand / LiteUnitCommand で十分機能します。それは、invoke()された（==ボタンがタップされた）ときに LifecycleOwner が「生きている」ことが確実だからです。

これに対して、バックグランドスレッド処理からinvoke()するコマンド、たとえば、ファイルのダウンロードが終わった時にメッセージボックスを表示する、というようなコマンドを考えます。

バックグラウンドスレッドでダウンロードが完了したときに、画面がオフになっていたり、ユーザーが他のアプリの画面に切り替えているかも知れません。これらの場合には、LifecycleOwnerのActivityが destroyされた状態で invoke() が呼ばれることになります。この時、LiteCommand / LiteUnitCommand で実装したコマンドは期待通りに動作しません。
- bind()されたハンドラは呼び出されません。
- bindForever()されたハンドラは直ちに実行されますが、アプリがバックグラウンドにいるため、メッセージボックスの表示要求が失敗します。

このようなコマンドには、ReliableCommand / ReliableUnitCommand を使用します。これらは、LifecycleOwner が destroy されている状態で invoke()されると、LifecycleOwnerが再構築されて利用可能になるまで、ハンドラの実行を保留します。つまり、LifecycleOwner が「生きている」状態でハンドラが実行されることが保証されます。

