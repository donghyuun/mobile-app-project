package com.example.unknownmap

class User {

    var id:String = "" //아이디
    var email:String  = "" //이메일
    var nickname:String  = ""//프로필 이름
    var token: String = ""//토큰

    fun user(id: String, email: String, nickName: String, token: String){
        this.id = id
        this.nickname = nickName 
        this.email = email
        this.token = token
    }
}

