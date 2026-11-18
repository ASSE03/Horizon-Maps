package com.headunit.launcher.ui

import com.headunit.launcher.L

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** The Google Group testers join (joining it is what lets them into the closed test). */
internal const val PLAY_GROUP_URL = "https://groups.google.com/g/horizon-maps-testers"
/** Google's page for joining the closed test, which then leads to the Play Store. */
internal const val PLAY_TEST_URL = "https://play.google.com/apps/testing/com.headunit.nav.play"

/**
 * Whether the "Install from Play" button is open yet: it stays behind its "Coming soon" sign until the repository holds a
 * play-test.txt saying "open" (added once Google has approved the test), so it opens for everyone without an app update.
 * Read once when the card shows; no signal, or anything unexpected, keeps it closed.
 */
private fun playTestOpen(): Boolean = try {
    if (com.headunit.launcher.Net.offline) false else {
        val c = java.net.URL("https://raw.githubusercontent.com/${com.headunit.launcher.BuildConfig.UPDATE_REPO}/main/play-test.txt")
            .openConnection() as java.net.HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 6000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        try { c.responseCode == 200 && c.inputStream.bufferedReader().use { it.readText() }.trim().lowercase().startsWith("open") }
        finally { try { c.disconnect() } catch (e: Exception) { } }
    }
} catch (e: Throwable) { false }

private fun openLink(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        android.widget.Toast.makeText(ctx, "Aucun navigateur sur cet appareil. Scannez le code avec votre téléphone.", android.widget.Toast.LENGTH_LONG).show()
    }
}

/** How the card was closed: "later" (show again in a few days), "never", or "done" (they went to install it). */
internal enum class InviteClose { LATER, NEVER, DONE }

/**
 * The GitHub edition's invitation to test Horizon Maps on Google Play: two steps (join the testers group, install from Play), a QR
 * code of the group page for head units without a browser, "Not now" and "Don't show again". Until the test is open, the Install
 * button sits behind a "Coming soon" sign, like a shop that's about to open. Wide (two panes) in landscape, one column in portrait.
 */
@Composable
internal fun PlayInviteDialog(dark: Boolean, onClose: (InviteClose) -> Unit) {
    val v = if (dark) DarkVw else LightVw
    val ctx = LocalContext.current
    val conf = LocalConfiguration.current
    val wide = conf.screenWidthDp > conf.screenHeightDp && conf.screenWidthDp >= 640
    val blue = if (dark) Color(0xFF8FB6FF) else Color(0xFF1F5FBF)
    val headBg = if (dark) Color(0xFF1F2E46) else Color(0xFFE2ECFB)
    val headText = if (dark) Color(0xFFD3DDEB) else Color(0xFF2C3E5C)
    val qr = remember { donateQr(PLAY_GROUP_URL, 400) }
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { open = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { playTestOpen() } }
    Dialog(onDismissRequest = { onClose(InviteClose.LATER) }, properties = DialogProperties(usePlatformDefaultWidth = !wide)) {
        val shape = RoundedCornerShape(28.dp)
        val maxH = (conf.screenHeightDp.dp - 32.dp).coerceAtLeast(240.dp)
        val head: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(headBg).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(blue.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Shop, null, tint = blue, modifier = Modifier.size(24.dp))
                }
                Text("Aidez à tester Horizon Maps sur Google Play", color = v.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp)
                Text("Horizon Nav arrive sur le Play Store sous le nom Horizon Maps, et Google demande d’abord au moins 12 testeurs pendant 14 jours. C’est la même appli, installée à part, elle ne touche donc pas à celle-ci. Gardez-la installée 14 jours et utilisez-la de temps en temps.",
                    color = headText, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        val steps: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((i, s) in listOf("Rejoignez le groupe de testeurs", "Installez Horizon Maps depuis le Play Store").withIndex()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(24.dp).clip(CircleShape).background(blue), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", color = if (dark) Color(0xFF0E1B30) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(s, color = v.text, fontSize = 15.sp)
                        }
                    }
                    Text("Connectez-vous avec le compte Google que vous utilisez pour le Play Store.", color = v.sub, fontSize = 13.sp, lineHeight = 18.sp)
                }
                if (qr != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(116.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(6.dp)) {
                        Image(qr, "QR code du groupe de testeurs", Modifier.size(104.dp), filterQuality = FilterQuality.None)
                    }
                    Text("Scannez avec votre téléphone", color = v.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        val buttons: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { openLink(ctx, PLAY_GROUP_URL) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = blue, contentColor = if (dark) Color(0xFF0E1B30) else Color.White)) {
                        Text("Rejoindre le groupe", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Button(onClick = { if (open) { openLink(ctx, PLAY_TEST_URL); onClose(InviteClose.DONE) } }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = v.tile, contentColor = v.text)) {
                            Text("Installer depuis Play", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        if (!open) ComingSoonSign(Modifier.clickable {
                            android.widget.Toast.makeText(ctx, "Ceci s’ouvrira dès que Google aura approuvé le test. Rejoignez le groupe maintenant pour être prêt.", android.widget.Toast.LENGTH_LONG).show()
                        })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pas maintenant", color = v.sub, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClose(InviteClose.LATER) }.padding(8.dp))
                    Text("Ne plus afficher", color = v.sub, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClose(InviteClose.NEVER) }.padding(8.dp))
                }
            }
        }
        if (wide) {
            val cardW = (conf.screenWidthDp.dp * 0.88f).coerceAtMost(980.dp)
            Row(Modifier.width(cardW).heightIn(max = maxH).clip(shape).background(v.surface).border(1.dp, v.line, shape).padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(0.45f)) { head() }
                Column(Modifier.weight(0.55f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) { steps(); buttons() }
            }
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = maxH).clip(shape).background(v.surface).border(1.dp, v.line, shape).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) { head(); steps(); buttons() }
        }
    }
}

/**
 * The "Coming soon" sign over the Install button: a yellow board with black and yellow hazard stripes along its edges, hung at
 * a slight angle, like the sign on a shop that's about to open.
 */
@Composable
private fun ComingSoonSign(modifier: Modifier = Modifier) {
    val yellow = Color(0xFFFFC928)
    val ink = Color(0xFF1A1A1A)
    Box(modifier.rotate(-5f).clip(RoundedCornerShape(6.dp)).background(yellow).drawBehind {
        // hazard stripes along the top and bottom edges
        val band = 6.dp.toPx(); val step = 12.dp.toPx()
        for (top in listOf(0f, size.height - band)) {
            var x = -band
            while (x < size.width + band) {
                val p = Path().apply {
                    moveTo(x, top + band); lineTo(x + step / 2f, top + band); lineTo(x + step / 2f + band, top); lineTo(x + band, top); close()
                }
                drawPath(p, ink)
                x += step
            }
        }
        drawLine(ink, Offset(0f, band), Offset(size.width, band), 1.dp.toPx())
        drawLine(ink, Offset(0f, size.height - band), Offset(size.width, size.height - band), 1.dp.toPx())
    }.border(2.dp, ink, RoundedCornerShape(6.dp)).padding(horizontal = 14.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text("BIENTÔT", color = ink, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
    }
}
