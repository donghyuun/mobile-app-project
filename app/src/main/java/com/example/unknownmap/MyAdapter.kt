    package com.example.unknownmap

    import android.content.Intent
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.ViewGroup
    import android.widget.Toast
    import androidx.core.content.ContextCompat.startActivity
    import androidx.recyclerview.widget.RecyclerView
    import com.example.unknownmap.databinding.CommentItemBinding
    import com.google.firebase.firestore.DocumentSnapshot
    import com.google.firebase.firestore.FirebaseFirestore
    import java.security.Timestamp
    import java.util.Objects

    class MyAdapter(val reviewList:MutableList<KeyValueElement>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        class MyViewHolder(val binding: CommentItemBinding): RecyclerView.ViewHolder(binding.root)//항목을 구성하는 view객체

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
            return MyViewHolder(CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }


        override fun getItemCount(): Int {
            return reviewList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            Log.d("review", "MyAdapter: onBindViewHolder: ${reviewList[position]}")
            Log.d("review", "MyAdapter: onBindViewHolder reviewList.size: ${reviewList.size}")
            Log.d("review", "MyAdapter: onBindViewHolder position: ${position}")
            Log.d("review", "MyAdapter: onBindViewHolder typeof reviewList[position]: ${reviewList[position]::class.java}")

            data class ReviewData(
                val content: String? = null,
                val createdDate: com.google.firebase.Timestamp? = null,
                val userNickName: String? = null,
                val userId: String? = null
            )

            try {
                if (reviewList != null && position >= 0 && position < reviewList.size) {
                    //reviewList[Position]이 애초에 DB로부터 HashMap 형태(KeyValueElement 타입 X!!)로 가져와졌기 때문에 해시맵을 이용해야 한다.
                    //따라서 내가 임의로 만든 KeyValueElement 타입의 객체에 reviewList[position]을 대입할 수 없다.
                    val dataFromFirestore: HashMap<String, Any> = reviewList[position] as HashMap<String, Any>
                    Log.d("review", "MyAdapter: typeof reviewList: ${reviewList::class.java}")
                    for ((key, value) in dataFromFirestore) {
                        Log.d("review","Key: $key, Value: $value")
                    }
                    val reviewData = ReviewData(
                        //저장할때는 KeyValueElement 타입이었지만, 가져올때는 HashMap 타입이다.

                        //저장할때는 Timestamp 타입이었지만, 가져올때는 com.google.firebase.Timestamp 타입이다.
                        createdDate = dataFromFirestore["createdDate"] as? com.google.firebase.Timestamp,
                        userNickName = dataFromFirestore["userNickName"] as String?,
                        userId = dataFromFirestore["userId"] as String?,
                        content = dataFromFirestore["content"] as String?
                    )
                    //*******댓글 아이템에 관한 설정*******//
                    val binding = (holder as MyViewHolder).binding
                    binding.commentUserId.text = reviewData.userNickName
                    binding.commentContent.text = reviewData.content
                    binding.commentNumberId.text = (position).toString()
                    if(MainActivity.staticUserId.toString() == reviewData.userId){
                        binding.commentDeleteBtn.visibility = ViewGroup.VISIBLE
                    }
                    else{
                        binding.commentDeleteBtn.visibility = ViewGroup.GONE
                    }
                    binding.commentNumberId.visibility = ViewGroup.GONE
                    binding.commentDeleteBtn.setOnClickListener{
                        Log.d("review in adapter", "MyAdapter: onBindViewHolder: commentDeleteBtn: ${reviewList[position]}")
                        Log.d("review in adapter", "MyAdapter: onBindViewHolder: commentDeleteBtn: ${position}")
                        //리뷰 등록
                        val db = FirebaseFirestore.getInstance()
                        val docRef = db.collection("reviews").document(MainActivity.currentMarkerId)
                        docRef.get()
                            .addOnSuccessListener { document: DocumentSnapshot ->
                                if(document != null && document.exists()){
                                    //기존에 문서가 존재하는 경우, 기존의 reviewList 가져옴
                                    val existingReviewList = document.data?.get("reviewList") as MutableList<KeyValueElement>
                                    //새 리뷰를 기존 reviewList에 추가
                                    existingReviewList.removeAt(position)
                                    //firebase 문서 업데이트
                                    docRef.update("reviewList", existingReviewList)
                                        .addOnSuccessListener {
                                            Log.d("DB", "reviewList successfully updated in existing document!")
//                                            binding.commentEditText.text.clear()
//                                            Toast.makeText(this, "리뷰가 등록되었습니다.", Toast.LENGTH_SHORT).show()
////
//                                            //화면 새로고침, Firestore의 데이터 변경사항이 적용된 후에 갱신하기 위해 여기에 작성
//                                            val intent = Intent(MainActivity, ShowPlaceActivity::class.java)
//                                            intent.putExtra("document_Id", documentId)
//                                            intent.putExtra("show_name", name)
//                                            intent.putExtra("show_latitude", latitude.toDouble())
//                                            intent.putExtra("show_longitude", longitude.toDouble())
//                                            intent.putExtra("show_category", category)
//                                            intent.putExtra("show_image", byteArray)
//                                            intent.putExtra("show_star", star)
//                                            intent.putExtra("show_id", id)
//                                            intent.putExtra("show_author", authorName)
//                                            startActivity(intent)
//                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.d("DB", "Fail, can not updated exsisting reviewList", e)
                                        }
                                } else{
//                                    //기존 문서가 존재하지 않는 경우, 새 문서 생성
//                                    docRef.set(review)
//                                        .addOnSuccessListener {
//                                            Log.d("DB", "new reviewList successfully created!")
//                                            Toast.makeText(this, "리뷰가 등록되었습니다.", Toast.LENGTH_SHORT).show()
//                                        }
//                                        .addOnFailureListener { e ->
//                                            Log.w("DB", "Fail, can not create new reviewList", e)
//                                            Toast.makeText(this, "Error, 리뷰가 등록되지 않았습니다.", Toast.LENGTH_SHORT).show()
//
//                                        }
                                }
                            }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("review", "MyAdapter: onBindViewHolder error: ${e}")
            }
            Log.d("review", "MyAdapter: onBindViewHolder end position")
//            Log.d("review", "MyAdapter: onBindViewHolder content: ${content}")
//            Log.d("review", "MyAdapter: onBindViewHolder reviewList[position].content: ${reviewList[position].get("content")}")
//            Log.d("MyAdapter", "onBindViewHolder: ${reviewList[position].get("content")}")

        }
    }

