package com.example.unknownmap

data class Star(
    var starList: MutableList<String> = mutableListOf()
) {
    fun addStar(markerId: String) {
        starList.add(markerId)
    }
}
