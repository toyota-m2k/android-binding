# Android Binding

## このライブラリについて

Androidアプリ用の、View-ViewModelバインディングを実現するライブラリです。

## モチベーション

`androidx.lifecycle.ViewModel` の登場により、Androidアプリの開発においても、MVVM構成によるアプリ開発が当たり前になりましたが、
`Android Jetpack`標準の `Databinding`があまりにも貧弱で、バインドできないプロパティはあるし、xmlの変更が反映されなかったり、突然、原因不明のkaptエラーに見舞われたり、悪いことばかり起きました。
数週間の格闘の末、見切りをつけて自作したのが、このライブラリです。

- バインディングを宣言的に記述
- Activity/Fragment のライフサイクルに合わせて動作
- カスタムビューやダイアログに適用可能
- 簡潔な記述で実装/メンテナンスコストを軽減

ちなみに、ViewBinding （xmlからビューの定義を生成する）の方は、(DataBindingと違って)とても安定していて使いやすいので、
Sampleなどで利用していますが、必須ではありません。

## インストール (Gradle)

settings.gradle.kts で、mavenリポジトリ https://jitpack.io への参照を定義。  

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}
```

モジュールの build.gradle で、dependencies を追加。
一部のクラスやメソッドが android-utilities のクラス/インターフェースを利用しているので、必要に応じて android-utilities も依存関係に追加してください。

```kotlin
dependencies {
    implementation("com.github.toyota-m2k:android-utilities:Tag")
    implementation("com.github.toyota-m2k:android-binding:Tag")
}
```

## ライブラリの基本構成

このライブラリでは、主に３種類のクラスを使ってバインディングを記述します。

### バインディングクラス

Viewのプロパティと ViewModelのObservableなプロパティ(LiveData/Flow) の関連付け（バインディング）を保持する `IBinding` i/f を実装したクラスです。

例えば、`EnableBinding` は ViewModel のFlow&lt;Boolean>（あるいは LiveData&lt;Boolean>）型のプロパティの変更を監視し、その変更を ViewのisEnabledプロパティに反映します（単方向バインディング）。`EditTextBinding`は、ViewModelの MutableStateFlow&lt;String>（あるいは MutableLiveData&lt;Boolean>）型のプロパティを監視し、その変更を EditTextの text プロパティに反映するとともに、EditTextの編集操作を監視し、編集結果を ViewModelのプロパティに書き戻します（双方向バインディング）。

このように、バインドするViewやプロパティの種類・動作に応じて種々のバインディングクラスを提供しています。詳細は [Reference](Reference-ja.md) をご参照ください。また、`BaseBinding`抽象クラスを継承することで、独自のビューやプロパティに対する新しいバインディングクラスを実装することも可能です。

尚、各バインディングクラスは、LifecycleOwner (Activityなど) のライフサイクルと連携しており、Activity が destroy されるとバインドは自動的に解除されます。これにより、オブザーバーのリークや、破棄されたActivityやViewを操作して予期しないエラーが発生したりすることがなくなります。

android-binder の以前のバージョン (v1.x) では、各バインディングクラスの静的メソッド `create()` を呼び出すことで、バインディングクラスをインスタンス化していましたが、v2.x 以降は、後述の `Binder` クラスと、その拡張関数を使ってバインディングを記述することを推奨しています。尚、拡張関数は、（一部の例外を除いて）バインディングクラス名の頭文字を小文字にした名前で定義されています。


### コマンドクラス

コマンドクラス（`ICommand` i/f）は、アクションハンドラ（関数）を登録(bind)して、任意のタイミングで発動(invoke)できるイベントシステムです。このコマンドクラスに View を接続(attach)すると、その クリック（タップ）イベントをトリガーに invoke() が呼ばれます。ただし、EditTextを接続した場合は、Returnキー押下イベントがトリガーとなります。

コマンドクラスインスタンスはどこに保持しても構いません。通常はまとめて ViewModelのフィールドとします。アクションハンドラは、ViewModel内に実装して、コマンドクラスのインスタンス化と同時にバインドしてもよいし、他のUI操作を伴うものは、Activity側に実装して、onCreateでViewとともにバインドしても構いません。つまり、コマンドクラスを使うことで、アクションハンドラの実装場所を選ばない柔軟性と、ViewModelやonCreateで宣言的にバインドできる可読性を両立できます。

コマンドクラスインスタンスと、View（and/or Action）のバインドには、Binderの拡張関数 `bindCommand()` を使うことを推奨します。 

### Binder クラス

`Binder` は、本ライブラリが提供するバインディングを、より簡潔に記述するためのユーティリティクラスです。

`Binder` は、`IDisposable` のコレクションであると同時に、それ自身も `IDisposable` を実装しており、その dispose() を呼び出すことで、コレクションに保持しているすべての IDisposable の dispose()を実行します。さらに、Binderは、owner()メソッドで設定された LifecycleOwner の lifecycle を監視し、それが destroyされたら、自動的にdispose()を実行します。これにより、onDestroy()での明示的な dispose()呼び出しが不要となります。

`Binder` を使うと次のような効果があります。

- バインディングクラスインスタンスや、バインド解除用 IDisposable を保持するメンバー変数を Activity確保する必要がなくなる。
- onDispose()によるBinding関連のクリーンアップ処理を自動化できる。
- clickBinding()や bindCommand()などの拡張関数が使え、より簡潔にバインディングが記述できる。

## チュートリアル

ここでは、説明のため簡単なログイン画面を作成してみます。
（実際の動作は、サンプルプログラムの [MainActivity](https://github.com/toyota-m2k/android-binding/blob/main/app/src/main/java/io/github/toyota32k/binder/MainActivity.kt) で確認いただけます。）

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
この AuthenticationViewModel では、認証情報としてユーザー名(`userName`)とパスワードの文字列(`password`)、認証中フラグ(`isBusy`)、およびパスワードを表示するかどうかのフラグ(`showPassword`) をMutableStateFlowとして保持しています。
`userName`, `password` および`isBusy` を combineして フロー `isReady` を生成しています。コマンドとして、`loginCommand`と`logoutCommand` を用意しました。
`loginCommand` は、その動作をビューモデル内に実装して、LiteUnitCommand のコンストラクタで初期化していますので、ログインボタンのクリックイベントなどをバインドするだけで実行できます。
一方、ログアウトボタンは、動作を定義していません。こちらは、後ほど、ビューをバインドするときに動作も定義します。
また、この例では、単純化のため、認証が成功すると `authenticated` にtrueをセットすることで、同じActivity内でビューを切り替えることとしました。

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
                pwd.inputType = if(show) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
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

以下、順番に説明します。
（この例では ViewBinding を使用していますが、その説明は割愛します。）

```kotlin
    private val viewModel by viewModels<AuthenticationViewModel>()
    private val binder = Binder()
```

Activityのメンバー変数として、`ViewModel`, `Binder` インスタンスを用意します。`Binder` は、Activity のライフサイクルにあわせて動作するので、普通にコンストラクタで初期化して大丈夫です。

```kotlin
binder
    .owner(this)
```
Binder に LifecycleOwner (this@MainActivity) が設定しています。これにより、以降、MainActivity のライフサイクルに従った動作を行います。すなわち、アクティビティがdestroyされたときに、すべてのバインディングが自動的に破棄されるので、リソースがリークする心配はありません。


```kotlin
    .editTextBinding(controls.userName, viewModel.userName)
    .editTextBinding(controls.password, viewModel.password)
    .checkBinding(controls.showPassword, viewModel.showPassword)
```

`editTextBinding()`, `checkBinding()` 拡張関数を使い、それぞれ、EditText の textプロパティとViewModelの文字列(MutableStateFlow&lt;String>)、CheckBox（CompoundButton）の isChecked プロパティと、ViewModel の showPassword(:MutableStateFlow&lt;Boolean>)を、双方向にバインドしています。

```kotlin
.enableBinding(controls.loginButton, viewModel.isReady)
.multiEnableBinding(arrayOf(controls.userName, controls.password, controls.showPassword), viewModel.isBusy, BoolConvert.Inverse)
```
ここでは まず、`enableBinding()`を使って、isReady をログインボタンボタンの有効、無効（isEnabledプロパティ）にバインドします。次に、isBusy は、`multiEnableBinding()` を使って、複数のビュー（ユーザー名、パスワード入力用のEditText, パスワードを表示チェックボックス）のisEnabledに対して、まとめてバインド設定しています。


```kotlin
.bindCommand(viewModel.loginCommand, controls.loginButton)
.bindCommand(viewModel.logoutCommand, controls.logoutButton, this@MainActivity::onLogout)
```
次は、ViewModelに用意したコマンドクラスインスタンスを介して、ボタンとアクションをバインドします。loginCommand はビューモデル内でハンドラまで定義しているので、ボタンをバインドするだけです。一方、logoutCommandの方は（説明のために）ビューモデル内でハンドラを定義していません。そこで、`bindCommand`の第３引数でハンドラ（この例ではMainActivityのメソッド）を渡しています。
このように、コマンドクラス(`ICommand`/`IUnitCommand`)を介して、ビューとアクション（ハンドラ）を柔軟にバインドすることができ、しかも、すべてのインタラクティブな処理を Binder#bindCommand に集約できるので、メンテナンスが楽になります。


```kotlin
.genericBinding(controls.password, viewModel.showPassword) { pwd, show ->
    pwd.inputType = 
        if(show==true) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
}
```
「パスワードを表示」チェックボックスの on/off に応じて、EditText の `inputType`プロパティ を更新したいのですが、
inputType と Boolean 値をバインドする専用のクラスは用意していません。
このような特殊なバインディングを実装したい場合は、`genericBinding()` を使います。
これにより、バインディングされたFlowの値の変化をコールバックとして受け取れるので、
自由にバインディング処理を追加できます。


```kotlin
.visibilityBinding(controls.authPanel, viewModel.authenticated, BoolConvert.Inverse, VisibilityBinding.HiddenMode.HideByGone)
.visibilityBinding(controls.mainPanel, viewModel.authenticated, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByGone)
```

最後に、authenticatedによって、auth_panelとmain_panel の表示をトグルしています。
visibilityBinding(), enableBinding(), checkBinding() など、Boolean型の状態プロパティをバインドするメソッドは、bool値を反転するかどうかを決める BoolConvert型の引数（デフォルトはStraight）指定できます。この場合は、BindingMode.OneWay なので、
```
.visibilityBinding(controls.mainPanel, viewModel.authenticated.map {!it}, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByGone)
```
と書くのと等価ですが、checkBinding を使う場合に、
 BoolConvert.Inverse` を指定すれば、bool値を反転しつつ双方向にリンクすることが可能です。


## サンプル

- [MainActivity](https://github.com/toyota-m2k/android-binding/blob/main/app/src/main/java/io/github/toyota32k/binder/MainActivity.kt)<br>
上の説明に使った認証画面の実装です。

- [CatalogActivity](https://github.com/toyota-m2k/android-binding/blob/main/app/src/main/java/io/github/toyota32k/binder/CatalogActivity.kt)<br>
主要なバインディングクラスの使用例を示しています。


## 詳細な情報

[Reference](Reference-ja.md)
