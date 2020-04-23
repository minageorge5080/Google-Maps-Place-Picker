package com.minageorge.placepicker

import android.app.Activity
import android.content.Intent
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.minageorge.placepicker.data.PlacePickerConstants

class PlacePickerIntentBuilder {

    private lateinit var activity: Activity
    private var showLatLong: Boolean = false
    private var latitude: Double = PlacePickerConstants.DEFAULT_LATITUDE
    private var longitude: Double = PlacePickerConstants.DEFAULT_LONGITUDE
    private var zoom: Float = PlacePickerConstants.DEFAULT_ZOOM
    private var addressRequired: Boolean = true
    private var hideMarkerShadow: Boolean = false
    private var markerDrawableRes: Int = -1
    private var markerImageColorRes: Int = -1
    private var fabBackgroundColorRes: Int = -1
    private var primaryTextColorRes: Int = -1
    private var secondaryTextColorRes: Int = -1
    private var bottomViewColorRes: Int = -1
    private var mapRawResourceStyleRes: Int = -1
    private var placePickerMapType: PlacePickerMapType = PlacePickerMapType.NORMAL
    private var onlyCoordinates: Boolean = false
    private var disableBottomSheetAnimation: Boolean = false
    private var googleApiKey: String? = null
    private var searchBarEnable: Boolean = false
    private var language: String? = null

    fun setLatLong(
        latitude: Double,
        longitude: Double
    ) = apply {
        this.latitude = latitude
        this.longitude = longitude
    }

    fun setPlaceSearchBar(value: Boolean, googleApiKey: String? = null) = apply {
        this.googleApiKey = googleApiKey
        this.searchBarEnable = value
    }

    fun setMapZoom(zoom: Float) = apply { this.zoom = zoom }

    fun setLanguage(language: String) = apply { this.language = language }

    fun setFabColor(@ColorRes fabBackgroundColor: Int) =
        apply { this.fabBackgroundColorRes = fabBackgroundColor }

    fun setMapRawResourceStyle(@RawRes mapRawResourceStyleRes: Int) =
        apply { this.mapRawResourceStyleRes = mapRawResourceStyleRes }

    fun setMapType(placePickerMapType: PlacePickerMapType) =
        apply { this.placePickerMapType = placePickerMapType }


    fun build(activity: Activity): Intent {
        this.activity = activity
        val intent = Intent(activity, PlacePickerActivity::class.java)
        intent.putExtra(PlacePickerConstants.ADDRESS_REQUIRED_INTENT, addressRequired)
        intent.putExtra(PlacePickerConstants.SHOW_LAT_LONG_INTENT, showLatLong)
        intent.putExtra(PlacePickerConstants.INITIAL_LATITUDE_INTENT, latitude)
        intent.putExtra(PlacePickerConstants.INITIAL_LONGITUDE_INTENT, longitude)
        intent.putExtra(PlacePickerConstants.INITIAL_ZOOM_INTENT, zoom)
        intent.putExtra(PlacePickerConstants.HIDE_MARKER_SHADOW_INTENT, hideMarkerShadow)
        intent.putExtra(PlacePickerConstants.MARKER_DRAWABLE_RES_INTENT, markerDrawableRes)
        intent.putExtra(PlacePickerConstants.MARKER_COLOR_RES_INTENT, markerImageColorRes)
        intent.putExtra(PlacePickerConstants.FAB_COLOR_RES_INTENT, fabBackgroundColorRes)
        intent.putExtra(PlacePickerConstants.PRIMARY_TEXT_COLOR_RES_INTENT, primaryTextColorRes)
        intent.putExtra(PlacePickerConstants.SECONDARY_TEXT_COLOR_RES_INTENT, secondaryTextColorRes)
        intent.putExtra(PlacePickerConstants.BOTTOM_VIEW_COLOR_RES_INTENT, bottomViewColorRes)
        intent.putExtra(PlacePickerConstants.MAP_RAW_STYLE_RES_INTENT, mapRawResourceStyleRes)
        intent.putExtra(PlacePickerConstants.MAP_TYPE_INTENT, placePickerMapType)
        intent.putExtra(PlacePickerConstants.ONLY_COORDINATES_INTENT, onlyCoordinates)
        intent.putExtra(PlacePickerConstants.GOOGLE_API_KEY, googleApiKey)
        intent.putExtra(PlacePickerConstants.SEARCH_BAR_ENABLE, searchBarEnable)
        intent.putExtra(PlacePickerConstants.LANGUAGE, language)
        return intent
    }
}
