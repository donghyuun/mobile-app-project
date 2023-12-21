package com.example.unknownmap

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.example.unknownmap.databinding.ActivityLoginBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlin.reflect.typeOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.TokenManagerProvider
import com.kakao.sdk.common.model.KakaoSdkError
import kotlinx.coroutines.runBlocking

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.activity_login)

        //로그인 관리
        //로그인 성공 시 해당 계정의 DB객체안에 생성된 토큰값을 넣는다.
        //사용자가 로그아웃할 때는 해당 토큰을 데이터베이스에서 삭제하여 사용자 세션을 종료할 수 있다.

        //기존 토큰 존재 여부 확인 - 존재하면 자동 로그인
        if (AuthApiClient.instance.hasToken()) {
            UserApiClient.instance.accessTokenInfo { token, error ->
                if (error != null) {
                    if (error is KakaoSdkError && error.isInvalidTokenError() == true) {
                        //로그인 필요
                        kakaoLogin()
                    } else {
                        //기타 에러
                        Log.d("LOGIN", "Token Identity Error")
                        finish()
                    }
                } else {
                    //토큰 유효성 체크 성공(필요 시 토큰 갱신됨)
                    Log.d("LOGIN", "Token Identity Success")

                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            Log.e("LOGIN", "사용자 정보 요청 실패", error)
                        } else if (user != null) {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("userId", user.id)
                            intent.putExtra("userEmail", user.kakaoAccount?.email)
                            intent.putExtra("userNickname", user.kakaoAccount?.profile?.nickname)

                            // 비동기로 토큰을 받아오는 함수
                            val token = runBlocking {
                                TokenManagerProvider.instance.manager.getToken()
                            }
                            intent.putExtra("userToken", token?.accessToken)

                            Log.d(
                                "LOGIN", "In LoginActivity, " +
                                        "User ID: ${user.id}, " +
                                        "Email: ${user.kakaoAccount?.email}, " +
                                        "Nickname: ${user.kakaoAccount?.profile?.nickname}, " +
                                        "token: ${token?.accessToken}"
                            )
                            //----------------------로그인시, DB에 사용자 정보 없으면 로그인한 유저 정보 저장------------------------//
                            val userInfo = User()
                            userInfo.id = user.id.toString()
                            userInfo.email = user.kakaoAccount?.email.toString()
                            userInfo.token = token.toString()
                            userInfo.nickname = user.kakaoAccount?.profile?.nickname.toString()

                            var firestore: FirebaseFirestore? = null
                            firestore = FirebaseFirestore.getInstance()
                            val userDocRef = firestore?.collection("users")?.document(userInfo.id)

                            userDocRef?.get()?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result
                                    if (document != null && document.exists()) {
                                        // 문서가 이미 존재하므로 여기에 원하는 작업 수행
                                        Log.d(
                                            "DB",
                                            "user info already exists in DB, skipping addition"
                                        )
                                    } else {
                                        // 문서가 존재하지 않으므로 추가 작업 수행
                                        userDocRef.set(userInfo)
                                            .addOnSuccessListener {
                                                Log.d(
                                                    "DB",
                                                    "User info was successfully stored in DB"
                                                )
                                            }
                                            .addOnFailureListener { e ->
                                                Log.d(
                                                    "DB",
                                                    "Error, failed to store user info in DB",
                                                    e
                                                )
                                            }
                                    }
                                } else {
                                    Log.e("DB", "Error getting document", task.exception)
                                }
                            }
                            //----------------------------------------------------------------//
                            // MainActivity로 이동
                            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                            finish()
                        }
                    }
                }
            }
        } else {
            //로그인 필요
            Log.d("LOGIN", "Login Needed")
            binding.loginView.visibility = View.VISIBLE
        }

        //----------------------카카오 로그인 버튼------------------------//
        binding.loginBtnKakao.setOnClickListener {
            kakaoLogin()
        }
    }

    fun kakaoLogin() {
        //카카오 계정으로 로그인 공통 callback 구성
        //카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("LOGIN", "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.i("LOGIN", "카카오 계정으로 로그인 성공 ${token.accessToken}")
            }
        }

        //카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                if (error != null) {
                    Log.e("LOGIN", "카카오톡으로 로그인 실패", error)

                    //사용자가 카카오톡 설치 후 디바이스 권한 요청에서 로그인을 취소한 경우,
                    //의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리(예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoAccount
                    }

                    //카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    Log.i("LOGIN", "카카오톡으로 로그인 성공 ${token.accessToken}")
                    //로그인 성공한 사용자의 정보 요청

                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            Log.e("LOGIN", "사용자 정보 요청 실패", error)
                        } else if (user != null) {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("userId", user.id)
                            intent.putExtra("userEmail", user.kakaoAccount?.email)
                            intent.putExtra("userNickname", user.kakaoAccount?.profile?.nickname)
                            intent.putExtra("userToken", token.accessToken)
                            Log.d(
                                "LOGIN", "In LoginActivity, " +
                                        "User ID: ${user.id}, " +
                                        "Email: ${user.kakaoAccount?.email}, " +
                                        "Nickname: ${user.kakaoAccount?.profile?.nickname}, " +
                                        "token: ${token.accessToken}"
                            )
                            //----------------------로그인시, DB에 사용자 정보 없으면 로그인한 유저 정보 저장------------------------//
                            val userInfo = User()
                            userInfo.id = user.id.toString()
                            userInfo.email = user.kakaoAccount?.email.toString()
                            userInfo.token = token.accessToken.toString()
                            userInfo.nickname = user.kakaoAccount?.profile?.nickname.toString()

                            var firestore: FirebaseFirestore? = null
                            firestore = FirebaseFirestore.getInstance()
                            val userDocRef = firestore?.collection("users")?.document(userInfo.id)

                            userDocRef?.get()?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result
                                    if (document != null && document.exists()) {
                                        // 문서가 이미 존재하므로 여기에 원하는 작업 수행
                                        Log.d(
                                            "DB",
                                            "user info already exists in DB, skipping addition"
                                        )
                                    } else {
                                        // 문서가 존재하지 않으므로 추가 작업 수행
                                        userDocRef.set(userInfo)
                                            .addOnSuccessListener {
                                                Log.d(
                                                    "DB",
                                                    "User info was successfully stored in DB"
                                                )
                                            }
                                            .addOnFailureListener { e ->
                                                Log.d(
                                                    "DB",
                                                    "Error, failed to store user info in DB",
                                                    e
                                                )
                                            }
                                    }
                                } else {
                                    Log.e("DB", "Error getting document", task.exception)
                                }
                            }
                            //----------------------------------------------------------------//
                            // MainActivity로 이동
                            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                            finish()
                        }
                    }
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }
}