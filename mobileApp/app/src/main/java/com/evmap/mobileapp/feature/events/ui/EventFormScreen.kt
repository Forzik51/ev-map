package com.evmap.mobileapp.feature.events.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evmap.mobileapp.feature.feed.data.toAbsoluteMediaUrl
import feature.events.ui.components.MediaHeader
import core.ui.components.DateField
import core.ui.components.CategoryChip
import core.ui.components.ChipsFlowRow
import core.designsystem.Spacing
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class,ExperimentalFoundationApi::class)
@Composable
fun EventFormScreen(onBack: ()->Unit,onSaved: (Long)->Unit,onOpenCatalog: ()->Unit,viewModel: EventFormViewModel=hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context=LocalContext.current
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { viewModel.addPhotos(it) }
    var dateTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var showLocations by rememberSaveable { mutableStateOf(false) }
    var categoryQuery by rememberSaveable { mutableStateOf("") }
    var categoryProposal by rememberSaveable { mutableStateOf<String?>(null) }
    var zoneDialog by rememberSaveable { mutableStateOf(false) }
    var zoneQuery by rememberSaveable { mutableStateOf("") }
    var deleteDraft by rememberSaveable { mutableStateOf(false) }
    var confirmPast by rememberSaveable { mutableStateOf(false) }
    val keys=listOf("name","description","startsAt","endsAt","timeZone","locationId","categoryIds","tags")
    val bring=remember { keys.associateWith { BringIntoViewRequester() } }
    LaunchedEffect(state.fieldErrors) { state.fieldErrors.keys.firstOrNull()?.let { bring[it]?.bringIntoView() } }
    LaunchedEffect(state.finishedEventId) { state.finishedEventId?.let(onSaved) }
    val editable=state.savedEventId==null && state.submission==null && !state.saving
    BackHandler { viewModel.leave(onBack) }
    fun chooseTime(end: Boolean) {
        val zone=runCatching { ZoneId.of(state.timeZone) }.getOrDefault(ZoneId.systemDefault())
        val initial=(if(end) state.endsAt ?: state.startsAt else state.startsAt)?.atZone(zone) ?: ZonedDateTime.now(zone)
        TimePickerDialog(context,{_,h,m->viewModel.updateTime(end,h,m)},initial.hour,initial.minute,true).show()
    }
    Scaffold(topBar={ TopAppBar(title={Text(if(state.editing) "Редагувати подію" else "Створити подію")},navigationIcon={IconButton(onClick={viewModel.leave(onBack)},enabled=!state.saving && !state.preparingPhotos) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад") }}) },bottomBar={
        if(state.ready) Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.m)) {
            Button(onClick={
                if(state.dateEnabled && state.startsAt?.let { if(state.timeEnabled) it.isBefore(Instant.now()) else it.atZone(ZoneId.of(state.timeZone)).toLocalDate().isBefore(LocalDate.now(ZoneId.of(state.timeZone))) }==true && state.submission==null && validateEventForm(state).isEmpty()) confirmPast=true else viewModel.save()
            },enabled=!state.saving && !state.preparingPhotos,modifier=Modifier.fillMaxWidth()) {
                Text(when { state.saving->"Зберігаємо…";state.savedEventId!=null->"Повторити завантаження фото";state.submission!=null->"Повторити публікацію";state.editing->"Зберегти зміни";else->stringResource(R.string.form_label_c46f45) })
            }
        }
    }) { padding ->
        if(!state.ready) Column(Modifier.padding(padding).padding(Spacing.m)) {
            if(state.loading) CircularProgressIndicator() else {Text(state.error.orEmpty());TextButton(onClick=viewModel::load) {Text(stringResource(R.string.form_label_c95c5c))}}
        } else Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.m),verticalArrangement=Arrangement.spacedBy(Spacing.m)) {
            Text(stringResource(R.string.form_label_0665cc),style=MaterialTheme.typography.bodySmall)
            if(state.draftRestored) Text(stringResource(R.string.form_label_5326c4))
            if(state.savedEventId==null && !state.saving) TextButton(onClick={deleteDraft=true}) {Text(stringResource(R.string.form_label_b86e18))}
            state.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it,color=MaterialTheme.colorScheme.primary) }
            val media=state.photoOrder.mapNotNull { key ->
                if(key.startsWith("s:")) state.existingPhotos.find { "s:${it.id}"==key && it.id !in state.photosToDelete }?.imagePath?.toAbsoluteMediaUrl()?.let {key to it}
                else state.pendingPhotos.find { "p:${it.uploadId}"==key }?.let {key to it.uri.toString()}
            }
            Text(stringResource(R.string.form_label_b93f8d))
            MediaHeader(media.map {it.second},onAddClick=if(!state.saving && !state.preparingPhotos) {{picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}} else null,onRemoveAt=if(!state.saving) {{index->val key=media[index].first;if(key.startsWith("s:")) viewModel.removeExistingPhoto(key.removePrefix("s:").toLong()) else viewModel.removePendingPhoto(key.removePrefix("p:"))}} else null)
            Text(stringResource(R.string.form_10_jpeg_png_webp_5_86955a),style=MaterialTheme.typography.bodySmall)
            if(state.preparingPhotos) LinearProgressIndicator(Modifier.fillMaxWidth())
            media.forEachIndexed { index,item ->
                Row(Modifier.fillMaxWidth()) {
                    Text("Фото ${index+1}${if(index==0) " · обкладинка" else ""}",Modifier.weight(1f))
                    if(index>0) TextButton(onClick={viewModel.movePhoto(item.first,first=true)},enabled=!state.saving) {Text(stringResource(R.string.form_label_55874d))}
                    TextButton(onClick={viewModel.movePhoto(item.first,delta=-1)},enabled=index>0 && !state.saving) {Text("←")}
                    TextButton(onClick={viewModel.movePhoto(item.first,delta=1)},enabled=index<media.lastIndex && !state.saving) {Text("→")}
                }
            }
            if(state.photosToDelete.isNotEmpty()) TextButton(onClick=viewModel::undoPhotoRemovals,enabled=!state.saving) {Text(stringResource(R.string.form_label_64638b))}
            if(state.savedEventId!=null) Text(stringResource(R.string.form_label_359293))
            OutlinedTextField(state.title,viewModel::updateTitle,label={Text(stringResource(R.string.form_label_24d4b0))},enabled=editable,singleLine=true,isError=state.fieldErrors["name"]!=null,supportingText={Text(state.fieldErrors["name"] ?: "${state.title.length}/100")},modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring.getValue("name")))
            OutlinedTextField(state.description,viewModel::updateDescription,label={Text(stringResource(R.string.form_label_03278d))},enabled=editable,minLines=3,isError=state.fieldErrors["description"]!=null,supportingText={Text(state.fieldErrors["description"] ?: "${state.description.length}/2000")},modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring.getValue("description")))
            Row { Text(stringResource(R.string.form_label_07d286),Modifier.weight(1f));Switch(state.dateEnabled,viewModel::setDateEnabled,enabled=editable) }
            if(state.dateEnabled) {
                DateField(state.startsAt.dateLabel(state.timeZone),"Дата початку *",onClick={if(editable) dateTarget="start"},modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring.getValue("startsAt")))
                state.fieldErrors["startsAt"]?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                DateField(state.endsAt.dateLabel(state.timeZone),"Дата завершення · необов’язково",onClick={if(editable) dateTarget="end"},modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring.getValue("endsAt")))
                state.fieldErrors["endsAt"]?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                if(state.endsAt!=null) TextButton(onClick=viewModel::clearEndDate,enabled=editable) {Text(stringResource(R.string.form_label_2473f7))}
                Row { Text(stringResource(R.string.form_label_faf136),Modifier.weight(1f));Switch(state.timeEnabled,viewModel::setTimeEnabled,enabled=editable) }
                if(state.timeEnabled) Row {
                    TextButton(onClick={chooseTime(false)},enabled=editable && state.startsAt!=null) {Text("Початок: ${state.startsAt.timeLabel(state.timeZone)}")}
                    if(state.endsAt!=null) TextButton(onClick={chooseTime(true)},enabled=editable) {Text("Кінець: ${state.endsAt.timeLabel(state.timeZone)}")}
                }
                TextButton(onClick={zoneDialog=true},enabled=editable,modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring.getValue("timeZone"))) {Text("Часовий пояс: ${state.timeZone}")}
                Text(stringResource(R.string.form_label_c410e7),style=MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick={showLocations=true},enabled=editable,modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring.getValue("locationId"))) {Text(if(state.locationId==null) "Місце проведення *" else state.locationName)}
            state.fieldErrors["locationId"]?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            Text(stringResource(R.string.form_1_3_cf714b),modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring.getValue("categoryIds")))
            OutlinedTextField(categoryQuery,{categoryQuery=it},label={Text(stringResource(R.string.form_label_50fbda))},enabled=editable,singleLine=true,modifier=Modifier.fillMaxWidth())
            ChipsFlowRow { state.categories.filter {it.name.contains(categoryQuery,true)}.forEach {cat->CategoryChip(cat.name,cat.id in state.categoryIds,{if(editable) viewModel.toggleCategory(cat.id)})} }
            state.fieldErrors["categoryIds"]?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            TextButton(onClick={categoryProposal=""},enabled=editable) {Text(stringResource(R.string.form_label_faa5cf))}
            OutlinedTextField(state.tagsText,viewModel::updateTags,label={Text(stringResource(R.string.form_label_92250b))},placeholder={Text(stringResource(R.string.form_label_3e3a96))},enabled=editable,isError=state.fieldErrors["tags"]!=null,supportingText={Text(state.fieldErrors["tags"] ?: "До 5 тегів через кому, кожен до 30 символів")},modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring.getValue("tags")))
            if(editable && state.tagSuggestions.isNotEmpty()) ChipsFlowRow {state.tagSuggestions.take(8).forEach {tag->CategoryChip(tag,false,{viewModel.chooseTag(tag)})}}
        }
    }
    if(showLocations) EventLocationDialog(state,viewModel,{showLocations=false})
    dateTarget?.let { target->
        val current=if(target=="start") state.startsAt else state.endsAt ?: state.startsAt
        val initial=current?.atZone(ZoneId.of(state.timeZone))?.toLocalDate()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val pickerState=rememberDatePickerState(initialSelectedDateMillis=initial)
        DatePickerDialog(onDismissRequest={dateTarget=null},confirmButton={TextButton(onClick={pickerState.selectedDateMillis?.let { millis->val date=Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();if(target=="start") viewModel.updateStartDate(date) else viewModel.updateEndDate(date);dateTarget=null}},enabled=pickerState.selectedDateMillis!=null) {Text(stringResource(R.string.form_label_9bee10))}},dismissButton={TextButton(onClick={dateTarget=null}) {Text(stringResource(R.string.form_label_8b4c20))}}) {DatePicker(pickerState)}
    }
    categoryProposal?.let { proposal->AlertDialog(onDismissRequest={categoryProposal=null},title={Text(stringResource(R.string.form_label_240247))},text={OutlinedTextField(proposal,{categoryProposal=it},label={Text(stringResource(R.string.form_label_24d4b0))},supportingText={Text("${proposal.length}/60")},isError=proposal.length>60)},confirmButton={TextButton(onClick={viewModel.proposeCategory(proposal);categoryProposal=null},enabled=proposal.trim().length in 1..60) {Text(stringResource(R.string.form_label_ab958f))}},dismissButton={TextButton(onClick={categoryProposal=null}) {Text(stringResource(R.string.form_label_8b4c20))}}) }
    if(zoneDialog) AlertDialog(onDismissRequest={zoneDialog=false},title={Text(stringResource(R.string.form_label_1de411))},text={Column {OutlinedTextField(zoneQuery,{zoneQuery=it},label={Text(stringResource(R.string.form_label_c85891))});Column(Modifier.heightIn(max=300.dp).verticalScroll(rememberScrollState())) {ZoneId.getAvailableZoneIds().sorted().filter {it.contains(zoneQuery,true)}.take(40).forEach {zone->TextButton(onClick={viewModel.updateTimeZone(zone);zoneDialog=false}) {Text(zone)}}}}},confirmButton={TextButton(onClick={zoneDialog=false}) {Text(stringResource(R.string.form_label_a62276))}})
    if(deleteDraft) AlertDialog(onDismissRequest={deleteDraft=false},title={Text(stringResource(R.string.form_label_684b17))},text={Text(stringResource(R.string.form_label_b1cc83))},confirmButton={TextButton(onClick={viewModel.deleteDraft();deleteDraft=false}) {Text(stringResource(R.string.form_label_eaf989))}},dismissButton={TextButton(onClick={deleteDraft=false}) {Text(stringResource(R.string.form_label_8b4c20))}})
    if(confirmPast) AlertDialog(onDismissRequest={confirmPast=false},title={Text(stringResource(R.string.form_label_98e1d0))},text={Text(stringResource(R.string.form_label_1c0118))},confirmButton={TextButton(onClick={confirmPast=false;viewModel.save()}) {Text(stringResource(R.string.form_label_c46f45))}},dismissButton={TextButton(onClick={confirmPast=false}) {Text(stringResource(R.string.form_label_3bd9a4))}})
}
private fun Instant?.dateLabel(zone: String)=this?.atZone(runCatching {ZoneId.of(zone)}.getOrDefault(ZoneId.systemDefault()))?.toLocalDate()?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).orEmpty()
private fun Instant?.timeLabel(zone: String)=this?.atZone(runCatching {ZoneId.of(zone)}.getOrDefault(ZoneId.systemDefault()))?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "вибрати"
