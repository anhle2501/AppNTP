package vn.bvntp.app.viewmodel

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedStateRegistryOwner

import kotlinx.coroutines.launch
import vn.bvntp.app.MainActivity
import vn.bvntp.app.model.UserLoginInfo
import vn.bvntp.app.network.RetrofitClient
import vn.bvntp.app.repository.UserRepository
import vn.bvntp.app.ui.activity.LoginActivity


enum class LoginStatus{ Success, Failed, NotYetLogin}

class LoginViewModel(
    val repository: UserRepository,
): ViewModel() {



    val isLogin = MutableLiveData(LoginStatus.NotYetLogin)
    val loginMessage = MutableLiveData("")

    private val _userInfo =  MutableLiveData(UserLoginInfo("",""))
    val userInfo: LiveData<UserLoginInfo> = _userInfo
    
    fun <T> updateUserInfo(fieldName: String, newValue: T) {
        _userInfo.value =
            when (fieldName) {
                "password" -> _userInfo.value!!.copy(Password = newValue as String)
                "username" -> _userInfo.value!!.copy(UserName = newValue  as String)
                else -> throw IllegalArgumentException("Unreachable code") // Improved safety
            }

    }

    fun updateIsLogin(newState: LoginStatus){
        isLogin.value = newState
    }

    fun viewLogin(context: Context) {
        viewModelScope.launch {
            _userInfo.value?.let {
                repository.login(context=context,username = _userInfo.value!!.UserName , password = _userInfo.value!!.Password) { result ->

                    result.onSuccess { loginResponse ->
                        if (loginResponse.success) {
                            isLogin.value = LoginStatus.Success
                            loginMessage.value = "Đăng nhập thành công"
                        } else {
                            isLogin.value = LoginStatus.Failed
                            loginMessage.value = loginResponse.errorMessage
                        }

                    }
                    result.onFailure { error ->
                        // Xử lý khi gặp lỗi
                        isLogin.value = LoginStatus.Success
                        loginMessage.value = error.message

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                        }

                    }
                }
            }

        }
    }
}

