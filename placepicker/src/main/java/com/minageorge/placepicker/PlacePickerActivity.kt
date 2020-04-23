package com.minageorge.placepicker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.gson.Gson
import com.minageorge.placepicker.data.NearByPlacesResponse
import com.minageorge.placepicker.data.PlacePickerConstants
import com.minageorge.placepicker.data.Result
import com.patloew.rxlocation.RxLocation
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_place_picker.*
import kotlinx.android.synthetic.main.layout_current_location.*
import org.json.JSONObject
import java.util.*


class PlacePickerActivity : AppCompatActivity() {

    private var latitude = PlacePickerConstants.DEFAULT_LATITUDE
    private var longitude = PlacePickerConstants.DEFAULT_LONGITUDE
    private var initLatitude = PlacePickerConstants.DEFAULT_LATITUDE
    private var initLongitude = PlacePickerConstants.DEFAULT_LONGITUDE
    private var showLatLong = true
    private var zoom = PlacePickerConstants.DEFAULT_ZOOM
    private var addressRequired: Boolean = true
    private var shortAddress = ""
    private var language = ""
    private var googleApiKey: String? = null
    private var searchBarEnable: Boolean = false
    private var hideMarkerShadow = false
    private var markerDrawableRes: Int = -1
    private var markerColorRes: Int = -1
    private var fabColorRes: Int = -1
    private var primaryTextColorRes: Int = -1
    private var secondaryTextColorRes: Int = -1
    private var bottomViewColorRes: Int = -1
    private var mapRawResourceStyleRes: Int = -1
    private var addresses: List<Address>? = null
    private var mapType: PlacePickerMapType = PlacePickerMapType.NORMAL
    private var onlyCoordinates: Boolean = false

    private val disposable = CompositeDisposable()
    private val nearByPlacesAdapter: NearByPlacesAdapter by lazy { NearByPlacesAdapter() }
    private val rxPermissions: RxPermissions by lazy { RxPermissions(this) }
    private val rxLocation: RxLocation by lazy { RxLocation(this) }
    private val locationRequest: LocationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setNumUpdates(1)

    private var googleMap: GoogleMap? = null
    private var currentLatLng: LatLng? = null
    private var markerLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_picker)
        setSupportActionBar(toolbar)
        getIntentData()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { p0 ->
            googleMap = p0
            googleMap?.setOnMapClickListener { changeMarkerPosition(it) }
            googleMap?.mapType = when (mapType) {
                PlacePickerMapType.NORMAL -> GoogleMap.MAP_TYPE_NORMAL
                PlacePickerMapType.SATELLITE -> GoogleMap.MAP_TYPE_SATELLITE
                PlacePickerMapType.HYBRID -> GoogleMap.MAP_TYPE_HYBRID
                PlacePickerMapType.TERRAIN -> GoogleMap.MAP_TYPE_TERRAIN
                PlacePickerMapType.NONE -> GoogleMap.MAP_TYPE_NONE
                else -> GoogleMap.MAP_TYPE_NORMAL
            }
            if (mapRawResourceStyleRes != -1) {
                googleMap?.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        this,
                        mapRawResourceStyleRes
                    )
                )
            }

            if (latitude == PlacePickerConstants.DEFAULT_LATITUDE || longitude == PlacePickerConstants.DEFAULT_LONGITUDE) {
                if (LocationUtils.isGPSEnabled(this))
                    getCurrentLocationFromGPS()
                else
                    LocationUtils.turnGPSOn(this, object : LocationUtils.OnGpsListener {
                        override fun gpsStatus(isGPSEnable: Boolean) {
                            if (isGPSEnable)
                                getCurrentLocationFromGPS()
                        }
                    })
            } else {
                currentLatLng = LatLng(latitude, longitude)
                changeMarkerPosition(currentLatLng!!)
            }
        }

        sliding_layout.isTouchEnabled = false
        if (searchBarEnable) {
            near_by_title.visibility = VISIBLE
            sheet_arrow.visibility = VISIBLE
            place_autocomplete_layout.visibility = VISIBLE
            toolbar_title.visibility = GONE
            near_by_recycler.minimumHeight = 400
            sliding_layout.isTouchEnabled = true
            near_by_recycler.adapter = nearByPlacesAdapter
            sliding_layout.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
                override fun onPanelSlide(panel: View?, slideOffset: Float) {

                }

                override fun onPanelStateChanged(
                    panel: View?,
                    previousState: SlidingUpPanelLayout.PanelState?,
                    newState: SlidingUpPanelLayout.PanelState?
                ) {
                    when (newState) {
                        SlidingUpPanelLayout.PanelState.EXPANDED -> {
                            sheet_arrow.setImageResource(R.drawable.ic_sheet_down)
                        }
                        SlidingUpPanelLayout.PanelState.DRAGGING -> {
                            sheet_arrow.setImageResource(R.drawable.ic_sheet_down)
                        }
                        SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                            sheet_arrow.setImageResource(R.drawable.ic_sheet_top)
                        }
                        else -> {
                        }
                    }
                }

            })
            initAutoComplete()
        }

        // fab button
        current_location_action.setOnClickListener { currentLatLng?.let { changeMarkerPosition(it) } }
        if (fabColorRes != -1) {
            current_location_action.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, fabColorRes))
            current_location_action.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, fabColorRes))
        }

        // select actions
        this_location.setOnClickListener {
            sliding_layout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            markerLatLng?.let { onLocationSelected(it) }
        }

        nearByPlacesAdapter.listener = object : NearByPlacesAdapter.OnLocationClickListener {
            override fun onClick(result: Result) {
                sliding_layout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                onLocationSelected(
                    LatLng(
                        result.geometry.location.lat,
                        result.geometry.location.lng
                    )
                )
            }

        }
    }

    private fun initAutoComplete() {
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, googleApiKey!!)
        }
        val placeAutocomplete =
            supportFragmentManager.findFragmentById(R.id.place_autocomplete) as AutocompleteSupportFragment
        placeAutocomplete.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS
            )
        )
        placeAutocomplete.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let { changeMarkerPosition(it) }
            }

            override fun onError(error: Status) {
                Log.d(TAG, error.toString())
            }
        })

    }

    override fun onDestroy() {
        disposable.clear()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            LocationUtils.GPS_REQUEST -> {
                if (resultCode == Activity.RESULT_OK)
                    getCurrentLocationFromGPS()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun getCurrentLocationFromGPS() {
        disposable.add(rxPermissions.requestEachCombined(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
            .filter { it.granted }
            .flatMap { rxLocation.location().updates(locationRequest) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                it?.let {
                    currentLatLng = LatLng(it.latitude, it.longitude)
                    changeMarkerPosition(currentLatLng!!)
                }

            }, { Log.d(TAG, it.localizedMessage) })
        )
    }

    private fun changeMarkerPosition(latLng: LatLng) {
        markerLatLng = latLng
        current_latlng.text =
            Location.convert(latLng.latitude, Location.FORMAT_DEGREES) + ", " + Location.convert(
                latLng.longitude,
                Location.FORMAT_DEGREES
            )
        googleMap?.clear()
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        googleMap?.addMarker(markerOptions)
        if (searchBarEnable)
            loadNearByPlaces(latLng)
    }

    private fun loadNearByPlaces(latLng: LatLng) {
        // TODO EMPTY VIEW...
        AndroidNetworking.cancel("nearByPlaces")
        AndroidNetworking.get("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
            .addQueryParameter("location", "${latLng.latitude},${latLng.longitude}")
            .addQueryParameter("language", language)
            .addQueryParameter("radius", "10000")
            .addQueryParameter("key", googleApiKey)
            .addHeaders("Accept", "application/json")
            .addHeaders("Content-Type", "application/json")
            .setTag("nearByPlaces")
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    response?.let {
                        val results: NearByPlacesResponse? =
                            Gson().fromJson(it.toString(), NearByPlacesResponse::class.java)
                        nearByPlacesAdapter.pushData(results?.results ?: Collections.emptyList())
                    }
                }

                override fun onError(anError: ANError?) {
                    Log.d(TAG, anError.toString())
                }

            })
    }

    private fun getIntentData() {
        latitude = intent.getDoubleExtra(
            PlacePickerConstants.INITIAL_LATITUDE_INTENT,
            PlacePickerConstants.DEFAULT_LATITUDE
        )
        longitude = intent.getDoubleExtra(
            PlacePickerConstants.INITIAL_LONGITUDE_INTENT,
            PlacePickerConstants.DEFAULT_LONGITUDE
        )
        initLatitude = latitude
        initLongitude = longitude
        showLatLong = intent.getBooleanExtra(PlacePickerConstants.SHOW_LAT_LONG_INTENT, false)
        addressRequired = intent.getBooleanExtra(PlacePickerConstants.ADDRESS_REQUIRED_INTENT, true)
        hideMarkerShadow =
            intent.getBooleanExtra(PlacePickerConstants.HIDE_MARKER_SHADOW_INTENT, false)
        zoom = intent.getFloatExtra(
            PlacePickerConstants.INITIAL_ZOOM_INTENT,
            PlacePickerConstants.DEFAULT_ZOOM
        )
        markerDrawableRes = intent.getIntExtra(PlacePickerConstants.MARKER_DRAWABLE_RES_INTENT, -1)
        markerColorRes = intent.getIntExtra(PlacePickerConstants.MARKER_COLOR_RES_INTENT, -1)
        fabColorRes = intent.getIntExtra(PlacePickerConstants.FAB_COLOR_RES_INTENT, -1)
        primaryTextColorRes =
            intent.getIntExtra(PlacePickerConstants.PRIMARY_TEXT_COLOR_RES_INTENT, -1)
        secondaryTextColorRes =
            intent.getIntExtra(PlacePickerConstants.SECONDARY_TEXT_COLOR_RES_INTENT, -1)
        bottomViewColorRes =
            intent.getIntExtra(PlacePickerConstants.BOTTOM_VIEW_COLOR_RES_INTENT, -1)
        mapRawResourceStyleRes =
            intent.getIntExtra(PlacePickerConstants.MAP_RAW_STYLE_RES_INTENT, -1)
        mapType =
            intent.getSerializableExtra(PlacePickerConstants.MAP_TYPE_INTENT) as PlacePickerMapType
        onlyCoordinates =
            intent.getBooleanExtra(PlacePickerConstants.ONLY_COORDINATES_INTENT, false)
        googleApiKey = intent.getStringExtra(PlacePickerConstants.GOOGLE_API_KEY)
        language =
            intent.getStringExtra(PlacePickerConstants.LANGUAGE) ?: Locale.getDefault().language
        searchBarEnable = intent.getBooleanExtra(PlacePickerConstants.SEARCH_BAR_ENABLE, false)
    }

    private fun onLocationSelected(latLng: LatLng) {
        if (!Geocoder.isPresent())
            return
        val returnIntent = Intent()
        try {
            val geoCoder = Geocoder(this, Locale(language))
            val addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses.isNotEmpty()) {
                returnIntent.putExtra(PlacePickerConstants.ADDRESS_INTENT, addresses[0])
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        } catch (e: Exception) {
            setResult(Activity.RESULT_CANCELED, returnIntent)
            finish()
        }

    }

    companion object {
        val TAG: String = PlacePickerActivity::class.java.simpleName
    }
}