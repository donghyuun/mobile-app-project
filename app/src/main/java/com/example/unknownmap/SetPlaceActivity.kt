package com.example.unknownmap

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.example.unknownmap.databinding.ActivityMainBinding
import com.example.unknownmap.databinding.ActivitySetPlaceBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapReverseGeoCoder
import net.daum.mf.map.api.MapView
import okhttp3.Address
import java.net.URI
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Collections
import java.util.UUID

//***********************장소 등록 버튼 눌렀을때 보이는 액티비티***********************//
class SetPlaceActivity : AppCompatActivity() {
    var isSet: Boolean = false

    // firestore 설정
    var firestore: FirebaseFirestore? = null
    var storage: FirebaseStorage? = null

    // 사진 업로드를 위한 Activity에서 결과 가져오기
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySetPlaceBinding.inflate(layoutInflater)

        //MainActivity의 static 변수에 저장된 유저 정보를 출력해본다
        Log.d("user", "in SetPlaceActivity, ${MainActivity.staticUserId}")
        Log.d("user", "in SetPlaceActivity, ${MainActivity.staticUserEmail}")
        Log.d("user", "in SetPlaceActivity, ${MainActivity.staticUserNickname}")
        Log.d("user", "in SetPlaceActivity, ${MainActivity.staticUserToken}")

        //MainActivity 에서 전달된 위도, 경도값을 변수로 꺼냄
        val latitude = intent.getDoubleExtra("create_latitude", 0.0)
        val longitude = intent.getDoubleExtra("create_longitude", 0.0)

        // firestore 설정
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        fun setAddress(address: String) {
            binding.placeAddress.setText(address)
        }

        var addr = ""
        MapReverseGeoCoder(
            KeyStore.getNativeAppKey(), MapPoint.mapPointWithGeoCoord(latitude, longitude),
            object : MapReverseGeoCoder.ReverseGeoCodingResultListener {
                override fun onReverseGeoCoderFoundAddress(
                    p0: MapReverseGeoCoder?,
                    address: String
                ) {
                    addr = address
                    setAddress(addr)
                }

                override fun onReverseGeoCoderFailedToFindAddress(p0: MapReverseGeoCoder?) {
                    Toast.makeText(this@SetPlaceActivity, "주소를 찾을 수 없습니다.", Toast.LENGTH_LONG)
                        .show()
                }
            }, this
        ).startFindingAddress()

        // 지금 설정된 카테고리 번호, 유저가 카테고리 선택시 변경됨
        var currentSelectedNum = 0

        Log.d("user", "first got ${latitude}, ${longitude}")

        // 툴바 뒤로 가기
        binding.setPlaceBackBtn.setOnClickListener {
            finish() //현재 액티비티 종료, 스택의 직전에 쌓여있던 엑티비티로 이동
        }

        val btnTrashBin = binding.categoryTrashBin
        val btnVendingMachine = binding.categoryVendingMachine
        val btnFish = binding.categoryFish
        val btnClothesDonation = binding.categoryClothesDonation
        val btnPullUpBar = binding.categoryPullUpBar
        val btnCigar = binding.categoryCigar

        val btnList: List<ImageView> = listOf<ImageView>(
            btnTrashBin,
            btnVendingMachine,
            btnFish,
            btnClothesDonation,
            btnPullUpBar,
            btnCigar
        )

        // 카테고리 버튼 클릭 시, 색깔 활성화 및 현재 눌려진 버튼 확인
        binding.categoryTrashBin.setOnClickListener { //쓰레기통 버튼 클릭 시
            btnTrashBin.setColorFilter(Color.parseColor("#00000000"))
            btnTrashBin.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 0
            for (i in btnList) {
                if (i != btnTrashBin) {
                    i.setColorFilter(Color.parseColor("#8f000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryVendingMachine.setOnClickListener {//자판기 버튼 클릭 시
            btnVendingMachine.setColorFilter(Color.parseColor("#00000000"))
            btnVendingMachine.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 1
            for (i in btnList) {
                if (i != btnVendingMachine) {
                    i.setColorFilter(Color.parseColor("#8f000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryFish.setOnClickListener {//붕어빵 버튼 클릭 시
            btnFish.setColorFilter(Color.parseColor("#00000000"))
            btnFish.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 2
            btnFish.setColorFilter(Color.parseColor("#00000000"))
            for (i in btnList) {
                if (i != btnFish) {
                    i.setColorFilter(Color.parseColor("#8f000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryClothesDonation.setOnClickListener {//헌옷수거함 버튼 클릭 시
            btnClothesDonation.setColorFilter(Color.parseColor("#00000000"))
            btnClothesDonation.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 3
            btnClothesDonation.setColorFilter(Color.parseColor("#00000000"))
            for (i in btnList) {
                if (i != btnClothesDonation) {
                    i.setColorFilter(Color.parseColor("#8f000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryPullUpBar.setOnClickListener {//철봉 버튼 클릭 시
            btnPullUpBar.setColorFilter(Color.parseColor("#00000000"))
            btnPullUpBar.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 4
            btnPullUpBar.setColorFilter(Color.parseColor("#00000000"))
            for (i in btnList) {
                if (i != btnPullUpBar) {
                    i.setColorFilter(Color.parseColor("#8f000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        binding.categoryCigar.setOnClickListener {//흡연장 버튼 클릭 시
            btnCigar.setColorFilter(Color.parseColor("#00000000"))
            btnCigar.setBackgroundResource(R.drawable.corner_round)
            currentSelectedNum = 5
            btnCigar.setColorFilter(Color.parseColor("#00000000"))
            for (i in btnList) {
                if (i != btnCigar) {
                    i.setColorFilter(Color.parseColor("#8f000000"))
                    i.setBackgroundResource(R.drawable.transparent)
                }
            }
        }

        var uri: Uri? = null
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // 이미지 받아와서 보여주기
            if (result.resultCode == RESULT_OK && result.data != null) {//정상적으로 이미지를 선택or촬영한 경우
                uri = result.data!!.data!!

                binding.setPlaceShowImage.setImageURI(uri)//이미지 뷰(장소 선택페이지의 아주 조그맣게 보이는 부분)에 이미지 출력
            }
        }
        // 이미지 등록 버튼 리스너
        binding.setPlaceSetImageBtn.setOnClickListener {
            // 이미지 추가를 위한 코드 시작
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val chooser = Intent.createChooser(galleryIntent, "이미지 등록하기")
            chooser.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                arrayOf(cameraIntent)
            )
            resultLauncher.launch(chooser)//이미지 선택or촬영하는 액티비티(chooser) 실행
        }

        // 장소 등록자가 설정한 평점 (기본값 5점)
        var currentScore = 5

        val star1 = binding.starScore1
        val star2 = binding.starScore2
        val star3 = binding.starScore3
        val star4 = binding.starScore4
        val star5 = binding.starScore5

        val starList: List<ImageButton> = listOf<ImageButton>(star1, star2, star3, star4, star5)

        binding.starScore1.setOnClickListener {
            currentScore = 1
            star1.setImageResource(R.drawable.star_light)
            for (i in starList) {
                if (i != star1)
                    i.setImageResource(R.drawable.star_dark)
            }
        }
        binding.starScore2.setOnClickListener {
            currentScore = 2
            star1.setImageResource(R.drawable.star_light)
            star2.setImageResource(R.drawable.star_light)
            star3.setImageResource(R.drawable.star_dark)
            star4.setImageResource(R.drawable.star_dark)
            star5.setImageResource(R.drawable.star_dark)
        }
        binding.starScore3.setOnClickListener {
            currentScore = 3
            star1.setImageResource(R.drawable.star_light)
            star2.setImageResource(R.drawable.star_light)
            star3.setImageResource(R.drawable.star_light)
            star4.setImageResource(R.drawable.star_dark)
            star5.setImageResource(R.drawable.star_dark)
        }
        binding.starScore4.setOnClickListener {
            currentScore = 4
            star1.setImageResource(R.drawable.star_light)
            star2.setImageResource(R.drawable.star_light)
            star3.setImageResource(R.drawable.star_light)
            star4.setImageResource(R.drawable.star_light)
            star5.setImageResource(R.drawable.star_dark)
        }
        binding.starScore5.setOnClickListener {
            currentScore = 5
            star1.setImageResource(R.drawable.star_light)
            star2.setImageResource(R.drawable.star_light)
            star3.setImageResource(R.drawable.star_light)
            star4.setImageResource(R.drawable.star_light)
            star5.setImageResource(R.drawable.star_light)
        }


        // 등록 버튼 리스너 ***Marker 클래스에 넣을 값들을 intent로 MainActivity로 넘겨줌***
        binding.setPlaceSetBtn.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            var lastSetTime = intent.getLongExtra("last_set_time", 0)

            // 마지막 등록 시간의 기본 값(없을 때)은 현재 시간
            if (lastSetTime == 0.toLong()) {
                lastSetTime = currentTime
            }

            val leftTime = currentTime - lastSetTime
            // 밀리초 단위
            val limitTime = 60000
            // 마지막 등록 시간으로부터 지난 시간이 1분 이하면 등록 안 됨 (수정 가능)

            // Show progress message
            val progressDialog = ProgressDialog(this@SetPlaceActivity)
            progressDialog.setMessage("마커를 등록 중입니다.")
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.setCancelable(false)
            progressDialog.show()

            if (leftTime in 1..limitTime) {
                val sec = (leftTime) / 1000
                Toast.makeText(this, "${limitTime / 1000 - sec}초 후에 등록이 가능해요.", Toast.LENGTH_SHORT).show()
            }
            else {
                val name = binding.placeName.text.toString()
                val uniqueId = UUID.randomUUID()
                    .toString()//랜덤한 아이디(키값) 생성, DB저장용, but MainActivity에서도 사용해야 하므로 intent로 넘겨줌

                // 등록 시간
                intent.putExtra("last_set_time", System.currentTimeMillis())

                intent.putExtra("set_latitude", latitude)
                intent.putExtra("set_longitude", longitude)
                intent.putExtra("set_name", name)

                intent.putExtra("isSet", true)
                intent.putExtra("categoryNum", currentSelectedNum)
                intent.putExtra("star", currentScore)
                intent.putExtra("id", uniqueId)

                if (uri != null) {
                    val storageRef = storage!!.getReference().child("images/${uniqueId}.jpg")
                    val uploadTask = storageRef.putFile(uri!!)

                    uploadTask.addOnProgressListener { taskSnapshot ->
                        val progress =
                            (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                        progressDialog.progress = progress.toInt()
                        Log.d("upload_image", "Upload is $progress% done")
                    }.addOnPausedListener {
                        Log.d("upload_image", "Upload is paused")
                    }

                    uploadTask.addOnSuccessListener { taskSnapshot ->
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            // 이미지의 다운로드 URL을 Firestore에 저장
                            firestore?.collection("sampleMarker")?.document(uniqueId)
                                ?.update("imageString", downloadUri.toString())

                            // Intent에 이미지의 URL 추가
                            intent.putExtra("image", downloadUri.toString())
                            setResult(RESULT_OK, intent)

                            val marker = Marker(
                                id = uniqueId,
                                name = name,
                                gps = GeoPoint(latitude, longitude),
                                category = currentSelectedNum,
                                imageString = downloadUri.toString(),
                                imageUri = downloadUri,
                                star = currentScore
                            )

                            firestore?.collection("sampleMarker")?.document(uniqueId)?.set(marker)
                                ?.addOnSuccessListener {
                                    progressDialog.dismiss() // Dismiss progress dialog
                                    finish()
                                }
                                ?.addOnFailureListener { e ->
                                    progressDialog.dismiss() // Dismiss progress dialog
                                    Log.e("kim", "Error Marker Written: ${e.message}", e)
                                }

                            Log.d(
                                "kim",
                                "$latitude, $longitude, $name, $currentSelectedNum transferred to MainActivity"
                            )
                        }
                    }.addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Log.e("kim", "Error uploading image: ${e.message}", e)
                    }
                } else {
                    // 이미지가 없을 경우의 처리
                    intent.putExtra("image", uri.toString())
                    setResult(RESULT_OK, intent)

                    val marker = Marker(
                        id = uniqueId,
                        name = name,
                        gps = GeoPoint(latitude, longitude),
                        category = currentSelectedNum,
                        imageString = uri.toString(),
                        imageUri = uri,
                        star = currentScore
                    )

                    firestore?.collection("sampleMarker")
                        ?.document(uniqueId)
                        ?.set(marker)
                        ?.addOnSuccessListener {
                            progressDialog.dismiss()
                            finish()
                        }
                        ?.addOnFailureListener { e ->
                            progressDialog.dismiss()
                            Log.e("kim", "Error Marker Written: ${e.message}", e)
                        }

                    Log.d(
                        "kim",
                        "$latitude, $longitude, $name, $currentSelectedNum transferred to MainActivity"
                    )

                    finish()

                }

                // finish()//스택에 쌓인 직전 엑티비티(=MainActivity)로 이동
            }
        }

        setContentView(binding.root)
    }

}