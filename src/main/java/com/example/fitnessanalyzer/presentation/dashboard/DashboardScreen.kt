package com.example.fitnessanalyzer.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnessanalyzer.domain.model.*
import com.example.fitnessanalyzer.presentation.theme.FitnessIntelligenceTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state: DashboardState by viewModel.viewState.collectAsStateWithLifecycle()
    
    var showProfileDialog by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(true) }
    var selectedWorkout by remember { mutableStateOf<WorkoutSession?>(null) }
    val sheetState = rememberModalBottomSheetState()

    FitnessIntelligenceTheme(darkTheme = isDarkMode) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Fitness Physiology", 
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { isDarkMode = !isDarkMode }) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, 
                                contentDescription = "Toggle Theme"
                            )
                        }
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Profile")
                        }
                        IconButton(onClick = { viewModel.triggerLocalSyncPipeline() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            if (showProfileDialog) {
                UserProfileDialog(
                    currentProfile = viewModel.getUserProfile(),
                    onDismiss = { showProfileDialog = false },
                    onSave = { 
                        viewModel.saveUserProfile(it)
                        showProfileDialog = false
                    }
                )
            }

            if (selectedWorkout != null) {
                WorkoutDetailSheet(
                    session = selectedWorkout!!,
                    sheetState = sheetState,
                    onDismiss = { selectedWorkout = null }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (val current = state) {
                    is DashboardState.Loading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                    is DashboardState.Error -> Text(current.reason, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    is DashboardState.Success -> {
                        if (current.sessions.isEmpty()) {
                            EmptyState(Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    SummaryHeader(current.sessions)
                                }
                                items(current.sessions) { item ->
                                    EnhancedWorkoutCard(
                                        session = item,
                                        onClick = { selectedWorkout = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryHeader(sessions: List<WorkoutSession>) {
    val avgVo2 = sessions.mapNotNull { it.estimatedVo2Max }.average().takeIf { !it.isNaN() } ?: 0.0
    val totalDistance = sessions.mapNotNull { it.distanceMeters }.sum() / 1000f
    val totalTrimp = sessions.mapNotNull { it.trimpScore }.sum()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A73E8), Color(0xFF0D47A1))
                    )
                )
                .padding(28.dp)
        ) {
            Column {
                Text("CARDIOVASCULAR BASELINE", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.1f", avgVo2),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(" ml/kg/min", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryStatItem("TOTAL DISTANCE", String.format("%.1f km", totalDistance))
                    SummaryStatItem("TOTAL LOAD", String.format("%.0f", totalTrimp))
                    SummaryStatItem("SESSIONS", "${sessions.size}")
                }
            }
        }
    }
}

@Composable
fun SummaryStatItem(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EnhancedWorkoutCard(session: WorkoutSession, onClick: () -> Unit) {
    val date = Date(session.startTimeMillis)
    val dayFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(date)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).background(workoutColor(session.workoutType).copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(workoutIcon(session.workoutType), null, tint = workoutColor(session.workoutType), modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.workoutType.name.lowercase().capitalize(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("$dayFormat", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                TrainingEffectBadge(session.trainingEffectLabel ?: "Analyzing")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Metrics
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MainStatItem("VO2 MAX", String.format("%.1f", session.estimatedVo2Max ?: 0f), Color(0xFF1A73E8))
                MainStatItem("LOAD", String.format("%.0f", session.trimpScore ?: 0f), Color(0xFFD93025))
                MainStatItem("RECOVERY", "${session.estimatedRecoveryHours ?: 0}h", Color(0xFF1E8E3E))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer row with informatic Dynamics
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                InformaticItem(Icons.Default.Speed, String.format("%.1f km/h", if (session.durationSeconds > 0) (session.distanceMeters ?: 0f) / session.durationSeconds * 3.6 else 0.0))
                InformaticItem(Icons.Default.Timer, "${session.durationSeconds / 60} min")
                InformaticItem(Icons.Default.Favorite, "${session.averageHeartRate?.toInt() ?: "--"}")
                InformaticItem(Icons.Default.DirectionsRun, "${session.totalSteps ?: "--"}")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            ZoneDistributionBar(session.hrZoneDistribution)
        }
    }
}

@Composable
fun InformaticItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailSheet(session: WorkoutSession, sheetState: SheetState, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
        ) {
            Text("${session.workoutType.name.capitalize()} Scientific Review", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (session.heartRateSamples.isNotEmpty()) {
                Text("Heart Rate Intensity Trend", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                SparklineChart(
                    data = session.heartRateSamples.map { it.bpm.toFloat() },
                    color = Color(0xFFD93025),
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
                Spacer(Modifier.height(32.dp))
            }

            // Research comparison (The core requirement)
            Text("Physiological Model Comparison", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    ModelCompareRow("Uth HR Ratio Method", String.format("%.1f", session.uthVo2Max ?: 0f))
                    ModelCompareRow("FRIEND Efficiency Model", String.format("%.1f", session.friendVo2Max ?: 0f))
                    ModelCompareRow("Cooper 12-min Volume", String.format("%.1f", session.cooperVo2Max ?: 0f))
                    ModelCompareRow("ACSM Metabolic Equation", String.format("%.1f", session.acsmVo2Max ?: 0f))
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    ModelCompareRow("Banister TRIMP (Exponential)", String.format("%.1f", session.trimpScore ?: 0f))
                    ModelCompareRow("Edwards TRIMP (Linear Zone)", String.format("%.1f", session.edwardsTrimp ?: 0f))
                    ModelCompareRow("Lucia TRIMP (Threshold)", String.format("%.1f", session.luciaTrimp ?: 0f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Complete Biological Metrics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    DetailRow("Average Heart Rate", "${session.averageHeartRate?.toInt() ?: "--"} bpm")
                    DetailRow("Peak Heart Rate", "${session.peakHeartRate ?: "--"} bpm")
                    DetailRow("Scientific Resting HR", "${session.restingHeartRate ?: "--"} bpm")
                    DetailRow("HR Recovery (1m drop)", "${session.heartRateRecovery ?: "--"} bpm")
                    DetailRow("Total Calories Burned", "${session.caloriesKcal?.toInt() ?: "--"} kcal")
                    DetailRow("Elevation Gained", String.format("%.1f m", session.elevationGainedMeters ?: 0f))
                    DetailRow("Average Power Output", if (session.averagePowerWatts != null) "${session.averagePowerWatts.toInt()} W" else "--")
                    DetailRow("Recovery Duration", "${session.estimatedRecoveryHours ?: "--"} hours")
                }
            }

            if (session.workoutType == WorkoutType.RUNNING || session.workoutType == WorkoutType.TREADMILL) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Mechanical GAIT Analysis", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        DetailRow("Average Cadence", "${session.averageCadence?.toInt() ?: 0} spm")
                        DetailRow("Average Stride Length", String.format("%.2f m", session.averageStrideLengthMeters ?: 0f))
                        val pace = session.averagePaceMinPerKm ?: 0f
                        val paceM = pace.toInt()
                        val paceS = ((pace - paceM) * 60).toInt()
                        DetailRow("Moving Pace", if (pace > 0) String.format("%d:%02d /km", paceM, paceS) else "--")
                        DetailRow("Aerobic Decoupling (Drift)", String.format("%.1f%%", session.heartRateDriftPercent ?: 0f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Intensity Zone Distribution", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            ZoneDistributionBar(session.hrZoneDistribution)
            Spacer(modifier = Modifier.height(12.dp))
            LegendGrid(session.hrZoneDistribution)
        }
    }
}

@Composable
fun LegendGrid(dist: HrZoneDistribution?) {
    if (dist == null) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LegendItem(Color(0xFF81C995), "Zone 1: Recovery", dist.zone1RecoverySec)
        LegendItem(Color(0xFF34A853), "Zone 2: Aerobic", dist.zone2AerobicSec)
        LegendItem(Color(0xFFFBBC04), "Zone 3: Tempo", dist.zone3TempoSec)
        LegendItem(Color(0xFFE37400), "Zone 4: Threshold", dist.zone4ThresholdSec)
        LegendItem(Color(0xFFD93025), "Zone 5: Anaerobic", dist.zone5AnaerobicSec)
    }
}

@Composable
fun LegendItem(color: Color, label: String, sec: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("${sec/60}m ${sec%60}s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ModelCompareRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SparklineChart(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val min = data.minOrNull() ?: 0f
    val max = data.maxOrNull() ?: 1f
    val range = (max - min).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1).coerceAtLeast(1)
        
        val path = Path()
        data.forEachIndexed { i, value ->
            val x = i * stepX
            val y = height - ((value - min) / range * height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
        
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent)))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ZoneDistributionBar(dist: HrZoneDistribution?) {
    if (dist == null) return
    val total = (dist.zone1RecoverySec + dist.zone2AerobicSec + dist.zone3TempoSec + dist.zone4ThresholdSec + dist.zone5AnaerobicSec).toFloat()
    if (total == 0f) return

    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp).clip(CircleShape)) {
        val width = size.width
        val h = size.height
        drawRect(color = Color.LightGray.copy(alpha = 0.2f), size = Size(width, h))
        var currentX = 0f
        fun drawZone(p: Float, color: Color) {
            val w = width * p
            drawRect(color = color, topLeft = Offset(currentX, 0f), size = Size(w, h))
            currentX += w
        }
        drawZone(dist.zone1RecoverySec/total, Color(0xFF81C995))
        drawZone(dist.zone2AerobicSec/total, Color(0xFF34A853))
        drawZone(dist.zone3TempoSec/total, Color(0xFFFBBC04))
        drawZone(dist.zone4ThresholdSec/total, Color(0xFFE37400))
        drawZone(dist.zone5AnaerobicSec/total, Color(0xFFD93025))
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Analytics, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Data Found", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
        Text("Tap sync to fetch Health Connect records", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
    }
}

fun workoutIcon(type: WorkoutType): ImageVector {
    return when (type) {
        WorkoutType.WALKING -> Icons.Default.DirectionsWalk
        WorkoutType.RUNNING -> Icons.Default.DirectionsRun
        WorkoutType.CYCLING -> Icons.Default.DirectionsBike
        WorkoutType.STRENGTH -> Icons.Default.FitnessCenter
        else -> Icons.Default.MoreHoriz
    }
}

fun workoutColor(type: WorkoutType): Color {
    return when (type) {
        WorkoutType.RUNNING -> Color(0xFFD93025)
        WorkoutType.CYCLING -> Color(0xFF1A73E8)
        WorkoutType.WALKING -> Color(0xFF1E8E3E)
        else -> Color(0xFF70757A)
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

@Composable
fun UserProfileDialog(currentProfile: UserProfile, onDismiss: () -> Unit, onSave: (UserProfile) -> Unit) {
    var age by remember { mutableStateOf(currentProfile.age?.toString() ?: "") }
    var weight by remember { mutableStateOf(currentProfile.weightKg?.toString() ?: "") }
    var height by remember { mutableStateOf(currentProfile.heightCm?.toString() ?: "") }
    var isMale by remember { mutableStateOf(currentProfile.isMale ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Physical Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isMale, onClick = { isMale = true }); Text("Male")
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = !isMale, onClick = { isMale = false }); Text("Female")
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(UserProfile(age.toIntOrNull(), weight.toFloatOrNull(), height.toFloatOrNull(), isMale)) }) { Text("Save") } }
    )
}

@Composable
fun TrainingEffectBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun MainStatItem(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontWeight = FontWeight.ExtraBold)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
    }
}
