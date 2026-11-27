package com.evmap.mobileapp.feature.events

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.evmap.mobileapp.core.data.dto.*
import com.evmap.mobileapp.core.data.media.ImageMultipart
import com.evmap.mobileapp.core.data.remote.EventsApi
import com.evmap.mobileapp.core.data.session.TokenStore
import com.evmap.mobileapp.feature.events.data.*
import com.evmap.mobileapp.feature.events.ui.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Proxy
import java.io.File
import feature.map.ui.MapSurface
import feature.map.ui.MapStyleStatus
import core.ui.model.MapCameraUi
import kotlinx.coroutines.flow.emptyFlow

@RunWith(AndroidJUnit4::class)
class EventFormUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun drawingAnAreaRendersItsVerticesAndPolygon() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val styleFile=File(context.cacheDir,"picker-ui-test.json")
        styleFile.writeText("""{"version":8,"sources":{},"layers":[{"id":"background","type":"background","paint":{"background-color":"#eef0f5"}}]}""")
        val vertices=mutableStateOf<List<Pair<Double,Double>>>(emptyList())
        val status=mutableStateOf(MapStyleStatus.LOADING)
        try {
            compose.setContent {
                MapSurface(initialCamera=MapCameraUi(50.45,30.52,10f),pins=emptyList(),clusters=emptyList(),location=null,
                    commands=emptyFlow(),styleAttempt=0,onStyleStatus={status.value=it},onViewport={_,_->},onPins={},onCluster={_,_->},onBackground={},
                    modifier=Modifier.fillMaxSize().testTag("area-map"),styleUrl="file://${styleFile.absolutePath}",pickerPoints=vertices.value,
                    onPick={lat,lng->vertices.value=vertices.value+(lat to lng)})
            }
            compose.waitUntil(20_000) {status.value==MapStyleStatus.READY}
            val map=compose.onNodeWithTag("area-map")
            map.performTouchInput {click(Offset(width*0.3f,height*0.3f))}
            compose.waitUntil {vertices.value.size==1}
            map.performTouchInput {click(Offset(width*0.7f,height*0.3f))}
            compose.waitUntil {vertices.value.size==2}
            map.performTouchInput {click(Offset(width*0.5f,height*0.7f))}
            compose.waitUntil {vertices.value.size==3}
            compose.waitForIdle()
            assertEquals(3,vertices.value.distinct().size)
            assertEquals(MapStyleStatus.READY,status.value)
        } finally {styleFile.delete()}
    }

    @Test fun emptyPhotosOptionalDateValidationAndPersistentDraft() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val tokens=TokenStore(context)
        val previous=tokens.session()
        tokens.save(AuthResponse("ui-test","ui-refresh",expiresInSeconds=3600,userId=999999991,username="form-test"))
        val moshi=Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val drafts=EventDraftStore(context,tokens,moshi)
        drafts.clear(tokens.session()!!.userId,null)
        val api=Proxy.newProxyInstance(EventsApi::class.java.classLoader,arrayOf(EventsApi::class.java)) { _,method,_->
            when(method.name) { "getCategories"->listOf(EventCategoryDto(1,"Музика"));else->error("Unexpected network operation: ${method.name}") }
        } as EventsApi
        val locations=LocationRepository(Retrofit.Builder().baseUrl("http://127.0.0.1:1/").addConverterFactory(MoshiConverterFactory.create(moshi)).build())
        val stores=mutableListOf<ViewModelStore>()
        fun newModel(): EventFormViewModel {
            val store=ViewModelStore().also(stores::add)
            return ViewModelProvider(store,object: ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T =
                    EventFormViewModel(SavedStateHandle(),EventsRepository(api),locations,ImageMultipart(context),drafts) as T
            })[EventFormViewModel::class.java]
        }
        val model=mutableStateOf(newModel())
        try {
            compose.setContent { EventFormScreen(onBack={},onSaved={},onOpenCatalog={},viewModel=model.value) }
            compose.waitUntil(10_000) { model.value.state.value.ready }
            compose.onNodeWithContentDescription("Додати фото").assertExists()
            compose.onAllNodes(hasContentDescription("Фото події",substring=true)).assertCountEquals(0)
            compose.onNodeWithText("Дата початку *").assertDoesNotExist()
            compose.onNodeWithText("Опублікувати").performClick()
            compose.waitUntil { model.value.state.value.fieldErrors.size==4 }
            compose.runOnIdle { model.value.updateTitle("Чернетка події") }
            compose.onAllNodes(isToggleable()).onFirst().performClick()
            compose.onNodeWithText("Дата початку *").assertExists()
            compose.waitUntil(10_000) { drafts.read(999999991,null)?.optString("title")=="Чернетка події" }
            compose.runOnIdle { model.value=newModel() }
            compose.waitUntil(10_000) { model.value.state.value.ready && model.value.state.value.draftRestored }
            assertEquals("Чернетка події",model.value.state.value.title)
            assertTrue(model.value.state.value.dateEnabled)
            compose.onNodeWithText("Чернетку відновлено. Зміни зберігаються на цьому телефоні.").assertExists()
        } finally {
            compose.runOnIdle {stores.forEach(ViewModelStore::clear)}
            drafts.clear(999999991,null)
            if(previous==null) tokens.clear() else tokens.save(AuthResponse(previous.accessToken,previous.refreshToken,expiresInSeconds=3600,userId=previous.userId,username=previous.username))
        }
    }
}
