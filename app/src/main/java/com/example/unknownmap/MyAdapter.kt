    package com.example.unknownmap

    import android.util.Log
    import android.view.LayoutInflater
    import android.view.ViewGroup
    import androidx.recyclerview.widget.RecyclerView
    import com.example.unknownmap.databinding.CommentItemBinding

    class MyAdapter(val reviewList:MutableList<KeyValueElement>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        class MyViewHolder(val binding: CommentItemBinding): RecyclerView.ViewHolder(binding.root)//항목을 구성하는 view객체

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
            return MyViewHolder(CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }


        override fun getItemCount(): Int {
            return reviewList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//            if (position < reviewList.size) {
//                Log.d("review", "onBindView ${reviewList[position]}")
//                val hsMap = reviewList[position]
//                Log.d("review", "hsMap: onBindView ${hsMap}")
//                // 뷰에 데이터 출력
//            } else {
//                Log.e("review", "Invalid position: $position")
//            }
        }
    }