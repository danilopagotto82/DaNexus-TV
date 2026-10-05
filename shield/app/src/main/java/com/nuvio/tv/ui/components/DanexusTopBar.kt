@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.nuvio.tv.ui.components

import com.nuvio.tv.ui.theme.DanexusCinematic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.core.danexus.DanexusOptions
import com.nuvio.tv.ui.v2.appearance.LocalV2Appearance
import com.nuvio.tv.ui.v2.components.GlassRole
import com.nuvio.tv.ui.v2.components.nuvioGlass
import com.nuvio.tv.ui.v2.components.nuvioV2Focus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DanexusClock(modifier: Modifier = Modifier) {
    var clock by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) { clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()); delay(10_000L) }
    }
    Text(clock, modifier, color = Color(0xFFE2EAF6), fontSize=14.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun DanexusTopBar(items: List<DrawerItem>, selectedRoute: String?, options: DanexusOptions,
    profileName: String, profileColor: String, avatarUrl: String?, firstFocus: FocusRequester,
    onProfile: () -> Unit, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val v2=LocalV2Appearance.current != null
    val shape=RoundedCornerShape(bottomStart=12.dp,bottomEnd=12.dp)
    Row(modifier.fillMaxWidth().height(58.dp)
        .then(if(v2) Modifier.nuvioGlass(GlassRole.NAVIGATION,shape=shape) else Modifier
            .background(DanexusCinematic.panelBrush)
            .border(1.dp,DanexusCinematic.edge,shape))
        .padding(horizontal=18.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        Button(onClick=onProfile,modifier=Modifier.focusRequester(firstFocus).widthIn(max=110.dp),
            contentPadding=PaddingValues(horizontal=7.dp,vertical=5.dp),
            scale=ButtonDefaults.scale(focusedScale=1.03f)) {
            ProfileAvatarCircle(profileName,profileColor,size=26.dp,avatarImageUrl=avatarUrl)
            Spacer(Modifier.width(6.dp))
            Text(profileName,fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Box(Modifier.padding(horizontal=5.dp).width(1.dp).height(23.dp).background(DanexusCinematic.edge))
        Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(2.dp),verticalAlignment=Alignment.CenterVertically) {
            items.forEach { item ->
                var focused by remember(item.route) { mutableStateOf(false) }
                val selected=item.route == selectedRoute
                val buttonShape=RoundedCornerShape(10.dp)
                Button(onClick={onNavigate(item.route)},
                    modifier=Modifier.onFocusChanged { focused=it.isFocused }
                        .nuvioV2Focus(focused,buttonShape),
                    contentPadding=PaddingValues(horizontal=8.dp,vertical=7.dp),
                    shape=ButtonDefaults.shape(buttonShape),
                    scale=ButtonDefaults.scale(focusedScale=1.025f),
                    colors=ButtonDefaults.colors(
                        containerColor=if(selected) DanexusCinematic.selectedSurface else Color.Transparent,
                        contentColor=if(selected) DanexusCinematic.text else DanexusCinematic.secondaryText)) {
                    if(options.expandLabels) {
                        item.icon?.let { Icon(it,item.label,Modifier.size(19.dp)) }
                        item.iconRes?.let { resource -> val context=androidx.compose.ui.platform.LocalContext.current
                            Icon(painter=coil3.compose.rememberAsyncImagePainter(coil3.request.ImageRequest.Builder(context).data(resource).size(64).build()),
                                contentDescription=item.label,modifier=Modifier.size(19.dp)) }
                    }
                    AnimatedVisibility(visible=!options.expandLabels || focused || selected,
                        enter=expandHorizontally(),exit=shrinkHorizontally()) {
                        Text(item.label,modifier=Modifier.padding(start=if(options.expandLabels) 6.dp else 0.dp),
                            fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Box(Modifier.padding(horizontal=5.dp).width(1.dp).height(23.dp).background(DanexusCinematic.edge))
        if(options.showClock) DanexusClock(Modifier.width(42.dp))
        BrandWordmark(Modifier.padding(start=7.dp).width(123.dp).height(40.dp),contentDescription="DaNexus")
    }
}
