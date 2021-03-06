package com.mapbox.navigation.ui.map.building

import android.graphics.Color
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.all
import com.mapbox.mapboxsdk.style.expressions.Expression.distance
import com.mapbox.mapboxsdk.style.expressions.Expression.eq
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.expressions.Expression.id
import com.mapbox.mapboxsdk.style.expressions.Expression.literal
import com.mapbox.mapboxsdk.style.expressions.Expression.lt
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.PropertyValue

/**
 * This class handles shared code for the [BuildingExtrusionHighlightLayer]
 * and [BuildingFootprintHighlightLayer] classes which show
 * [com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer] 3D buildings
 * and [com.mapbox.mapboxsdk.style.layers.FillLayer] polygons, respectively.
 * For now, this class is only compatible with the Mapbox Streets v7 vector tile source
 * because that's what the default Navigation UI SDK styles use.
 */
internal class BuildingLayerSupport {

    /**
     * Gets the correct [Expression.all] expression to show the building
     * extrusion associated with the query [LatLng].
     *
     * @param queryLatLng the [LatLng] to use in determining the building ID to use in the
     * expression and which building is closest to the coordinate.
     * @return an [Expression.all] expression
     */
    fun getBuildingFilterExpression(queryLatLng: LatLng, buildingInt: Int): Expression {
        return all(
                eq(get("extrude"), "true"),
                eq(get("type"), "building"),
                eq(get("underground"), "false"),
                eq(id(), literal(buildingInt)),
                lt(distance(Point.fromLngLat(queryLatLng.longitude, queryLatLng.latitude)),
                        literal(QUERY_DISTANCE_MAX_METERS))
        )
    }

    /**
     * Gets the specific ID from the building layer [Feature] that the
     * queryLatLng is within.
     *
     * @param mapboxMap the [MapboxMap] to query
     * @param queryLatLng the [LatLng] to use in determining the building ID to
     * use in the expression and which building is closest to the
     * coordinate.
     * @return the building ID as an integer
     */
    fun getBuildingId(mapboxMap: MapboxMap, queryLatLng: LatLng?): Int {
        queryLatLng?.let { queryCoordinate ->
            val renderedBuildingFootprintFeatures = mapboxMap.queryRenderedFeatures(
                    mapboxMap.projection.toScreenLocation(queryCoordinate), BUILDING_LAYER_ID)
            if (renderedBuildingFootprintFeatures.isNotEmpty()) {
                renderedBuildingFootprintFeatures[0].id()?.let { id ->
                    return Integer.valueOf(id)
                }
            }
        }
        return 0
    }

    /**
     * Sets a new property on a specified layer
     */
    fun updateLayerProperty(propertyValueToSet: PropertyValue<*>?, mapboxMap: MapboxMap, layerId: String) {
        mapboxMap.getStyle { style ->
            val mapLayer = style.getLayerAs<Layer>(layerId)
            mapLayer?.setProperties(propertyValueToSet)
        }
    }

    companion object {
        const val COMPOSITE_SOURCE_ID = "composite"
        const val BUILDING_LAYER_ID = "building"
        const val DEFAULT_HIGHLIGHT_COLOR = Color.RED
        const val DEFAULT_HIGHLIGHT_OPACITY = 1f
        const val QUERY_DISTANCE_MAX_METERS = 20f
    }
}
