package com.example.unknownmap

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.BoringLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.unknownmap.databinding.ActivityMainBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import net.daum.mf.map.api.CalloutBalloonAdapter
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapReverseGeoCoder
import net.daum.mf.map.api.MapView
import net.daum.mf.map.api.MapView.CurrentLocationEventListener
import net.daum.mf.map.api.MapView.CurrentLocationTrackingMode

class MainActivity : AppCompatActivity(), MapView.POIItemEventListener, MapView.MapViewEventListener, MapView.CurrentLocationEventListener {

    // 현재 MapPoint 위치
    lateinit var currentMapPoint : MapPoint

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

    private var currentTagsNum = 0  // 생성된 마커의 개수
    // Marker 생성 함수
    fun createMarker(name: String?, latitude:Double, longtitude:Double, uri: Uri?, categoryType: Int?) : MapPOIItem {
        val point = MapPoint.mapPointWithGeoCoord(latitude, longtitude)
        val marker = MapPOIItem()
        val contentResolver = contentResolver

        marker.apply {
            itemName = name
            tag = currentTagsNum
            currentTagsNum += 1
            mapPoint = point
            customImageBitmap = uriToBitmap(contentResolver, uri)
        }
        when (categoryType) {
            // 추후 마커 커스텀 이미지로 설정할 것!
            0 -> marker.markerType = MapPOIItem.MarkerType.RedPin // 쓰레기통
            1 -> marker.markerType = MapPOIItem.MarkerType.BluePin // 자판기
            2 -> marker.markerType = MapPOIItem.MarkerType.YellowPin // 붕어빵
        }

        return marker
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Intent를 받아옴
        val intent = intent

        // Intent에서 데이터를 추출
        val userId = intent.getLongExtra("userId",0)//Long 타입임, 주의.
        val userEmail = intent.getStringExtra("userEmail") ?: ""
        val userNickname = intent.getStringExtra("userNickname") ?: ""
        val userToken = intent.getStringExtra("userToken") ?: ""

        // 추출한 데이터를 사용
        Log.d("LOGIN", "In MainActivity, User ID: $userId, Email: $userEmail, Nickname: $userNickname, Token: $userToken")

        //----------------------카카오 로그아웃 버튼------------------------//
        binding.mainMypageBtn.setOnClickListener{
            kakaoLogout()
        }

        val mapView = MapView(this)
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

        // SetPlaceActivity 로부터 결과를 받아오는 코드 ***등록할 좌표 정보, 이름, 카테고리 등을 받아와서 mapView에 좌표로 등록***
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result ->
            // 서브 액티비티로부터 돌아올 때의 결과 값을 받아 올 수 있는 구문
            if (result.resultCode == RESULT_OK){

                val name = result.data?.getStringExtra("set_name") ?: ""
                val latitude = result.data?.getDoubleExtra("set_latitude", 0.0)
                val longitude = result.data?.getDoubleExtra("set_longitude", 0.0)
                val category = result.data?.getIntExtra("categoryNum", 0)
                val imageString = result.data?.getStringExtra("image")
                val imageUri = Uri.parse(imageString)

                Log.d("kim", "got name : ${name}, got lat :${latitude}, got lon : ${longitude}")
                mapView.addPOIItem(createMarker(name, latitude!!, longitude!!, imageUri, category))
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

            resultLauncher.launch(intent)
            Log.d("kim", "${latitude}, ${longitude} transferred to setPlaceActivity")
        }

        // 경북대학교 마커 생성
        mapView.addPOIItem(createMarker("경북대학교", 35.8888, 128.6103, null, 0))
        // 현위치 모드 설정
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
        // mapView(지도)의 중심 위치를 경북대학교로 설정
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(35.8888, 128.6103), true);
//        mapView.setMapCenterPoint(currentMapPoint, true);
        // 시작 화면 줌 상태
        mapView.setZoomLevel(1, true)
        // 현위치 마커 표시
        mapView.setShowCurrentLocationMarker(true)

        binding.myLocationBtn.setOnClickListener {
            // 현재 위치 추적 모드로 변경
            if (mapView.currentLocationTrackingMode == CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving) {
                Toast.makeText(this@MainActivity, "현재 위치로 고정하여 표시합니다.", Toast.LENGTH_SHORT).show()
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
            // 해제
            } else if (mapView.currentLocationTrackingMode == CurrentLocationTrackingMode.TrackingModeOnWithoutHeading) {
                Toast.makeText(this@MainActivity, "현재 위치 고정을 해제합니다.", Toast.LENGTH_SHORT).show()
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving)
            }
        }
    }

    // 커스텀 말풍선 - binding으로 코드를 더 깔끔하게 수정할 수 있을 듯함
    class CustomBalloonAdapter(inflater: LayoutInflater): CalloutBalloonAdapter {
        val mCalloutBalloon: View = inflater.inflate(R.layout.balloon_layout, null)
        val category: TextView = mCalloutBalloon.findViewById(R.id.ball_category)
        val name: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_name)
        val address: TextView = mCalloutBalloon.findViewById(R.id.ball_tv_address)
        val image: ImageView = mCalloutBalloon.findViewById(R.id.ball_show_image)
        override fun getCalloutBalloon(poiItem: MapPOIItem?): View {
            // 마커 클릭 시 나오는 말풍선
            category.text = when (poiItem?.markerType) {
                MapPOIItem.MarkerType.RedPin -> "쓰레기통"
                MapPOIItem.MarkerType.BluePin -> "자판기"
                MapPOIItem.MarkerType.YellowPin -> "붕어빵"
                else -> "기타"
            }
            name.text = poiItem?.itemName   // 해당 마커의 정보 이용 가능
            image.setImageBitmap(poiItem?.customImageBitmap)
            image.apply {
                baselineAlignBottom = true
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            address.text = "getCalloutBalloon"
            return mCalloutBalloon
        }

        override fun getPressedCalloutBalloon(poiItem: MapPOIItem?): View {
            // 말풍선 클릭 시
            address.text = "getPressedCalloutBalloon"
            return mCalloutBalloon
        }
    }

    // CurrentLocationListener Interface start
    override fun onCurrentLocationUpdate(p0: MapView?, p1: MapPoint?, p2: Float) {
        if (p1 != null) {
            currentMapPoint = p1
        }
        Log.d("kim", "current map point : " + p1.toString())
    }
    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) { }
    override fun onCurrentLocationUpdateFailed(p0: MapView?) { }
    override fun onCurrentLocationUpdateCancelled(p0: MapView?) { }

    // CurrentLocationListener Interface end

    // POIItemListener Interface
    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) { }
    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?) {}
    override fun onCalloutBalloonOfPOIItemTouched(
        p0: MapView?,
        p1: MapPOIItem?,
        p2: MapPOIItem.CalloutBalloonButtonType?
    ) {
        TODO("Not yet implemented")
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

    // KAKAO 로그인 코드 start
    fun kakaoLogout(){
        // 로그아웃 다이얼로그 생성
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("정말 로그아웃 하시겠습니까?")
            .setCancelable(false)
            .setPositiveButton("확인", DialogInterface.OnClickListener {
                    dialog, id -> confirmLogout()
            })
            .setNegativeButton("취소", DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
            })

        val alert = dialogBuilder.create()
        alert.setTitle("로그아웃")
        alert.show()
    }

    private fun confirmLogout(){
        //로그아웃
        UserApiClient.instance.logout { error ->
            if (error != null) {
                Log.e("LOGOUT", "로그아웃 실패. SDK에서 토큰 삭제됨", error)
            } else {
                Log.i("LOGOUT", "로그아웃 성공. SDK에서 토큰 삭제됨")
                val intent = Intent(this, LoginActivity::class.java)
                //스택에 남아있는 모든 액티비티를 제거하고, 해당 엑티비티를 시작함
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                //현재 액티비티를 종료
                finish()
            }
        }
    }
    fun kakaoUnlink() {
        //연결 끊기
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Log.e("LOGOUT", "연결 끊기 실패", error)
            } else {
                Log.i("LOGOUT", "연결 끊기 성공. SDK에서 토큰 삭제 됨")
            }
        }
        finish()
    }
    // 카카오 로그인 코드 end
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
