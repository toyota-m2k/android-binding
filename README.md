# android-binding ... View-ViewModel Binding for Android Application


Androidアプリ用の、View-ViewModelバインディングを実現するライブラリです。

Windows WFP/UWP方面からAndroidに移住してきた私は、
layout.xmlとjava/ktのコードをバインドする手段が、あまりにも貧弱（「無い」といっても過言ではない！）ことに驚きました。
DataBinding という仕組みはあるものの、バインドできないプロパティはあるし、ついさっきまで動いていたコードが、突然kaptエラーになったり、
悪いことばかり起きるので、数週間の格闘の末、完全に見切りをつけました。しかし、リアクティブなプログラムを書くためには、バインディングの仕掛けは不可欠です。
そこでやむを得ず自作したのが、このバインディングライブラリです。
今後は Jetpack Compose がUI開発の中心になって、layout.xml は消えゆく運命なのかもしれませんが、
個人的には、複雑なUIを書くとき layout.xml の方が自由に小細工できて書きやすいので、当面は開発を続けたいと思っています。
ちなみに、ViewBinding （xmlからビューの定義を生成するやつ）の方は、（まれに、定義が作られなくてリビルドしたりするけど）安定しているので、このライブラリと併用していますが、必須ではありません。。

## Gradle

settings.gradle.kts で、mavenリポジトリ https://jitpack.io への参照を定義。  

```kotlin
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

モジュールの build.gradle で、dependencies を追加。
一部のクラスが android-utilities のクラス/インターフェースを継承/実装しているので android-utilities も依存関係に追加してください。

```kotlin
dependencies {
    implementation("com.github.toyota-m2k:android-utilities:Tag")
    implementation("com.github.toyota-m2k:android-binding:Tag")
}
```

## 使い方

ここでは、説明のため簡単なログイン画面を作成してみます。

### ビューモデルの作成

```kotlin
    class AuthenticationViewModel : ViewModel() {
    val userName = MutableStateFlow<String>("")
    val password = MutableStateFlow<String>("")
    val showPassword = MutableStateFlow<Boolean>(false)
    val isBusy = MutableStateFlow<Boolean>(false)
    val isReady = combine(userName, password, isBusy) { u, p, b ->
        u.isNotEmpty() && p.isNotEmpty() && !b
    }
    val loginCommand = LiteUnitCommand {
        // 認証中はビジーフラグを立てる
        isBusy.value = true
        // サブスレッドで認証を実行
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (MyAuthenticator.tryLogin(userName.value, password.value)) {
                    // 認証成功
                    authenticated.value = true
                }
            } finally {
                isBusy.value = false
            }
        }
    }
    val logoutCommand = LiteUnitCommand()
    val authenticated: MutableStateFlow<Boolean> = MutableStateFlow(false)
}
```
ビューモデルでは、認証情報としてユーザー名(`userName`)とパスワードの文字列(`password`)、認証中フラグ(`isBusy`)、および、パスワードを表示するかどうかのフラグ(`showPassword`) をMutableStateFlowとして保持しています。
`userName`, `password` および、、`isBusy` を combineして フロー `isReady` を生成しています。コマンドとして、`loginCommand`と`logoutCommand` を用意しました。
説明のため、`loginCommand` は、その動作をビューモデル内に実装して、LiteUnitCommand のコンストラクタで初期化する例を、`logoutCommand` は、
コマンドのインスタンスだけビューモデル内に用意し、ビューとのバインド時に処理内容を渡す構成にしました。
また、この例では、単純化のため、認証が成功すると `authenticated` にtrueをセットすることで、同じActivity内でビューを切り替えるようにしています。

### レイアウト(layout.xml)の作成

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <LinearLayout
        android:id="@+id/auth_panel"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center_vertical"
    >

        <EditText
            android:id="@+id/user_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="User Name"
        />
        <EditText
            android:id="@+id/password"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textPassword"
            android:hint="password"
        />
        <CheckBox
            android:id="@+id/show_password"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end"
            android:text="Show Password" />
        <Button
            android:id="@+id/login_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:text="Login" />
    </LinearLayout>
    <LinearLayout
        android:id="@+id/main_panel"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
    >
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Authenticated"
            android:textSize="40sp"
        />
        <Button
            android:id="@+id/logout_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:text="Logout" />
    </LinearLayout>

</FrameLayout>
```
未認証状態で表示される `auth_panel`には、ユーザー名、パスワードの入力欄、「パスワードを表示」するチェックボックス、ログインボタンを、
認証状態で表示される、`main_panel` には、"Authenticated" の文字とログアウトボタンを配置しました。
ViewModelとバインドするコントロールに id を振ること以外に、特別なルールはありません。

### バインディングの記述

```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<AuthenticationViewModel>()
    private val binder = Binder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)

        binder
            .owner(this)
            .editTextBinding(controls.userName, viewModel.userName)
            .editTextBinding(controls.password, viewModel.password)
            .checkBinding(controls.showPassword, viewModel.showPassword)
            .enableBinding(controls.loginButton, viewModel.isReady)
            .multiEnableBinding(arrayOf(controls.userName, controls.password, controls.showPassword), viewModel.isBusy, BoolConvert.Inverse)
            .bindCommand(viewModel.loginCommand, controls.loginButton)
            .bindCommand(viewModel.logoutCommand, controls.logoutButton, this@MainActivity::onLogout)
            .genericBinding(controls.password, viewModel.showPassword) { pwd, show ->
                pwd.inputType = if(show==true) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            .visibilityBinding(controls.authPanel, viewModel.authenticated, BoolConvert.Inverse, VisibilityBinding.HiddenMode.HideByGone)
            .visibilityBinding(controls.mainPanel, viewModel.authenticated, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByGone)
    }

    private fun onLogout() {
        viewModel.authenticated.value = false
    }
}
```

まず、Activityのメンバー変数として、`viewModel`, `binder` インスタンスを用意します。これらは、Activity のライフサイクルにあわせて動作するので、普通にコンストラクタで初期化して大丈夫です。

```
    private val viewModel by viewModels<AuthenticationViewModel>()
    private val binder = Binder()
```


以下、onCreateで、binder を使って、ビューとビューモデル、動作をバインドしていきます。
最初に、binder.owener()で、ライフサイクルオーナー(this@MainActivity) を設定しています。これにより、この binder インスタンスは、MainActivity のライフサイクルに従った動作を行います。すなわち、アクティビティがdestroyされたときに、すべてのバインディングが自動的に破棄されるので、リソースがリークする心配がありません。

```kotlin
        binder
            .owner(this)
```

まず、EditText とビューモデルの文字列(MutableStateFlow&lt;String>)を、双方向にバインドします。
また、CheckBox（CompoundButton）の isChecked プロパティと、ビューモデルのshowPassword(:MutableStateFlow&lt;String>)も、双方向にバインドします。
```kotlin
            .editTextBinding(controls.userName, viewModel.userName)
            .editTextBinding(controls.password, viewModel.password)
            .checkBinding(controls.showPassword, viewModel.showPassword)
```

さらに、isReady をログインボタンボタンの有効、無効（isEnabledプロパティ）にバインドします。
isBusy は、`multiEnableBinding`() を使って、複数のビュー（ユーザー名、パスワード入力用のEditText, パスワードを表示チェックボックス）のisEnabledに対して、まとめてバインド設定しています。

```kotlin
            .enableBinding(controls.loginButton, viewModel.isReady)
            .multiEnableBinding(arrayOf(controls.userName, controls.password, controls.showPassword), viewModel.isBusy, BoolConvert.Inverse)
```

次に、コマンドを介して、ボタンとアクションをバインドします。<br>
loginCommand はビューモデル内でハンドラまで定義しているので、コマンドとボタンをバインドするだけです。一方、logoutCommandの方は（説明のために）ビューモデル内でハンドラを定義していないません。そこで、`bindCommand`の第３引数でハンドラ（この例ではMainActivityのメソッド）を渡しています。
このように、コマンド(`ICommand`/`IUnitCommand`)を介して、ビューとハンドラを柔軟にバインドすることができます。
```kotlin
            .bindCommand(viewModel.loginCommand, controls.loginButton)
            .bindCommand(viewModel.logoutCommand, controls.logoutButton, this@MainActivity::onLogout)
```


ここで「パスワードを表示」チェックボックスの on/off に応じて、EditText の inputType を更新したいのですが、inputType のような特殊なプロパティとBoolean値をバインドする専用のクラスは用意していません。このような特殊なバインディングを実装したい場合は、genericBinding() を使います。そうすれば、バインディングされたFlowの値の変化をコールバックとして受け取れるので、自由にバインディング的な処理を追加できます。

```kotlin
            .genericBinding(controls.password, viewModel.showPassword) { pwd, show ->
                pwd.inputType = if(show==true) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
```

最後に、authenticatedによって、auth_panelとmain_panel の表示をトグルしています。
このように、visibilityBinding, enableBinding, checkBinding など、Boolean型の状態プロパティとビューの属性をバインドするメソッドは、
BoolConvert型の引数（デフォルトはStraight）をとることができます。visibility, enable など単方向バインドしか存在しな場合は、
`viewModel.authenticated.map {!it}` を渡しても同じ効果が得られます。しかし、checkBindingで双方向バインドしたい場合には、この方法は使えません。
 map() が返すのは Flow&lt;Boolean> であって、MutableStateFlowに値を書き戻すことができないからです。このような時、
`BoolConvert.Inverse` を指定すれば、bool値を反転しつつ双方向にリンクすることが可能です。
```kotlin
            .visibilityBinding(controls.authPanel, viewModel.authenticated, BoolConvert.Inverse, VisibilityBinding.HiddenMode.HideByGone)
            .visibilityBinding(controls.mainPanel, viewModel.authenticated, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByGone)
```

## LiteCommand&lt;T> と LiteUnitCommand

LiteCommand&lt;T> は、ハンドラに１個引数を取ります。
例えば、okボタンとキャンセルボタンに１つのハンドラを登録するような場合に、LiteCommand&lt;Boolean>を使えば、１つのcommandインスタンス＋１つのハンドラで、ok/cancel を処理できます。
ただ、実際にコードを書いてみると、１イベント１ハンドラで書いた方がスッキリするケース多く、引数を要求したくなるコマンドは、かなりレアです。そのため、`LiteCommand&lt;Unit>` と書くことが多く、その場合、ハンドラは（ラムダで書くときはよいけれど、メンバ関数などを渡すときは）、
```kotlin
fun action(@Suppress("UNUSED_PARAMETER")u:Unit) {
    ...
}
``` 

のように書かないといけなくなって面倒でしかたないので、引数なしのコマンドを、LiteUnitCommand として分離しました。引数の有無の違いだけで、考え方は同じなので、以下の説明では、まとめて、LiteCommand と表記しています。また、後述の ReliableCommand も同様に、引数なし版は、ReliableUnitCommand です。

## LiteCommand と ReliableCommand

先の例では、LiteCommand (LiteUnitCommand) を使用しました。
すべての状態変更がビューモデル内で閉じているので、アクティビティの回転や、アプリがバックグラウンドに隠れて戻ってくるようなケースでも正しく動作します。

ここで、ログインが成功したらメッセージボックスを表示するケースを考えてみましょう。
さっそく、MainActivity に、メッセージ表示用メソッドを追加します。
```kotlin
    private fun onAuthenticated() {
        AlertDialog.Builder(this) // FragmentではActivityを取得して生成
            .setTitle("サンプル")
            .setMessage("ログインしました")
            .setPositiveButton("OK", { dialog, which ->
            })
            .show()
    }
```

では、authenticated を監視して、値がtrueになったらメッセージボックスを表示してみます。
尚、`disposableObserve()`は、Flow に生やした拡張メソッドで、値監視を開始し、監視終了用の `IDisposable` ([android-utilities](https://github.com/toyota-m2k/android-utilities)で定義した.NET/Rx風のi/f) を返します。これを bindier に add() しておくことで、Activityが destroy されるときに、監視を自動的に終了します。
```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        binder
            .owner(this)
                ...（略）
            .add(viewModel.authenticated.disposableObserve{
                if(it) {
                    onAuthenticated()
                }
            })
    }
```

すでにお気づきかもしれませんが、このコードは期待通りには動きません。いったん認証が成功すると、viewModel.authenticated.value == true になっているので、この状態でデバイスを回転すると、onCreate()で、監視を開始するたびに、onAuthenticated が呼ばれてダイアログが表示されてしまいます。


作戦を変更して、`commandOnAuthenticated:LiteUnitCommand`を介して、ハンドラ(onAuthenticated)を呼び出すことにしましょう。AuthenticationViewModel を次のように変更します。
```kotlin
class AuthenticationViewModel : ViewModel() {
        （略）
    val commandOnAuthenticated = LiteUnitCommand()  // 追加
    val loginCommand = LiteUnitCommand {
        // 認証中はビジーフラグを立てる
        isBusy.value = true
        // サブスレッドで認証を実行
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (MyAuthenticator.tryLogin(userName.value, password.value)) {
                    // 認証成功
                    commandOnAuthenticated.invoke()
                    authenticated.value = true
                }
            } finally {
                isBusy.value = false
            }
        }
    }
    val logoutCommand = LiteUnitCommand()
    val authenticated: MutableStateFlow<Boolean> = MutableStateFlow(false) 
}
```

さらに、onCreate()で、commandOnAuthenticatedに、ハンドラをバインドしておきます。
```kotlin
        binder
            .owner(this)
                ...（略）
            .add(viewModel.commandOnAuthenticated.bind(this@MainActivity::onAuthenticated))
```

だいたい期待通りに動くようになりましたが、まだうまく動かない場合があります。
問題は、ビューモデルから、`commandOnAuthenticated.invoke()` が非同期に（サブスレッドから）呼び出されることです。すでに説明した通り、android-binding ライブラリが提供する Binder をはじめとするバインディングの仕掛けは、Activityのライフサイクルに従って動作します。デバイスを回転したとき、Activity#onDestroy が呼ばれ、バインド先のビューがはきされ情報はすべて破棄され、次に onCreate()が呼ばれて、再びバインド処理が実行されるまでの間は、コマンドハンドラが登録されていない状態となります。つまり、onDestroy()から、次の onCreate() までの間に認証が終わってしまうと、commandOnAuthenticated.invoke()は空振りし、その後、onAuthenticated() が呼ばれるチャンスはありません。

このように、サブスレッドで処理を行った後、ActivityやViewを操作するコマンドを実装する場合は、ReliableCommand を使用してください。　
このプログラムは、AuthenticationViewModel を次のように変更するだけで正しく動作します。

```kotlin
class AuthenticationViewModel : ViewModel() {
        （略）
    val commandOnAuthenticated = ReliableUnitCommand()  // LiteUnitCommand()から変更
```

ちなみに、ReleableCommand は、トリガーされた状態をハンドラに渡すまで、内部(MutableLiveData)に保持していて、ハンドラをコールしたら、状態をリセットすることで、複数回の呼び出しを回避しています。つまり、flowを監視する方法とコマンドハンドラを使う方法の「いいとこどり」です。ただし、LiteCommand に比べて、オーバーヘッドが大きいので、利用は必要最小限にとどめましょう。また、LiteCommand は、bind() メソッドで、ハンドラをいくつでも登録できるのに対し、ReliableCommand は、ハンドラを１つしか bind() できません（複数回 bind() を実行すると IllegalStateException をスローします）。とはいえ、１つのコマンドに複数のハンドラを登録しなければならない状況はほとんどないので、実用上は問題にならないと思います。

## バインディングモード

XAMLのバインディングと同じように（というかマネしたので）、android-binding にも、バインディングモードがあります。

### BindingMode.OneWay

ViewModel（`Flow` や `LiveDate`）から、ビューのプロパティへの単方向バインディング。<br>
visibilityBinding, enableBinding, textBinding, progressBarBinding などは、OneWayモードだけをサポートします。

### BindingMode.TwoWay

ViewModel（`MutableStateFlow` や `MutableLiveData`) と、ビューのプロパティとの双方向バインディング。
editTextBinding, checkBinding, seekBarBinding などは、TwoWayモードをサポートします。TwoWayモードをサポートするバインディングは、OneWay, OneWayToSourceモードもサポートしており、bindingMode引数で動作を指定できます。

### BindingMode.OneWayToSource

ビューのプロパティから、ViewModel（`MutableStateFlow` や `MutableLiveData`) への単方向バインディング。<br>
XAMLのバインディングモードにあったからマネして作ったけれど、TwoWay で代用できるので、ほとんど（まったく？）使ったことがないかも。

## Binder拡張関数とバインディングクラス

visibilityBinding(), textBinding() などは、Binderクラスの拡張関数です。これらは、それぞれ、VisibilityBinding, TextBinding などの`バインディングクラス` を生成してBinderに登録します。
通常、これらのバインディングクラスを直接作成して扱う必要はありませんが、より柔軟で細かい制御を行いたい場合は、それぞれのクラスを作成して利用することも可能です。

### Boolean型バインディング

| 拡張関数                   | バインディングクラス       | ビュー                                                 | プロパティ                              | バインディングモード  |
|------------------------|------------------------|-------------------------------------------------------|------------------------------------|-------------|
| checkBinding           | CheckBinding           | CompoundButtons (CheckBox, Switch, ToggleButton, ...) | isChecked                          | TwoWay      |
| enableBinding          | EnableBinding          | View                                                  | isEnabled                          | OneWay      |
| multiEnableBinding     | MultiEnableBinding     | View(s)                                               | isEnabled                          | OneWay      |
| visibilityBinding      | VisibilityBinding      | View                                                  | visibility                         | OneWay      |
| multiVisibilityBinding | MultiVisibilityBinding | View(s)                                               | visibility                         | OneWay      |
| genericBoolBinding     |GenericBoolBinding|View| (any)                              |OneWay|
| fadeInOutBinding       | FadeInOutBinding       | View                                                  | visibility with fade in/out effect | OneWay      |
| multiFadeInOutBinding|MultiFadeInOutBinding   | View(s)   | visibility with fade in/out effect | OneWay|
| animationBinding       | AnimationBinding       | View                                                  | reversible animation effect        | OneWay      |

- 各Boolean型バインディングは、true/false を反転するかどうかを、`BoolConvert` 型の引数で指定できます。
- enableBinding, multiEnableBinding は、Float型の `alphaOnDisabled` 引数で、disable時の透過度を指定することができます。アイコンボタンなど、isEnabled = false にしても、自動的に無効表示にならないViewの場合に、簡易的な無効表示を提供すます。
- visibilityBinding, multiVisibilityBinding, fadeInOutBinding, multiFadeInOutBinding は、HiddenMode 引数により、hidden時のVisibility (View.GONE/View.INVISIBLE) を指定できます。

    
### Text型 バインディング

| 拡張関数      | バインディングクラス   | ビュー   | プロパティ | データ型 | バインディングモード  |
|--------|---------|---------|------------|-----|------|
|textBinding|TextBinding|View|text|String|OneWay|
|intBinding|IntBinding|View|text|Int|OneWay|
|longBinding|LongBinding|View|text|Long|OneWay|
|floatBinding|FloatBinding|View|text|Float|OneWay|
|editTextBinding|EditTextBinding|EditText|text|String|TwoWay|
|editIntBinding|EditIntBinding|EditText|text|Int|TwoWay|
|editLongBinding|EditLongBinding|EditText|text|Long|TwoWay|
|editFloatBinding|EditFloatBinding|EditText|text|Float|TwoWay|

- textBinding は、ViewModelの Flow&lt;String>型データを、View（ButtonやTextViewなど）の text プロパティに単方向バインドします。
- int/long/floatBinding は、それぞれ、Flow&lt;Int>/Flow&lt;Long>/Flow&lt;Float>型データを、String型に変換して、Viewの text プロパティに単方向バインドします。
- editTextBinding は、ViewModelの MutableStateFlow&lt;String>型データを、EditText の text プロパティと双方向にバインドします。
- editInt/Long/Float/Binding は、対応する数値型 MutableStateFlow との間で双方向バインドします。
- デフォルトの実装は、String-数値型の変換に、toString/toIntなどの単純な関数を利用しています。map() や、ConvertLiveData で、FlowやLiveDataを加工して TextBinding または、EditTextBinding に渡すことにより、より高度な文字列の整形が可能になります。


### ProgressBar/SeekBar/Slider のバインディング

| 拡張関数      | バインディングクラス   | ビュー   | プロパティ | データ型 | バインディングモード  |
|--------|---------|---------|------------|-----|------|
|progressBarBinding|ProgressBarBinding|ProgressBar|progress|Int|OneWay|
||||min|Int|OneWay|
||||max|Int|OneWay|
|seekBarBinding|SeekBarBinding|SeekBar|value|Int|TwoWay|
||||min|Int|OneWay|
||||max|Int|OneWay|
|sliderBinding|SliderBinding|Slider (Material Components)|value|Float|TwoWay|
||||valueFrom|Float|OneWay|
||||valueTo|Float|OneWay|

- `progressBarBinding` は、ViewModelのFlow&lt;Int>型データを、`ProgressBar` の `progress` プロパティに単方向バインドします。必要に応じて、min/max プロパティに対する単方向バインドも設定できます。
- `seekBarBinding` は、ViewModelのMutableStateFlow&lt;Int>型データを、`SeekBar`の `value` プロパティと双方向バインドします。必要に応じて、`min`/`max` プロパティに対する単方向バインドも設定できます。
- `sliderBinding` は、ViewModelのMutableStateFlow&lt;Float>型データを、Material Component の `Slider` の `value` プロパティと双方向バインドします。必要に応じて、valueFrom/valueTo プロパティに対する単方向バインドも設定できます。

### ラジオボタンのバインディング


| 拡張関数      | バインディングクラス   | ビュー   | データ型 | バインディングモード  |
|---|---|---|---|---|
|radioGroupBinding|RadioGroupBinding|RadioGroup|T:Any (IIDValueResolver&lt;T>を使用)|TwoWay|
|materialRadioButtonGroupBinding|MaterialRadioButtonGroupBinding|MaterialButtonToggleGroup (isSingleSelection=true, isSelectionRequired=true)|T:Any (IIDValueResolver&lt;T>を使用)|TwoWay|
|materialRadioUnSelectableButtonGroupBinding|MaterialRadioButtonUnSelectableGroupBinding|MaterialButtonToggleGroup (isSingleSelection=true, isSelectionRequired=true)|T:Any (IIDValueResolver&lt;T>を使用)|TwoWay|

- `radioGroupBinding`と`materialRadioButtonGroupBinding`は、それぞれ`RadioGroup` の `checkedRadioButtonId` `プロパティ、MaterialButtonToggleGroup` の `checkedButtonId` プロパティとViewModelが持つ任意のMutableStateFlow&ltT>（通常 T はenum 値）を双方向バインドします。
- `materialRadioUnSelectableButtonGroupBinding`は、「選択なし」という状態を許容する以外は、materialRadioButtonGroupBinding と同じです。
- ボタンのID(android:id) と &lt;T>型を相互変換するために、`IIDValueResolver` を実装する必要があります。
```kotlin
interface IIDValueResolver<T> {
    fun id2value(@IdRes id:Int) : T?
    fun value2id(v:T): Int
}
```
例えば次のような enum class を定義して利用します。
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
class ViewModel {
    val rgb = MutableStateFlow<RGB?>(null)
}

...
binder.materialRadioUnSelectableButtonGroupBinding(buttonToggleGroup, viewModel.rgb, RGB.IDResolver)
```

### 複数選択可能なトグルボタングループ（MaterialButtonToggleGroup）のバインディング


| 拡張関数      | バインディングクラス   | ビュー   | データ型 | バインディングモード  |
|---|---|---|---|---|
|materialToggleButtonGroupBinding|MaterialToggleButtonGroupBinding|MaterialButtonToggleGroup|checkedButtonIds|List&lt;T:Any> (using IIDValueResolver&lt;t>)||TwoWay|
|materialToggleButtonsBinding|MaterialToggleButtonsBinding|MaterialButtonToggleGroup|checkedButtonIds|Boolean(s)|TwoWay|

- `materialToggleButtonGroupBinding` は、`MaterialButtonToggleGroup` の `checkedButtonIds` プロパティを `IIDValueResolver` によって変換された List&lt;T>を、  ViewModel の `MutableStateFlow&lt;List&lt;T>>` にバインドします。
- `materialToggleButtonsBinding` は、個々のボタン（MaterialButtonToggleGroupの子要素）のチェック状態（MaterialButtonToggleGroupのcheckedButtonIdsから取得）を、ViewModel の MutableStateFlow&lt;Boolean> を１つずつ双方向にバインドします。


```kotlin
class ViewModel {
    val red = MutableStateFlow<Boolean>(false)
    val green = MutableStateFlow<Boolean>(false)
    val blue = MutableStateFlow<Boolean>(false)
}

binder.materialToggleButtonsBinding(group, BindingMode.TwoWay) {
    bind(controls.redButton, viewModel.red)
    bind(controls.greenButton, viewModel.green)
    bind(controls.blueButton, viewModel.blue)
}

```

### RecyclerView のバインディング

| 拡張関数      | バインディングクラス   | ビュー   | データ型 | バインディングモード  |
|---|---|---|---|---|
|recyclerViewBinding|RecyclerViewBinding|RecyclerView|ObservableList|TwoWay|
|recyclerViewGestureBinding|RecyclerViewBinding|RecyclerView|ObservableList|TwoWay|


- `RecyclerView` と、その要素リストをバインドするため、要素の変更を監視可能な、MutableList派生クラス `ObservableList`を使用します。
- recyclerViewBindingは、RecyclerView と ViewModel の ObservableList とを双方向にバインドします。D&Dによるリストの並べ替えもサポートしており、その有効・無効も MutableStateFlow&lt;Boolean>型の変数にバインドして、動的に切り替えることも可能です。
- recyclerViewGestureBindingは、リスト要素の右スワイプジェスチャーによる要素削除をサポートします。こちらもジェスチャーの有効・無効を MutableStateFlow&lt;Boolean>型の変数にバインドして、動的に切り替えることも可能です。
- 個々の要素と、それを表示するためのItem View は、引数 `bindView:(Binder, View, T)->Unit)` で接続しますが、その第一引数で RecyclerView の Item View のライフサイクルに合わせて動作するBinderインスタンスが渡されるので、 bindView内で、Item View に対するバインディングを定義できます。

```kotlin
class ViewModel {
     val videoSources = ObservableList<MediaSource>()
     val currentSource = MutableStateFlow<MediaSource?>(null)
}

...
val binder = Binder()
fun onCreate(savedInstanceState:Bundle?) {
    binder
    .owner(this)
    .recyclerViewBinding(model.videoSources, R.layout.list_item_video) { itemBinder, view, item ->
        itemBinder
        // タイトル
        .textBinding(view.findViewById(R.id_textview_title), ConstantLiveData(item.title))
        // 再生中のアイテムのチェックを on
        .checkBinding(view.findViewById(R.id_checkbox_current_item), viewModel.currentSource.map { it == item })
        // アイテムタップで再生開始（playCommandにviewをバインド）
        .clickBinding(view) { play(item) }
    }
}
```

### その他のバインディング

- GenericBinding<br>
    任意のViewインスタンスと、任意のビューモデル（LiveData / Flow）を、action関数を介してバインドします。例えば、enum値によって背景色を変える、など、特殊なバインドを実現するために使用します。
- GenericBoolBinding<br>
    Boolean型に特化した、GenericBindingです。BoolConvert型引数によって、bool値の反転をサポートします。
- HeadlessBinding<br>
    ビューを介さず、ビューモデルを action に直接バインドします。内部的には、[DisposableObserve](https://github.com/toyota-m2k/android-utilities/blob/main/libUtils/src/main/java/io/github/toyota32k/utils/DisposableObserver.kt) そのものですが、他のバインディングと同じ流儀にそろえることができます。
- AlphaBinding<br>
    ビューの透過度(alphaプロパティ)にビューモデル(Flow&lt;Float>)にバインドします。
- AnimationBinding / FadeInOutBinding / MultiFadeInOutBinding<br>
    `AnimationBinding` は、ViewModel の Flow&lt;Boolean>型プロパティを、ビューのアニメーションにバインドします。ただし、このバインディングクラスは、`SequentialAnimation`や`ParallelAnimation`（ともに`IReversibleAnimation` 実装クラス）などを使った、複雑なアニメーションを実現するために用意しました。bool 値による単純なビューの FadeIn/Out には、`FadeInOutBinding` （複数のビューを同時にFadeIn/Outするなら `MultiFadeInOutBinding`）が便利です。
- ReadOnlyBinding<br>
    EditText をリードオンリーとするかどうかを、ビューモデル（Flow<Boolean>）にバインドします。余談ですが、Android の EditText は、他のOS (WinやiOS)のそれと違って、isReadOnly プロパティ的なやつが存在しないことに驚きました。
- ActivityBinding<br>
    Activity の各種フラグをビューモデルにバインドします。内部的には、`HeadlessBinding` を使って機能を実現しています。
    - activityStatusBarBinding<br>
    `StatusBar` 表示状態を、ビューモデル(Flow&lt;Boolean>)にバインドします。
    - activityActionBarBinding<br>
    `ActionBar` の表示状態を、ビューモデル(Flow&lt;Boolean>)にバインドします。
    - activityOrientationBinding<br>
    Activityの Orientation を、ビューモデル(Flow&lt;[ActivityOrientation](https://github.com/toyota-m2k/android-utilities/blob/cf408fb4aee6e45763f6970ddccdb071b781125b/libUtils/src/main/java/io/github/toyota32k/utils/ActivityExt.kt#L49)>)にバインドします。
    - activityOptionsBinding<br>
    上記３つ（StatusBar, ActionBar の表示/非表示、Orientation）をまとめてバインドします。
    バインドするデータ型は、[ActivityOptions](https://github.com/toyota-m2k/android-utilities/blob/cf408fb4aee6e45763f6970ddccdb071b781125b/libUtils/src/main/java/io/github/toyota32k/utils/ActivityExt.kt#L59) です。

    
