package com.example.unknownmap

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.example.unknownmap.databinding.ActivityShowPlaceBinding
import com.example.unknownmap.databinding.CommentItemBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import java.util.Date

class ShowPlaceActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)//이거 있어야 이미치 처리할 수 있음
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityShowPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // firestore 설정
        var firestore: FirebaseFirestore? = null

        // Intent에서 데이터를 추출
        val token = intent.getStringExtra("token") ?:""
        val documentId=intent.getStringExtra("document_Id") ?:"" ///문서 id 받는 val documentId추가
        val name = intent.getStringExtra("show_name") ?: ""
        val latitude = String.format("%.2f", intent.getDoubleExtra("show_latitude", 0.0))
        val longitude = String.format("%.2f", intent.getDoubleExtra("show_longitude", 0.0))
        val category = intent.getStringExtra("show_category") ?: "0"
        val byteArray = intent.getByteArrayExtra("show_image")
        val star = intent.getIntExtra("show_star", 0)
        val id = intent.getStringExtra("show_id") ?: ""
        val markerId = intent.getStringExtra("show_markerId") ?: ""//마커 id 추출
        val imageBitmap = if (byteArray != null) {
            // 바이트 배열을 Bitmap으로 변환
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } else {
            null
        }
        val imageUri = intent.getStringExtra("show_image") ?: ""
        val authorName = intent.getStringExtra("show_author") ?: ""
        MainActivity.currentMarkerId = id//현재 마커 id를 MainActivity의 currentMarkerId에 저장

        //MainActivity의 static 변수에 저장된 유저 정보를 출력해본다
        Log.d("user", "in ShowPlaceActivity, ${MainActivity.staticUserId}")
        Log.d("user", "in ShowPlaceActivity, ${MainActivity.staticUserEmail}")
        Log.d("user", "in ShowPlaceActivity, ${MainActivity.staticUserNickname}")
        Log.d("user", "in ShowPlaceActivity, ${MainActivity.staticUserToken}")
        Log.d("user", "in ShowPlaceActivity, ${authorName}")


        //닫기 버튼
        binding.closeButton.setOnClickListener {
            finish()
        }
        binding.heartButton.setOnClickListener {
            // 현재 이미지 리소스 가져오기
            val currentImageResource = binding.heartButton.drawable
            val star = Star()
            // 현재 이미지와 비교하여 변경
            if (currentImageResource.constantState == resources.getDrawable(R.drawable.blank_heart).constantState) {
                // 현재 이미지가 blank_heart이면 filled_heart로 변경
                binding.heartButton.setImageResource(R.drawable.red_heart)
                // db의 해당 회원의 즐겨찾기 리스트에 해당 마커 id를 추가함
                //입력창 내용 가져오기
                star.addStar(id)
                Log.d("star", "markerId: ${id}")
                //리뷰 등록
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("stars").document(MainActivity.staticUserId.toString())
                docRef.get()
                    .addOnSuccessListener { document: DocumentSnapshot ->
                        if(document != null && document.exists()){
                            //기존에 문서가 존재하는 경우, 기존의 starList 가져옴
                            val existingStarList = document.data?.get("starList") as MutableList<String>
                            //새 리뷰를 기존 reviewList에 추가
                            existingStarList.addAll(star.starList)
                            //firebase 문서 업데이트
                            docRef.update("starList", existingStarList)
                                .addOnSuccessListener {
                                    Log.d("DB", "starList successfully updated in existing document!")
                                    binding.commentEditText.text.clear()
                                    Toast.makeText(this, "즐겨찾기가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.d("DB", "Fail, can not updated exsisting starList", e)
                                    Toast.makeText(this, "Error, 즐겨찾기가 등록되지 않았습니다.", Toast.LENGTH_SHORT).show()
                                }
                        } else{
                            //기존 문서가 존재하지 않는 경우, 새 문서 생성
                            docRef.set(star)
                                .addOnSuccessListener {
                                    Log.d("DB", "new starList successfully created!")
                                    Toast.makeText(this, "즐겨찾기가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("DB", "Fail, can not create new reviewList", e)
                                    Toast.makeText(this, "Error, 즐겨찾기가 등록되지 않았습니다.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }

            } else {
                // 현재 이미지가 red_heart이면 blank_heart로 변경
                binding.heartButton.setImageResource(R.drawable.blank_heart)
                // db의 해당 회원의 즐겨찾기 리스트에서 해당 마커 id를 제거함
                star.starList.remove(id)
                // 리뷰 등록
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("stars").document(MainActivity.staticUserId.toString())
                docRef.update("starList", star.starList)
                    .addOnSuccessListener {
                        Log.d("DB", "starList successfully updated in existing document!")
                        Toast.makeText(this, "즐겨찾기가 제거되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.d("DB", "Fail, can not updated existing starList", e)
                        Toast.makeText(this, "Error, 즐겨찾기가 제거되지 않았습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        //*********************리뷰 등록 버튼*********************//
        binding.submitCommentButton.setOnClickListener {
            //입력창 내용 가져오기
            val content = binding.commentEditText.text.toString()
            val review = Review()
            review.markerId = id
            review.addReview(MainActivity.staticUserId.toString(), MainActivity.staticUserNickname, content, Date())
            //리뷰 등록
            //리뷰 등록
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("reviews").document(id)
            docRef.get()
                .addOnSuccessListener { document: DocumentSnapshot ->
                    if(document != null && document.exists()){
                        //기존에 문서가 존재하는 경우, 기존의 reviewList 가져옴
                        val existingReviewList = document.data?.get("reviewList") as MutableList<KeyValueElement>
                        //새 리뷰를 기존 reviewList에 추가
                        existingReviewList.addAll(review.reviewList)
                        //firebase 문서 업데이트
                        docRef.update("reviewList", existingReviewList)
                            .addOnSuccessListener {
                                Log.d("DB", "reviewList successfully updated in existing document!")
                                binding.commentEditText.text.clear()
                                Toast.makeText(this, "리뷰가 등록되었습니다.", Toast.LENGTH_SHORT).show()

                                //화면 새로고침, Firestore의 데이터 변경사항이 적용된 후에 갱신하기 위해 여기에 작성
                                val intent = Intent(this, ShowPlaceActivity::class.java)
                                intent.putExtra("document_Id", documentId)
                                intent.putExtra("show_name", name)
                                intent.putExtra("show_latitude", latitude.toDouble())
                                intent.putExtra("show_longitude", longitude.toDouble())
                                intent.putExtra("show_category", category)
                                // intent.putExtra("show_image", byteArray)
                                intent.putExtra("show_image", imageUri)
                                intent.putExtra("show_star", star)
                                intent.putExtra("show_id", id)
                                intent.putExtra("show_author", authorName)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.d("DB", "Fail, can not updated exsisting reviewList", e)
                                Toast.makeText(this, "Error, 리뷰가 등록되지 않았습니다.", Toast.LENGTH_SHORT).show()
                            }
                    } else{
                        //기존 문서가 존재하지 않는 경우, 새 문서 생성
                        docRef.set(review)
                            .addOnSuccessListener {
                                Log.d("DB", "new reviewList successfully created!")
                                Toast.makeText(this, "리뷰가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.w("DB", "Fail, can not create new reviewList", e)
                                Toast.makeText(this, "Error, 리뷰가 등록되지 않았습니다.", Toast.LENGTH_SHORT).show()

                            }
                    }
                }
        }

        //*********************리뷰 삭제 버튼*********************//
        // 마커 작성자일때만 삭제 버튼 활성화
        if(authorName == MainActivity.staticUserNickname) {
            binding.removeButton.visibility = ViewGroup.VISIBLE
        }else{
            binding.removeButton.visibility = ViewGroup.GONE
        }

        binding.removeButton.setOnClickListener{
            val builder = AlertDialog.Builder(this@ShowPlaceActivity) // 'context' 대신 'this@MainActivity' 사용
            val itemList = arrayOf( "삭제하기", "취소")
            builder.setItems(itemList) { dialog, which ->
                when (which) {

                    0 -> {

                        val collectionName = "sampleMarker"
                        val db= Firebase.firestore
                        Log.d("dcid=",documentId)
                        db.collection(collectionName).document(documentId)
                            .delete()
                            .addOnSuccessListener {
                                // 성공적으로 삭제된 경우 처리할 로직 추가
                                Log.d("song", "Document successfully deleted with ID: $documentId")
                            }
                            .addOnFailureListener { e ->
                                // 삭제 중에 오류가 발생한 경우 처리할 로직 추가
                                Log.w("song", "Error deleting document with ID: $documentId", e)
                            }
//                        val mainIntent = Intent(this@ShowPlaceActivity, MainActivity::class.java)
//                        startActivity(mainIntent)
                        finish()  // Optional: Close the current activity if needed
                    }
                    1 ->{
                        dialog.dismiss()
                    }
                    // 대화상자 닫기
                }
            }
            builder.show()
        }


        if(star == 0){
            binding.starScore1.setImageResource(R.drawable.star_dark)
            binding.starScore2.setImageResource(R.drawable.star_dark)
            binding.starScore3.setImageResource(R.drawable.star_dark)
            binding.starScore4.setImageResource(R.drawable.star_dark)
            binding.starScore5.setImageResource(R.drawable.star_dark)
        }else if(star == 1){
            binding.starScore1.setImageResource(R.drawable.star_light)
            binding.starScore2.setImageResource(R.drawable.star_dark)
            binding.starScore3.setImageResource(R.drawable.star_dark)
            binding.starScore4.setImageResource(R.drawable.star_dark)
            binding.starScore5.setImageResource(R.drawable.star_dark)
        }else if(star == 2) {
            binding.starScore1.setImageResource(R.drawable.star_light)
            binding.starScore2.setImageResource(R.drawable.star_light)
            binding.starScore3.setImageResource(R.drawable.star_dark)
            binding.starScore4.setImageResource(R.drawable.star_dark)
            binding.starScore5.setImageResource(R.drawable.star_dark)
        }else if(star == 3) {
            binding.starScore1.setImageResource(R.drawable.star_light)
            binding.starScore2.setImageResource(R.drawable.star_light)
            binding.starScore3.setImageResource(R.drawable.star_light)
            binding.starScore4.setImageResource(R.drawable.star_dark)
            binding.starScore5.setImageResource(R.drawable.star_dark)
        }else if(star == 4) {
            binding.starScore1.setImageResource(R.drawable.star_light)
            binding.starScore2.setImageResource(R.drawable.star_light)
            binding.starScore3.setImageResource(R.drawable.star_light)
            binding.starScore4.setImageResource(R.drawable.star_light)
            binding.starScore5.setImageResource(R.drawable.star_dark)
        }else if(star == 5){
            binding.starScore1.setImageResource(R.drawable.star_light)
            binding.starScore2.setImageResource(R.drawable.star_light)
            binding.starScore3.setImageResource(R.drawable.star_light)
            binding.starScore4.setImageResource(R.drawable.star_light)
            binding.starScore5.setImageResource(R.drawable.star_light)
        }

        Log.d("show_image from intent", imageBitmap?.toString() ?: "Bitmap is null")

        // XML에서 뷰 찾기
        val nameTextView: TextView = findViewById(R.id.show_name_textView)
        val locationTextView: TextView = findViewById(R.id.show_location_textView)
        val categoryTextView: TextView = findViewById(R.id.show_category_textView)
        val imageView: ImageView = findViewById(R.id.show_imageView)

        // 값 설정
        nameTextView.text = name
        locationTextView.text = "위도: $latitude\n경도: $longitude"
        categoryTextView.text = getCategoryString(category)
//        imageView.setImageBitmap(imageBitmap)
        //


        val storage = Firebase.storage
        val storageReference = storage.reference

        val storageRef: StorageReference = storageReference.child("images/${id}.jpg"  )

        storageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(imageView.context)
                .load(uri)
                .into(imageView)
            Log.d("test2","1111 " + uri.toString())
        }.addOnFailureListener { exception ->
            imageView.apply {
                setImageResource(R.drawable.ic_launcher_round)
                baselineAlignBottom = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                Log.d("test2", "2222 " )
            }
        }



        //********************리뷰 가져와서 저장하기 위한 변수********************//
        // firestore 데이터를 가져오기 위한 객체
        val db = FirebaseFirestore.getInstance()
        var reviewList = mutableListOf<KeyValueElement>()
        db.collection("reviews").document(id).get()
            .addOnSuccessListener { document ->
                if(document != null && document.exists() && document.data?.get("markerId") == id){
                    reviewList = document.data?.get("reviewList") as MutableList<KeyValueElement>
                    Log.d("DB", "review list exists")
                    Log.d("DB", reviewList.toString())
                    Log.d("DB", reviewList.size.toString())

                    //여기서 리사이클러뷰에 리뷰 목록을 출력하도록 해야함, 안그러면 DB에서 리뷰 목록 받아오는 것보다 더 빨리 실행되서 리뷰가 정상적으로 출력되지 않음
                    val layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)//한줄에 항목을 몇개 배치할지
                    binding.commentsRecyclerView.layoutManager = layoutManager
                    Log.d("review", "ShowPlaceActivity: reviewList.toString: ${reviewList.toString()}")
                    Log.d("review", "ShowPlaceActivity: reviewList.size.toString: ${reviewList.size.toString()}")
                    binding.commentsRecyclerView.adapter = MyAdapter(this, reviewList)
                } else{
                    Log.d("review", "ShowPlaceActivity: review list does not exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("review", "ShowPlaceActivity: get failed with ", exception)
            }

        var isFavorite : Boolean = false
        lateinit var existingPlaceList : MutableList<String>
        var usersDB = db.collection("users").document(MainActivity.staticUserId.toString())
        usersDB.get().addOnSuccessListener() { document: DocumentSnapshot ->
            existingPlaceList = document.data?.get("places") as MutableList<String>

            // 즐겨찾기 places 에 속하면 하트 활성화
            for (item in existingPlaceList) {
                if (item == documentId) {
                    binding.heartButton.setImageResource(R.drawable.red_heart)
                    isFavorite = true
                    break
                }
            }
        }

        binding.heartButton.setOnClickListener {
            // 현재 이미지 리소스 가져오기
            val currentImageResource = binding.heartButton.drawable

            // 현재 이미지와 비교하여 변경
            if (!isFavorite) {
                // 현재 이미지가 blank_heart이면 filled_heart로 변경
                binding.heartButton.setImageResource(R.drawable.red_heart)
                isFavorite = true

                usersDB.get()
                    .addOnSuccessListener() { document: DocumentSnapshot ->
                        if(document != null && document.exists()){
                            //기존에 즐겨찾기가 존재하는 경우, 기존의 places 가져옴
                            //새 즐겨찾기를 기존 places에 추가
                            existingPlaceList.add(documentId)
                            //firebase 문서 업데이트
                            usersDB.update("places", existingPlaceList)
                                ?.addOnSuccessListener {
                                    Log.d("DB", "placeList successfully updated in existing document!")
                                    Toast.makeText(this, "즐겨찾기가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                                }
                                ?.addOnFailureListener { e ->
                                    Log.d("DB", "Fail, can not updated exsisting placeList", e)
                                    Toast.makeText(this, "Error, 즐겨찾기가 등록되지 않았습니다.", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            //기존 문서가 존재하지 않는 경우, 새 문서 생성
                            existingPlaceList.add(documentId)
                            usersDB.set(existingPlaceList)
                                .addOnSuccessListener {
                                    Log.d("DB", "new placeList successfully created!")
                                    Toast.makeText(this, "즐겨찾기가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("DB", "Fail, can not create new placeList", e)
                                    Toast.makeText(this, "Error, 즐겨찾기가 등록되지 않았습니다.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
            } else {
                // 현재 이미지가 red_heart이면 blank_heart로 변경
                binding.heartButton.setImageResource(R.drawable.blank_heart)
                isFavorite = false
                existingPlaceList.remove(documentId)
                usersDB.update("places", existingPlaceList)
                    ?.addOnSuccessListener {
                        Log.d("DB", "placeList successfully updated in existing document!")
                        Toast.makeText(this, "즐겨찾기가 제거되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    ?.addOnFailureListener { e ->
                        Log.d("DB", "Fail, can not updated exsisting placeList", e)
                        Toast.makeText(this, "Error, 즐겨찾기가 제거되지 않았습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // ShowPlaceActivity가 끝날 때 Result 값 11로 지정
        setResult(11)
    }

    private fun getCategoryString(category: String): String {
        val category = category.toInt()
        return when (category) {
            0 -> "쓰레기통"
            1 -> "자판기"
            2 -> "붕어빵"
            3 -> "의류 수거함"
            4 -> "철봉"
            5 -> "흡연장"
            else -> "기타"
        }
    }

}
