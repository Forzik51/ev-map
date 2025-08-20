package core.ui.components

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
