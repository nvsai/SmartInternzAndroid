package com.example.nearbybusinessapp

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import android.util.Log
import android.util.Log.INFO
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.nearbybusinessapp.Common.Common
import com.example.nearbybusinessapp.Model.MyPlaces
import com.example.nearbybusinessapp.Remote.IGoogleAPIService


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.nearbybusinessapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.internal.NavigationMenuItemView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private var latitude:Double=0.toDouble()
    private var longitude:Double=0.toDouble()
    private lateinit var mLastLocation:Location
    private var mMarker: Marker?=null
    lateinit var FusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    companion object{
        private const val MY_PERMISSION_CODE: Int=1000
    }

    lateinit var mService:IGoogleAPIService

    internal lateinit var currentPlace: MyPlaces

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Init Service
        mService= Common.googleaApiService

        //Request runtime permission
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            if(checkLocationPermission()) {
                buildLocationRequest();
                buildLocationCallBack();

                FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                FusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                );
            }


        }
        var bottom_navigation_view : BottomNavigationView=findViewById(R.id.bottom_navigation_view);
        bottom_navigation_view.setOnNavigationItemSelectedListener{ item->
            when(item.itemId)
            {
                R.id.action_hospital -> nearByPlace("hospital")
                R.id.action_market -> nearByPlace("market")
                R.id.action_restaurant -> nearByPlace("restaurant")
                R.id.action_school -> nearByPlace("school");

            }
            true

        }
    }
    fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
        var drawable = ContextCompat.getDrawable(this, drawableId) ?: return null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = DrawableCompat.wrap(drawable).mutate()
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888) ?: return null
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }


    private fun nearByPlace(typePlace: String) {
        mMap.clear();
        val url=getUrl(latitude,longitude,typePlace)

        mService.getNearbyPlaces(url)
            .enqueue(object : Callback<MyPlaces>{
                override fun onFailure(call: Call<MyPlaces>, t: Throwable) {
                    Toast.makeText(baseContext,""+t!!.message,Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<MyPlaces>, response: Response<MyPlaces>) {
                 currentPlace= response.body()!!
                    if(response!!.isSuccessful)
                    {

                        for (i in 0 until response!!.body()!!.results!!.size)
                        {
                            val markerOptions=MarkerOptions()
                            val googlePlace=response.body()!!.results!![i]
                            val lat=googlePlace.geometry!!.location!!.lat
                            val lng=googlePlace.geometry!!.location!!.lng
                            val placeName=googlePlace.name
                            val latLng=LatLng(lat,lng)
                            markerOptions.position(latLng)
                            markerOptions.title(placeName)
                            if(typePlace.equals("hospital"))
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.ic_action_hospital)))
                            else if(typePlace.equals("market"))
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.ic_action_market)))
                            else if(typePlace.equals("restaurant"))
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.ic_action_restaurent)))
                            else if(typePlace.equals("school"))
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.ic_action_school)))
                            else
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))

                            markerOptions.snippet(i.toString()) // Assign index for marker
                            //Add marker to map
                            mMap!!.addMarker(markerOptions)
                            //move camera
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                            mMap!!.animateCamera(CameraUpdateFactory.zoomTo(13f))
                        }


                    }
                }

            })
    }

    private fun getUrl(latitude: Double, longitude: Double, typePlace: String): String {
        val googlePlaceUrl=StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=10000") //10km
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&key=")
        Log.d("URL_DEBug",googlePlaceUrl.toString())
        return googlePlaceUrl.toString()
    }


    private fun buildLocationCallBack() {
        locationCallback= object :  LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                mLastLocation=p0!!.locations.get(p0!!.locations.size-1)
                if(mMarker!=null)
                {
                    mMarker!!.remove()
                }
                latitude=mLastLocation.latitude
                longitude=mLastLocation.longitude

                val latLng=LatLng(latitude,longitude)
                val markerOptions=MarkerOptions()
                    .position(latLng)
                    .title("Your position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                mMarker=mMap!!.addMarker(markerOptions)

                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))

            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest= LocationRequest()
        locationRequest.priority=LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval=5000
        locationRequest.fastestInterval=3000
        locationRequest.smallestDisplacement=10f
    }

    private fun checkLocationPermission(): Boolean {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
        {
            Log.i(INFO.toString(),"Ask Location")
            print("Ask location")
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION

                    ), MY_PERMISSION_CODE
                )
            }
            else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION

                    ), MY_PERMISSION_CODE
                )
            }
            return false

        }
        else {
            print("Ask Granted")
            Log.i(INFO.toString(),"Ask Granted")
            return true
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode)
        {
            MY_PERMISSION_CODE->{
                if(grantResults.size>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
                    {
                        if(checkLocationPermission()) {
                            buildLocationRequest();
                            buildLocationCallBack();

                            FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                            FusedLocationProviderClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                            );
                            mMap!!.isMyLocationEnabled = true
                        }
                    }
                }
                else
                {
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStop(){
        FusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //Init Google Play Services
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                mMap!!.isMyLocationEnabled = false
            }
        }
        else
            mMap!!.isMyLocationEnabled=true

        //Enable Zoom control
        mMap.uiSettings.isZoomControlsEnabled=true
        // Add a marker in Sydney and move the camera
        """"val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney)) """
    }
}

