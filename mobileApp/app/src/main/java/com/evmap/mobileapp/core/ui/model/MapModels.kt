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
