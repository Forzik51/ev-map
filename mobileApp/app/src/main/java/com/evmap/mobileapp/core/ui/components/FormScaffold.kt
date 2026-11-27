package core.ui.components
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import core.designsystem.Spacing

@Composable
fun FormScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {


    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.l),
                verticalArrangement = Arrangement.Center,
        ) {
            androidx.compose.material3.Text(stringResource(R.string.form_label_85e3ea),style=androidx.compose.material3.MaterialTheme.typography.bodySmall)
            content()
        }
    }


    //Column(
    //    modifier = modifier
    //        .fillMaxSize()
    //        .statusBarsPadding()
    //        .imePadding()
    //        .verticalScroll(rememberScrollState())
    //        .padding(horizontal = Spacing.l),
    //    verticalArrangement = Arrangement.Center,
    //) {
    //    content()
    //}
}
