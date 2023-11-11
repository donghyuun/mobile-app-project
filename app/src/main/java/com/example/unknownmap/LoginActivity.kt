package com.example.unknownmap

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.unknownmap.databinding.ActivityLoginBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.activity_login)

        //로그인 관리
        //로그인 성공 시 해당 계정의 DB객체안에 생성된 토큰값을 넣는다.
        //사용자가 로그아웃할 때는 해당 토큰을 데이터베이스에서 삭제하여 사용자 세션을 종료할 수 있다.
        //----------------------카카오 로그인 버튼------------------------//
        binding.loginBtnKakao.setOnClickListener{
            kakaoLogin()
        }
    }

    fun kakaoLogin(){
        //카카오 계정으로 로그인 공통 callback 구성
        //카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨

        val callback: (OAuthToken?, Throwable?) -> Unit = {
                token, error ->
            if (error != null) {
                Log.e("LOGIN", "카카오계정으로 로그인 실패", error)
            } else if (token != null){
                Log.i("LOGIN", "카카오 계정으로 로그인 성공 ${token.accessToken}")
            }
        }

        //카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if(UserApiClient.instance.isKakaoTalkLoginAvailable(this)){
            UserApiClient.instance.loginWithKakaoAccount(this){
                    token, error ->
                if (error != null) {
                    Log.e("LOGIN", "카카오톡으로 로그인 실패", error)

                    //사용자가 카카오톡 설치 후 디바이스 권한 요청에서 로그인을 취소한 경우,
                    //의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리(예: 뒤로 가기)
                    if(error is ClientError && error.reason == ClientErrorCause.Cancelled){
                        return@loginWithKakaoAccount
                    }

                    //카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                }
                else if (token != null){
                    Log.i("LOGIN", "카카오톡으로 로그인 성공 ${token.accessToken}")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

                    //로그인 성공한 사용자의 정보 요청
                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            Log.e("LOGIN", "사용자 정보 요청 실패", error)
                        } else if (user != null) {
                            Log.i("LOGIN", "사용자 정보 요청 성공" +
                                    "\n회원번호: ${user.id}" +
                                    "\n이메일: ${user.kakaoAccount?.email}" +
                                    "\n닉네임: ${user.kakaoAccount?.profile?.nickname}")
                        }
                    }


                    finish()
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }
}