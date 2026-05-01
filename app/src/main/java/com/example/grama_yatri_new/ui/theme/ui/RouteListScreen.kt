package com.gramayatri.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.model.RouteStop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    routes: List<BusRoute>,
    onRouteSelected: (BusRoute) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grama-Yatri Routes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5B47D6),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF1F6FF),
                            Color(0xFFF8F2FF)
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RouteSummaryCard(totalRoutes = routes.size)
            }

            items(routes) { route ->
                RouteCard(route = route, onRouteSelected = onRouteSelected)
            }
        }
    }
}

@Composable
private fun RouteSummaryCard(totalRoutes: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EDFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Choose your route",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$totalRoutes routes available right now",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RouteCard(
    route: BusRoute,
    onRouteSelected: (BusRoute) -> Unit
) {
    val startStation = route.stops.firstOrNull()?.name ?: "N/A"
    val stopStation = route.stops.lastOrNull()?.name ?: "N/A"
    val totalMinutes = route.stops.drop(1).sumOf { it.travelTimeFromPrev.coerceAtLeast(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onRouteSelected(route) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEDF1FF),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsBus,
                            contentDescription = null,
                            tint = Color(0xFF4D63FF)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = route.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(
                    text = "${route.stops.size} stops",
                    containerColor = Color(0xFFEAF4FF),
                    contentColor = Color(0xFF145AA3)
                )
                StatPill(
                    text = "$totalMinutes mins",
                    containerColor = Color(0xFFEEF9F2),
                    contentColor = Color(0xFF1C7C43)
                )
            }

            Surface(
                color = Color(0xFFF6F3FF),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EndpointLabel(
                        title = "Start",
                        stationName = startStation,
                        accentColor = Color(0xFF1AA75B)
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF8D80D8),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    EndpointLabel(
                        title = "Stop",
                        stationName = stopStation,
                        accentColor = Color(0xFFD7394D)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(shape = CircleShape, color = containerColor) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EndpointLabel(
    title: String,
    stationName: String,
    accentColor: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
            Text(
                text = stationName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RouteListScreenPreview() {
    val sampleRoutes = listOf(
        BusRoute(
            id = "route_1",
            name = "Town Hall -> East Colony",
            description = "6 stops • Approx 30 mins",
            stops = listOf(
                RouteStop(id = "s1", name = "Town Hall", travelTimeFromPrev = 0),
                RouteStop(id = "s2", name = "Main Market", travelTimeFromPrev = 8),
                RouteStop(id = "s3", name = "Hospital Road", travelTimeFromPrev = 7)
            )
        ),
        BusRoute(
            id = "route_2",
            name = "Railway Station -> Lake View",
            description = "7 stops • Approx 40 mins",
            stops = listOf(
                RouteStop(id = "s1", name = "Railway Station", travelTimeFromPrev = 0),
                RouteStop(id = "s2", name = "City Park", travelTimeFromPrev = 11),
                RouteStop(id = "s3", name = "Lake View", travelTimeFromPrev = 14)
            )
        )
    )

    MaterialTheme {
        RouteListScreen(
            routes = sampleRoutes,
            onRouteSelected = {}
        )
    }
}
