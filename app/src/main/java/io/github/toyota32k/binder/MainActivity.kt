package io.github.toyota32k.binder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.binder.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * binder の概要説明用
 *
 * See https://github.com/toyota-m2k/android-binding?tab=readme-ov-file#%E4%BD%BF%E3%81%84%E6%96%B9
 */
class MainActivity : AppCompatActivity() {
    object MyAuthenticator {
        suspend fun tryLogin(name:String,pwd:String) : Boolean {
            delay(2000)
            return true
        }
    }

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
                        // 認証成功 --> ダイアログを閉じる
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


    private val viewModel by viewModels<AuthenticationViewModel>()
    private lateinit var controls: ActivityMainBinding
    private val binder = Binder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)

        ViewCompat.setOnApplyWindowInsetsListener(controls.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binder
            .owner(this)
            .editTextBinding(controls.userName, viewModel.userName)
            .editTextBinding(controls.password, viewModel.password)
            .checkBinding(controls.showPassword, viewModel.showPassword)
            .enableBinding(controls.loginButton, viewModel.isReady)
            .multiEnableBinding(arrayOf(controls.userName, controls.password, controls.showPassword), viewModel.isBusy, BoolConvert.Inverse)
            .bindCommand(viewModel.loginCommand, controls.loginButton)
            .bindCommand(viewModel.logoutCommand, controls.logoutButton, this@MainActivity::onLogout)
            .clickBinding(controls.catalogButton, this@MainActivity::showCatalog)
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

    private fun showCatalog(@Suppress("UNUSED_PARAMETER") v: View) {
        val intent = Intent(this, CatalogActivity::class.java)
        startActivity(intent)
    }

}