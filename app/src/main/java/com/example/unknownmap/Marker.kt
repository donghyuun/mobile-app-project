package com.example.unknownmap

import android.net.Uri
import com.google.firebase.firestore.GeoPoint
import java.io.Serializable

data class Marker(
    val id: String,
    val name: String,
    val gps: GeoPoint,
    val category: Int,
    val imageString: String? = null,
    val imageUri: Uri? = null,
    val star: Int,
    val author: String? = null
) : Serializable

data class ReviewData(
    val content: String? = null,
    val createdDate: com.google.firebase.Timestamp? = null,
    val userNickName: String? = null,
    val userId: String? = null
)
