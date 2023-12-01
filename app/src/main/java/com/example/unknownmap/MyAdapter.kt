package com.example.unknownmap

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unknownmap.databinding.CommentItemBinding
import kotlin.reflect.typeOf

class MyAdapter(val reviewList:MutableList<KeyValueElement>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class MyViewHolder(val binding: CommentItemBinding): RecyclerView.ViewHolder(binding.root)//항목을 구성하는 view객체

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
        Log.d("review", "A")
        Log.d("review", "A: ${reviewList.toString()}")
        return MyViewHolder(CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }


    override fun getItemCount(): Int {
        Log.d("review", "B")
        Log.d("review", "B: getItemCount : ${reviewList.size}")
        Log.d("review", "B: ${reviewList.toString()}")

        return reviewList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d("review", "C")
        Log.d("review", "onBindViewHolder : $position")
//        Log.d("review", "${reviewList[position].userId} : ${reviewList[position].content}")
        val binding = (holder as MyViewHolder).binding
        // 뷰에 데이터 출력

        Log.d("review", "C1: ${reviewList[position]}")

        binding.commentUserId.text = (reviewList[position]["content"] as? String) ?: "DefaultContent"
        binding.commentContent.text = "usercontent"
        Log.d("review", "C2: ${reviewList.toString()}")

    }
}