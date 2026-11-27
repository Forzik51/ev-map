package com.evmap.mobileapp.feature.events.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.evmap.mobileapp.core.data.dto.*
import com.evmap.mobileapp.feature.events.data.eventError
import core.ui.model.MapCameraUi
import feature.map.ui.MapSurface
import feature.map.ui.MapStyleStatus
import feature.map.ui.MapAttribution
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun EventLocationDialog(state: EventFormState,viewModel: EventFormViewModel,onDismiss: ()->Unit) {
    var selected by remember { mutableStateOf<LocationDto?>(null) }
    var points by remember { mutableStateOf<List<Pair<Double,Double>>>(emptyList()) }
    var polygon by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var mapStatus by remember { mutableStateOf(MapStyleStatus.LOADING) }
    var mapAttempt by remember { mutableStateOf(0) }
    val scope=rememberCoroutineScope()
    val uriHandler=androidx.compose.ui.platform.LocalUriHandler.current
    Dialog(onDismissRequest={if(!saving) onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.form_label_01b28b),style=MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.form_label_fb3274))
                OutlinedTextField(state.locationQuery,viewModel::updateLocationQuery,label={Text(stringResource(R.string.form_label_425438))},singleLine=true,modifier=Modifier.fillMaxWidth())
                Button(onClick={viewModel.searchLocation()},enabled=!state.locationSearching && state.locationQuery.trim().length>=2) {Text(stringResource(R.string.form_label_5749a7))}
                if(state.locationSearching) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.locationError?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                state.locationResults.take(10).forEach { item->
                    OutlinedButton(onClick={selected=item;name=item.name;points=emptyList()},modifier=Modifier.fillMaxWidth()) {Text(item.address ?: item.name)}
                }
                Text(stringResource(R.string.form_openstreetmap_contributors_d9f23d),style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={uriHandler.openUri("https://www.openstreetmap.org/copyright")}) { Text(stringResource(R.string.form_label_02a689)) }
                Row { FilterChip(!polygon,{polygon=false;selected=null;points=emptyList()},label={Text(stringResource(R.string.form_label_4ffa37))});Spacer(Modifier.width(8.dp));FilterChip(polygon,{polygon=true;selected=null;points=emptyList()},label={Text(stringResource(R.string.form_label_6f8da4))}) }
                Text(if(polygon) "Натискайте на карту, щоб додати вершини контуру" else "Виберіть результат пошуку або натисніть на карту")
                MapSurface(initialCamera=MapCameraUi(50.4501,30.5234,10f),pins=emptyList(),clusters=emptyList(),location=null,commands=remember {emptyFlow()},styleAttempt=mapAttempt,onStyleStatus={mapStatus=it},onViewport={_,_->},onPins={},onCluster={_,_->},onBackground={},modifier=Modifier.fillMaxWidth().height(320.dp),pickerPoints=points,selectedGeometry=selected?.geometry,onPick={lat,lng->selected=null;points=if(polygon) (points+(lat to lng)).take(500) else listOf(lat to lng)})
                MapAttribution(Modifier.fillMaxWidth())
                if(mapStatus==MapStyleStatus.LOADING) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(mapStatus==MapStyleStatus.ERROR) {
                    Text(stringResource(R.string.form_label_bc068e),color=MaterialTheme.colorScheme.error)
                    TextButton(onClick={mapAttempt++}) {Text(stringResource(R.string.form_label_3bd980))}
                }
                if(points.isNotEmpty()) TextButton(onClick={points=points.dropLast(1)}) {Text(stringResource(R.string.form_label_5f325c))}
                if(polygon) Text("Вершин: ${points.size}/500 · потрібно щонайменше 3")
                OutlinedTextField(name,{name=it},enabled=selected==null,label={Text(stringResource(R.string.form_label_de5be1))},isError=name.length>200,supportingText={Text("${name.length}/200")},modifier=Modifier.fillMaxWidth())
                error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                Button(onClick={
                    saving=true;error=null
                    scope.launch {
                        try {
                            val item=selected ?: run {
                                val coordinates=points.map {JSONArray(listOf(it.second,it.first))}.toMutableList()
                                val geometry=if(polygon) {coordinates.add(coordinates.first());JSONObject().put("type","Polygon").put("coordinates",JSONArray().put(JSONArray(coordinates)))} else JSONObject().put("type","Point").put("coordinates",coordinates.first())
                                viewModel.createLocation(CreateLocationRequest(name.trim(),if(polygon) "CUSTOM_AREA" else "POINT",geometry.toString()))
                            }
                            viewModel.confirmLocation(item);onDismiss()
                        } catch(ex: kotlinx.coroutines.CancellationException) {throw ex} catch(ex: Exception) {error=ex.eventError()} finally {saving=false}
                    }
                },enabled=!saving && name.trim().length in 1..200 && (selected!=null || if(polygon) points.size>=3 else points.size==1),modifier=Modifier.fillMaxWidth()) {Text(if(saving) "Зберігаємо…" else "Підтвердити місце")}
                TextButton(onClick=onDismiss,enabled=!saving) {Text(stringResource(R.string.form_label_8b4c20))}
            }
        }
    }
}
