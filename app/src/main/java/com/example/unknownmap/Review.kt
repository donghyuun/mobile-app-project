package com.example.unknownmap

import java.util.Date

data class KeyValueElement(
    val userId: String,
    val userNickName: String,
    val content: String,
    val createdDate: Date
)


class Review()// 기본 생성자를 사용하면서 reviewList를 초기화
{
    companion object{
        val companionReviewList = mutableListOf<KeyValueElement>() //리뷰 리스트
    }
    var markerId: String = "" //마커 아이디
    var reviewList = Review.companionReviewList//컴페니언 오브젝트는 firebase에 저장되지 않는다!!


    // 리뷰를 추가하는 메서드
    fun addReview(userId: String, userNickName: String, content: String, createdDate: Date) {
        companionReviewList.add(KeyValueElement(userId, userNickName, content, createdDate))
        reviewList = companionReviewList
    }
}