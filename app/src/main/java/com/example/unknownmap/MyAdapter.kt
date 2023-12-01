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
                val createdDate: com.google.firebase.Timestamp? = null,
                val userNickName: String? = null,
                val userId: String? = null,
                val content: String? = null
            )

            try {
                if (reviewList != null && position >= 0 && position < reviewList.size) {
                    val dataFromFirestore: HashMap<String, Any> = reviewList[position] as HashMap<String, Any>
                    for ((key, value) in dataFromFirestore) {
                        Log.d("review","Key: $key, Value: $value")
                    }
                    val reviewData = ReviewData(
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