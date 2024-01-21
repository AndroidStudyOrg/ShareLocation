package org.shop.sharelocation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import org.shop.sharelocation.databinding.ActivityMapBinding
import java.lang.Long.MAX_VALUE

class MapActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener {
    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val markerMap = hashMapOf<String, Marker>()

    private var trackingPersonId: String = ""

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // 새로 요청된 위치 정보
            for (location in locationResult.locations) {
                Log.e(
                    "MapActivity onLocationResult",
                    "latitude: ${location.latitude} / longitude: ${location.longitude}"
                )

                // Firebase에 내 위치 업로드 / 지도의 마커 움직이기
                val uid = Firebase.auth.currentUser?.uid.orEmpty()

                val locationMap = mutableMapOf<String, Any>()
                locationMap["latitude"] = location.latitude
                locationMap["longitude"] = location.longitude
                Firebase.database.reference.child("Person").child(uid).updateChildren(locationMap)
            }
        }
    }

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // fine location 권한이 있다
                    getCurrentLocation()
                }

                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // coarse location 권한이 있다
                    getCurrentLocation()
                }

                else -> {
                    // TODO 교육용 팝업을 띄워서 다시 권한 요청하기 or 설정으로 보내기
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setUpBasicEmojiAnimationView()
        setUpHeartEmojiAnimationView()
        setUpThumbEmojiAnimationView()

        setUpCurrentLocationView()

        requestLocationPermission()
        setUpFirebaseDatabase()
    }

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun getCurrentLocation() {
        // https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest.Builder
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        // 이 순간부터는 권한이 있는 상태
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        moveLastLocation()
    }

    private fun requestLocationPermission() {
        // 권한을 요청하도록
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun setUpBasicEmojiAnimationView() {
        binding.emojiBasicLottieAnimationView.setOnClickListener {
            if (trackingPersonId != "") {
                val lastEmoji = mutableMapOf<String, Any>()
                lastEmoji["type"] = "basic"
                lastEmoji["lastModifier"] = System.currentTimeMillis()
                Firebase.database.reference.child("Emoji").child(trackingPersonId)
                    .updateChildren(lastEmoji)
            }

            binding.emojiBasicLottieAnimationView.playAnimation()

            binding.dummyBasicLottieAnimationView.apply {
                animate().scaleX(3f).scaleY(3f).alpha(0f).withStartAction {
                    this.scaleX = 1f
                    this.scaleY = 1f
                    this.alpha = 1f
                }.withEndAction {
                    this.scaleX = 1f
                    this.scaleY = 1f
                    this.alpha = 1f
                }.start()
            }
        }
        binding.centerLottieAnimationView.speed = 3f
    }

    private fun setUpHeartEmojiAnimationView() {
        binding.emojiHeartLottieAnimationView.setOnClickListener {
            if (trackingPersonId != "") {
                val lastEmoji = mutableMapOf<String, Any>()
                lastEmoji["type"] = "heart"
                lastEmoji["lastModifier"] = System.currentTimeMillis()
                Firebase.database.reference.child("Emoji").child(trackingPersonId)
                    .updateChildren(lastEmoji)
            }

            binding.emojiHeartLottieAnimationView.playAnimation()

            binding.dummyHeartLottieAnimationView.apply {
                animate().scaleX(3f).scaleY(3f).alpha(0f).withStartAction {
                    this.scaleX = 1f
                    this.scaleY = 1f
                    this.alpha = 1f
                }.withEndAction {
                    this.scaleX = 1f
                    this.scaleY = 1f
                    this.alpha = 1f
                }.start()
            }
        }
        binding.centerLottieAnimationView.speed = 3f
    }

    private fun setUpThumbEmojiAnimationView() {
        binding.emojiThumbLottieAnimationView.setOnClickListener {
            if (trackingPersonId != "") {
                val lastEmoji = mutableMapOf<String, Any>()
                lastEmoji["type"] = "thumb"
                lastEmoji["lastModifier"] = System.currentTimeMillis()
                Firebase.database.reference.child("Emoji").child(trackingPersonId)
                    .updateChildren(lastEmoji)
            }

            binding.emojiThumbLottieAnimationView.playAnimation()

            binding.dummyThumbLottieAnimationView.apply {
                animate().scaleX(3f).scaleY(3f).alpha(0f).withStartAction {
                    this.scaleX = 1f
                    this.scaleY = 1f
                    this.alpha = 1f
                }.withEndAction {
                    this.scaleX = 1f
                    this.scaleY = 1f
                    this.alpha = 1f
                }.start()
            }
        }
        binding.centerLottieAnimationView.speed = 3f
    }

    private fun setUpCurrentLocationView() {
        binding.currentLocationButton.setOnClickListener {
            trackingPersonId = ""
            moveLastLocation()
        }
    }

    private fun moveLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener {
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        it.latitude,
                        it.longitude
                    ), 16.0f
                )
            )
        }
    }

    private fun setUpFirebaseDatabase() {
        Firebase.database.reference.child("Person")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val person = snapshot.getValue(Person::class.java) ?: return
                    val uid = person.uid ?: return

                    if (markerMap[uid] == null) {
                        markerMap[uid] = makeNewMarker(person, uid) ?: return
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val person = snapshot.getValue(Person::class.java) ?: return
                    val uid = person.uid ?: return

                    if (markerMap[uid] == null) {
                        markerMap[uid] = makeNewMarker(person, uid) ?: return
                    } else {
                        // Marker Map에 있는 position 업데이트
                        markerMap[uid]?.position =
                            LatLng(person.latitude ?: 0.0, person.longitude ?: 0.0)
                    }

                    if (uid == trackingPersonId) {
                        googleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder().target(
                                    LatLng(person.latitude ?: 0.0, person.longitude ?: 0.0)
                                ).zoom(16.0f).build()
                            )
                        )
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {}

            })

        Firebase.database.reference.child("Emoji").child(Firebase.auth.currentUser?.uid ?: "")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val emoji = snapshot.getValue(EmojiType::class.java) ?: return
                    Log.e("MapActivity-Emoji", emoji.toString())
                    Log.e("MapActivity-Emoji-currentTime", System.currentTimeMillis().toString())
                    Log.e("MapActivity-Emoji-lastModifier", emoji.lastModifier.toString())

                    if (System.currentTimeMillis() < (emoji.lastModifier ?: MAX_VALUE)) {
                        when (emoji.type) {
                            "basic" -> {
                                binding.centerLottieAnimationView.apply {
                                    setAnimation(R.raw.emoji_basic)
                                    playAnimation()
                                    animate().scaleX(3f).scaleY(3f).alpha(0.3f)
                                        .setDuration(this.duration / 3)
                                        .withEndAction {
                                            this.scaleX = 0f
                                            this.scaleY = 0f
                                            this.alpha = 1f
                                        }.start()
                                }
                            }

                            "heart" -> {
                                binding.centerLottieAnimationView.apply {
                                    setAnimation(R.raw.emoji_heart)
                                    playAnimation()
                                    animate().scaleX(3f).scaleY(3f).alpha(0.3f)
                                        .setDuration(this.duration / 3)
                                        .withEndAction {
                                            this.scaleX = 0f
                                            this.scaleY = 0f
                                            this.alpha = 1f
                                        }.start()
                                }
                            }

                            "thumb" -> {
                                binding.centerLottieAnimationView.apply {
                                    setAnimation(R.raw.emoji_thumbsup)
                                    playAnimation()
                                    animate().scaleX(3f).scaleY(3f).alpha(0.3f)
                                        .setDuration(this.duration / 3)
                                        .withEndAction {
                                            this.scaleX = 0f
                                            this.scaleY = 0f
                                            this.alpha = 1f
                                        }.start()
                                }
                            }

                            else -> {}
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun makeNewMarker(person: Person, uid: String): Marker? {
        val marker = googleMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    person.latitude ?: 0.0,
                    person.longitude ?: 0.0
                )
            ).title(person.name.orEmpty())
        ) ?: return null

        marker.tag = uid

        Glide.with(this).asBitmap().load(person.profilePhoto).transform(RoundedCorners(60))
            .override(200)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.let {
                        runOnUiThread {
                            marker.setIcon(
                                BitmapDescriptorFactory.fromBitmap(resource)
                            )
                        }
                    }
                    return true
                }

            }).submit()

        return marker
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        googleMap.setMaxZoomPreference(20.0f)
        googleMap.setMinZoomPreference(10.0f)

        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener {
            trackingPersonId = ""

            val bottomSheetBehavior = BottomSheetBehavior.from(binding.emojiBottomSheetLayout)
            bottomSheetBehavior.state = STATE_HIDDEN
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        /**
         *  return true: 이벤트 소비했다 간주하고 무시
         *  return false: 기본 동작
         */
        trackingPersonId = marker.tag as? String ?: ""

        // BottomSheetBehavior 불러오기
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.emojiBottomSheetLayout)
        bottomSheetBehavior.state = STATE_EXPANDED  // 펼쳐진 상태. 마커 눌렀을 때 expand
        return false
    }
}