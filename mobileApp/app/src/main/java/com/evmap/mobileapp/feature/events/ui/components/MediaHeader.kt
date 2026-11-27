package feature.events.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.evmap.mobileapp.R

@Composable
fun MediaHeader(media: List<String>,onAddClick: (() -> Unit)?=null,onRemoveAt: ((Int)->Unit)?=null,modifier: Modifier=Modifier) {
    LazyRow(modifier.fillMaxWidth().height(if(media.isEmpty()) 56.dp else 151.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=8.dp)) {
        itemsIndexed(media) { index,url ->
            Box(Modifier.width(168.dp).height(135.dp).clip(RoundedCornerShape(20.dp))) {
                AsyncImage(url,contentDescription="Фото події ${index+1}",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
                if(onRemoveAt!=null) IconButton(onClick={onRemoveAt(index)},modifier=Modifier.align(Alignment.TopEnd)) {Icon(Icons.Default.Close,stringResource(R.string.form_remove_image_5f94b0))}
            }
        }
        if(onAddClick!=null) item {
            OutlinedButton(onClick=onAddClick,modifier=Modifier.height(if(media.isEmpty()) 40.dp else 135.dp)) {
                Icon(Icons.Default.Add,stringResource(R.string.form_add_photo_d14349))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.form_add_photo_d14349))
            }
        }
    }
}
