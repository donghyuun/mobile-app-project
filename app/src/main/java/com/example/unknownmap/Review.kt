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
    //db에 저장된 배열을 꺼내서 여기 있는 배열의 값이랑 합쳐서 다시 넣어줘야 한다.
    var markerId: String = "" //마커 아이디
    var reviewList = mutableListOf<KeyValueElement>()//컴페니언 오브젝트는 firebase에 저장되지 않는다.


    // 리뷰를 추가하는 메서드
    fun addReview(userId: String, userNickName: String, content: String, createdDate: Date) {
        reviewList.add(KeyValueElement(userId, userNickName, content, createdDate))
    }
}