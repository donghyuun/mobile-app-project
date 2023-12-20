package com.example.unknownmap

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.unknownmap.databinding.ActivityMainBinding
import com.example.unknownmap.databinding.BalloonLayoutBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.kakao.sdk.user.UserApiClient
import net.daum.mf.map.api.CalloutBalloonAdapter
import net.daum.mf.map.api.CameraUpdate
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import net.daum.mf.map.api.MapView.CurrentLocationTrackingMode
import okio.IOException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity(), MapView.POIItemEventListener, MapView.MapViewEventListener, MapView.CurrentLocationEventListener {

    //-----------현재 로그인한 유저(나의 기기)-----------//
    companion object {
        var staticUserId: Long = 0//Long 타입임, 주의.
        var staticUserEmail: String = ""
        var staticUserNickname: String = ""
        var staticUserToken: String = ""
    }
    //----------------------------------------------//

    // 현재 MapPoint 위치
    lateinit var currentMapPoint : MapPoint

    // 현재 모든 POIItems 담는 배열
    lateinit var currentPOIItems: Array<MapPOIItem>

    // firestore 데이터를 가져오기 위한 객체
    var firestore : FirebaseFirestore? = null
    val db = Firebase.firestore

    // setPlaceActivity의 결과를 가져오기 위한 객체
    private lateinit var resultLauncher : ActivityResultLauncher<Intent>

    fun uriToBitmap(contentResolver: ContentResolver, uri: Uri?): Bitmap? {
        try {
            // URI에서 스트림 열기
            val inputStream = contentResolver.openInputStream(uri!!)

            // 스트림에서 비트맵 디코딩
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private var lastSetTime : Long? = 0 // 마지막 등록 시간
    private var currentTagsNum = 0  // 생성된 마커의 개수

    // Marker 생성 함수
    fun createMarker(name: String?, latitude:Double, longtitude:Double, uri: Uri?, categoryType: Int?, star: Int, id: String) : MapPOIItem {
        val point = MapPoint.mapPointWithGeoCoord(latitude, longtitude)
        val marker = MapPOIItem()
        val contentResolver = contentResolver

        marker.apply {
            itemName = name
            tag = star//평점, 최초등록자가 남김
            mapPoint = point
            customImageBitmap = uriToBitmap(contentResolver, uri)
            userObject = id//마커의 unique id
        }
        when (categoryType) {
            0 -> {
                marker.markerType = MapPOIItem.MarkerType.CustomImage
                marker.customImageResourceId = R.drawable.trash_bin
            }
            1 -> {
                marker.markerType = MapPOIItem.MarkerType.CustomImage
                marker.customImageResourceId = R.drawable.vending_machine
            }
            2 -> {
                marker.markerType = MapPOIItem.MarkerType.CustomImage
                marker.customImageResourceId = R.drawable.fish
            }
            3 -> {
                marker.markerType = MapPOIItem.MarkerType.CustomImage
                marker.customImageResourceId = R.drawable.clothes_donation
            }
            4 -> {
                marker.markerType = MapPOIItem.MarkerType.CustomImage
                marker.customImageResourceId = R.drawable.pull_up_bar
            }
            5 -> {
                marker.markerType = MapPOIItem.MarkerType.CustomImage
                marker.customImageResourceId = R.drawable.cigar
            }
        }

        /*
        when (categoryType) {
            // 추후 마커 커스텀 이미지로 설정할 것!
            0 -> marker.markerType = MapPOIItem.MarkerType.CustomImage // 쓰레기통
            1 -> marker.markerType = MapPOIItem.MarkerType.CustomImage // 자판기
            2 -> marker.markerType = MapPOIItem.MarkerType.CustomImage // 붕어빵
        }

        when (categoryType) {
            0 -> marker.customImageResourceId = R.drawable.trash_bin // 쓰레기통 이미지 리소스
            1 -> marker.customImageResourceId = R.drawable.vending_machine // 자판기 이미지 리소스
            2 -> marker.customImageResourceId = R.drawable.fish // 붕어빵 이미지 리소스
        }
        */

        return marker//마커 반환
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            var permissions = arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, 101)
        }

        // Intent를 받아옴
        val intent = intent

        // Intent에서 데이터를 추출
        val userId = intent.getLongExtra("userId",0)//Long 타입임, 주의
        val userEmail = intent.getStringExtra("userEmail") ?: ""
        val userNickname = intent.getStringExtra("userNickname") ?: ""
        val userToken = intent.getStringExtra("userToken") ?: ""

        //static 변수 초기화
        staticUserId = userId
        staticUserEmail = userEmail
        staticUserNickname = userNickname
        staticUserToken = userToken

        // 추출한 데이터를 사용
        Log.d("LOGIN", "In MainActivity, User ID: $userId, Email: $userEmail, Nickname: $userNickname, Token: $userToken")

        //----------------------마이페이지 이동 버튼------------------------//
        binding.mainMypageBtn.setOnClickListener{
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
//            kakaoLogout()
        }

        val mapView = MapView(this)
        mapView.setPOIItemEventListener(this)
        val mapViewContainer = binding.mapView as ViewGroup
        mapViewContainer.addView(mapView)

        // 커스텀 말풍선 설정
        mapView.setCalloutBalloonAdapter(CustomBalloonAdapter(layoutInflater))

        //**********지도를 클릭시 해당 위치의 위/경도 좌표 출력*************
        mapView.setMapViewEventListener(object : MapView.MapViewEventListener {
            override fun onMapViewInitialized(p0: MapView?) {}
            override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {}
            override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {}
            override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
                Log.d("location", "onMapViewSingleTapped run")
                val latitude = p1?.mapPointGeoCoord?.latitude
                val longitude = p1?.mapPointGeoCoord?.longitude
                Log.d("kim", "위도: ${latitude}, 경도: ${longitude}")
            }
            override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {}
            override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {}
            override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}
            override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}
            override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {}
        })

        // DB에 저장된 데이터 불러오기
        fun loadDB() {
            db.collection("sampleMarker")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val name = document.getString("name") ?: ""
                        val latitude = (document["gps"] as GeoPoint).latitude
                        val longitude = (document["gps"] as GeoPoint).longitude
                        val category = document.getLong("category")?.toInt() ?: 0
                        val imageUri: Uri? = null
                        val imageString: String? = null
                        val id: String = document.id
                        val star: Int = document.getLong("star")?.toInt() ?: 0
                        mapView.addPOIItem(createMarker(name, latitude, longitude, imageUri, category, star, id))
                    }
                    currentPOIItems = mapView.poiItems
                    // Now you have the most up-to-date list of items
                }
                .addOnFailureListener { exception ->
                    Log.w("kim", "Error getting documents.", exception)
                }
        }

        // 다른 액티비티로부터 결과를 받아오는 코드 ***등록할 좌표 정보, 이름, 카테고리 등을 받아와서 mapView에 좌표로 등록***
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result ->
            // SetPlaceActivity에서 등록 성공하여 돌아온다면
            if (result.resultCode == RESULT_OK) {

                val name = result.data?.getStringExtra("set_name") ?: ""
                val latitude = result.data?.getDoubleExtra("set_latitude", 0.0)
                val longitude = result.data?.getDoubleExtra("set_longitude", 0.0)
                val category = result.data?.getIntExtra("categoryNum", 0)
                val imageString = result.data?.getStringExtra("image")
                val imageUri = Uri.parse(imageString)
                var nullableStar = result.data?.getIntExtra("star", 0)
                var star = nullableStar ?: 0

                val id = result.data?.getStringExtra("id") ?: ""
                lastSetTime = result.data?.getLongExtra("last_set_time", 0)

                Log.d("kim", "got name : ${name}, got lat :${latitude}, got lon : ${longitude}")
                mapView.addPOIItem(createMarker(name, latitude!!, longitude!!, imageUri, category, star, id))
            }
            // ShowPlaceActivity에서 돌아온다면
            else if (result.resultCode == 11) {
                // 지도 새로고침
                mapView.removeAllPOIItems()
                loadDB()
            }
        }

        // 장소 등록 버튼 리스너, ***누르면 장소 등록 activity 로 이동***
        var addr = ""
        binding.btnSetPlace.setOnClickListener{
            val intent = Intent(this@MainActivity, SetPlaceActivity::class.java)

            val latitude = mapView.mapCenterPoint.mapPointGeoCoord.latitude // 화면 중심의 위도를 얻어옴
            val longitude = mapView.mapCenterPoint.mapPointGeoCoord.longitude // 화면 중심의 경도를 얻어옴

            val mapPoint: MapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)

            // 화면 중심 위치의 위도, 경도 SetPlaceActivity에 전달
            intent.putExtra("create_latitude", latitude)
            intent.putExtra("create_longitude", longitude)
            intent.putExtra("last_set_time", lastSetTime)

            resultLauncher.launch(intent)
            Log.d("kim", "${latitude}, ${longitude} transferred to setPlaceActivity")
        }

        // 경북대학교 마커 생성
        // mapView.addPOIItem(createMarker("경북대학교", 35.8888, 128.6103, null, 0, 0, "testid"))
        // 현위치 모드 설정
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
        // mapView(지도)의 중심 위치를 경북대학교로 설정
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(35.8888, 128.6103), true)
//        mapView.setMapCenterPoint(currentMapPoint, true);
        // 시작 화면 줌 상태
        mapView.setZoomLevel(1, true)
        // 현위치 마커 표시
        mapView.setShowCurrentLocationMarker(true)

        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithMarkerHeadingWithoutMapMoving)

        // 주기적으로 현재 위치를 받아오는 Listener
        mapView.setCurrentLocationEventListener(this@MainActivity)

        binding.myLocationBtn.setOnClickListener {
            mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(currentMapPoint.mapPointGeoCoord.latitude,  currentMapPoint.mapPointGeoCoord.longitude), true)
        }

        // DB 불러오기
        loadDB()

        // 카테고리 버튼 색상 초기화
        fun buttonColorInit() {
            binding.onlyAllBtn.setBackgroundResource(R.drawable.button_before)
            binding.onlyTrashBinBtn.setBackgroundResource(R.drawable.button_before)
            binding.onlyVendingMachineBtn.setBackgroundResource(R.drawable.button_before)
            binding.onlyFishBtn.setBackgroundResource(R.drawable.button_before)
            binding.onlyClothesDonationBtn.setBackgroundResource(R.drawable.button_before)
            binding.onlyPullUpBarBtn.setBackgroundResource(R.drawable.button_before)
            binding.onlyCigarBtn.setBackgroundResource(R.drawable.button_before)
        }

        // 쓰레기통 마커만 보여주는 버튼 리스너
        binding.onlyTrashBinBtn.setOnClickListener {
            buttonColorInit()
            binding.onlyTrashBinBtn.setBackgroundResource(R.drawable.button_after)

            mapView.removeAllPOIItems()

            for (poiItem in currentPOIItems) {
                if (poiItem.customImageResourceId == R.drawable.trash_bin)
                    mapView.addPOIItem(poiItem)
            }
        }
        // 자판기 마커만 보여주는 버튼 리스너
        binding.onlyVendingMachineBtn.setOnClickListener {
            buttonColorInit()
            binding.onlyVendingMachineBtn.setBackgroundResource(R.drawable.button_after)

            mapView.removeAllPOIItems()

            for (poiItem in currentPOIItems) {
                if (poiItem.customImageResourceId == R.drawable.vending_machine)
                    mapView.addPOIItem(poiItem)
            }
        }
        // 붕어빵 마커만 보여주는 버튼 리스너
        binding.onlyFishBtn.setOnClickListener {
            buttonColorInit()
            binding.onlyFishBtn.setBackgroundResource(R.drawable.button_after)

            mapView.removeAllPOIItems()

            for (poiItem in currentPOIItems) {
                if (poiItem.customImageResourceId == R.drawable.fish)
                    mapView.addPOIItem(poiItem)
            }
        }
        // 의류 수거함 마커만 보여주는 버튼 리스너
        binding.onlyClothesDonationBtn.setOnClickListener {
            buttonColorInit()
            binding.onlyClothesDonationBtn.setBackgroundResource(R.drawable.button_after)

            mapView.removeAllPOIItems()

            for (poiItem in currentPOIItems) {
                if (poiItem.customImageResourceId == R.drawable.clothes_donation)
                    mapView.addPOIItem(poiItem)
            }
        }
        // 철봉 마커만 보여주는 버튼 리스너
        binding.onlyPullUpBarBtn.setOnClickListener {
            buttonColorInit()
            binding.onlyPullUpBarBtn.setBackgroundResource(R.drawable.button_after)

            mapView.removeAllPOIItems()

            for (poiItem in currentPOIItems) {
                if (poiItem.customImageResourceId == R.drawable.pull_up_bar)
                    mapView.addPOIItem(poiItem)
            }
        }
        // 흡연장 마커만 보여주는 버튼 리스너
        binding.onlyCigarBtn.setOnClickListener {
            buttonColorInit()
            binding.onlyCigarBtn.setBackgroundResource(R.drawable.button_after)

            mapView.removeAllPOIItems()

            for (poiItem in currentPOIItems) {
                if (poiItem.customImageResourceId == R.drawable.cigar)
                    mapView.addPOIItem(poiItem)
            }
        }
        // 모든 마커 보여주는 버튼 리스너
        binding.onlyAllBtn.setOnClickListener {
            buttonColorInit()
            binding.onlyAllBtn.setBackgroundResource(R.drawable.button_after)

            val poiItems: Array<MapPOIItem> = mapView.poiItems
            mapView.removeAllPOIItems()

            for (poiItem in currentPOIItems) {
                mapView.addPOIItem(poiItem)
            }
        }
        // 새로 고침 버튼
        binding.refreshButton.setOnClickListener {
            mapView.removeAllPOIItems()
            loadDB()
        }
    }

    // 커스텀 말풍선 - binding으로 코드를 더 깔끔하게 수정할 수 있을 듯함
    // CustomBalloonAdapter 클래스 수정
    class CustomBalloonAdapter(inflater: LayoutInflater): CalloutBalloonAdapter {
        val mCalloutBalloon: View = inflater.inflate(R.layout.balloon_layout, null)
        val category: TextView = mCalloutBalloon.findViewById(R.id.ball_category)
        val name: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_name)
        val address: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_address)
        val image: ImageView = mCalloutBalloon.findViewById(R.id.ball_show_image)

        override fun getCalloutBalloon(poiItem: MapPOIItem?): View {
            // 마커 클릭 시 나오는 말풍선
            category.text = when (poiItem?.customImageResourceId) {
                R.drawable.trash_bin -> "쓰레기통"
                R.drawable.vending_machine -> "자판기"
                R.drawable.fish -> "붕어빵"
                R.drawable.clothes_donation -> "의류 수거함"
                R.drawable.pull_up_bar -> "철봉"
                R.drawable.cigar -> "흡연장"
                else -> "기타"
            }
            name.text = poiItem?.itemName  // 해당 마커의 정보 이용 가능
            image.setImageBitmap(poiItem?.customImageBitmap)
            image.apply {
                baselineAlignBottom = true
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            Log.d("window", "getCalloutBalloon run")
            return mCalloutBalloon
        }

        override fun getPressedCalloutBalloon(poiItem: MapPOIItem?): View {
            // 말풍선 클릭 시
            address.text = "getPressedCalloutBalloon"
            Log.d("window", "getPressedCalloutBalloon run")

            return mCalloutBalloon
        }
    }


    // CurrentLocationListener Interface start
    override fun onCurrentLocationUpdate(p0: MapView?, p1: MapPoint?, p2: Float) {
        if (p1 != null) {
            currentMapPoint = p1
        }
        Log.d("ijh", "current map point : " + p1.toString())
    }
    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) { }
    override fun onCurrentLocationUpdateFailed(p0: MapView?) { }
    override fun onCurrentLocationUpdateCancelled(p0: MapView?) { }

    // CurrentLocationListener Interface end

    // POIItemListener Interface
    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) { }
    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, poiItem: MapPOIItem?) {
        // 마커의 인포윈도우를 터치했을 때의 동작을 정의하는 부분
        Log.d("window", "onCalloutBalloonOfPOIItemTouched1 run")

        // 여기에서 Intent를 생성하고 필요한 데이터를 추가
        val intent = Intent(this@MainActivity, ShowPlaceActivity::class.java)
        //위도와 경도를 찾은 후 문서 정보도 넘김

        val collectionName = "sampleMarker"
        var documentId = ""  // Declare documentId here
        var authorName = "" // 마커 생성자 이름
        db.collection(collectionName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val latitude = (document["gps"] as GeoPoint).latitude
                    val longitude = (document["gps"] as GeoPoint).longitude
                    Log.d("song", "Latitude from database: $latitude")
                    Log.d("song", "Longitude from database: $longitude")
                    Log.d("song", "Latitude from poiItem: ${poiItem?.mapPoint?.mapPointGeoCoord?.latitude}")
                    Log.d("song", "Longitude from poiItem: ${poiItem?.mapPoint?.mapPointGeoCoord?.longitude}")
                    authorName = document.getString("author") ?: ""//마커 생성자

                    // GPS 좌표를 비교하여 일치하는 문서를 찾음
                    if (latitude == poiItem?.mapPoint?.mapPointGeoCoord?.latitude && longitude == poiItem?.mapPoint?.mapPointGeoCoord?.longitude) {
                        documentId = document.id
                        Log.d("song", "Found document with ID: $documentId")
                        break
                    }
                }

                // 마커에 대한 정보를 Intent에 추가
                intent.putExtra("document_Id", documentId)
                intent.putExtra("show_name", poiItem?.itemName)
                Log.d("show_name", poiItem?.itemName.toString())
                intent.putExtra("show_latitude", poiItem?.mapPoint?.mapPointGeoCoord?.latitude ?: 0.0)
                intent.putExtra("show_longitude", poiItem?.mapPoint?.mapPointGeoCoord?.longitude ?: 0.0)
                intent.putExtra("show_category", getCategoryType(poiItem?.markerType))
                intent.putExtra("show_star", poiItem?.tag)//추가된 것(점수
                intent.putExtra("show_id", poiItem?.userObject.toString())//마커 id
                intent.putExtra("show_author", authorName)//마커 생성자

                // 이미지를 특정 크기로 조절하고 회전 정보 고려
                val scaledAndRotatedBitmap = rotateBitmap(
                    scaleBitmap(poiItem?.customImageBitmap, 500, 500),
                    getRotationFromExif(poiItem)
                )

                // 이미지를 ByteArrayOutputStream에 압축
                val stream = ByteArrayOutputStream()
                scaledAndRotatedBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()

                // Intent에 바이트 배열 추가
                intent.putExtra("show_image", byteArray)

                // 생성된 Intent를 사용하여 다른 Activity 시작
                resultLauncher.launch(intent)
            }
    }

    // 이미지의 회전 정보를 가져오는 함수
    private fun getRotationFromExif(poiItem: MapPOIItem?): Int {
        try {
            val outputStream = ByteArrayOutputStream()
            poiItem?.customImageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val bytes = outputStream.toByteArray()

            val ei = ExifInterface(ByteArrayInputStream(bytes))
            return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 0
    }

    // 이미지를 주어진 각도로 회전하는 함수
    private fun rotateBitmap(bitmap: Bitmap?, degrees: Int): Bitmap? {
        if (bitmap == null) return null

        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmap(bitmap: Bitmap?, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (bitmap == null) return null
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
    private fun getCategoryType(markerType: MapPOIItem.MarkerType?): Int {
        return when (markerType) {
            MapPOIItem.MarkerType.RedPin -> 0 // 쓰레기통
            MapPOIItem.MarkerType.BluePin -> 1 // 자판기
            MapPOIItem.MarkerType.YellowPin -> 2 // 붕어빵
            else -> 3 // 기타
        }
    }


    override fun onCalloutBalloonOfPOIItemTouched(
        p0: MapView?,
        p1: MapPOIItem?,
        p2: MapPOIItem.CalloutBalloonButtonType?
    ) {
        Log.d("window", "onCalloutBalloonOfPOIItemTouched2 run")
    }

    override fun onDraggablePOIItemMoved(p0: MapView?, p1: MapPOIItem?, p2: MapPoint?) {}

    // POIItemListener Interface end

    // MapViewEventListener Interface
    override fun onMapViewInitialized(p0: MapView?) { }
    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {}
    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {}

    // MapViewEventListener Interface end

    //------------------생명 주기 확인--------------------//
    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart") // 로그 추가
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume") // 로그 추가
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause") // 로그 추가
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop") // 로그 추가
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy") // 로그 추가
    }

}

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
        getAppKeyHash()
        */
