package com.example.unknownmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.unknownmap.databinding.ActivityShowPlaceBinding
import net.daum.mf.map.api.MapPOIItem

class ShowPlaceActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)//이거 있어야 이미치 처리할 수 있음
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_place)

        // Intent에서 데이터를 추출
        val name = intent.getStringExtra("show_name") ?: ""
        val latitude = intent.getDoubleExtra("show_latitude", 0.0)
        val longitude = intent.getDoubleExtra("show_longitude", 0.0)
        val category = intent.getIntExtra("show_category", 0)
        val byteArray = intent.getByteArrayExtra("show_image")
        val imageBitmap = if (byteArray != null) {
            // 바이트 배열을 Bitmap으로 변환
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } else {
            null
        }

        Log.d("show_image from intent", imageBitmap?.toString() ?: "Bitmap is null")

        // XML에서 뷰 찾기
        val nameTextView: TextView = findViewById(R.id.show_name_textView)
        val locationTextView: TextView = findViewById(R.id.show_location_textView)
        val categoryTextView: TextView = findViewById(R.id.show_category_textView)
        val imageView: ImageView = findViewById(R.id.show_imageView)

        // 값 설정
        nameTextView.text = name
        locationTextView.text = "위도: $latitude, 경도: $longitude"
        categoryTextView.text = getCategoryString(category)
        imageView.setImageBitmap(imageBitmap)
    }

    private fun getCategoryString(category: Int): String {
        return when (category) {
            0 -> "쓰레기통"
            1 -> "자판기"
            2 -> "붕어빵"
            else -> "기타"
        }
    }
}
