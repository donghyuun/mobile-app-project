package com.example.unknownmap

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unknownmap.databinding.ActivityMainBinding
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView


class MainActivity : AppCompatActivity(), MapView.POIItemEventListener, MapView.MapViewEventListener {

    val currentLocationMarker: MapPOIItem = MapPOIItem()
    var centerPoint: MapPoint? = null
    var isDialogOpen = false // 다이얼로그가 열려 있는지를 나타내는 플래그

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val mapView = MapView(this)
        val mapViewContainer = binding.mapView as ViewGroup
        mapViewContainer.addView(mapView)
        //mapView(지도)의 중심 위치를 경북대학교로 설정
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(35.8888, 128.6103), true);

        //**********지도를 클릭시 해당 위치의 위/경도 좌표 출력*************
        mapView.setMapViewEventListener(object : MapView.MapViewEventListener {
            override fun onMapViewInitialized(p0: MapView?) {}

            override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {}

            override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {}

            override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
                Log.d("location", "onMapViewSingleTapped run")
                if (!isDialogOpen) {
                    isDialogOpen = true
                    p1?.let {

                        val latitude = it.mapPointGeoCoord.latitude
                        val longitude = it.mapPointGeoCoord.longitude
                        Log.d("kim", "위도: ${latitude}, 경도: ${longitude}")

                        val dialogBuilder =
                            AlertDialog.Builder(this@MainActivity)// this를 this@MainActivity로 변경
                        val inflater =
                            LayoutInflater.from(this@MainActivity)// layoutInflater를 LayoutInflater.from(this@MainActivity)로 변경
                        val dialogView = inflater.inflate(R.layout.dialog_marker_content, null)
                        dialogBuilder.setView(dialogView)

                        dialogBuilder.setTitle("마커 내용 입력")
                        dialogBuilder.setMessage("마커에 들어갈 내용을 입력하세요")

                        var markerContent: String = "" // 사용자 입력을 저장할 변수

                        dialogBuilder.setPositiveButton("확인") { dialog, whichButton ->
                            val editText =
                                dialogView.findViewById<EditText>(R.id.markerContentEditText)
                            markerContent = editText.text.toString() // 사용자 입력을 저장

                            // 이미지 추가를 위한 코드 시작
                            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                            val chooser = Intent.createChooser(galleryIntent, "Select Image")
                            chooser.putExtra(
                                Intent.EXTRA_INITIAL_INTENTS,
                                arrayOf(cameraIntent)
                            )

                            startActivityForResult(chooser, 101) // 이미지 선택 또는 촬영 결과를 받기 위해 startActivityForResult 호출
                            // 이미지 추가를 위한 코드 끝

                            val message =
                                "클릭한 위치의 위도는 $latitude 이고, 경도는 $longitude 입니다. 마커 내용은 $markerContent 입니다."
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                            Log.d(
                                "location",
                                "위도: ${latitude}, 경도: ${longitude}, 마커 내용: $markerContent"
                            )

                            val marker = MapPOIItem()
                            marker.itemName = markerContent
                            marker.tag = 1
                            val mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)
                            marker.mapPoint = mapPoint
                            marker.markerType = MapPOIItem.MarkerType.RedPin // 필요에 따라 마커 유형을 설정하세요
                            mapView.addPOIItem(marker)
                        }

                        dialogBuilder.setNegativeButton("취소") { dialog, whichButton ->
                            // 취소 버튼을 눌렀을 때 처리할 내용
                        }

                        val b = dialogBuilder.create()
                        b.setOnDismissListener {
                            isDialogOpen = false // 다이얼로그가 닫혔음을 나타내는 플래그 설정
                        }
                        b.show()
                    }
                }
            }


            override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {}

            override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {}

            override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}

            override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}

            override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {}
        })

        //**********지도에 마커(경북대학교) 표시**********
        val univPoint = MapPoint.mapPointWithGeoCoord(35.8888,128.6103)
        val marker = MapPOIItem()
        marker.itemName = "이곳은 경북대학교입니다."
        marker.tag = 0
        marker.mapPoint = univPoint
        marker.markerType = MapPOIItem.MarkerType.BluePin // 기본으로 제공하는 BluePin 마커 모양.

        marker.selectedMarkerType =
            MapPOIItem.MarkerType.RedPin // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.

        mapView.addPOIItem(marker)



/*        // 앱 해시 키 얻는 코드
        fun getAppKeyHash() {
            try {
                val info =
                    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                for (signature in info.signatures) {
                    var md: MessageDigest
                    md = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())
                    val something = String(android.util.Base64.encode(md.digest(), 0))
                    Log.e("Hash key", something)
                }
            } catch (e: Exception) {

                Log.e("name not found", e.toString())
            }
        }
        getAppKeyHash()*/
    }

    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) {
        TODO("Not yet implemented")
    }

    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?) {
        TODO("Not yet implemented")
    }

    override fun onCalloutBalloonOfPOIItemTouched(
        p0: MapView?,
        p1: MapPOIItem?,
        p2: MapPOIItem.CalloutBalloonButtonType?
    ) {
        TODO("Not yet implemented")
    }

    override fun onDraggablePOIItemMoved(p0: MapView?, p1: MapPOIItem?, p2: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewInitialized(p0: MapView?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {
        TODO("Not yet implemented")
    }

//    // onActivityResult 함수
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
//            val selectedImage: Uri? = data?.data
//
//            // 선택한 이미지를 비트맵으로 변환
//            val bitmap: Bitmap? = selectedImage?.let { getBitmapFromUri(it) }
//
//            // 비트맵을 원하는 크기로 조정
//            val resizedBitmap: Bitmap? = bitmap?.let { getResizedBitmap(it, 100, 100) }
//
//            // 마커에 이미지 추가
//            val marker = MapPOIItem()
//            // ... (마커 내용 및 위치 설정)
//            if (resizedBitmap != null) {
//                marker.setCustomImageBitmap(resizedBitmap) // 마커에 이미지 설정
//            } else {
//                // 리사이즈된 이미지가 없는 경우 기본 이미지를 사용하거나 예외 처리를 수행할 수 있습니다.
//            }
//            mapView.addPOIItem(marker)
//        }
//    }
//
//    // URI에서 비트맵을 가져오는 함수
//    private fun getBitmapFromUri(uri: Uri): Bitmap? {
//        return try {
//            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
//            val fileDescriptor = parcelFileDescriptor?.fileDescriptor
//            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
//            parcelFileDescriptor?.close()
//            image
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }
//
//    // 비트맵을 원하는 크기로 조정하는 함수
//    private fun getResizedBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
//        return Bitmap.createScaledBitmap(bitmap, width, height, true)
//    }

}