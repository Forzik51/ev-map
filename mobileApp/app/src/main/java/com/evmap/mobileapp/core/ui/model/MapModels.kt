package core.ui.model

data class MapPinUi(
    val id: String,
    val lat: Double,
    val lng: Double,
    val eventTitle: String? = null
)

data class MapCameraUi(
    val lat: Double,
    val lng: Double,
    val zoom: Float
)

data class MapViewport(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val zoom: Int,
) {
    fun apiBounds(): List<MapViewport> {
        val width = east - west
        if (kotlin.math.abs(width) >= 360.0) return listOf(copy(west = -180.0, east = 180.0))
        if (west in -180.0..180.0 && east in west..180.0) return listOf(this)

        val start = ((west + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        val end = start + (width % 360.0 + 360.0) % 360.0
        return if (end <= 180.0) {
            listOf(copy(west = start, east = end))
        } else {
            listOf(copy(west = start, east = 180.0), copy(west = -180.0, east = end - 360.0))
        }
    }

    val showClusters: Boolean get() = zoom < 12
}
