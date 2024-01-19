package org.shop.sharelocation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import org.shop.sharelocation.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var emailLoginResult: ActivityResultLauncher<Intent>
    private lateinit var pendingUser: User

    private val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            // 로그인 실패
            showErrorToast()
            error.printStackTrace()
        } else if (token != null) {
            // 로그인 성공
            getKakaoAccountInfo()
            Log.e("LoginActivity Callback", "login in with kakao account token: $token")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        KakaoSdk.init(this, resources.getString(R.string.native_app_key))

        emailLoginResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // 결과값
                if (it.resultCode == RESULT_OK) {
                    val email = it.data?.getStringExtra("email")

                    if (email == null) {
                        showErrorToast()
                        return@registerForActivityResult
                    } else {
                        signInFirebase(pendingUser, email)
                    }
                }
            }

        binding.kakaoTalkLoginButton.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                // 카카오톡 로그인
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    if (error != null) {
                        // 카카오톡 로그인 실패
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    } else if (token != null) {
                        // 로그인 성공
                        Log.e("LoginActivity token", "token == $token")

                        if (Firebase.auth.currentUser == null) {
                            // 카카오톡에서 정보를 가져와서 Firebase 로그인
                            getKakaoAccountInfo()
                        } else {
                            navigateToMapActivity()
                        }
                    }
                }
            } else {
                // 카카오 계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
            }
        }
    }

    private fun showErrorToast() {
        Toast.makeText(this, "사용자 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun getKakaoAccountInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                // 에러 상황
                showErrorToast()
                error.printStackTrace()
                Log.e("LoginActivity getKakaoInfo", "fail: $error")
            } else if (user != null) {
                // 사용자 정보 요청 성공
                Log.e(
                    "LoginActivity getKakaoInfo",
                    "user: 회원번호 : ${user.id} / 이메일 : ${user.kakaoAccount?.email} / 닉네임 : ${user.kakaoAccount?.profile?.nickname} / 프로필 사진 : ${user.kakaoAccount?.profile?.thumbnailImageUrl}"
                )

                checkKakaoUserData(user)
            }
        }
    }

    private fun checkKakaoUserData(user: User) {
        val kakaoEmail = user.kakaoAccount?.email.orEmpty()
        if (kakaoEmail.isEmpty()) {
            // 추가로 이메일을 받는 작업
            pendingUser = user

            emailLoginResult.launch(Intent(this, EmailLoginActivity::class.java))
            return
        }

        signInFirebase(user, kakaoEmail)
    }

    private fun signInFirebase(user: User, email: String) {
        val uId = user.id.toString()

        Firebase.auth.createUserWithEmailAndPassword(email, uId).addOnCompleteListener {
            if (it.isSuccessful) {
                // 성공적으로 유저 create + 자동으로 로그인
                updateFirebaseDatabase(user)
            } else {
                showErrorToast()
            }
        }.addOnFailureListener {
            if (it is FirebaseAuthUserCollisionException) {
                // 이미 가입된 계정인 경우
                Firebase.auth.signInWithEmailAndPassword(email, uId)
                    .addOnCompleteListener { result ->
                        if (result.isSuccessful) {
                            // 다음 과정
                            updateFirebaseDatabase(user)
                        } else {
                            showErrorToast()
                        }
                    }.addOnFailureListener { error ->
                        error.printStackTrace()
                        showErrorToast()
                    }
            } else {
                showErrorToast()
            }
        }
    }

    private fun updateFirebaseDatabase(user: User) {
        val uid = Firebase.auth.currentUser?.uid.orEmpty()

        val personMap = mutableMapOf<String, Any>()
        personMap["uid"] = uid
        personMap["name"] = user.kakaoAccount?.profile?.nickname.orEmpty()
        personMap["profilePhoto"] = user.kakaoAccount?.profile?.thumbnailImageUrl.orEmpty()

        Firebase.database.reference.child("Person").child(uid).updateChildren(personMap)

        navigateToMapActivity()
    }

    private fun navigateToMapActivity() {
        startActivity(Intent(this, MapActivity::class.java))
    }
}