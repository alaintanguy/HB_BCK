package com.healthbridge

import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapManager(
    private val fragmentManager: FragmentManager
) : OnMapReadyCallback {

    private lateinit var googleMap:
            GoogleMap

    private val memberMarkers =
        mutableMapOf<String, Marker>()

    private val pendingLocations =
        linkedMapOf<String, MemberLocation>()

    fun initialize() {

        val mapFragment =
            fragmentManager
                .findFragmentById(
                    R.id.map
                ) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(
        map: GoogleMap
    ) {

        googleMap = map

        val start =
            LatLng(
                38.5816,
                -122.5825
            )

        googleMap.moveCamera(
            CameraUpdateFactory
                .newLatLngZoom(
                    start,
                    15f
                )
        )

        pendingLocations
            .values
            .forEach(::showMemberLocation)

        pendingLocations.clear()
    }

    fun updateMemberLocation(
        memberId: String,
        memberName: String,
        latitude: Double,
        longitude: Double
    ) {

        val location =
            MemberLocation(
                memberId,
                memberName,
                latitude,
                longitude
            )

        if (!::googleMap.isInitialized) {
            pendingLocations[memberId] = location
            return
        }

        showMemberLocation(location)
    }

    private fun showMemberLocation(
        location: MemberLocation
    ) {

        val position =
            LatLng(
                location.latitude,
                location.longitude
            )

        val marker =
            memberMarkers[location.memberId]

        if (marker == null) {

            val newMarker =
                googleMap.addMarker(

                    MarkerOptions()
                        .position(position)
                        .title(location.memberName)
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                if (location.memberId == "M1")
                                    BitmapDescriptorFactory.HUE_RED
                                else
                                    BitmapDescriptorFactory.HUE_BLUE
                            )
                        )
                )

            if (newMarker != null) {
                memberMarkers[location.memberId] = newMarker
            }

        } else {

            marker.position = position
        }

        googleMap.animateCamera(
            CameraUpdateFactory
                .newLatLng(position)
        )
    }

    private data class MemberLocation(
        val memberId: String,
        val memberName: String,
        val latitude: Double,
        val longitude: Double
    )
}
