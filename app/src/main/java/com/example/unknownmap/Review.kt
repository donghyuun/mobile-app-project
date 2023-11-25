package com.example.unknownmap

import java.util.Date

data class KeyValueElement(
    val userId: String,
    val userNickName: String,
    val content: String,
    val createdDate: Date
)

data class Review(
    var markerId: String = "",
    var reviewList: MutableList<KeyValueElement> = mutableListOf()
) {
    fun addReview(userId: String, userNickName: String, content: String, createdDate: Date) {
        reviewList.add(KeyValueElement(userId, userNickName, content, createdDate))
    }
}
