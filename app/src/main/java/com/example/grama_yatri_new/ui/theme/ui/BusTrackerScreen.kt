package com.gramayatri.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gramayatri.app.model.BusLocation
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.model.RouteStop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTrackerScreen(
    route: BusRoute,
    busLocation: BusLocation?,
    onBack: () -> Unit,
    onDriverMode: () -> Unit
) {
    val startStation = route.stops.firstOrNull()?.name ?: "N/A"
    val stopStation = route.stops.lastOrNull()?.name ?: "N/A"
    val fallbackStartMillis = androidx.compose.runtime.remember(route.id) { System.currentTimeMillis() }
    val routeStartMillis = busLocation?.updatedAtMillis ?: fallbackStartMillis
    val arrivingTimes = androidx.compose.runtime.remember(route.stops, routeStartMillis) {
        calculateArrivingTimes(route.stops, routeStartMillis)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.name) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5B47D6),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onDriverMode) {
                        Text("Driver", color = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFEAF3FF),
                            Color(0xFFF6EDFF)
                        )
                    )
                )
                .padding(padding)
        ) {
            if (busLocation != null) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2A47)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("LIVE GPS ACTIVE", color = Color(0xFF88D9FF), style = MaterialTheme.typography.labelSmall)
                            Text("Bus is currently moving", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            RouteEndpointsCard(
                startStation = startStation,
                stopStation = stopStation
            )

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                itemsIndexed(route.stops) { index, stop ->
                    val stationType = when (index) {
                        0 -> StationType.START
                        route.stops.lastIndex -> StationType.STOP
                        else -> StationType.INTERMEDIATE
                    }
                    // Logic to check if GPS is near this stop would go here
                    TimelineItem(
                        name = stop.name,
                        isPassed = false,
                        arrivingTime = arrivingTimes.getOrElse(index) { "--:--" },
                        stationType = stationType
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(name: String, isPassed: Boolean, arrivingTime: String, stationType: StationType) {
    val accentColor = when (stationType) {
        StationType.START -> Color(0xFF1AA75B)
        StationType.STOP -> Color(0xFFD7394D)
        StationType.INTERMEDIATE -> Color(0xFF2F7DFF)
    }
    val itemBackground = when (stationType) {
        StationType.START -> Color(0xFFEAF9F0)
        StationType.STOP -> Color(0xFFFFEEF1)
        StationType.INTERMEDIATE -> Color(0xFFEEF4FF)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = itemBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (isPassed) Color(0xFF1AA75B) else accentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StationTypeChip(stationType = stationType)
                    Text(
                        text = "Arriving: $arrivingTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteEndpointsCard(startStation: String, stopStation: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EndpointTile(
                label = "START STATION",
                stationName = startStation,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            EndpointTile(
                label = "STOP STATION",
                stationName = stopStation,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EndpointTile(
    label: String,
    stationName: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stationName, style = MaterialTheme.typography.titleSmall, color = contentColor)
        }
    }
}

@Composable
private fun StationTypeChip(stationType: StationType) {
    val (label, containerColor, contentColor) = when (stationType) {
        StationType.START -> Triple(
            "Start",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        StationType.STOP -> Triple(
            "Stop",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        StationType.INTERMEDIATE -> Triple(
            "Stopover",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

private enum class StationType {
    START,
    INTERMEDIATE,
    STOP
}

private fun calculateArrivingTimes(stops: List<RouteStop>, startMillis: Long): List<String> {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    var cumulativeMinutes = 0
    return stops.mapIndexed { index, stop ->
        if (index > 0) {
            cumulativeMinutes += stop.travelTimeFromPrev.coerceAtLeast(0)
        }
        val reachedAtMillis = startMillis + cumulativeMinutes * 60_000L
        formatter.format(Date(reachedAtMillis))
    }
}

@Preview(showBackground = true)
@Composable
private fun BusTrackerScreenPreview() {
    val sampleRoute = BusRoute(
        id = "route_preview",
        name = "Town Hall -> East Colony",
        description = "6 stops • Approx 30 mins",
        stops = listOf(
            RouteStop(id = "s1", name = "Town Hall", travelTimeFromPrev = 0),
            RouteStop(id = "s2", name = "Main Market", travelTimeFromPrev = 6),
            RouteStop(id = "s3", name = "School Circle", travelTimeFromPrev = 8),
            RouteStop(id = "s4", name = "East Colony", travelTimeFromPrev = 10)
        )
    )
    val sampleLocation = BusLocation(
        routeId = "route_preview",
        latitude = 12.9716,
        longitude = 77.5946,
        speed = 9.4f,
        updatedAtMillis = System.currentTimeMillis()
    )

    MaterialTheme {
        BusTrackerScreen(
            route = sampleRoute,
            busLocation = sampleLocation,
            onBack = {},
            onDriverMode = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineItemPreview() {
    MaterialTheme {
        TimelineItem(
            name = "Main Market",
            isPassed = true,
            arrivingTime = "09:42 AM",
            stationType = StationType.INTERMEDIATE
        )
    }
}
