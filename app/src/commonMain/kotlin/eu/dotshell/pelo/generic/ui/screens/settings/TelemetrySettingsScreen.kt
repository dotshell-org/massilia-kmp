package eu.dotshell.pelo.generic.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.dotshell.pelo.generic.data.telemetry.DailyReportState
import eu.dotshell.pelo.generic.data.telemetry.TelemetryEvent
import eu.dotshell.pelo.generic.ui.theme.PrimaryColor
import eu.dotshell.pelo.generic.ui.theme.SecondaryColor
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetrySettingsScreen(
    snapshot: DailyReportState?,
    onBackClick: () -> Unit,
    onWipeHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    val prettyJson = remember {
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }
    }

    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Supprimer l'historique ?") },
            text = { Text("Voulez-vous vraiment supprimer tout votre historique local (favoris et trajets) ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWipeHistory()
                        showWipeConfirmDialog = false
                    }
                ) {
                    Text("Oui", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Non", color = SecondaryColor)
                }
            },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = SecondaryColor,
            textContentColor = Color.Gray
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Confidentialité",
                        color = SecondaryColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = SecondaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor
                )
            )
        },
        containerColor = PrimaryColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Compact delete history button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { showWipeConfirmDialog = true }),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Supprimer l'historique local",
                        color = Color(0xFFEF4444),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Données collectées aujourd'hui",
                color = SecondaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            when (snapshot) {
                null -> InfoCard(
                    title = "Aucune donnée",
                    body = "Aucun événement n'a encore été enregistré."
                )
                else -> {
                    // Compact Side-by-Side Metadata Cards
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = snapshot.day, color = SecondaryColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = "${snapshot.events.size} évènements", color = SecondaryColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    EventBreakdownCard(events = snapshot.events)
                    Spacer(Modifier.height(12.dp))
                    RawJsonCard(text = prettyJson.encodeToString(DailyReportState.serializer(), snapshot))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EventBreakdownCard(events: List<TelemetryEvent>) {
    if (events.isEmpty()) return

    val grouped = events.groupBy { eventShortLabel(it::class.simpleName ?: "Unknown") }
        .map { (label, list) ->
            val firstEvent = list.first()
            val (icon, color) = eventIconAndColor(firstEvent::class.simpleName ?: "Unknown")
            Triple(label, icon, list.size)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Événements", color = SecondaryColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            grouped.forEach { (label, icon, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = SecondaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(label, color = SecondaryColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("$count", color = SecondaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun RawJsonCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Payload brut (JSON)",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = text,
                color = Color(0xFFD0D0D5),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = SecondaryColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(body, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

private fun eventIconAndColor(simpleName: String): Pair<ImageVector, Color> = when (simpleName) {
    "SessionOpened", "SessionClosed" -> Icons.Default.AccountCircle to Color(0xFF9CA3AF)
    "SearchStop", "StopClicked" -> Icons.Default.Place to Color(0xFFEF4444)
    "SearchLine", "LineClicked" -> Icons.Default.Map to Color(0xFF3B82F6)
    "SearchItinerary", "ItineraryCalculated", "ItineraryChosen" -> Icons.Default.Directions to Color(0xFFF59E0B)
    "AlertSubmitted", "AlertRead" -> Icons.Default.Warning to Color(0xFFF59E0B)
    else -> Icons.Default.Notifications to Color(0xFF9CA3AF)
}

private fun eventShortLabel(simpleName: String): String = when (simpleName) {
    "SessionOpened", "SessionClosed" -> "Sessions"
    "SearchStop", "StopClicked" -> "Arrêts"
    "SearchLine", "LineClicked" -> "Lignes"
    "SearchItinerary", "ItineraryCalculated", "ItineraryChosen" -> "Itinéraires"
    "AlertSubmitted", "AlertRead" -> "Alertes"
    else -> simpleName
}
