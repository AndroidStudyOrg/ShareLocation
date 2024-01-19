# ShareLocation
위치 공유 앱
- GoogleMap
- GPS 사용 (FusedLocationClient)
- Kakao Auth SDK
- Firebase Realtime Database
- Firebase Auth
- Glide
- Lottie Animation
- View Animation

## [Google Map](https://developers.google.com/maps/documentation/android-sdk?hl=ko)
  - 마커, 다각형, 오버레이를 지도에 추가하여 지도 위치에 대한 정보를 추가로 제공하거나 사용자 상호작용을 지원할 수 있다
  - [Google Map 요금 청구 방식](https://developers.google.com/maps/documentation/android-sdk/usage-and-billing?hl=ko)

## GPS 사용
  - ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION 권한 받아오기
  - FusedLocationClient 가져오기
  - requestLocationUpdates로 현재 위치 가져오기
  - lastLocation을 통해 마지막 위치 가져오기
  - 백그라운드 위치는 구현 X

## [Kakao Auth SDK](https://developers.kakao.com/docs/latest/ko/kakaologin/android)
  - 카카오 로그인을 이용해 OAuth 로그인 구현
  - OAuth는 비밀번호를 제공하지 않고, 다른 웹사이트 상의 정보에 대해 접근 권한을 부여할 수 있는 공통적인 수단으로 사용되는, 접근 위임을위한 개방형 표준이다
  - 구글 로그인, 페이스북 로그인, 카카오톡 로그인, 애플 로그인 등이 이에 해당하며, OAuth 로그인을 통해 발급 받은 토큰을 통해 해당 서버에서 부여받은 권한에 따른 정보를 얻어올 수 있다
