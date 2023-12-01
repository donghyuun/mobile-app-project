    package com.example.unknownmap

    import android.util.Log
    import android.view.LayoutInflater
    import android.view.ViewGroup
    import androidx.recyclerview.widget.RecyclerView
    import com.example.unknownmap.databinding.CommentItemBinding
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
                    val binding = (holder as MyViewHolder).binding
                    binding.commentUserId.text = reviewData.userNickName
                    binding.commentContent.text = reviewData.content
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

