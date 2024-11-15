package app.saboten.androidUi.image

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.saboten.androidUi.utils.shimmer
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    url: Any?,
    colorFilter: ColorFilter? = null,
    shimmer: Boolean = true
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        modifier = modifier,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = {
            if (shimmer) Box(modifier = modifier.shimmer())
            else Box(modifier = modifier)
        },
        colorFilter = colorFilter
    )
}