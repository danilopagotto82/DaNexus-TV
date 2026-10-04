package com.nuvio.tv.ui.components
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nuvio.tv.R
@Composable
fun BrandWordmark(modifier: Modifier = Modifier, contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit, alpha: Float = 1f, drawableOverride: Int? = null) {
    Image(painterResource(R.drawable.danexus_wordmark), contentDescription, modifier,
        contentScale = contentScale, alpha = alpha)
}
