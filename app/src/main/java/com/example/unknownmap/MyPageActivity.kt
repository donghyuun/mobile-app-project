package com.example.unknownmap

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.unknownmap.databinding.ActivityMyPageBinding
import com.kakao.sdk.user.UserApiClient

class MyPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.myPageUserId.text = "아이디\n${MainActivity.staticUserId.toString()}"
        binding.myPageUserEmail.text = "이메일\n${MainActivity.staticUserEmail}"
        binding.myPageUserNickname.text = "닉네임\n${MainActivity.staticUserNickname}"
        binding.myPageUserToken.text = "토큰 정보\n${MainActivity.staticUserToken}"

        //닫기 버튼
        binding.closeButton.setOnClickListener {
            finish()
        }
        //로그아웃 버튼
        binding.logoutBtn.setOnClickListener{
            kakaoLogout()
        }
        //홈버튼
        binding.mainHomeBtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // KAKAO 로그인 코드 start
    fun kakaoLogout(){
        // 로그아웃 다이얼로그 생성
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("정말 로그아웃 하시겠습니까?")
            .setCancelable(false)
            .setPositiveButton("확인", DialogInterface.OnClickListener {
                    dialog, id -> confirmLogout()
            })
            .setNegativeButton("취소", DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
            })

        val alert = dialogBuilder.create()
        alert.setTitle("로그아웃")
        alert.show()
    }

    private fun confirmLogout(){
        //로그아웃
        UserApiClient.instance.logout { error ->
            if (error != null) {
                Log.e("LOGOUT", "로그아웃 실패. SDK에서 토큰 삭제됨", error)
            } else {
                Log.i("LOGOUT", "로그아웃 성공. SDK에서 토큰 삭제됨")
                val intent = Intent(this, LoginActivity::class.java)
                //스택에 남아있는 모든 액티비티를 제거하고, 해당 엑티비티를 시작함
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                //현재 액티비티를 종료
                finish()
            }
        }
    }
    fun kakaoUnlink() {
        //연결 끊기
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Log.e("LOGOUT", "연결 끊기 실패", error)
            } else {
                Log.i("LOGOUT", "연결 끊기 성공. SDK에서 토큰 삭제 됨")
            }
        }
        finish()
    }
    // 카카오 로그인 코드 end
}