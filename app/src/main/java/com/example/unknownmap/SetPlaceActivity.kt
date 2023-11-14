package com.example.unknownmap

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.example.unknownmap.databinding.ActivityMainBinding
import com.example.unknownmap.databinding.ActivitySetPlaceBinding
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import java.net.URI
import java.util.Collections

//***********************장소 등록 버튼 눌렀을때 보이는 액티비티***********************//
class SetPlaceActivity : AppCompatActivity() {
    var isSet : Boolean = false

    // 사진 업로드를 위한 Activity에서 결과 가져오기
    private lateinit var resultLauncher : ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySetPlaceBinding.inflate(layoutInflater)

        //MainActivity 에서 전달된 위도, 경도값을 변수로 꺼냄
        val latitude = intent.getDoubleExtra("create_latitude", 0.0)
        val longitude = intent.getDoubleExtra("create_longitude", 0.0)

        // 지금 설정된 카테고리 번호, 유저가 카테고리 선택시 변경됨
        var currentSelectedNum = 0

        Log.d("user", "first got ${latitude}, ${longitude}")

        // 툴바 뒤로 가기
        binding.setPlaceBackBtn.setOnClickListener{
            finish() //현재 액티비티 종료, 스택의 직전에 쌓여있던 엑티비티로 이동
        }

        val btnTrashBin = binding.categoryTrashBin
        val btnVendingMachine = binding.categoryVendingMachine
        val btnFish = binding.categoryFish
        val btnClothesDonation = binding.categoryClothesDonation
        val btnPullUpBar = binding.categoryPullUpBar
        val btnCigar = binding.categoryCigar

        val btnList : List<ImageView> = listOf<ImageView>(btnTrashBin, btnVendingMachine, btnFish, btnClothesDonation, btnPullUpBar, btnCigar)

        // 카테고리 버튼 클릭 시, 색깔 활성화 및 현재 눌려진 버튼 확인
        binding.categoryTrashBin.setOnClickListener{ //쓰레기통 버튼 클릭 시
            btnTrashBin.setColorFilter(Color.parseColor("#00000000"))
            btnTrashBin.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 0
            for (i in btnList) {
                if (i != btnTrashBin){
                    i.setColorFilter(Color.parseColor("#af000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryVendingMachine.setOnClickListener{//자판기 버튼 클릭 시
            btnVendingMachine.setColorFilter(Color.parseColor("#00000000"))
            btnVendingMachine.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 1
            for (i in btnList) {
                if (i != btnVendingMachine) {
                    i.setColorFilter(Color.parseColor("#af000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryFish.setOnClickListener{//붕어빵 버튼 클릭 시
            btnFish.setColorFilter(Color.parseColor("#00000000"))
            btnFish.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 2
            btnFish.setColorFilter(Color.parseColor("#00000000"))
            for (i in btnList) {
                if (i != btnFish) {
                    i.setColorFilter(Color.parseColor("#af000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryClothesDonation.setOnClickListener{//헌옷수거함 버튼 클릭 시
            btnClothesDonation.setColorFilter(Color.parseColor("#00000000"))
            btnClothesDonation.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 3
            btnClothesDonation.setColorFilter(Color.parseColor("#00000000"))
            for (i in btnList) {
                if (i != btnClothesDonation) {
                    i.setColorFilter(Color.parseColor("#af000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryPullUpBar.setOnClickListener{//철봉 버튼 클릭 시
            btnPullUpBar.setColorFilter(Color.parseColor("#00000000"))
            btnPullUpBar.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 4
            btnPullUpBar.setColorFilter(Color.parseColor("#00000000"))
            for (i in btnList) {
                if (i != btnPullUpBar) {
                    i.setColorFilter(Color.parseColor("#af000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryCigar.setOnClickListener{//흡연장 버튼 클릭 시
            btnCigar.setColorFilter(Color.parseColor("#00000000"))
            btnCigar.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 5
            btnCigar.setColorFilter(Color.parseColor("#00000000"))
            for (i in btnList) {
                if (i != btnCigar) {
                    i.setColorFilter(Color.parseColor("#af000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        var uri : Uri? = null
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result ->
            // 이미지 받아와서 보여주기
            if (result.resultCode == RESULT_OK && result.data != null) {//정상적으로 이미지를 선택or촬영한 경우
                uri = result.data!!.data!!

                binding.setPlaceShowImage.setImageURI(uri)//이미지 뷰(장소 선택페이지의 아주 조그맣게 보이는 부분)에 이미지 출력
            }
        }
        // 이미지 등록 버튼 리스너
        binding.setPlaceSetImageBtn.setOnClickListener {
            // 이미지 추가를 위한 코드 시작
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val chooser = Intent.createChooser(galleryIntent, "이미지 등록하기")
            chooser.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                arrayOf(cameraIntent)
            )
            resultLauncher.launch(chooser)//이미지 선택or촬영하는 액티비티(chooser) 실행
        }

        // 등록 버튼 리스너 ***Marker 클래스에 넣을 값들을 intent로 MainActivity로 넘겨줌***
        binding.setPlaceSetBtn.setOnClickListener {
            val name = binding.placeName.text.toString()

            intent.putExtra("set_latitude", latitude)
            intent.putExtra("set_longitude", longitude)
            intent.putExtra("set_name", name)

            intent.putExtra("isSet", true)
            intent.putExtra("categoryNum", currentSelectedNum)
            intent.putExtra("image", uri.toString())
            // uri는 String으로 변환해서 intent로 넘기고, 받을 때 다시 parse 해야 함

            setResult(RESULT_OK, intent)

            Log.d("kim", "$latitude, $longitude, $name, $currentSelectedNum transferred to MainActivity")
            finish()//스택에 쌓인 직전 엑티비티(=MainActivity)로 이동
        }

        setContentView(binding.root)
    }

}