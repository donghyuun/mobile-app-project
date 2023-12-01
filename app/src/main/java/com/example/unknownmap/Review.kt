package com.example.unknownmap

import java.util.Date

data class KeyValueElement(
    val userId: String,
    val userNickName: String,
    val content: String,
    val createdDate: Date
) {
    operator fun get(s: String): Any {
        return when (s) {
            "userId" -> userId
            "userNickName" -> userNickName
            "content" -> content
            "createdDate" -> createdDate
            else -> "DefaultContent"
        }
    }
}

data class Review(
    var markerId: String = "",
    var reviewList: MutableList<KeyValueElement> = mutableListOf()
) {
    fun addReview(userId: String, userNickName: String, content: String, createdDate: Date) {
        reviewList.add(KeyValueElement(userId, userNickName, content, createdDate))
    }
}
