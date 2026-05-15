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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.model.RouteStop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    routes: List<BusRoute>,
    errorMessage: String? = null,
    showAddRouteAction: Boolean = false,
    isAddingRoute: Boolean = false,
    onAddRoute: (String, String, String, (String?, String?) -> Unit) -> Unit = { _, _, _, _ -> },
    onRouteSelected: (BusRoute) -> Unit
) {
    var showAddRouteDialog by rememberSaveable { mutableStateOf(false) }
    var addRouteInfoMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var addRouteDialogError by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grama-Yatri Routes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5B47D6),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (showAddRouteAction) {
                FloatingActionButton(
                    onClick = {
                        addRouteInfoMessage = null
                        addRouteDialogError = null
                        showAddRouteDialog = true
                    },
                    containerColor = Color(0xFF5B47D6),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add route"
                    )
                }
            }
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

            addRouteInfoMessage?.let { message ->
                item {
                    SuccessCard(message = message)
                }
            }

            errorMessage?.let { message ->
                item {
                    ErrorCard(message = message)
                }
            }

            items(routes) { route ->
                RouteCard(route = route, onRouteSelected = onRouteSelected)
            }
        }
    }

    if (showAddRouteDialog && showAddRouteAction) {
        AddRouteDialog(
            isSaving = isAddingRoute,
            errorMessage = addRouteDialogError,
            onDismiss = {
                if (!isAddingRoute) {
                    showAddRouteDialog = false
                    addRouteDialogError = null
                }
            },
            onSave = { routeName, routeDescription, stopsInput ->
                addRouteDialogError = null
                onAddRoute(routeName, routeDescription, stopsInput) { successMessage, error ->
                    if (successMessage != null) {
                        addRouteInfoMessage = successMessage
                        addRouteDialogError = null
                        showAddRouteDialog = false
                    } else if (error != null) {
                        addRouteInfoMessage = null
                        addRouteDialogError = error
                    }
                }
            }
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFFB00020),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun SuccessCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F7EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFF156C2F),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun AddRouteDialog(
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var routeName by rememberSaveable { mutableStateOf("") }
    var routeDescription by rememberSaveable { mutableStateOf("") }
    var stopDrafts by rememberSaveable(stateSaver = stopDraftListSaver) {
        mutableStateOf(
            listOf(
                StopDraft(name = "Start Stop", minutesFromPrevious = "0"),
                StopDraft(name = "Middle Stop", minutesFromPrevious = "8"),
                StopDraft(name = "End Stop", minutesFromPrevious = "10")
            )
        )
    }
    var localErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val displayedErrorMessage = localErrorMessage ?: errorMessage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add route") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = {
                        routeName = it
                        localErrorMessage = null
                    },
                    label = { Text("Route name") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = routeDescription,
                    onValueChange = {
                        routeDescription = it
                        localErrorMessage = null
                    },
                    label = { Text("Description") },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    color = Color(0xFFF5F7FF),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Stops",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Add, remove, or edit stops. The first stop is always treated as route start.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        stopDrafts.forEachIndexed { index, stopDraft ->
                            StopEditorCard(
                                index = index,
                                stopDraft = stopDraft,
                                isSaving = isSaving,
                                canDelete = stopDrafts.size > 2,
                                onUpdate = { updated ->
                                    localErrorMessage = null
                                    stopDrafts = stopDrafts.toMutableList().also { drafts ->
                                        drafts[index] = updated
                                    }
                                },
                                onDelete = {
                                    localErrorMessage = null
                                    if (stopDrafts.size > 2) {
                                        stopDrafts = stopDrafts.filterIndexed { currentIndex, _ ->
                                            currentIndex != index
                                        }
                                    }
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                localErrorMessage = null
                                stopDrafts = stopDrafts + StopDraft(minutesFromPrevious = "5")
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Add another stop")
                        }
                    }
                }
                if (!displayedErrorMessage.isNullOrBlank()) {
                    Text(
                        text = displayedErrorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    localErrorMessage = validateStopDrafts(stopDrafts)
                    if (localErrorMessage == null) {
                        onSave(routeName, routeDescription, stopDrafts.toStopsInput())
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StopEditorCard(
    index: Int,
    stopDraft: StopDraft,
    isSaving: Boolean,
    canDelete: Boolean,
    onUpdate: (StopDraft) -> Unit,
    onDelete: () -> Unit
) {
    val isFirstStop = index == 0
    val title = if (isFirstStop) {
        "Stop ${index + 1} (Start)"
    } else {
        "Stop ${index + 1}"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isFirstStop) {
                    IconButton(
                        onClick = onDelete,
                        enabled = !isSaving && canDelete
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete stop",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = stopDraft.name,
                onValueChange = { onUpdate(stopDraft.copy(name = it)) },
                label = { Text("Stop name") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (isFirstStop) "0" else stopDraft.minutesFromPrevious,
                    onValueChange = {
                        if (!isFirstStop) {
                            onUpdate(stopDraft.copy(minutesFromPrevious = it))
                        }
                    },
                    label = { Text("Mins from previous") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isSaving && !isFirstStop,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = stopDraft.latitude,
                    onValueChange = { onUpdate(stopDraft.copy(latitude = it)) },
                    label = { Text("Latitude (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = stopDraft.longitude,
                onValueChange = { onUpdate(stopDraft.copy(longitude = it)) },
                label = { Text("Longitude (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class StopDraft(
    val name: String = "",
    val minutesFromPrevious: String = "",
    val latitude: String = "",
    val longitude: String = ""
)

private val stopDraftListSaver = run {
    val separator = '\u0001'
    androidx.compose.runtime.saveable.listSaver<List<StopDraft>, String>(
        save = { drafts ->
            drafts.map { draft ->
                listOf(
                    draft.name,
                    draft.minutesFromPrevious,
                    draft.latitude,
                    draft.longitude
                ).joinToString(separator.toString())
            }
        },
        restore = { saved ->
            saved.map { serialized ->
                val parts = serialized.split(separator)
                StopDraft(
                    name = parts.getOrElse(0) { "" },
                    minutesFromPrevious = parts.getOrElse(1) { "" },
                    latitude = parts.getOrElse(2) { "" },
                    longitude = parts.getOrElse(3) { "" }
                )
            }
        }
    )
}

private fun validateStopDrafts(stopDrafts: List<StopDraft>): String? {
    if (stopDrafts.size < 2) {
        return "Add at least 2 stops to create a route."
    }

    stopDrafts.forEachIndexed { index, stopDraft ->
        if (stopDraft.name.trim().isBlank()) {
            return "Stop ${index + 1} name is required."
        }

        if (index > 0) {
            if (stopDraft.minutesFromPrevious.trim().toIntOrNull() == null) {
                return "Stop ${index + 1} must include travel minutes as a whole number."
            }
        }

        if (stopDraft.latitude.trim().isNotBlank() && stopDraft.latitude.trim().toDoubleOrNull() == null) {
            return "Stop ${index + 1} has invalid latitude."
        }

        if (stopDraft.longitude.trim().isNotBlank() && stopDraft.longitude.trim().toDoubleOrNull() == null) {
            return "Stop ${index + 1} has invalid longitude."
        }
    }

    return null
}

private fun List<StopDraft>.toStopsInput(): String {
    return mapIndexed { index, stopDraft ->
        val name = stopDraft.name.trim()
        val minutes = if (index == 0) "0" else stopDraft.minutesFromPrevious.trim()
        val latitude = stopDraft.latitude.trim()
        val longitude = stopDraft.longitude.trim()

        val columns = mutableListOf(name, minutes)
        if (latitude.isNotBlank() || longitude.isNotBlank()) {
            columns += latitude
            columns += longitude
        }
        columns.joinToString("|")
    }.joinToString("\n")
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
            errorMessage = null,
            showAddRouteAction = true,
            isAddingRoute = false,
            onAddRoute = { _, _, _, _ -> },
            onRouteSelected = {}
        )
    }
}
