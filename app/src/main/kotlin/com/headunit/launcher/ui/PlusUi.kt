package com.headunit.launcher.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.headunit.launcher.L
import com.headunit.launcher.Plus
import com.headunit.launcher.PlusBilling

// Horizon Maps Plus (Play edition only, once the key server is set up): the subscribe screen, the setup choice between Plus and
// the free OpenStreetMap version, and the banner at the top of Settings. Laid out as the mockup agreed on 18 November.

private val PlusBlue = Color(0xFF3C7BFF)

private fun android.content.Context.activity(): android.app.Activity? {
    var c: android.content.Context? = this
    while (c is android.content.ContextWrapper) { if (c is android.app.Activity) return c; c = c.baseContext }
    return null
}

/** Google Play's own page for managing (or cancelling) the subscription. */
internal fun openPlusManage(ctx: android.content.Context) {
    try {
        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://play.google.com/store/account/subscriptions?sku=${PlusBilling.PRODUCT_ID}&package=${ctx.packageName}"))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { }
}

/** The subscribe screen: what Plus adds over the free version, the two plans, and the free trial. */
@Composable
internal fun PlusDialog(onClose: () -> Unit) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    val conf = LocalConfiguration.current
    var plan by remember { mutableStateOf("monthly") }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.widthIn(max = 460.dp).fillMaxWidth().padding(16.dp).heightIn(max = (conf.screenHeightDp.dp - 32.dp).coerceAtLeast(240.dp))
                .clip(RoundedCornerShape(26.dp)).background(c.panel).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(PlusBlue.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = PlusBlue, modifier = Modifier.size(22.dp))
                }
                Text("Horizon Maps Plus", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
            }
            // what each gets
            val rows = listOf(
                Triple("Carte", "OpenStreetMap", "3D"),
                Triple("Vue satellite", "", "✓"),
                Triple("Trafic en direct et retards", "", "✓"),
                Triple("Itinéraires qui évitent les bouchons", "", "✓"),
                Triple("Voies et panneaux d’échangeur", "de base", "✓"),
                Triple("Recherche d’adresses et de lieux", "OpenStreetMap", "✓"),
                Triple("Radars et limitations de vitesse", "✓", "✓"),
                Triple("Android Auto", "✓", "✓"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row { Box(Modifier.weight(1.6f)); Text("Gratuit", color = c.sub, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                    Text("Plus", color = PlusBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f)) }
                for ((what, free, plus) in rows) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(what, color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1.6f))
                    Text(free.ifBlank { "—" }, color = if (free == "✓") Good else c.sub, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                    Text(plus, color = if (plus == "✓") Good else c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                }
            }
            if (Plus.active) {
                Text("Plus est activé. Merci de soutenir Horizon Maps !", color = Good, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                SButton("Gérer dans Google Play") { openPlusManage(ctx) }
            } else if (Plus.offers.isEmpty()) {
                Text(Plus.problem.ifBlank { "Récupération des prix sur Google Play…" }, color = c.sub, fontSize = 14.sp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (o in Plus.offers) {
                        val sel = o.plan == plan
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (sel) PlusBlue.copy(alpha = 0.14f) else c.tile)
                            .border(if (sel) 2.dp else 1.dp, if (sel) PlusBlue else c.line, RoundedCornerShape(14.dp)).clickable { plan = o.plan }.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(o.price, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(if (o.plan == "yearly") "par an · 20 % d’économie" else "par mois", color = c.sub, fontSize = 12.sp)
                        }
                    }
                }
                val pick = Plus.offers.firstOrNull { it.plan == plan } ?: Plus.offers.first()
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(PlusBlue).clickable {
                    ctx.activity()?.let { PlusBilling.buy(it, pick) }
                }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text(if (pick.trialDays > 0) "Commencer l’essai gratuit de ${pick.trialDays} jours" else "S’abonner", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Résiliable à tout moment dans Google Play. Sans Plus, Horizon Maps continue de fonctionner avec OpenStreetMap.", color = c.sub, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Fermer", color = c.sub, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable(onClick = onClose).padding(8.dp))
        }
    }
}

/** First-run setup: Plus or the free version. */
@Composable
internal fun PlusChoice(onPlus: () -> Unit, onFree: () -> Unit) {
    val c = LocalVw.current
    val monthly = Plus.offers.firstOrNull { it.plan == "monthly" }?.price
    ChoiceCard(Icons.Rounded.AutoAwesome, "Horizon Maps Plus", if (monthly != null) "à partir de $monthly par mois" else "",
        "Carte 3D et satellite, trafic en direct, itinéraires plus malins et recherche complète.", Plus.active, PlusBlue, onPlus)
    ChoiceCard(Icons.Rounded.Map, "Gratuit", "", "Carte, recherche et itinéraires OpenStreetMap (depuis un pack pays téléchargé). Pas de trafic. Passez à Plus à tout moment.", !Plus.active, Good, onFree)
    @Suppress("UNUSED_VARIABLE") val unused = c
}

@Composable
private fun ChoiceCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tag: String, sub: String, on: Boolean, tint: Color, onClick: () -> Unit) {
    val c = LocalVw.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) tint.copy(alpha = 0.12f) else c.tile)
        .border(if (on) 2.dp else 1.dp, if (on) tint else c.line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp).weight(1f))
            if (tag.isNotBlank()) Text(tag, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(tint).padding(horizontal = 8.dp, vertical = 2.dp))
            if (on) Icon(Icons.Rounded.Check, null, tint = tint, modifier = Modifier.padding(start = 6.dp).size(18.dp))
        }
        Text(sub, color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The top of Settings on Play: Plus, or how to get it. */
@Composable
internal fun PlusBanner(onClick: () -> Unit) {
    val c = LocalVw.current
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(18.dp))
            .background(PlusBlue.copy(alpha = if (c.dark) 0.14f else 0.10f)).border(1.dp, PlusBlue.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(PlusBlue.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = PlusBlue, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(if (Plus.active) "Horizon Maps Plus est activé" else "Obtenir Horizon Maps Plus", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(if (Plus.active) "Merci pour votre abonnement · touchez pour gérer" else "Carte 3D, satellite, trafic en direct et itinéraires plus malins", color = c.sub, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = PlusBlue, modifier = Modifier.size(20.dp))
    }
}
