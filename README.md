# Android Binding


## About This Library
This library facilitates View-ViewModel binding for Android applications.

## Motivation
With the introduction of `androidx.lifecycle.ViewModel`, developing Android applications using the MVVM architecture has become common practice. However, the `Databinding` provided by Android Jetpack is extremely inadequate. It lacks the ability to bind certain properties, fails to reflect changes in XML, and suddenly encounters inexplicable kapt errors, leading to numerous issues. After several weeks of struggle, I decided to create my own library.

- Declarative binding description
- Operates in accordance with the lifecycle of Activities/Fragments
- Applicable to custom views and dialogs
- Reduces implementation and maintenance costs with concise descriptions

By the way, ViewBinding (which generates definitions from XML) is very stable and easy to use (unlike DataBinding), so I use it in samples, although it is not mandatory.

## Gradle

Define a reference to the Maven repository `https://jitpack.io` in `settings.gradle.kts`.

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

Add dependencies in the module's `build.gradle`. 
Since some classes inherit/implement classes/interfaces from `android-utilities`, please add `android-utilities` to the dependencies if needed. 

```kotlin
dependencies { 
    implementation("com.github.toyota-m2k:android-utilities:Tag") 
    implementation("com.github.toyota-m2k:android-binding:Tag") 
}
```

## Basic Structure of the Library

This library mainly uses three types of classes to describe bindings.

### Binding Classes

These classes implement the `IBinding` interface, which maintains the association (binding) between the properties of a View and the observable properties (LiveData/Flow) of a ViewModel.

For example, `EnableBinding` monitors changes in a ViewModel's Flow&lt;Boolean> (or LiveData&lt;Boolean>) property and reflects those changes in the View's isEnabled property (one-way binding). `EditTextBinding` monitors changes in a ViewModel's MutableStateFlow&lt;String> (or MutableLiveData&lt;Boolean>) property, reflects those changes in the EditText's text property, and also monitors edit operations in the EditText to write the editing results back to the ViewModel's property (two-way binding).

Various binding classes are provided according to the types and behaviors of the Views and properties to be bound. For details, please refer to the [Reference](Reference-ja.md). Additionally, by inheriting the `BaseBinding` abstract class, it is possible to implement new binding classes for custom views and properties.

Moreover, each binding class is linked with the lifecycle of a `LifecycleOwner` (such as an `Activity`). When the Activity is destroyed, the bindings are automatically released. This prevents observer leaks and unexpected errors from operating on destroyed Activities or Views.

In previous versions of android-binder (v1.x), binding classes were instantiated by calling the static method `create()` of each binding class. However, from v2.x onwards, it is recommended to describe bindings using the `Binder` class and its extension functions. Note that, with a few exceptions, the extension functions are defined with names that are the lowercase versions of the binding class names.


### Command Classes

Command classes (`ICommand` interface) are an event system that registers (binds) action handlers (functions) and can be invoked at any time. When a View is attached to this command class, the invoke() method is called in response to its click (tap) event. However, when EditText is attached, the Return key press event triggers the invoke() method.

Instances of command classes can be stored anywhere. Typically, they are collectively stored as fields in the ViewModel. Action handlers can be implemented within the ViewModel and bound at the time of instantiating the command class, or if they involve other UI operations, they can be implemented on the Activity side and bound together with the View in onCreate. In other words, by using command classes, you can achieve both the flexibility of choosing the implementation location for action handlers and the readability of declaratively binding them in the ViewModel or onCreate.

It is recommended to use the `bindCommand()` extension function of the Binder for binding instances of command classes with Views (and/or Actions).

### Binder Class

The `Binder` class is a utility class provided by this library to describe bindings more concisely.

`Binder` is a collection of `IDisposable` objects and also implements `IDisposable` itself. Calling its `dispose()` method will dispose of all the `IDisposable` objects it holds. Furthermore, the Binder monitors the lifecycle of the LifecycleOwner set by the `owner()` method, and when it is destroyed,`dispose()` is automatically executed. This eliminates the need for explicit dispose() calls in onDestroy().

Using `Binder` has the following benefits:

- No need to secure member variables in the Activity to hold instances of binding classes or disposables for unbinding.
- Automates binding-related cleanup processes through onDispose().
- Allows more concise binding descriptions by using extension functions such as clickBinding() and bindCommand().

## Tutorial

Here, for the sake of explanation, we will create a simple login screen. (The actual behavior can be confirmed in the sample program [MainActivity](https://github.com/toyota-m2k/android-binding/blob/main/app/src/main/java/io/github/toyota32k/binder/MainActivity.kt).)

### Creating the ViewModel

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
        // Set the busy flag during authentication
        isBusy.value = true
        // Perform authentication in a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (MyAuthenticator.tryLogin(userName.value, password.value)) {
                    // Authentication successful
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

In this `AuthenticationViewModel`, the authentication information such as the username (`userName`), password string (`password`), busy flag (`isBusy`), and the flag to show or hide the password (`showPassword`) are maintained as `MutableStateFlow` objects. The `isReady` flow is generated by combining `userName`, `password`, and `isBusy`. Commands such as `loginCommand` and `logoutCommand` are prepared.

The `loginCommand` is implemented within the ViewModel and initialized in the constructor of `LiteUnitCommand`, so it can be executed simply by binding to events like the click event of a login button. On the other hand, the logout button has no defined behavior yet. This will be defined later when binding the view.

In this example, to simplify, upon successful authentication, `authenticated` is set to `true`, which toggles the view within the same Activity.

### Creating the Layout (layout.xml)

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

The `auth_panel`, which is displayed in an unauthenticated state, includes input fields for username and password, a checkbox for "Show Password," and a login button. The `main_panel`, displayed in an authenticated state, contains the text "Authenticated" and a logout button. There are no special rules other than assigning IDs to the controls that will be bound to the ViewModel.

### Describing the Binding

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
                pwd.inputType = 
                    if (show) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
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

Let's explain each part step-by-step. (In this example, ViewBinding is used, but its explanation is omitted.)

```kotlin
    private val viewModel by viewModels<AuthenticationViewModel>()
    private val binder = Binder()
```

Prepare instances of `ViewModel` and `Binder` as member variables of the Activity. The `Binder` operates according to the Activity's lifecycle, so it is fine to initialize it in the constructor.

```kotlin
binder
    .owner(this)
```

Set the LifecycleOwner (this@MainActivity) in the Binder. This ensures that the Binder operates according to the lifecycle of MainActivity. That means when the Activity is destroyed, all bindings are automatically released, preventing resource leaks.

```kotlin
    .editTextBinding(controls.userName, viewModel.userName)
    .editTextBinding(controls.password, viewModel.password)
    .checkBinding(controls.showPassword, viewModel.showPassword)
```

Using the `editTextBinding()` and `checkBinding()` extension functions, bind the text properties of EditText with the string properties (MutableStateFlow&lt;String>) in the ViewModel, and the isChecked property of CheckBox (CompoundButton) with the showPassword property (MutableStateFlow&lt;Boolean>) in the ViewModel, both bidirectionally.

```kotlin
.enableBinding(controls.loginButton, viewModel.isReady)
.multiEnableBinding(arrayOf(controls.userName, controls.password, controls.showPassword), viewModel.isBusy, BoolConvert.Inverse)
```

First, use `enableBinding()` to bind the isReady property to the enabled state (isEnabled) of the login button. Next, use `multiEnableBinding()` to bind the isBusy property to the enabled state of multiple views (username and password EditTexts and the show password CheckBox).

```kotlin
.bindCommand(viewModel.loginCommand, controls.loginButton)
.bindCommand(viewModel.logoutCommand, controls.logoutButton, this@MainActivity::onLogout)
```

Next, bind buttons and actions via the command class instances prepared in the ViewModel. The `loginCommand` is already defined within the ViewModel, so we only need to bind it to the button. The `logoutCommand`, however, doesn't have a defined handler in the ViewModel (for explanation purposes), so we pass the handler (a method in MainActivity) as the third argument to `bindCommand`.

This allows flexible binding of views and actions (handlers) through the command class (`ICommand`/`IUnitCommand`). Also, by consolidating all interactive processing into `Binder#bindCommand`, maintenance becomes easier.

```kotlin
.genericBinding(controls.password, viewModel.showPassword) { pwd, show ->
    pwd.inputType = 
        if (show) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
}
```

We want to update the `inputType` property of EditText according to the on/off state of the "Show Password" CheckBox, but there's no dedicated class to bind inputType to a Boolean value. For such special bindings, we use `genericBinding()`. It allows us to receive changes in the bound Flow as a callback and freely add binding processing.

```kotlin
.visibilityBinding(controls.authPanel, viewModel.authenticated, BoolConvert.Inverse, VisibilityBinding.HiddenMode.HideByGone)
.visibilityBinding(controls.mainPanel, viewModel.authenticated, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByGone)
```

Finally, we toggle the visibility of `auth_panel` and `main_panel` based on the authenticated property. Methods like `visibilityBinding()`, `enableBinding()`, and `checkBinding()` that bind Boolean state properties can specify a BoolConvert argument (default is Straight) to determine whether to invert the Boolean value. In this case, since it is BindingMode.OneWay, it is equivalent to writing:

```kotlin
.visibilityBinding(controls.mainPanel, viewModel.authenticated.map { !it }, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByGone)
```

However, if using checkBinding, specifying `BoolConvert.Inverse` allows two-way linking while inverting the Boolean value.

## Samples

- [MainActivity](https://github.com/toyota-m2k/android-binding/blob/main/app/src/main/java/io/github/toyota32k/binder/MainActivity.kt)  
This is the implementation of the authentication screen used in the explanations above.

- [CatalogActivity](https://github.com/toyota-m2k/android-binding/blob/main/app/src/main/java/io/github/toyota32k/binder/CatalogActivity.kt)  
This shows examples of using the main binding classes.

## Detailed Information

[Reference](Reference.md)