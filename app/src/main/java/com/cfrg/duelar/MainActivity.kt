package com.cfrg.duelar

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.media.MediaPlayer
import android.widget.Toast
import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect

enum class Screen {
    Splash, Welcome, LossType, Test, Intensity, Result, Home, DayList, DayDetail, History, Insights, Final, Settings
}

enum class GriefStage(val label: String) {
    Shock("Shock y negación"),
    Anger("Rabia"),
    Guilt("Culpa y negociación"),
    Sadness("Tristeza"),
    Acceptance("Aceptación y reconstrucción")
}

data class TestOption(
    val text: String,
    val stage: GriefStage
)

data class TestQuestion(
    val question: String,
    val options: List<TestOption>
)

data class DayContent(
    val title: String,
    val reading: String,
    val exercise: String,
    val journal: String,
    val meditation: String,
    val action: String
)

data class JournalInsight(
    val mainText: String,
    val secondaryText: String,
    val evolutionText: String,
    val suggestionText: String
)

data class TodaySignal(
    val title: String,
    val body: String
)

data class InsightPattern(
    val key: String,
    val title: String,
    val score: Int,
    val days: String,
    val message: String,
    val suggestion: String
)

data class EmotionDay(
    val day: Int,
    val label: String,
    val intensity: Int,
    val color: Color,
    val hasData: Boolean
)

class MainActivity : ComponentActivity() {

    private var ambientPlayer: MediaPlayer? = null

    private val bg = Color(0xFFF8FBFA)
    private val bgSoftBlue = Color(0xFFEAF4F8)
    private val bgWarm = Color(0xFFFFFAF2)
    private val card = Color(0xFFFFFFFF)
    private val ink = Color(0xFF243B40)
    private val muted = Color(0xFF667F84)
    private val accent = Color(0xFF6FA9B6)
    private val accentSoft = Color(0xFFD8EEF2)
    private val coralSoft = Color(0xFFFFDDD2)
    private val lavenderSoft = Color(0xFFEDE8FF)
    private val soft = Color(0xFFDDEEF1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2001)
        }

        val prefs = getSharedPreferences("duelar_compose", Context.MODE_PRIVATE)

        setContent {
            var screen by remember { mutableStateOf(Screen.Splash) }

            LaunchedEffect(Unit) {
                delay(2500)
                screen = if (prefs.getBoolean("onboardingDone", false)) Screen.Home else Screen.Welcome
            }
            var selectedLoss by remember { mutableStateOf(prefs.getString("lossType", "") ?: "") }
            var currentDay by remember { mutableIntStateOf(prefs.getInt("currentDay", 1)) }
            var selectedDay by remember { mutableIntStateOf(currentDay) }
            var intensity by remember { mutableIntStateOf(prefs.getInt("intensity", 5)) }
            var testIndex by remember { mutableIntStateOf(0) }
            var scores by remember { mutableStateOf(mutableMapOf<GriefStage, Int>().withDefault { 0 }) }
            var ambientSound by remember { mutableStateOf("Zen instrumental") }
            var isAmbientPlaying by remember { mutableStateOf(false) }

            var primaryStage by remember {
                mutableStateOf(
                    prefs.getString("primaryStage", null)?.let { saved ->
                        GriefStage.values().firstOrNull { it.name == saved }
                    } ?: GriefStage.Sadness
                )
            }

            var secondaryStage by remember {
                mutableStateOf(
                    prefs.getString("secondaryStage", null)?.let { saved ->
                        GriefStage.values().firstOrNull { it.name == saved }
                    } ?: GriefStage.Shock
                )
            }

            Surface(color = bg) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        if (initialState == Screen.Splash || targetState == Screen.Splash) {
                            fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
                        } else {
                            val enterTransition = if (targetState.ordinal > initialState.ordinal) {
                                slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)) { it } + fadeIn(animationSpec = tween(400))
                            } else {
                                slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)) { -it } + fadeIn(animationSpec = tween(400))
                            }
                            val exitTransition = if (targetState.ordinal > initialState.ordinal) {
                                slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)) { -it / 3 } + fadeOut(animationSpec = tween(350))
                            } else {
                                slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)) { it / 3 } + fadeOut(animationSpec = tween(350))
                            }
                            enterTransition togetherWith exitTransition
                        }
                    },
                    label = "screenTransition"
                ) { currentScreen ->
                    when (currentScreen) {
                    Screen.Splash -> SplashScreen()
                    Screen.Welcome -> WelcomeScreen(
                        onStart = { screen = Screen.LossType }
                    )

                    Screen.LossType -> LossTypeScreen(
                        selectedLoss = selectedLoss,
                        onSelect = {
                            selectedLoss = it
                            prefs.edit().putString("lossType", it).apply()
                        },
                        onContinue = {
                            if (selectedLoss.isNotBlank()) {
                                testIndex = 0
                                scores = mutableMapOf<GriefStage, Int>().withDefault { 0 }
                                screen = Screen.Test
                            }
                        }
                    )

                    Screen.Test -> TestScreen(
                        questionIndex = testIndex,
                        question = testQuestions[testIndex],
                        total = testQuestions.size,
                        onAnswer = { stage ->
                            val updated = scores.toMutableMap().withDefault { 0 }
                            updated[stage] = updated.getValue(stage) + 1
                            scores = updated

                            if (testIndex < testQuestions.lastIndex) {
                                testIndex += 1
                            } else {
                                val ordered = GriefStage.values().sortedByDescending { scores.getValue(it) }
                                primaryStage = ordered[0]
                                secondaryStage = ordered.getOrElse(1) { ordered[0] }
                                prefs.edit()
                                    .putString("primaryStage", primaryStage.name)
                                    .putString("secondaryStage", secondaryStage.name)
                                    .apply()
                                screen = Screen.Intensity
                            }
                        }
                    )

                    Screen.Intensity -> IntensityScreen(
                        intensity = intensity,
                        onChange = {
                            intensity = it
                            prefs.edit().putInt("intensity", it).apply()
                        },
                        onContinue = {
                            prefs.edit().putBoolean("onboardingDone", true).apply()
                            prefs.edit()

                                .putBoolean("notificationsEnabled", true)

                                .putInt("notificationHour", 9)

                                .putInt("notificationMinute", 0)

                                .apply()

                            scheduleDailyReminder(9, 0)
                            screen = Screen.Result
                        }
                    )

                    Screen.Result -> ResultScreen(
                        lossType = selectedLoss,
                        primaryStage = primaryStage,
                        secondaryStage = secondaryStage,
                        intensity = intensity,
                        onContinue = { screen = Screen.Home }
                    )

                    Screen.Home -> HomeScreen(
                        lossType = selectedLoss,
                        currentDay = currentDay,
                        primaryStage = primaryStage,
                        secondaryStage = secondaryStage,
                        intensity = intensity,
                        completedCount = completedCount(prefs),
                        streak = currentStreak(prefs),
                        favoriteCount = favoriteCount(prefs),
                        prefs = prefs,
                        onOpenDay = {
                            selectedDay = currentDay
                            screen = Screen.DayDetail
                        },
                        onOpenList = { screen = Screen.DayList },
                        onOpenHistory = { screen = Screen.History },
                        onOpenInsights = { screen = Screen.Insights },
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenFinal = { screen = Screen.Final },
                        onExport = { exportJournal(prefs) },
                        onRedoTest = {
                            testIndex = 0
                            scores = mutableMapOf<GriefStage, Int>().withDefault { 0 }
                            screen = Screen.LossType
                        },
                        onReset = {
                            prefs.edit().clear().apply()
                            selectedLoss = ""
                            currentDay = 1
                            selectedDay = 1
                            intensity = 5
                            primaryStage = GriefStage.Sadness
                            secondaryStage = GriefStage.Shock
                            screen = Screen.Welcome
                        }
                    )

                    Screen.DayList -> DayListScreen(
                        currentDay = currentDay,
                        onSelect = {
                            selectedDay = it
                            screen = Screen.DayDetail
                        },
                        onBack = { screen = Screen.Home }
                    )

                    Screen.DayDetail -> {
                        var journalText by remember(selectedDay) {
                            mutableStateOf(prefs.getString("journal_$selectedDay", "") ?: "")
                        }
                        var isFavorite by remember(selectedDay) {
                            mutableStateOf(prefs.getBoolean("favorite_$selectedDay", false))
                        }

                        DayDetailScreen(
                            dayNumber = selectedDay,
                            content = buildDayContent(selectedDay, selectedLoss, primaryStage, secondaryStage, intensity),
                            primaryStage = primaryStage,
                            journalText = journalText,
                            ambientSound = ambientSound,
                            isAmbientPlaying = isAmbientPlaying,
                            isFavorite = isFavorite,
                            onToggleFavorite = {
                                isFavorite = !isFavorite
                                prefs.edit().putBoolean("favorite_$selectedDay", isFavorite).apply()
                            },
                            onJournalChange = { value ->
                                journalText = value
                                prefs.edit().putString("journal_$selectedDay", value).apply()
                            },
                            onAmbientChange = { sound ->
                                ambientSound = sound
                                if (isAmbientPlaying) playAmbient(sound)
                            },
                            onToggleAmbient = {
                                if (isAmbientPlaying) {
                                    stopAmbient()
                                    isAmbientPlaying = false
                                } else {
                                    playAmbient(ambientSound)
                                    isAmbientPlaying = true
                                }
                            },
                            onBack = { screen = Screen.Home },
                            onNext = {
                                if (selectedDay < 30) selectedDay += 1
                            },
                            onPrevious = {
                                if (selectedDay > 1) selectedDay -= 1
                            },
                            onComplete = {
                                prefs.edit()
                                    .putBoolean("completed_$selectedDay", true)
                                    .putString("completedDate_$selectedDay", java.time.LocalDate.now().toString())
                                    .apply()
                                if (selectedDay >= currentDay) {
                                    currentDay = minOf(30, selectedDay + 1)
                                    prefs.edit().putInt("currentDay", currentDay).apply()
                                }
                                screen = if (selectedDay == 30) Screen.Final else Screen.Home
                            }
                        )
                    }

                    Screen.History -> HistoryScreen(
                        prefs = prefs,
                        onOpenDay = {
                            selectedDay = it
                            screen = Screen.DayDetail
                        },
                        onExport = { exportJournal(prefs) },
                        onBack = { screen = Screen.Home }
                    )

                    Screen.Insights -> InsightsScreen(
                        prefs = prefs,
                        onBack = { screen = Screen.Home }
                    )

                    Screen.Settings -> SettingsScreen(
                        prefs = prefs,
                        onNotificationsChange = { enabled, hour, minute ->
                            prefs.edit()
                                .putBoolean("notificationsEnabled", enabled)
                                .putInt("notificationHour", hour)
                                .putInt("notificationMinute", minute)
                                .apply()

                            if (enabled) {
                                scheduleDailyReminder(hour, minute)
                                Toast.makeText(this@MainActivity, "Notificación diaria activada", Toast.LENGTH_SHORT).show()
                            } else {
                                cancelDailyReminder()
                                Toast.makeText(this@MainActivity, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReset = {
                            cancelDailyReminder()
                            prefs.edit().clear().apply()
                            selectedLoss = ""
                            currentDay = 1
                            selectedDay = 1
                            intensity = 5
                            primaryStage = GriefStage.Sadness
                            secondaryStage = GriefStage.Shock
                            screen = Screen.Welcome
                        },
                        onBack = { screen = Screen.Home }
                    )

                    Screen.Final -> FinalScreen(
                        completedCount = completedCount(prefs),
                        journalCount = (1..30).count { (prefs.getString("journal_$it", "") ?: "").isNotBlank() },
                        favoriteCount = favoriteCount(prefs),
                        onExport = { exportJournal(prefs) },
                        onRestart = {
                            prefs.edit().putInt("currentDay", 1).apply()
                            currentDay = 1
                            selectedDay = 1
                            screen = Screen.Home
                        },
                        onBack = { screen = Screen.Home }
                    )
                    }
                }
            }
        }
    }

    @Composable
    private fun SplashScreen() {
        var startAnimation by remember { mutableStateOf(false) }
        val alpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(1400), label = "alpha"
        )
        val scale by animateFloatAsState(
            targetValue = if (startAnimation) 1.02f else 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            ), label = "scale"
        )
        val slideUp by animateFloatAsState(
            targetValue = if (startAnimation) 0f else 60f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ), label = "slide"
        )

        LaunchedEffect(Unit) {
            delay(100)
            startAnimation = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEAF4FF),
                            Color(0xFFB6D3F2),
                            Color(0xFFFF8A65)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_duelar),
                contentDescription = "Duelar",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                        this.translationY = slideUp
                    },
                contentScale = ContentScale.Fit
            )
        }
    }

    @Composable
    private fun SettingsScreen(
        prefs: android.content.SharedPreferences,
        onNotificationsChange: (Boolean, Int, Int) -> Unit,
        onReset: () -> Unit,
        onBack: () -> Unit
    ) {
        var notificationsEnabled by remember {
            mutableStateOf(prefs.getBoolean("notificationsEnabled", true))
        }

        var hour by remember {
            mutableIntStateOf(prefs.getInt("notificationHour", 9))
        }

        var minute by remember {
            mutableIntStateOf(prefs.getInt("notificationMinute", 0))
        }

        Page {
            Title("Configuración")

            Body("Ajustá cómo querés que Duelar te acompañe durante tu proceso.")

            Spacer(Modifier.height(16.dp))

            SectionCard(
                "Privacidad",
                "Tus registros se guardan únicamente en este dispositivo. Duelar no envía ni comparte tu información. Si desinstalás la app, tus registros se perderán."
            )

            SectionCard(
                "Uso responsable",
                "Duelar es una herramienta de acompañamiento emocional y reflexión personal. No constituye atención médica, psicológica ni psiquiátrica, y no reemplaza tratamiento profesional."
            )

            SectionCard(
                "Emergencia emocional",
                "Si sentís riesgo inmediato para tu seguridad, o aparecen pensamientos de hacerte daño, buscá ayuda profesional o contactá emergencias locales de inmediato."
            )

            NotificationSettingsCard(
                enabled = notificationsEnabled,
                hour = hour,
                minute = minute,
                onEnabledChange = {
                    notificationsEnabled = it
                    onNotificationsChange(notificationsEnabled, hour, minute)
                },
                onHourChange = {
                    hour = it
                    onNotificationsChange(notificationsEnabled, hour, minute)
                },
                onMinuteChange = {
                    minute = it
                    onNotificationsChange(notificationsEnabled, hour, minute)
                }
            )

            PrimaryButton("Reiniciar proceso", onReset)

            PrimaryButton("Volver", onBack)
        }
    }

    @Composable
    private fun NotificationSettingsCard(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        onEnabledChange: (Boolean) -> Unit,
        onHourChange: (Int) -> Unit,
        onMinuteChange: (Int) -> Unit
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    "Recordatorio diario",
                    color = ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Elegí si querés recibir una pausa diaria para volver a tu proceso.",
                    color = muted,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (enabled) "Activado" else "Desactivado",
                        color = ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = { onEnabledChange(!enabled) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enabled) accent else soft,
                            contentColor = if (enabled) Color.White else ink
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(if (enabled) "Apagar" else "Prender")
                    }
                }

                if (enabled) {
                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Hora de notificación",
                        color = ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onHourChange(if (hour == 0) 23 else hour - 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = card, contentColor = ink),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-")
                        }

                        Text(
                            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                            color = ink,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f),
                        )

                        Button(
                            onClick = { onHourChange(if (hour == 23) 0 else hour + 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = card, contentColor = ink),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onMinuteChange(0) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (minute == 0) accent else card,
                                contentColor = if (minute == 0) Color.White else ink
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(":00")
                        }

                        Button(
                            onClick = { onMinuteChange(15) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (minute == 15) accent else card,
                                contentColor = if (minute == 15) Color.White else ink
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(":15")
                        }

                        Button(
                            onClick = { onMinuteChange(30) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (minute == 30) accent else card,
                                contentColor = if (minute == 30) Color.White else ink
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(":30")
                        }

                        Button(
                            onClick = { onMinuteChange(45) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (minute == 45) accent else card,
                                contentColor = if (minute == 45) Color.White else ink
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(":45")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Vas a recibir un recordatorio diario a esa hora.",
                        color = muted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
    @Composable
    private fun DuelarLogoHeader() {
        Image(
            painter = painterResource(id = R.drawable.logo_duelar),
            contentDescription = "Duelar",
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentScale = ContentScale.Fit
        )
    }



    private fun completedCount(prefs: android.content.SharedPreferences): Int = (1..30).count { prefs.getBoolean("completed_$it", false) }

    private fun favoriteCount(prefs: android.content.SharedPreferences): Int = (1..30).count { prefs.getBoolean("favorite_$it", false) }

    private fun currentStreak(prefs: android.content.SharedPreferences): Int {
        var streak = 0
        for (day in 1..30) {
            if (prefs.getBoolean("completed_$day", false)) streak++ else if (day <= prefs.getInt("currentDay", 1)) break
        }
        return streak
    }

    private fun exportJournal(prefs: android.content.SharedPreferences) {
        val builder = StringBuilder()
        builder.append("Mi proceso en Duelar\n\n")
        for (day in 1..30) {
            val journal = prefs.getString("journal_$day", "") ?: ""
            val completed = prefs.getBoolean("completed_$day", false)
            if (completed || journal.isNotBlank()) {
                builder.append("Día $day, ${dayTitles[day - 1]}\n")
                if (completed) builder.append("Completado: ${prefs.getString("completedDate_$day", "")}\n")
                if (journal.isNotBlank()) builder.append(journal).append("\n")
                builder.append("\n")
            }
        }
        if (builder.toString().trim() == "Mi proceso en Duelar") {
            builder.append("Todavía no hay registros escritos.\n")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mi proceso en Duelar")
            putExtra(Intent.EXTRA_TEXT, builder.toString())
        }
        startActivity(Intent.createChooser(intent, "Exportar mi proceso"))
    }

    override fun onDestroy() {
        ambientPlayer?.release()
        ambientPlayer = null
        super.onDestroy()
    }

    private fun playAmbient(sound: String) {
        val resId = when (sound) {
            "Lluvia suave" -> R.raw.ambient_rain
            "Olas lentas" -> R.raw.ambient_waves
            "Zen instrumental" -> R.raw.ambient_zen
            "Piano calmo" -> R.raw.ambient_piano
            "Cuencos suaves" -> R.raw.ambient_bowls
            else -> R.raw.ambient_zen
        }

        ambientPlayer?.release()

        val player = MediaPlayer.create(this, resId)
        if (player == null) {
            Toast.makeText(this, "No se pudo cargar el sonido ambiente", Toast.LENGTH_SHORT).show()
            return
        }

        ambientPlayer = player.apply {
            isLooping = true
            setVolume(0.85f, 0.85f)
            start()
        }
    }

    private fun stopAmbient() {
        ambientPlayer?.release()
        ambientPlayer = null
    }

    private fun scheduleDailyReminder(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = java.util.Calendar.getInstance()

        val reminder = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)

            if (before(now)) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            reminder.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelDailyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    @Composable
    private fun Page(content: @Composable ColumnScope.() -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            bgWarm,
                            bg,
                            bgSoftBlue
                        )
                    )
                )
        ) {
            Surface(
                color = accentSoft.copy(alpha = 0.35f),
                shape = RoundedCornerShape(180.dp),
                modifier = Modifier
                    .size(240.dp)
                    .offset(x = 210.dp, y = (-70).dp)
            ) {}

            Surface(
                color = lavenderSoft.copy(alpha = 0.32f),
                shape = RoundedCornerShape(180.dp),
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = (-90).dp, y = 420.dp)
            ) {}

            Surface(
                color = coralSoft.copy(alpha = 0.22f),
                shape = RoundedCornerShape(180.dp),
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = 40.dp)
            ) {}

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp, vertical = 34.dp),
                content = content
            )
        }
    }

    @Composable
    private fun Title(text: String) {
        Text(
            text = text,
            color = ink,
            fontSize = 38.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(18.dp))
    }

    @Composable
    private fun Body(text: String) {
        Text(
            text = text,
            color = muted,
            fontSize = 18.sp,
            lineHeight = 29.sp
        )
    }

    @Composable
    private fun SectionCard(title: String, body: String) {
        Card(
            colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(title, color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(body, color = muted, fontSize = 17.sp, lineHeight = 27.sp)
            }
        }
    }

    @Composable
    private fun TodaySignalCard(signal: TodaySignal) {
        Card(
            colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.94f)),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    signal.title,
                    color = ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    signal.body,
                    color = muted,
                    fontSize = 17.sp,
                    lineHeight = 27.sp
                )
            }
        }
    }

    @Composable
    private fun PrimaryButton(text: String, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(58.dp)
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }


    @Composable
    private fun SecondaryButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = card.copy(alpha = 0.92f),
                contentColor = ink
            ),
            shape = RoundedCornerShape(22.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
            modifier = modifier
                .padding(top = 8.dp)
                .height(54.dp)
        ) {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    private fun Choice(text: String, selected: Boolean = false, onClick: () -> Unit) {
        val color = if (selected) accent else card.copy(alpha = 0.9f)
        val textColor = if (selected) Color.White else ink

        Surface(
            color = color,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp)
                .clickable { onClick() }
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(18.dp)
            )
        }
    }

    @Composable
    private fun OptionsMenuButton(label: String = "Más opciones", items: List<Pair<String, () -> Unit>>) {
        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        ) {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = card.copy(alpha = 0.92f), contentColor = ink),
                shape = RoundedCornerShape(22.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(card)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.first, color = ink, fontSize = 16.sp) },
                        onClick = {
                            expanded = false
                            item.second()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun WelcomeScreen(onStart: () -> Unit) {
        Page {
            Spacer(Modifier.height(28.dp))
            DuelarLogoHeader()
            Spacer(Modifier.height(12.dp))
            Body("Un acompañamiento de 30 días para atravesar una pérdida a tu ritmo, con pausas, reflexión y pequeños pasos posibles.")
            Spacer(Modifier.height(22.dp))
            SectionCard(
                "Antes de empezar",
                "No hay una forma correcta de vivir una pérdida.\n" +
                        "Esta guía no busca apurarte ni decirte cómo sentirte.\n" +
                        "Solo ofrecerte un espacio para acompañarte día a día."
            )
            SectionCard(
                "Cuidado importante",
                "Duelar no reemplaza terapia ni atención profesional.\n" +
                        "Si sentís que el dolor se vuelve inmanejable, o aparecen pensamientos de hacerte daño, buscá ayuda profesional o contactá emergencias de inmediato."
            )
            PrimaryButton("Empezar", onStart)
        }
    }

    @Composable
    private fun LossTypeScreen(selectedLoss: String, onSelect: (String) -> Unit, onContinue: () -> Unit) {
        Page {
            Title("Qué estás atravesando")
            Body("Cada experiencia de pérdida es distinta. Elegí la opción que más se acerque a lo que estás viviendo hoy.")
            Spacer(Modifier.height(22.dp))
            lossTypes.forEach {
                Choice(it, selectedLoss == it) { onSelect(it) }
            }
            PrimaryButton("Continuar", onContinue)
        }
    }

    @Composable
    private fun TestScreen(questionIndex: Int, question: TestQuestion, total: Int, onAnswer: (GriefStage) -> Unit) {
        Page {
            Text(
                "Pregunta ${questionIndex + 1} de $total",
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Title(question.question)
            Body("Elegí la respuesta que más se parezca a lo que aparece en vos ahora. No hay respuestas correctas.")
            Spacer(Modifier.height(22.dp))
            question.options.forEach { option ->
                Choice(option.text) { onAnswer(option.stage) }
            }
        }
    }

    @Composable
    private fun IntensityScreen(intensity: Int, onChange: (Int) -> Unit, onContinue: () -> Unit) {
        Page {
            Title("Cómo se siente hoy")
            Body("Marcá la intensidad del dolor en este momento. Esto ayuda a ajustar el ritmo de la guía.")
            Spacer(Modifier.height(28.dp))
            Text(
                "$intensity de 10",
                color = ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Slider(
                value = intensity.toFloat(),
                onValueChange = { onChange(it.toInt().coerceIn(1, 10)) },
                valueRange = 1f..10f,
                steps = 8
            )
            if (intensity >= 9) {
                SectionCard(
                    "Una señal para pedir ayuda",
                    "Si hoy sentís que no podés sostener este dolor en soledad, buscá apoyo profesional o contactá a alguien de confianza. No tenés que atravesar esto sin ayuda."
                )
            }
            PrimaryButton("Ver mi guía", onContinue)
        }
    }

    @Composable
    private fun ResultScreen(
        lossType: String,
        primaryStage: GriefStage,
        secondaryStage: GriefStage,
        intensity: Int,
        onContinue: () -> Unit
    ) {
        Page {
            Title("Tu punto de partida")
            Body("Este resultado no te define. Solo ayuda a que la guía empiece desde el lugar emocional en el que estás hoy.")
            Spacer(Modifier.height(16.dp))
            SectionCard("Lo que estás atravesando", lossType)
            SectionCard("Etapa más presente", primaryStage.label)
            SectionCard("También aparece", secondaryStage.label)
            SectionCard("Intensidad actual", "$intensity de 10")
            SectionCard("Orientación inicial", stageMessage(primaryStage, intensity))
            PrimaryButton("Comenzar día 1", onContinue)
        }
    }

    @Composable
    private fun HomeScreen(
        lossType: String,
        currentDay: Int,
        primaryStage: GriefStage,
        secondaryStage: GriefStage,
        intensity: Int,
        completedCount: Int,
        streak: Int,
        favoriteCount: Int,
        prefs: android.content.SharedPreferences,
        onOpenDay: () -> Unit,
        onOpenList: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenInsights: () -> Unit,
        onOpenSettings: () -> Unit,
        onOpenFinal: () -> Unit,
        onExport: () -> Unit,
        onRedoTest: () -> Unit,
        onReset: () -> Unit
    ) {
        Page {
            Title("Tu camino")
            Body(homeMessage(currentDay, intensity))
            Spacer(Modifier.height(22.dp))

            TodaySignalCard(buildTodaySignal(prefs))

            ProgressSummary(currentDay, completedCount, streak, favoriteCount, prefs)

            SectionCard("Pérdida", lossType)
            SectionCard("Etapa actual", primaryStage.label)
            SectionCard("También presente", secondaryStage.label)
            SectionCard("Intensidad", "$intensity de 10")

            PrimaryButton("Continuar con el día $currentDay", onOpenDay)
            PrimaryButton("Ver los 30 días", onOpenList)
            PrimaryButton("Ver mis señales", onOpenInsights)

            val menuItems = buildList<Pair<String, () -> Unit>> {
                add("Mi historial" to onOpenHistory)
                add("Configuración" to onOpenSettings)
                add("Exportar registros" to onExport)
                if (currentDay >= 30) add("Ver cierre del ciclo" to onOpenFinal)
                add("Revisar mi momento actual" to onRedoTest)
            }
            OptionsMenuButton("Más opciones", menuItems)
        }
    }

    private fun homeMessage(currentDay: Int, intensity: Int): String {
        return when {
            intensity >= 8 -> "Hoy no necesitás resolver todo. Solo volver a un lugar seguro dentro de este día."
            currentDay <= 5 -> "Estás empezando. No busques hacerlo perfecto. Un paso honesto alcanza."
            currentDay <= 15 -> "Ya hay camino recorrido. Volvé a tu pausa diaria sin exigirte linealidad."
            currentDay < 30 -> "Ya recorriste parte del camino.\n" +
                    "No hace falta hacerlo perfecto, solo seguir presente."
            else -> "Llegaste al cierre del ciclo. Podés revisar, agradecer y decidir cómo seguir."
        }
    }

    @Composable
    private fun ProgressSummary(currentDay: Int, completedCount: Int, streak: Int, favoriteCount: Int, prefs: android.content.SharedPreferences) {
        Card(
            colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.94f)),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Mi avance", color = ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("$completedCount de 30 días completados", color = muted, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { completedCount / 30f },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = accent,
                    trackColor = soft
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Racha: $streak", color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Favoritos: $favoriteCount", color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(completedCount * 100 / 30)}%", color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                ProgressDots(currentDay, prefs)
            }
        }
    }

    @Composable
    private fun ProgressDots(currentDay: Int, prefs: android.content.SharedPreferences) {
        Column {
            for (row in 0 until 5) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 1..6) {
                        val day = row * 6 + col
                        val completed = prefs.getBoolean("completed_$day", false)
                        val color = when {
                            completed -> accent
                            day == currentDay -> coralSoft
                            else -> soft
                        }
                        Surface(color = color, shape = RoundedCornerShape(50.dp), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(day.toString(), color = if (completed) Color.White else ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    @Composable
    private fun DayListScreen(currentDay: Int, onSelect: (Int) -> Unit, onBack: () -> Unit) {
        Page {
            Title("Guía de 30 días")
            Body("Podés avanzar en orden o volver a un día anterior cuando lo necesites.")
            Spacer(Modifier.height(16.dp))
            dayTitles.forEachIndexed { index, title ->
                val day = index + 1
                Choice(
                    text = if (day == currentDay) "Día $day, $title, actual" else "Día $day, $title"
                ) { onSelect(day) }
            }
            PrimaryButton("Volver", onBack)
        }
    }

    @Composable
    private fun DayDetailScreen(
        dayNumber: Int,
        content: DayContent,
        primaryStage: GriefStage,
        journalText: String,
        ambientSound: String,
        isAmbientPlaying: Boolean,
        isFavorite: Boolean,
        onToggleFavorite: () -> Unit,
        onJournalChange: (String) -> Unit,
        onAmbientChange: (String) -> Unit,
        onToggleAmbient: () -> Unit,
        onBack: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onComplete: () -> Unit
    ) {
        Page {
            Text(
                "Día $dayNumber",
                color = accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Title(content.title)
            Body("Acompañamiento orientado a ${primaryStage.label.lowercase()}.")
            OptionsMenuButton(
                "Opciones del día",
                listOf(
                    (if (isFavorite) "Quitar de favoritos" else "Guardar este día como favorito") to onToggleFavorite,
                    "Volver al inicio" to onBack
                )
            )
            Spacer(Modifier.height(16.dp))

            SectionCard("Lectura", content.reading)
            SectionCard("Ejercicio", content.exercise)
            SectionCard("Para escribir", content.journal)
            SectionCard("Meditación", content.meditation)
            AmbientPlayerCard(
                selectedSound = ambientSound,
                isPlaying = isAmbientPlaying,
                onSelectSound = onAmbientChange,
                onToggle = onToggleAmbient
            )
            MeditationTimerCard()
            SectionCard("Acción mínima", content.action)
            JournalInput(value = journalText, onChange = onJournalChange)

            Spacer(Modifier.height(18.dp))

            PrimaryButton("Completar día", onComplete)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (dayNumber > 1) {
                    Button(
                        onClick = onPrevious,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = card.copy(alpha = 0.95f),
                            contentColor = ink
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text("← Anterior", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (dayNumber < 30) {
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = card.copy(alpha = 0.95f),
                            contentColor = ink
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text("Siguiente →", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }


    @Composable
    private fun JournalInput(value: String, onChange: (String) -> Unit) {
        Card(
            colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Mi registro de hoy", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Escribí lo que quieras conservar de este día. Queda guardado solo en tu celular.", color = muted, fontSize = 16.sp, lineHeight = 25.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("Hoy me doy cuenta de...") }
                )
            }
        }
    }

    @Composable
    private fun AmbientPlayerCard(
        selectedSound: String,
        isPlaying: Boolean,
        onSelectSound: (String) -> Unit,
        onToggle: () -> Unit
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("Sonido ambiente", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Usalo solo si te ayuda a entrar en pausa. Si no lo escuchás, revisá el volumen multimedia del celular.", color = muted, fontSize = 16.sp, lineHeight = 25.sp)
                Spacer(Modifier.height(12.dp))
                listOf("Zen instrumental", "Piano calmo", "Cuencos suaves", "Lluvia suave", "Olas lentas").forEach { sound ->
                    Choice(sound, selectedSound == sound) { onSelectSound(sound) }
                }
                PrimaryButton(if (isPlaying) "Pausar sonido" else "Reproducir sonido", onToggle)
            }
        }
    }

    @Composable
    private fun MeditationTimerCard() {
        var totalSeconds by remember { mutableIntStateOf(0) }
        var remainingSeconds by remember { mutableIntStateOf(0) }
        var running by remember { mutableStateOf(false) }

        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        val cycleProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cycleProgress"
        )

        val (breathStage, breathScale, breathAlpha) = if (running) {
            when {
                cycleProgress < 0.33f -> {
                    val p = cycleProgress / 0.33f
                    Triple("Inhalá", 0.85f + (p * 0.3f), 0.4f + (p * 0.4f))
                }
                cycleProgress < 0.66f -> {
                    val p = (cycleProgress - 0.33f) / 0.33f
                    Triple("Exhalá", 1.15f - (p * 0.3f), 0.8f - (p * 0.4f))
                }
                else -> Triple("Pausa", 0.85f, 0.4f)
            }
        } else {
            Triple("Respirá", 1f, 0.5f)
        }

        LaunchedEffect(running, remainingSeconds) {
            if (running && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds -= 1
            }
            if (remainingSeconds <= 0 && totalSeconds > 0) {
                running = false
                totalSeconds = 0
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pausa de respiración", color = ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Seguí el ritmo del círculo para volver a tu centro.",
                    color = muted,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(36.dp))

                Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    // Outer decorative rings
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2
                        drawCircle(
                            color = accent.copy(alpha = 0.05f),
                            radius = radius,
                            style = Fill
                        )
                        if (running) {
                            drawCircle(
                                color = accent.copy(alpha = 0.1f),
                                radius = radius * breathScale,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // Circular Progress
                    if (running && totalSeconds > 0) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { remainingSeconds.toFloat() / totalSeconds },
                            modifier = Modifier.fillMaxSize(),
                            color = accent,
                            strokeWidth = 4.dp,
                            trackColor = soft.copy(alpha = 0.4f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }

                    // Main breathing circle
                    Surface(
                        color = if (running) accent.copy(alpha = breathAlpha) else soft,
                        shape = RoundedCornerShape(200.dp),
                        modifier = Modifier
                            .size(140.dp)
                            .graphicsLayer {
                                scaleX = breathScale
                                scaleY = breathScale
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    breathStage,
                                    color = if (running) Color.White else ink.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (running) {
                                    Text(
                                        "${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(36.dp))

                if (!running) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TimerOption("1 min") { remainingSeconds = 60; totalSeconds = 60; running = true }
                        TimerOption("3 min") { remainingSeconds = 180; totalSeconds = 180; running = true }
                        TimerOption("5 min") { remainingSeconds = 300; totalSeconds = 300; running = true }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { running = !running },
                            modifier = Modifier.weight(1f).height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = soft),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(if (running) "Pausar" else "Continuar", color = ink, fontWeight = FontWeight.SemiBold)
                        }
                        
                        Button(
                            onClick = { running = false; remainingSeconds = 0; totalSeconds = 0 },
                            modifier = Modifier.weight(1f).height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, soft)
                        ) {
                            Text("Detener", color = muted, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.TimerOption(label: String, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            color = soft.copy(alpha = 0.7f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(52.dp).weight(1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    private fun HistoryScreen(prefs: android.content.SharedPreferences, onOpenDay: (Int) -> Unit, onExport: () -> Unit, onBack: () -> Unit) {
        Page {
            Title("Mi historial")
            Body("Acá podés volver a tus días completados y releer lo que escribiste.")
            Spacer(Modifier.height(16.dp))
            var anyCompleted = false
            for (day in 1..30) {
                val completed = prefs.getBoolean("completed_$day", false)
                val journal = prefs.getString("journal_$day", "") ?: ""
                if (completed || journal.isNotBlank()) {
                    anyCompleted = true
                    val date = prefs.getString("completedDate_$day", "") ?: ""
                    val preview = if (journal.isBlank()) "Sin registro escrito todavía." else journal.take(90) + if (journal.length > 90) "..." else ""
                    Card(
                        colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.92f)),
                        shape = RoundedCornerShape(26.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp)
                            .clickable { onOpenDay(day) }
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Día $day, ${dayTitles[day - 1]}", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            if (date.isNotBlank()) Text("Completado: $date", color = muted, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(preview, color = muted, fontSize = 16.sp, lineHeight = 23.sp)
                        }
                    }
                }
            }
            if (!anyCompleted) {
                SectionCard("Todavía no hay historial", "Cuando completes días o escribas tus registros, van a aparecer acá.")
            }
            PrimaryButton("Exportar registros", onExport)
            PrimaryButton("Volver", onBack)
        }
    }

    @Composable
    private fun MiniMetric(title: String, value: String, modifier: Modifier = Modifier) {
        Surface(
            color = accentSoft.copy(alpha = 0.55f),
            shape = RoundedCornerShape(22.dp),
            modifier = modifier
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    title,
                    color = muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    value,
                    color = ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp
                )
            }
        }
    }

    @Composable
    private fun EmotionTimelineCard(prefs: android.content.SharedPreferences) {
        val days = buildEmotionDays(prefs)
        val withData = days.filter { it.hasData }
        val recentSignals = withData.sortedBy { it.day }.takeLast(3)

        val recentEmotionText = if (recentSignals.isNotEmpty()) {
            recentSignals
                .map { shortEmotionLabel(it.label) }
                .distinct()
                .joinToString(" · ")
        } else {
            "Sin registros"
        }

        val recentAverageIntensity = if (recentSignals.isNotEmpty()) {
            recentSignals
                .map { it.intensity }
                .average()
                .toInt()
                .coerceIn(1, 10)
        } else {
            0
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = card.copy(alpha = 0.96f)),
            shape = RoundedCornerShape(36.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📈", fontSize = 22.sp)
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tu proceso emocional",
                            color = ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Evolución de tus registros diarios",
                            color = muted,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniMetric(
                        title = "Tendencia reciente",
                        value = recentEmotionText,
                        modifier = Modifier.weight(1.2f)
                    )
                    MiniMetric(
                        title = "Intensidad",
                        value = if (recentAverageIntensity > 0) "$recentAverageIntensity/10" else "—",
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Spacer(Modifier.height(28.dp))

                EmotionBarChart(days)

                Spacer(Modifier.height(20.dp))

                Text(
                    "Cada barra muestra la intensidad estimada del día. El color indica la emoción predominante en tu journal.",
                    color = muted.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(20.dp))

                EmotionLegend()
            }
        }
    }

    @Composable
    private fun EmotionBarChart(days: List<EmotionDay>) {
        val chartHeight = 280.dp

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .width(28.dp)
                        .height(chartHeight - 44.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("10", "8", "6", "4", "2", "1").forEach {
                        Text(
                            it,
                            color = muted.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(chartHeight)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val topPad = 24f
                        val bottomPad = 44f
                        val chartH = h - topPad - bottomPad
                        val chartW = w

                        val gridColor = muted.copy(alpha = 0.06f)
                        
                        fun yForIntensity(value: Int): Float {
                            val v = value.coerceIn(1, 10)
                            return topPad + ((10 - v) / 9f) * chartH
                        }

                        fun xForDay(day: Int): Float {
                            return ((day - 1) / 29f) * chartW
                        }

                        // Horizontal Grid Lines
                        listOf(2, 4, 6, 8, 10).forEach { value ->
                            val y = yForIntensity(value)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }

                        val barWidth = (chartW / 30f) * 0.8f
                        
                        days.forEach { item ->
                            if (item.hasData) {
                                val x = xForDay(item.day)
                                val y = yForIntensity(item.intensity)
                                
                                // Modern Bar with Gradient
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            item.color,
                                            item.color.copy(alpha = 0.4f)
                                        ),
                                        startY = y,
                                        endY = h - bottomPad
                                    ),
                                    topLeft = Offset(x - barWidth / 2f, y),
                                    size = Size(barWidth, (h - bottomPad) - y),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                                
                                // Optional: subtle highlight on top
                                drawLine(
                                    color = item.color,
                                    start = Offset(x - barWidth / 2f + 4f, y + 2f),
                                    end = Offset(x + barWidth / 2f - 4f, y + 2f),
                                    strokeWidth = 3f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            } else {
                                // Subtle placeholder for empty days
                                val x = xForDay(item.day)
                                drawCircle(
                                    color = muted.copy(alpha = 0.1f),
                                    radius = 3f,
                                    center = Offset(x, h - bottomPad - 4f)
                                )
                            }
                        }

                        // Floating Labels for recent data
                        val dataDays = days.filter { it.hasData }.sortedBy { it.day }
                        val labelDays = dataDays.takeLast(2).map { it.day }.toSet()
                        val labelPaint = Paint().apply {
                            color = ink.toArgb()
                            textSize = 24f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        val placedLabelRects = mutableListOf<Rect>()
                        
                        dataDays.filter { it.day in labelDays }.reversed().forEach { item ->
                            val x = xForDay(item.day)
                            val y = yForIntensity(item.intensity)
                            val label = shortEmotionLabel(item.label)
                            val labelWidth = labelPaint.measureText(label)
                            
                            val rectPaddingHorizontal = 16f
                            val rectWidth = labelWidth + rectPaddingHorizontal * 2
                            val rectHeight = 44f
                            
                            val labelX = x.coerceIn(rectWidth / 2f + 8f, w - rectWidth / 2f - 8f)
                            val labelY = (y - 50f).coerceAtLeast(40f)
                            val rectLeft = labelX - rectWidth / 2f
                            val rectTop = labelY - 32f
                            
                            val labelRect = Rect(rectLeft, rectTop, rectLeft + rectWidth, rectTop + rectHeight)

                            if (!placedLabelRects.any { it.overlaps(labelRect) }) {
                                placedLabelRects.add(labelRect)

                                // Label Shadow (simplified)
                                drawRoundRect(
                                    color = Color.Black.copy(alpha = 0.08f),
                                    topLeft = Offset(rectLeft + 2f, rectTop + 2f),
                                    size = Size(rectWidth, rectHeight),
                                    cornerRadius = CornerRadius(16f, 16f)
                                )

                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(rectLeft, rectTop),
                                    size = Size(rectWidth, rectHeight),
                                    cornerRadius = CornerRadius(16f, 16f)
                                )

                                drawRoundRect(
                                    color = item.color.copy(alpha = 0.15f),
                                    topLeft = Offset(rectLeft, rectTop),
                                    size = Size(rectWidth, rectHeight),
                                    cornerRadius = CornerRadius(16f, 16f)
                                )

                                drawContext.canvas.nativeCanvas.drawText(
                                    label,
                                    labelX,
                                    labelY,
                                    labelPaint
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("1", "5", "10", "15", "20", "25", "30").forEach {
                    Text(
                        it,
                        color = muted.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun EmotionLegend() {
        val items = listOf(
            "Shock" to colorForPattern("shock"),
            "Culpa" to colorForPattern("culpa"),
            "Tristeza" to colorForPattern("tristeza"),
            "Ansiedad" to colorForPattern("ansiedad"),
            "Rabia" to colorForPattern("rabia"),
            "Aceptación" to colorForPattern("aceptacion"),
            "Esperanza" to colorForPattern("esperanza")
        )

        Column {
            Text(
                "Guía de colores",
                color = muted.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(14.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { (label, color) ->
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, RoundedCornerShape(50.dp))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                label,
                                color = ink.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun InsightsScreen(
        prefs: android.content.SharedPreferences,
        onBack: () -> Unit
    ) {
        val insight = buildJournalInsight(prefs)

        Page {
            Title("Tus señales")
            Body("Tus registros pueden ayudarte a ver patrones emocionales que a veces pasan desapercibidos.\n" +
                    "Esta lectura es solo orientativa, para ayudarte a observar tu proceso con más claridad.")
            Spacer(Modifier.height(16.dp))

            EmotionTimelineCard(prefs)

            Spacer(Modifier.height(12.dp))

            SectionCard("Patrón emocional más presente", insight.mainText)

            if (insight.evolutionText.isNotBlank()) {
                SectionCard("Cómo viene cambiando", insight.evolutionText)
            }

            SectionCard("Una sugerencia para acompañarte hoy", insight.suggestionText)

            if (insight.secondaryText.isNotBlank()) {
                SectionCard("También aparece", insight.secondaryText)
            }

            PrimaryButton("Volver", onBack)
        }
    }

    @Composable
    private fun FinalScreen(
        completedCount: Int,
        journalCount: Int,
        favoriteCount: Int,
        onExport: () -> Unit,
        onRestart: () -> Unit,
        onBack: () -> Unit
    ) {
        Page {
            Title("Cierre del ciclo")
            Body("Llegar hasta acá no significa que el dolor desapareció.\n" +
                    "Significa que elegiste darte espacio, mirar lo que duele y seguir avanzando aun en días difíciles.")
            Spacer(Modifier.height(16.dp))
            SectionCard("Tu recorrido", "$completedCount días completados, $journalCount registros escritos y $favoriteCount días guardados como favoritos.")
            SectionCard("Lo que hiciste", "Le diste un espacio al dolor, al cuerpo, a la memoria, a la culpa, al amor y a la reconstrucción. Ese recorrido importa.")
            SectionCard("Carta a tu yo futuro", "Si algún día volvés a sentirte perdido, recordá esto:\n" +
                    "ya atravesaste momentos difíciles antes.\n" +
                    "No porque no dolieran, sino porque seguiste estando.")
            SectionCard("Frase de cierre", "Lo vivido forma parte de mi historia.\n" +
                    "No necesito cargarlo de la misma manera para honrarlo.")
            PrimaryButton("Exportar mi proceso", onExport)
            PrimaryButton("Comenzar nuevo ciclo", onRestart)
            PrimaryButton("Volver al inicio", onBack)
        }
    }

    private fun buildDayContent(
        day: Int,
        lossType: String,
        primary: GriefStage,
        secondary: GriefStage,
        intensity: Int
    ): DayContent {
        val title = dayTitles[day - 1]
        val reading = dailyReadings[day - 1]

        return DayContent(
            title = title,
            reading = reading,
            exercise = dailyExercises[day - 1],
            journal = dailyJournals[day - 1],
            meditation = dailyMeditation(day, primary, secondary, intensity),
            action = dailyActions[day - 1]
        )
    }

    private fun stageMessage(stage: GriefStage, intensity: Int): String {
        val core = when (stage) {
            GriefStage.Shock -> "Tu sistema todavía está intentando asimilar lo ocurrido. La prioridad es darte seguridad y no exigirte sentir todo de golpe."
            GriefStage.Anger -> "La rabia suele aparecer cuando algo se siente injusto. La prioridad es darle salida sin convertirla en daño."
            GriefStage.Guilt -> "La mente intenta volver al pasado para corregirlo. La prioridad es separar responsabilidad real de castigo."
            GriefStage.Sadness -> "La tristeza está más presente. La prioridad es sostener lo básico y no quedarte completamente solo con el dolor."
            GriefStage.Acceptance -> "Hay dolor, pero también señales de integración. La prioridad es reconstruir sin negar lo vivido."
        }
        return if (intensity >= 8) "$core Hoy conviene bajar la exigencia y pedir apoyo si lo necesitás." else core
    }

    private fun dailyMeditation(day: Int, primary: GriefStage, secondary: GriefStage, intensity: Int): String {
        val base = when (day) {
            1 -> "Cerrá los ojos y nombrá en silencio lo que estás atravesando. No busques cambiarlo. Solo decí: esto está pasando, y hoy puedo empezar mirándolo de a poco."
            2 -> "Llevá una mano al pecho y otra al abdomen. Inhalá suave. Al exhalar, repetí: no tengo que poder con todo hoy."
            3 -> "Inhalá contando hasta cuatro. Exhalá contando hasta seis. Cada exhalación es una pequeña forma de volver a vos."
            4 -> "Recorré mentalmente tu cuerpo desde los pies hasta la cabeza. Donde encuentres tensión, no pelees. Solo aflojá un poco."
            5 -> "Dejá que la emoción principal tenga un nombre. Rabia, tristeza, miedo, culpa o vacío. Nombrarla ya es empezar a sostenerla."
            6 -> "Imaginá frente a vos las palabras que no salieron. Dejalas aparecer sin corregirlas. Respirá y permití que existan."
            7 -> "Cada vez que aparezca un reproche, inhalá y repetí: puedo mirar esto con honestidad sin destruirme."
            8 -> "Traé un recuerdo a la mente. Observá qué trae al cuerpo. Respirá diciendo: puedo recordar sin quedarme atrapado."
            9 -> "Imaginá que alguien confiable se sienta cerca tuyo en silencio. No te arregla. No te exige. Solo acompaña."
            10 -> "Permití que la tristeza esté presente unos instantes. Repetí: esto duele porque importó. No necesito esconderlo."
            11 -> "Antes de seguir, preguntale a tu cuerpo qué necesita. Agua, alimento, descanso, movimiento o silencio. Escuchá sin discutir."
            12 -> "Si podés, hacé esta meditación caminando. Con cada paso repetí: sigo acá. Con cada exhalación: un paso a la vez."
            13 -> "Visualizá una ola. Dejá que suba y baje. Repetí: esto es una ola emocional. No soy la ola completa."
            14 -> "Traé una imagen, frase o símbolo de lo vivido. Inhalá gratitud. Exhalá apego. Honrar también puede ser soltar."
            15 -> "Pensá en algo pequeño que sí lograste sostener. Respirá sobre eso. Repetí: también esto cuenta."
            16 -> "Creá un momento de silencio. Si tenés una vela o un objeto cerca, usalo como ancla. Decí: le doy un lugar a este dolor."
            17 -> "Cuando aparezca el pasado imposible, volvé al cuerpo. Sentí los pies. Repetí: no puedo volver, pero puedo estar acá."
            18 -> "Bajá la exigencia del día. Inhalá diciendo: hoy. Exhalá diciendo: suficiente. Quedate con esa palabra unos segundos."
            19 -> "Imaginá una rutina simple como una línea suave que sostiene tu día. No tiene que ser perfecta. Solo tiene que ayudarte a volver."
            20 -> "Llevá atención al pecho. Preguntate dónde todavía hay amor, aunque duela. No respondas rápido. Dejá que aparezca."
            21 -> "Abrí los ojos y mirá algo cercano. Nombralo. Tocá algo. Escuchá un sonido. Repetí: estoy en este momento."
            22 -> "Pensá en una parte tuya que se culpa. Hablale como le hablarías a alguien que amás. Respirá con esa frase."
            23 -> "Sentí la garganta y el pecho. Inhalá verdad. Exhalá miedo. Repetí: puedo decir lo que siento con calma."
            24 -> "Acostado o sentado, soltá la frente, la mandíbula y los hombros. Decí: por hoy, dejo esto en pausa."
            25 -> "Imaginá que el vínculo cambia de forma. No desaparece, no se fuerza. Solo encuentra otro lugar dentro de vos."
            26 -> "Visualizá en tus manos algo valioso que querés conservar. Guardalo simbólicamente en el pecho. Lo demás puede descansar."
            27 -> "Imaginá una carga apoyada en el suelo. No la empujes lejos. Solo dejá de sostenerla por unos minutos."
            28 -> "Mirà hacia adelante sin exigir claridad. Solo un pequeño punto de luz. Repetí: no necesito ver todo el camino."
            29 -> "Respirá pensando en lo aprendido. No como explicación perfecta, sino como una marca de vida. Repetí: esto también me transforma."
            else -> "Cerrá los ojos. Inhalá gratitud por haber llegado hasta acá. Exhalá lo que ya no necesitás cargar. Repetí: honro, acepto y sigo."
        }

        val stageLine = when (primary) {
            GriefStage.Shock -> " Si aparece bloqueo o irrealidad, no lo fuerces. Volvé al cuerpo."
            GriefStage.Anger -> " Si aparece rabia, dejala pasar por la respiración antes de convertirla en acción."
            GriefStage.Guilt -> " Si aparece culpa, recordá que castigarte no repara el pasado."
            GriefStage.Sadness -> " Si aparece tristeza, no la expulses. Acompañala con suavidad."
            GriefStage.Acceptance -> " Si aparece calma, recibila sin culpa. También forma parte del proceso."
        }

        val intensityLine = if (intensity >= 8) {
            " Si hoy se siente demasiado, buscá a alguien seguro después de esta pausa."
        } else {
            ""
        }

        return base + stageLine + intensityLine + " Si también aparece ${secondary.label.lowercase()}, dejalo estar sin pelear."
    }



    private fun buildEmotionDays(prefs: android.content.SharedPreferences): List<EmotionDay> {
        return (1..30).map { day ->
            val text = readJournalForDay(prefs, day)

            if (text.isBlank()) {
                EmotionDay(
                    day = day,
                    label = "Sin registro",
                    intensity = 0,
                    color = soft,
                    hasData = false
                )
            } else {
                val pattern = scorePatterns(text).filter { it.score > 0 }.maxByOrNull { it.score }
                val score = pattern?.score ?: 1
                val intensity = estimateIntensity(text, score)

                EmotionDay(
                    day = day,
                    label = pattern?.title?.replaceFirstChar { it.uppercase() } ?: "Proceso mixto",
                    intensity = intensity,
                    color = colorForPattern(pattern?.key),
                    hasData = true
                )
            }
        }
    }

    private fun readJournalForDay(prefs: android.content.SharedPreferences, day: Int): String {
        val keys = listOf(
            "journal_$day",
            "journalText_$day",
            "journal_day_$day",
            "day_${day}_journal",
            "entry_$day"
        )

        keys.forEach { key ->
            val value = prefs.getString(key, null)
            if (!value.isNullOrBlank()) return value.trim()
        }

        return ""
    }

    private fun estimateIntensity(text: String, score: Int): Int {
        val clean = normalizeText(text)
        val words = normalizedWords(text).size
        val exclamations = text.count { it == '!' }

        val veryHighSignals = listOf(
            "no puedo mas",
            "no puedo más",
            "no aguanto",
            "me quiero morir",
            "no quiero vivir",
            "insoportable",
            "desesperado",
            "desesperada",
            "devastado",
            "devastada",
            "destrozado",
            "destrozada",
            "colapsado",
            "colapsada",
            "me supera",
            "me desborda",
            "panico",
            "pánico"
        )

        val highSignals = listOf(
            "mucho dolor",
            "duele mucho",
            "me duele mucho",
            "horrible",
            "terrible",
            "demasiado",
            "vacío",
            "vacio",
            "angustia",
            "ansiedad",
            "rabia",
            "bronca",
            "culpa",
            "llorar",
            "llore",
            "lloré",
            "tristeza profunda",
            "sin fuerzas",
            "agotado",
            "agotada"
        )

        val mediumSignals = listOf(
            "triste",
            "dolor",
            "enojo",
            "miedo",
            "extraño",
            "extrañar",
            "soledad",
            "solo",
            "sola",
            "preocupado",
            "preocupada",
            "nervioso",
            "nerviosa"
        )

        val lowSignals = listOf(
            "calma",
            "tranquilo",
            "tranquila",
            "acepto",
            "aceptacion",
            "aceptación",
            "alivio",
            "esperanza",
            "mejor",
            "paz",
            "agradecido",
            "agradecida",
            "claridad"
        )

        var intensity = 1

        veryHighSignals.forEach { phrase ->
            if (clean.contains(normalizeText(phrase))) intensity += 4
        }

        highSignals.forEach { phrase ->
            if (clean.contains(normalizeText(phrase))) intensity += 3
        }

        mediumSignals.forEach { phrase ->
            if (clean.contains(normalizeText(phrase))) intensity += 2
        }

        lowSignals.forEach { phrase ->
            if (clean.contains(normalizeText(phrase))) intensity -= 1
        }

        intensity += (score / 2).coerceIn(0, 3)

        if (words > 40) intensity += 1
        if (words > 90) intensity += 1
        if (exclamations >= 2) intensity += 1

        return intensity.coerceIn(1, 10)
    }

    private fun colorForPattern(key: String?): Color {
        return when (key) {
            "shock" -> Color(0xFF9FB0B8)
            "culpa" -> Color(0xFFB9A7D9)
            "autoexigencia" -> Color(0xFFC7B6E8)
            "tristeza" -> Color(0xFF8DB7D9)
            "rabia" -> Color(0xFFE6A39A)
            "ansiedad" -> Color(0xFFE7C77D)
            "soledad" -> Color(0xFF9FB4C7)
            "agotamiento" -> Color(0xFFB5C0C8)
            "apego" -> Color(0xFFD5B895)
            "aceptacion" -> Color(0xFF8FC7A8)
            "esperanza" -> Color(0xFF79C8D8)
            else -> accent
        }
    }

    private fun buildTodaySignal(prefs: android.content.SharedPreferences): TodaySignal {
        val entries = (1..30).mapNotNull { day ->
            val text = readJournalForDay(prefs, day)
            if (text.isNotBlank()) day to text else null
        }

        if (entries.size < 2) {
            return TodaySignal(
                title = "Señal de hoy",
                body = "Todavía hay poco registro como para leer un patrón claro. Por ahora, lo importante es que estás creando un espacio para escucharte."
            )
        }

        val recentEntries = entries.takeLast(5)
        val recentText = recentEntries.joinToString(" ") { it.second }

        val patterns = scorePatterns(recentText)
            .filter { it.score > 0 }
            .sortedByDescending { it.score }

        val mainPattern = patterns.firstOrNull()

        if (mainPattern == null) {
            return TodaySignal(
                title = "Señal de hoy",
                body = "Tus últimos registros muestran un proceso mezclado, sin una emoción dominante clara. No hace falta forzar una conclusión. Seguí escribiendo con honestidad."
            )
        }

        val intensities = recentEntries.map { (_, text) ->
            val patternScore = scorePatterns(text).maxOfOrNull { it.score } ?: 0
            estimateIntensity(text, patternScore)
        }

        val avgIntensity = intensities.average().toInt().coerceIn(1, 10)
        val firstHalf = intensities.take((intensities.size / 2).coerceAtLeast(1)).average()
        val secondHalf = intensities.takeLast((intensities.size / 2).coerceAtLeast(1)).average()

        val trendText = when {
            secondHalf >= firstHalf + 2 -> "En los últimos registros la intensidad parece haber subido un poco."
            secondHalf <= firstHalf - 2 -> "En los últimos registros la intensidad parece haber bajado un poco."
            else -> "La intensidad parece mantenerse relativamente estable."
        }

        val (dynamicMessage, dynamicSuggestion) = getDetailedInsight(mainPattern.key, avgIntensity)

        return TodaySignal(
            title = "Señal de hoy",
            body = "En tus últimos registros aparece con más fuerza ${mainPattern.title.lowercase()}. $trendText $dynamicMessage $dynamicSuggestion"
        )
    }

    private fun getDetailedInsight(key: String, intensity: Int): Pair<String, String> {
        val i = intensity.coerceIn(1, 10)
        return when (key) {
            "shock" -> when (i) {
                1 -> "Aparece una leve sensación de irrealidad, como si estuvieras viendo una película." to "Solo observalo sin intentar forzar una conexión mayor."
                2 -> "Te sentís un poco distraído o desconectado de lo que pasa alrededor." to "Hacé algo que te devuelva al cuerpo, como lavarte la cara con agua fría."
                3 -> "A ratos parece que lo ocurrido no fuera del todo cierto." to "Nombrá tres cosas reales que veas ahora mismo."
                4 -> "La sensación de estar 'en automático' se vuelve más frecuente." to "No te exijas decisiones importantes hoy. Mantené la rutina simple."
                5 -> "Hay un bloqueo claro que te impide sentir la magnitud de lo pasado." to "Tu sistema te está protegiendo. No lo fuerces, dale tiempo."
                6 -> "Cuesta mucho concentrarse; el mundo se siente extraño y lejano." to "Buscá un lugar seguro y quedate ahí unos minutos en silencio."
                7 -> "La confusión es constante. No terminás de entender dónde estás parado." to "Hacé una lista muy corta de lo único que tenés que hacer hoy."
                8 -> "Un embotamiento profundo. Las emociones parecen estar tras un muro." to "Solo respirá. No intentes 'sentir' nada por obligación."
                9 -> "Desorientación fuerte. La realidad se siente totalmente ajena." to "Buscá a alguien que te hable con calma y te ayude a anclarte."
                10 -> "Shock total. El bloqueo es tan grande que parece que nada fuera real." to "No estás solo. Si la irrealidad te asusta, buscá apoyo profesional de inmediato."
                else -> "" to ""
            }
            "culpa" -> when (i) {
                1 -> "Un pequeño 'y si...' aparece en tus pensamientos de forma aislada." to "Reconocé que es un pensamiento, no una verdad absoluta."
                2 -> "Te cuestionás una decisión pequeña que tomaste recientemente." to "Recordá que decidiste con lo que sabías en ese momento."
                3 -> "Aparece un reproche suave por algo que sentís que faltó." to "Tratate con la misma compasión que tratarías a un amigo."
                4 -> "La mente vuelve al pasado buscando errores para corregir." to "Escribí ese reproche y al lado poné una frase de perdón."
                5 -> "Sentís el peso de una responsabilidad que quizá no te corresponde." to "Diferenciá entre lo que podías controlar y lo que no."
                6 -> "El juicio interno se vuelve más severo y punzante." to "Buscá un momento para respirar y soltar la tensión del pecho."
                7 -> "Te castigás mentalmente de forma repetitiva por lo ocurrido." to "La culpa no repara el pasado, solo daña tu presente. Elegí cuidarte hoy."
                8 -> "Un sentimiento de fallo personal que te nubla el ánimo." to "Hablalo con alguien. A veces el juicio externo es mucho más tierno que el propio."
                9 -> "La culpa es constante y parece una carga imposible de llevar." to "No sos tus errores. Permitite un momento de tregua hoy."
                10 -> "Un autorreproche devastador que te hace sentir que todo fue tu culpa." to "Buscá ayuda para procesar este peso. No tenés que cargar este juicio solo."
                else -> "" to ""
            }
            "autoexigencia" -> when (i) {
                1 -> "Aparece un leve deseo de 'estar mejor' más rápido." to "Recordá que el duelo no tiene cronómetro."
                2 -> "Te comparás un poco con cómo creés que 'deberías' estar." to "Cada proceso es único. El tuyo también."
                3 -> "Sentís la presión de cumplir con todas tus tareas sin fallar." to "Elegí una tarea para dejarla para mañana sin culpa."
                4 -> "El 'tengo que poder' empieza a sonar fuerte en tu cabeza." to "Buscá una forma de simplificar una sola acción hoy."
                5 -> "Te cuesta permitirte un momento de vulnerabilidad frente a otros." to "Permitite decir 'hoy no puedo' al menos una vez."
                6 -> "La autoexigencia te agota más que el propio dolor." to "Bajá el estándar del día. El 50% hoy es un 100%."
                7 -> "Sentís que si aflojás, todo se va a desmoronar." to "Confía en que podés descansar y el mundo va a seguir ahí."
                8 -> "Una presión constante por ser fuerte y no mostrar debilidad." to "Llorar o cansarse no es debilidad, es ser humano."
                9 -> "Te exigís funcionar al máximo cuando el cuerpo pide frenar." to "Hacé un pacto con vos mismo: hoy lo mínimo es suficiente."
                10 -> "Un perfeccionismo paralizante en medio del dolor." to "Soltá el control por hoy. Dejate sostener por la rutina básica."
                else -> "" to ""
            }
            "tristeza" -> when (i) {
                1 -> "Una melancolía suave, como un día nublado pero tranquilo." to "Disfrutá la calma de este momento."
                2 -> "Un cansancio afectivo que pide un poco de retiro." to "Tomate un té o café en silencio, sin pantallas."
                3 -> "La ausencia se nota, pero podés habitar el día." to "Mirá una foto o recordá algo lindo sin apuro."
                4 -> "La pena se siente más presente en el cuerpo, como un peso." to "Date permiso para estar un poco más lento hoy."
                5 -> "Un nudo en el pecho que aparece con más frecuencia." to "Poné en palabras lo que te duele en un papel."
                6 -> "El vacío se vuelve más difícil de ignorar." to "Buscá compañía o un refugio que te dé paz."
                7 -> "Cuesta encontrar energía para las tareas más simples." to "No fuerces la alegría. La tristeza también necesita ser mirada."
                8 -> "Una pena densa que parece teñir todo lo que hacés." to "Llorá si lo necesitás. El llanto es un lenguaje necesario."
                9 -> "Un dolor profundo que te quita las fuerzas para lo básico." to "Pedí que alguien te cocine o te acompañe en silencio."
                10 -> "Un desborde total. El dolor es tan grande que nubla el futuro." to "No atravieses esto solo. Buscá apoyo profesional o un abrazo seguro."
                else -> "" to ""
            }
            "rabia" -> when (i) {
                1 -> "Una leve irritabilidad ante cosas pequeñas." to "Respirá hondo antes de responder a algo molesto."
                2 -> "Te molesta que el mundo siga girando como si nada." to "Reconocé que tu ritmo hoy es distinto al de los demás."
                3 -> "Aparece un pensamiento de injusticia por lo pasado." to "Escribí por qué se siente injusto para sacarlo de la mente."
                4 -> "Sentís tensión en la mandíbula o en las manos." to "Hacé un poco de ejercicio físico para descargar la energía."
                5 -> "El enojo empieza a buscar culpables, incluso en vos mismo." to "No tomes el enojo como una verdad, sino como una señal de dolor."
                6 -> "Sentís ganas de gritar o romper con la rutina." to "Gritá en una almohada o escribí una carta llena de furia."
                7 -> "La bronca es constante y te cuesta tratar bien a los demás." to "Tomate un tiempo a solas para no lastimar a quien querés."
                8 -> "Un sentimiento de furia por la injusticia de la pérdida." to "Reconocé que la rabia es tristeza que no encuentra salida."
                9 -> "La rabia es tan fuerte que te dan ganas de romper algo." to "Buscá una forma segura de descargar: rompé papeles o caminá rápido."
                10 -> "Un odio o resentimiento profundo que te consume." to "Buscá ayuda para canalizar este fuego antes de que te queme por dentro."
                else -> "" to ""
            }
            "ansiedad" -> when (i) {
                1 -> "Una leve inquietud, como si olvidaras algo." to "Hacé una lista de lo que tenés que hacer hoy."
                2 -> "Te cuesta un poco quedarte quieto en un solo lugar." to "Caminá unos minutos por la casa o el barrio."
                3 -> "Aparecen dudas sobre cómo vas a seguir adelante." to "Concentrate solo en las próximas dos horas."
                4 -> "Sentís que el corazón late un poco más rápido a ratos." to "Hacé tres respiraciones conscientes ahora mismo."
                5 -> "Te preocupa el futuro y te sentís vulnerable." to "Volvé al presente: tocá algo frío o sentí tus pies en el suelo."
                6 -> "La mente no para de proyectar escenarios difíciles." to "Escribí tus miedos para que dejen de dar vueltas en tu cabeza."
                7 -> "Sentís opresión en el pecho o falta de aire." to "Hacé un ejercicio de respiración guiado (tenés uno en el día)."
                8 -> "Un estado de alerta constante, como si algo malo fuera a pasar." to "Bajá los estímulos: apagá la tele y el celular por un rato."
                9 -> "Pánico o desborde inminente. Te sentís atrapado." to "Llamá a alguien que te dé seguridad y te ayude a calmarte."
                10 -> "Ansiedad paralizante. El miedo ocupa todo el espacio." to "Buscá ayuda médica o profesional si sentís que perdés el control."
                else -> "" to ""
            }
            "soledad" -> when (i) {
                1 -> "Un pensamiento pasajero de que nadie entiende del todo." to "Recordá que tu proceso es tuyo, pero no tenés que estar solo."
                2 -> "Extrañás una charla simple con alguien que ya no está." to "Hablale en voz alta, aunque parezca raro, ayuda a soltar."
                3 -> "Sentís que hay un muro entre vos y los demás." to "Mandá un mensaje corto a alguien que te quiera."
                4 -> "El silencio de la casa o el entorno se vuelve pesado." to "Poné música suave o un podcast para acompañar el ambiente."
                5 -> "Te sentís solo incluso cuando estás rodeado de gente." to "No fuerces la conexión, pero permitite estar presente."
                6 -> "Aparece la idea de que a nadie le importa realmente tu dolor." to "Es la tristeza hablando. Alguien está pensando en vos hoy."
                7 -> "Un aislamiento buscado pero que empieza a doler." to "Abrí la ventana o salí a ver gente, aunque no hables con nadie."
                8 -> "Sentís que sos el único en el mundo con este dolor." to "Buscá grupos o testimonios de otros que pasaron por lo mismo."
                9 -> "Un vacío de compañía que se siente insoportable." to "Llamá a una línea de apoyo o a un amigo muy cercano ya mismo."
                10 -> "Soledad absoluta y desoladora. Sentís abandono total." to "Buscá refugio en un profesional o en un grupo de contención."
                else -> "" to ""
            }
            "agotamiento" -> when (i) {
                1 -> "Un poco más de sueño que lo habitual." to "Andate a dormir 15 minutos antes hoy."
                2 -> "Sentís que las piernas pesan un poco al caminar." to "Hacé movimientos suaves de estiramiento."
                3 -> "Te cuesta concentrarte en una lectura larga." to "Leé textos cortos o escuchá audios breves."
                4 -> "El cuerpo pide siesta o descanso a media tarde." to "Si podés, dormí 20 minutos. El cuerpo está procesando mucho."
                5 -> "Hacer lo básico (bañarse, cocinar) requiere esfuerzo." to "Pedí comida o hacé algo muy simple que no requiera energía."
                6 -> "Sentís un cansancio mental que no se va con dormir." to "Evitá las pantallas y buscá luz natural un rato."
                7 -> "Te sentís agotado emocionalmente, sin ganas de hablar." to "Respetá tu necesidad de silencio. No tenés que explicarte."
                8 -> "Un cansancio que llega hasta los huesos. Todo pesa." to "No te exijas nada hoy. Solo existí y cuidate."
                9 -> "Agotamiento extremo. Sentís que no podés ni pensar." to "Dejá todo lo que no sea vital para otro momento."
                10 -> "Colapso físico y mental por el peso del duelo." to "Descansá de verdad. Tu salud es lo primero ahora."
                else -> "" to ""
            }
            "apego" -> when (i) {
                1 -> "Un recuerdo que viene y se va con suavidad." to "Agradecé ese momento que viviste."
                2 -> "Mirás un objeto y te quedás pensando unos segundos." to "Sonreíle al recuerdo, es parte de tu tesoro."
                3 -> "Deseás que las cosas fueran como hace un tiempo." to "Aceptá que el pasado fue hermoso, pero el hoy es lo que tenés."
                4 -> "Te cuesta desprenderte de algo que ya no sirve." to "No te apures. Soltá cuando sientas que tenés dónde apoyarte."
                5 -> "Sentís que si olvidás un detalle, perdés a la persona." to "El vínculo está en tu corazón, no solo en los datos."
                6 -> "El pasado parece mucho más real y brillante que el presente." to "Traé un valor de ese pasado a una acción de hoy."
                7 -> "Te aferrás a rutinas que ya no tienen sentido." to "Probá cambiar una sola cosa pequeña de tu lugar."
                8 -> "Un deseo desesperado de que el tiempo vuelva atrás." to "Respirá y sentí tus pies en el suelo. Estás acá ahora."
                9 -> "Sentís que no podés vivir sin lo que perdiste." to "Buscá ayuda para encontrar nuevas formas de sostenerte."
                10 -> "Apego total que te impide ver cualquier futuro." to "El pasado no se va, se transforma. Trabajalo con un profesional."
                else -> "" to ""
            }
            "aceptacion" -> when (i) {
                1 -> "Un momento fugaz de paz mientras hacés algo cotidiano." to "Atesorá esa calma, es una semilla de lo que viene."
                2 -> "Podés hablar de lo pasado sin que se te quiebre la voz." to "Reconocé el camino recorrido hasta este punto."
                3 -> "Empezás a imaginar cambios positivos en tu rutina." to "Hacé un plan pequeño para la semana que viene."
                4 -> "Sentís que el dolor ya no es el protagonista de cada hora." to "Permitite disfrutar de algo nuevo sin sentir culpa."
                5 -> "Entendés que lo pasado es parte de tu historia, no todo el libro." to "Escribí lo que aprendiste de este proceso."
                6 -> "Aparece una curiosidad renovada por la vida." to "Inscribite en algo o empezá un hobby pequeño."
                7 -> "Sentís que podés ayudar a otros con tu experiencia." to "Escuchá a alguien que esté sufriendo, tu presencia vale."
                8 -> "La aceptación se siente como un suelo firme donde pararse." to "Agradecé tu fortaleza para transitar el desierto."
                9 -> "Mirás el futuro con una claridad que antes no tenías." to "Empezá a construir ese nuevo proyecto que soñaste."
                10 -> "Paz profunda e integración total de la pérdida." to "Celebrá la vida. Honrar lo perdido es vivir plenamente."
                else -> "" to ""
            }
            "esperanza" -> when (i) {
                1 -> "Una pequeña idea de que 'esto también pasará'." to "Mantené ese pensamiento cerca hoy."
                2 -> "Vés una luz en el camino, aunque sea lejana." to "Caminá hacia ella, un paso a la vez."
                3 -> "Sentís que tenés recursos para salir adelante." to "Hacé una lista de tus fortalezas."
                4 -> "Aparece un proyecto que te entusiasma mínimamente." to "Dales una oportunidad a tus nuevas ideas."
                5 -> "Creés que la vida todavía tiene cosas buenas para vos." to "Abrite a recibir una sorpresa agradable hoy."
                6 -> "La esperanza se convierte en una acción concreta." to "Invertí tiempo en algo que te haga bien a largo plazo."
                7 -> "Sentís que el dolor te transformó para mejor." to "Honrá tu transformación con un gesto de bondad."
                8 -> "Vibrás con una energía de reconstrucción y vida." to "Compartí tu esperanza con los que te rodean."
                9 -> "El futuro se ve lleno de posibilidades nuevas." to "No tengas miedo de soñar en grande otra vez."
                10 -> "Plena confianza en el flujo de la vida y tu lugar en ella." to "Viví con intensidad y gratitud cada minuto."
                else -> "" to ""
            }
            else -> "Seguí explorando tu proceso con paciencia." to "Cada día es una oportunidad para conocerte mejor."
        }
    }

    private fun buildJournalInsight(prefs: android.content.SharedPreferences): JournalInsight {
        val entries = (1..30).mapNotNull { day ->
            val text = readJournalForDay(prefs, day)
            if (text.isNotBlank()) day to text else null
        }

        val allText = entries.joinToString(" ") { it.second }
        val totalWords = normalizedWords(allText).size

        if (entries.isEmpty() || totalWords < 5) {
            return JournalInsight(
                mainText = "Todavía escribiste poco como para reconocer un patrón emocional confiable. Con algunos días más de journal, esta sección va a poder acompañarte mejor.",
                secondaryText = "",
                evolutionText = "",
                suggestionText = "Por ahora, usá el journal sin buscar conclusiones. Escribí una frase honesta por día, aunque sea breve."
            )
        }

        val patterns = scorePatterns(allText)
            .filter { it.score > 0 }
            .sortedByDescending { it.score }

        if (patterns.isEmpty()) {
            return JournalInsight(
                mainText = "Lo que venís escribiendo todavía no muestra una emoción dominante clara. Eso también puede pasar cuando el proceso está mezclado.",
                secondaryText = "",
                evolutionText = buildEvolutionText(entries),
                suggestionText = "Seguí escribiendo con honestidad. No hace falta que tenga sentido perfecto."
            )
        }

        val intensities = entries.map { (_, text) ->
            val patternScore = scorePatterns(text).maxOfOrNull { it.score } ?: 0
            estimateIntensity(text, patternScore)
        }
        val avgIntensity = intensities.average().toInt().coerceIn(1, 10)

        val first = patterns[0]
        val (firstMessage, firstSuggestion) = getDetailedInsight(first.key, avgIntensity)
        val second = patterns.getOrNull(1)

        return JournalInsight(
            mainText = "Al leer lo que venís escribiendo, aparece con más fuerza ${first.title.lowercase()}. $firstMessage",
            secondaryText = second?.let { s ->
                val (sMessage, _) = getDetailedInsight(s.key, avgIntensity)
                "También se asoma ${s.title.lowercase()}. $sMessage"
            }.orEmpty(),
            evolutionText = buildEvolutionText(entries),
            suggestionText = buildSuggestionTextForProgress(prefs, first, firstSuggestion)
        )
    }

    private fun buildSuggestionTextForProgress(
        prefs: android.content.SharedPreferences,
        pattern: InsightPattern,
        dynamicSuggestion: String
    ): String {
        val suggestedDays = findRelevantCompletedDays(prefs, pattern)

        return if (suggestedDays.isNotEmpty()) {
            val daysText = suggestedDays.joinToString(", ")
            "$dynamicSuggestion También podría ayudarte volver a revisar ${if (suggestedDays.size == 1) "el día" else "los días"} $daysText, porque ahí ya trabajaste algo relacionado con esto."
        } else {
            "$dynamicSuggestion Por ahora no hace falta volver a días anteriores. Seguí con el día actual y observá si esta emoción vuelve a aparecer."
        }
    }

    private fun findRelevantCompletedDays(
        prefs: android.content.SharedPreferences,
        pattern: InsightPattern
    ): List<Int> {
        val completedDays = (1..30).filter { day ->
            prefs.getBoolean("completed_$day", false)
        }

        if (completedDays.isEmpty()) return emptyList()

        val rankedDays = completedDays.mapNotNull { day ->
            val journalText = readJournalForDay(prefs, day)

            val dayText = buildString {
                append(journalText)
                append(" ")
                append(dayTitles.getOrNull(day - 1).orEmpty())
                append(" ")
                append(dailyReadings.getOrNull(day - 1).orEmpty())
                append(" ")
                append(dailyExercises.getOrNull(day - 1).orEmpty())
                append(" ")
                append(dailyJournals.getOrNull(day - 1).orEmpty())
                append(" ")
                append(dailyActions.getOrNull(day - 1).orEmpty())
            }

            val dayPattern = scorePatterns(dayText)
                .firstOrNull { it.key == pattern.key }

            val score = dayPattern?.score ?: 0

            if (score > 0) {
                day to score
            } else {
                null
            }
        }

        return rankedDays
            .sortedWith(
                compareByDescending<Pair<Int, Int>> { it.second }
                    .thenByDescending { it.first }
            )
            .take(3)
            .map { it.first }
            .sorted()
    }

    private fun normalizedWords(text: String): List<String> {
        val clean = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-zñáéíóúü\\s]".toRegex(), " ")
        return clean.split("\\s+".toRegex()).filter { it.length > 2 }
    }

    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }

    private fun scorePatterns(text: String): List<InsightPattern> {
        val clean = normalizeText(text)

        fun score(words: List<String>, phrases: List<String>): Int {
            var total = 0
            words.forEach { word ->
                total += Regex("\\b${Regex.escape(word)}\\b").findAll(clean).count()
            }
            phrases.forEach { phrase ->
                total += Regex(Regex.escape(phrase)).findAll(clean).count() * 3
            }
            return total
        }

        return listOf(
            InsightPattern(
                key = "shock",
                title = "shock y bloqueo",
                score = score(
                    words = listOf("shock", "bloqueo", "bloqueado", "bloqueada", "irreal", "automatico", "automático", "confuso", "confundido", "desconectado"),
                    phrases = listOf("no lo puedo creer", "no puede ser", "me siento en automatico", "me siento en automático", "como si no fuera real", "no termino de entender")
                ),
                days = "1, 2 y 3",
                message = "Aparecen señales de bloqueo o irrealidad. Tu sistema puede estar intentando procesar algo que todavía se siente demasiado.",
                suggestion = "Hoy no fuerces claridad. Volvé a lo básico: respirar, nombrar lo que pasa y darte seguridad."
            ),
            InsightPattern(
                key = "culpa",
                title = "culpa y reproche personal",
                score = score(
                    words = listOf("culpa", "culpable", "falle", "fallé", "error", "perdon", "perdón", "arrepiento", "responsable"),
                    phrases = listOf("si hubiera", "deberia haber", "debería haber", "tendria que haber", "tendría que haber", "fue mi culpa", "no hice suficiente", "pude haber")
                ),
                days = "7, 17 y 22",
                message = "Hay frases que vuelven al pasado buscando una forma de corregirlo o castigarte por lo que no pudiste hacer.",
                suggestion = "Tal vez hoy no necesitás exigirte una respuesta perfecta, sino mirarte con más honestidad y menos castigo."
            ),
            InsightPattern(
                key = "autoexigencia",
                title = "autoexigencia",
                score = score(
                    words = listOf("deberia", "debería", "tengo", "obligacion", "obligación", "perfecto", "fuerte", "exijo", "exigirme", "cumplir"),
                    phrases = listOf("tengo que poder", "deberia estar mejor", "debería estar mejor", "no puedo aflojar", "tengo que ser fuerte", "no deberia sentir")
                ),
                days = "2, 18 y 22",
                message = "Se nota una presión por estar mejor, funcionar o sostener una versión de fortaleza que quizá hoy pesa demasiado.",
                suggestion = "Podría ayudarte bajar el estándar del día y quedarte solo con una acción posible."
            ),
            InsightPattern(
                key = "tristeza",
                title = "tristeza profunda",
                score = score(
                    words = listOf("triste", "tristeza", "lloro", "llorar", "vacio", "vacío", "dolor", "duele", "extraño", "extrañar", "ausencia", "pena"),
                    phrases = listOf("no tengo ganas", "me siento vacio", "me siento vacío", "me duele mucho", "no puedo parar de llorar", "me pesa todo")
                ),
                days = "10, 14 y 24",
                message = "Aparece una sensación de peso emocional, ausencia o cansancio afectivo que pide espacio, no apuro.",
                suggestion = "Hoy puede servirte permitir la tristeza en un marco cuidado, sin dejar que ocupe todo el día."
            ),
            InsightPattern(
                key = "rabia",
                title = "rabia e injusticia",
                score = score(
                    words = listOf("enojo", "enojado", "rabia", "bronca", "injusto", "injusticia", "odio", "molesta", "molesto", "furia"),
                    phrases = listOf("no es justo", "me da bronca", "me da rabia", "no deberia haber pasado", "no debería haber pasado")
                ),
                days = "5, 6 y 13",
                message = "El enojo parece estar señalando algo que vivís como injusto, no dicho o difícil de aceptar.",
                suggestion = "Antes de actuar desde la rabia, dale una salida segura: escribir, caminar o respirar hasta que baje la intensidad."
            ),
            InsightPattern(
                key = "ansiedad",
                title = "ansiedad y desborde",
                score = score(
                    words = listOf("miedo", "angustia", "ansiedad", "ansioso", "nervioso", "temor", "panico", "pánico", "desbordado", "desborde"),
                    phrases = listOf("no puedo con esto", "no puedo mas", "no puedo más", "me supera", "me siento desbordado", "me cuesta respirar")
                ),
                days = "3, 11 y 21",
                message = "Hay señales de tensión, anticipación o sensación de no poder sostener todo al mismo tiempo.",
                suggestion = "Hoy no intentes ordenar toda tu vida. Volvé al cuerpo, a la respiración y a una tarea concreta."
            ),
            InsightPattern(
                key = "soledad",
                title = "soledad y necesidad de sostén",
                score = score(
                    words = listOf("solo", "sola", "soledad", "aislado", "aislada", "nadie", "acompañe", "compania", "compañía", "abandonado"),
                    phrases = listOf("me siento solo", "me siento sola", "no tengo a nadie", "nadie entiende", "no quiero molestar")
                ),
                days = "9, 20 y 23",
                message = "Aparece una necesidad de compañía o de ser comprendido sin tener que explicar demasiado.",
                suggestion = "Podría ayudarte acercarte a una persona segura con una frase simple, sin pedir soluciones."
            ),
            InsightPattern(
                key = "agotamiento",
                title = "agotamiento emocional",
                score = score(
                    words = listOf("cansado", "cansada", "agotado", "agotada", "pesado", "pesada", "energia", "energía", "dormir", "sueño"),
                    phrases = listOf("no tengo energia", "no tengo energía", "me pesa todo", "estoy agotado", "estoy agotada", "solo quiero dormir")
                ),
                days = "4, 11 y 24",
                message = "Se siente mucho desgaste. El cuerpo parece estar pidiendo menos exigencia y más reparación.",
                suggestion = "Quizá hoy el avance no sea hacer más, sino cuidar lo básico sin culpa."
            ),
            InsightPattern(
                key = "apego",
                title = "apego a lo perdido",
                score = score(
                    words = listOf("volver", "recuperar", "antes", "extraño", "aferrar", "aferrarme", "soltar", "perder", "pasado"),
                    phrases = listOf("quiero que vuelva", "como antes", "no puedo soltar", "me aferro", "volver atras", "volver atrás")
                ),
                days = "14, 25 y 27",
                message = "Hay una parte de vos que todavía intenta sostener la forma anterior de lo que se perdió.",
                suggestion = "Tal vez hoy la pregunta no sea cómo recuperar lo anterior, sino qué forma nueva puede tomar el vínculo con lo vivido."
            ),
            InsightPattern(
                key = "aceptacion",
                title = "aceptación en proceso",
                score = score(
                    words = listOf("acepto", "aceptar", "entiendo", "comprendo", "proceso", "calma", "paz", "aprendo", "seguir", "continuar"),
                    phrases = listOf("de a poco", "puedo seguir", "empiezo a aceptar", "me siento mas tranquilo", "me siento más tranquilo")
                ),
                days = "21, 28 y 30",
                message = "Empiezan a aparecer palabras de integración, calma o posibilidad. Eso no borra el dolor, pero muestra movimiento.",
                suggestion = "Permitite recibir esos momentos de calma sin culpa. También son parte del duelo."
            ),
            InsightPattern(
                key = "esperanza",
                title = "esperanza y reconstrucción",
                score = score(
                    words = listOf("esperanza", "mejor", "avanzar", "futuro", "posible", "camino", "vida", "reconstruir", "quiero"),
                    phrases = listOf("quiero estar mejor", "puedo avanzar", "hay algo posible", "quiero seguir", "un paso mas", "un paso más")
                ),
                days = "15, 19 y 28",
                message = "Se asoman señales de reconstrucción. No son negación del dolor, son pequeñas partes tuyas buscando vida.",
                suggestion = "Cuidá esas señales sin apurarlas. Un paso pequeño alcanza."
            )
        )
    }

    private fun buildEvolutionText(entries: List<Pair<Int, String>>): String {
        if (entries.size < 4) {
            return "Todavía hay pocas notas como para ver una evolución clara, pero ya hay algo importante: estás dejando registro de lo que pasa dentro tuyo."
        }

        val firstText = entries.take(3).joinToString(" ") { it.second }
        val lastText = entries.takeLast(3).joinToString(" ") { it.second }

        val firstTop = scorePatterns(firstText).filter { it.score > 0 }.maxByOrNull { it.score }
        val lastTop = scorePatterns(lastText).filter { it.score > 0 }.maxByOrNull { it.score }

        if (firstTop == null && lastTop == null) {
            return "Tus notas todavía se sienten variadas, sin un patrón dominante claro. Eso puede pasar cuando el duelo se mueve entre varias emociones."
        }

        if (firstTop != null && lastTop != null && firstTop.key != lastTop.key) {
            return "En tus primeras notas aparecía más ${firstTop.title.lowercase()}. En las más recientes empieza a aparecer más ${lastTop.title.lowercase()}. No significa que una etapa haya terminado, pero sí que algo se está moviendo."
        }

        if (lastTop != null) {
            return "En las notas recientes sigue apareciendo ${lastTop.title.lowercase()}. Tal vez esta emoción todavía necesita espacio, cuidado y menos apuro."
        }

        return "Tus notas muestran movimiento, aunque todavía no aparece una dirección emocional clara. Seguí escribiendo sin forzar conclusiones."
    }

    private val lossTypes = listOf(
        "Fallecimiento de un ser querido",
        "Separación o ruptura",
        "Distancia o conflicto familiar",
        "Pérdida laboral o de un proyecto",
        "Pérdida de una mascota",
        "Cambio personal, dejar atrás una versión de mí",
        "Otro tipo de pérdida"
    )

    private val testQuestions = listOf(
        TestQuestion(
            "Cuando pensás en lo ocurrido, qué aparece primero",
            listOf(
                TestOption("Siento que todavía no termino de creerlo.", GriefStage.Shock),
                TestOption("Siento enojo o injusticia.", GriefStage.Anger),
                TestOption("Pienso qué podría haber hecho distinto.", GriefStage.Guilt),
                TestOption("Siento vacío, tristeza o agotamiento.", GriefStage.Sadness),
                TestOption("Duele, pero empiezo a aceptar que pasó.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Qué te pasa cuando intentás seguir con tu día",
            listOf(
                TestOption("Me siento desconectado, como en automático.", GriefStage.Shock),
                TestOption("Me irrito rápido o me molesta todo.", GriefStage.Anger),
                TestOption("Me quedo repasando escenas del pasado.", GriefStage.Guilt),
                TestOption("Me cuesta tener energía para lo básico.", GriefStage.Sadness),
                TestOption("Puedo hacer algunas cosas, aunque duela.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Qué frase se parece más a tu diálogo interno",
            listOf(
                TestOption("Esto no puede estar pasando.", GriefStage.Shock),
                TestOption("No es justo.", GriefStage.Anger),
                TestOption("Tendría que haber hecho algo más.", GriefStage.Guilt),
                TestOption("No tengo fuerzas.", GriefStage.Sadness),
                TestOption("No lo quería, pero necesito aprender a vivir con esto.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Cómo aparece el dolor en tu cuerpo",
            listOf(
                TestOption("Como bloqueo o adormecimiento.", GriefStage.Shock),
                TestOption("Como tensión, calor o presión.", GriefStage.Anger),
                TestOption("Como nudo en el pecho o en el estómago.", GriefStage.Guilt),
                TestOption("Como cansancio profundo.", GriefStage.Sadness),
                TestOption("Como oleadas que puedo observar un poco mejor.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Qué necesitás más hoy",
            listOf(
                TestOption("Sentirme seguro y bajar la exigencia.", GriefStage.Shock),
                TestOption("Descargar sin hacer daño.", GriefStage.Anger),
                TestOption("Dejar de castigarme.", GriefStage.Guilt),
                TestOption("Sentirme acompañado.", GriefStage.Sadness),
                TestOption("Recuperar una rutina posible.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Qué te cuesta más aceptar",
            listOf(
                TestOption("Que esto sea real.", GriefStage.Shock),
                TestOption("Que haya pasado de esta manera.", GriefStage.Anger),
                TestOption("Que no pueda volver atrás.", GriefStage.Guilt),
                TestOption("La ausencia y el vacío que dejó.", GriefStage.Sadness),
                TestOption("Que mi vida tenga que seguir de otra forma.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Cuando hablás de esto con alguien",
            listOf(
                TestOption("Me cuesta ponerlo en palabras.", GriefStage.Shock),
                TestOption("Me sale bronca o dureza.", GriefStage.Anger),
                TestOption("Termino justificándome o culpándome.", GriefStage.Guilt),
                TestOption("Me quiebro o prefiero callarme.", GriefStage.Sadness),
                TestOption("Puedo contarlo con dolor, pero con algo más de claridad.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Qué pasa cuando recordás",
            listOf(
                TestOption("Siento irrealidad, como si fuera una película.", GriefStage.Shock),
                TestOption("Aparece rabia por lo que faltó o sobró.", GriefStage.Anger),
                TestOption("Me quedo atrapado en los detalles.", GriefStage.Guilt),
                TestOption("Me invade la tristeza.", GriefStage.Sadness),
                TestOption("Puedo recordar algunas cosas sin romperme por completo.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Qué relación tenés hoy con el futuro",
            listOf(
                TestOption("Me cuesta imaginarlo.", GriefStage.Shock),
                TestOption("Me molesta que todo tenga que seguir igual.", GriefStage.Anger),
                TestOption("Pienso que no merezco estar bien todavía.", GriefStage.Guilt),
                TestOption("Lo veo pesado o lejano.", GriefStage.Sadness),
                TestOption("Puedo imaginar pequeños pasos, aunque no todo esté claro.", GriefStage.Acceptance)
            )
        ),
        TestQuestion(
            "Qué sería un avance realista para hoy",
            listOf(
                TestOption("Aceptar una pequeña parte de lo ocurrido.", GriefStage.Shock),
                TestOption("Expresar enojo sin lastimar.", GriefStage.Anger),
                TestOption("Soltar un reproche por unas horas.", GriefStage.Guilt),
                TestOption("Hacer una acción básica de cuidado.", GriefStage.Sadness),
                TestOption("Dar un paso hacia mi vida actual.", GriefStage.Acceptance)
            )
        )
    )

    private val dayTitles = listOf(
        "Nombrar la pérdida",
        "Bajar la exigencia",
        "Respirar antes de entender",
        "Cuidar el cuerpo",
        "Permitir lo que aparece",
        "Escribir lo no dicho",
        "Mirar la culpa con honestidad",
        "Ordenar recuerdos",
        "Pedir sostén",
        "Atravesar la tristeza",
        "Volver a lo básico",
        "Caminar con el dolor",
        "Reconocer disparadores",
        "Honrar sin aferrarse",
        "Reconocer pequeños avances",
        "Crear un ritual",
        "Soltar el pasado imposible",
        "Aceptar días difíciles",
        "Recuperar una rutina",
        "Abrir espacio al amor",
        "Volver al presente",
        "Perdonarte de a poco",
        "Hablar con verdad",
        "Dormir y reparar",
        "Transformar el vínculo",
        "Elegir qué conservar",
        "Soltar una carga",
        "Mirar hacia adelante",
        "Integrar lo vivido",
        "Cerrar este ciclo"
    )

    private val dailyReadings = listOf(
        "Hoy el primer paso no es entender todo. Es permitirte nombrar lo que pasó sin maquillarlo y sin exigirte estar mejor. Una pérdida empieza a ordenarse cuando deja de ser una sombra sin nombre.",
        "El duelo suele traer una exigencia silenciosa: estar fuerte, responder bien, seguir funcionando. Hoy no necesitás cumplir con una imagen de fortaleza. Necesitás tratarte con un poco menos de dureza.",
        "Cuando el dolor aparece, la mente quiere explicaciones inmediatas. Pero antes de pensar mejor, el cuerpo necesita sentirse a salvo. Respirar no resuelve la pérdida, pero puede darte un suelo para atravesarla.",
        "El cuerpo también duela. A veces lo muestra como cansancio, hambre cerrada, tensión, sueño liviano o falta de energía. Cuidarlo no es superficial. Es una manera concreta de decir: sigo acá.",
        "No hay emociones correctas en el duelo. Puede aparecer tristeza, bronca, alivio, culpa, confusión o vacío. Lo importante hoy no es juzgar lo que aparece, sino aprender a observarlo sin hacerte daño.",
        "Muchas heridas quedan enganchadas en lo que no pudo decirse. Palabras pendientes, despedidas incompletas, pedidos que llegaron tarde. Escribir no cambia el pasado, pero puede darle salida a lo que quedó encerrado.",
        "La culpa intenta convencerte de que si te castigás lo suficiente, algo se repara. Pero castigarte no devuelve lo perdido. Hoy la tarea es mirar con honestidad sin convertirte en tu propio juez.",
        "Recordar puede doler porque trae de vuelta algo que ya no está igual. Pero también puede ayudarte a distinguir entre amor y apego, entre memoria y prisión. No todo recuerdo tiene que romperte.",
        "El dolor se vuelve más pesado cuando se vive completamente solo. Pedir sostén no significa pedir que alguien te salve. Significa permitir que otro sea testigo de tu proceso por un momento.",
        "La tristeza no es una falla del proceso. Es una forma de reconocer que algo importó. Hoy no hace falta empujarla afuera. Hace falta darle un espacio cuidado para que no tenga que invadirlo todo.",
        "Volver a lo básico puede parecer poco, pero en duelo es mucho. Comer, dormir, tomar agua, bañarte o salir unos minutos son formas silenciosas de reconstrucción. No subestimes lo pequeño.",
        "Caminar con el dolor no significa que dejó de pesar. Significa que dejás de esperar a estar perfecto para moverte. A veces el cuerpo avanza primero y el ánimo alcanza después.",
        "Una recaída no borra el camino hecho. Muchas veces solo muestra que algo tocó una zona sensible. Identificar qué la activó puede ayudarte a cuidarte mejor la próxima vez.",
        "Honrar lo vivido no exige quedarte detenido. Podés darle un lugar digno a esa historia sin entregar tu presente por completo. Honrar también puede ser aprender a vivir con más conciencia.",
        "A veces esperás grandes señales de avance, pero el duelo cambia en detalles: respirar un poco mejor, llorar sin hundirte, pedir ayuda, comer algo, dormir una hora más. Eso también cuenta.",
        "Un ritual simple puede ordenar lo que por dentro se siente caótico. No tiene que ser religioso ni solemne. Solo necesita darte un momento claro para reconocer, agradecer, llorar o soltar.",
        "La mente vuelve al pasado buscando una salida que ya no existe. Repetir la escena una y otra vez agota. Hoy la práctica es reconocer ese impulso y volver, aunque sea por segundos, al presente.",
        "Habrá días que parezcan retroceso. No lo son necesariamente. Un día difícil no invalida el proceso. Solo pide una versión más simple de vos, con menos exigencia y más cuidado.",
        "La rutina no borra la pérdida, pero ayuda a que la vida vuelva a tener bordes. Un pequeño hábito puede funcionar como una cuerda para salir lentamente del desorden emocional.",
        "El amor no desaparece porque algo cambió o terminó. A veces queda sin forma conocida y por eso duele. Hoy podés empezar a preguntarte cómo amar sin aferrarte al modo anterior.",
        "El presente puede sentirse muy pequeño frente a todo lo perdido. Pero es el único lugar desde donde podés respirar, decidir, pedir ayuda y cuidarte. Volver al presente es volver a vos.",
        "Perdonarte no significa negar errores ni evitar responsabilidades. Significa dejar de usar el dolor como castigo permanente. La reparación, si existe, necesita más conciencia que crueldad.",
        "Decir la verdad de cómo estás puede dar miedo, sobre todo si estás acostumbrado a sostener una imagen. Pero una frase honesta puede abrir una puerta de alivio donde antes había aislamiento.",
        "El descanso también es parte del duelo. No tenés que resolver todo antes de dormir. A veces reparar empieza cuando aceptás dejar algo pendiente hasta mañana.",
        "Algunos vínculos no terminan, cambian de forma. Tal vez ya no están en la rutina, en la presencia o en el plan que imaginabas. Pero pueden quedar como enseñanza, memoria, límite o amor transformado.",
        "No todo debe irse con la pérdida. Algunas cosas pueden quedarse en vos como fuerza, ternura, aprendizaje o claridad. Elegir qué conservar evita que el dolor se lleve también lo valioso.",
        "Soltar no es borrar. Soltar es dejar de cargar algo de una manera que te lastima. Puede ser una culpa, una expectativa, una promesa imposible o una versión de la historia que ya no te ayuda.",
        "Mirar hacia adelante puede sentirse como una traición, pero no lo es. Seguir viviendo no niega lo perdido. Es una forma de aceptar que tu vida todavía necesita tu presencia.",
        "Integrar no significa que ya no duela. Significa que lo vivido empieza a ocupar un lugar dentro de tu historia sin ocupar todo el espacio. Es dolor con más aire alrededor.",
        "Cerrar este ciclo no significa cerrar el amor ni olvidar. Significa reconocer el camino recorrido, agradecer tu esfuerzo y elegir seguir. No perfecto. No sin dolor. Pero sí con más conciencia."
    )

    private val dailyExercises = listOf(
        "Escribí: lo que perdí fue... y lo que más me duele de esto es...",
        "Elegí una sola obligación que puedas bajar, postergar o simplificar hoy.",
        "Hacé cinco respiraciones lentas, inhalando en cuatro tiempos y exhalando en seis.",
        "Tomá agua, comé algo simple y mové el cuerpo durante diez minutos.",
        "Escribí sin filtro lo que aparece, aunque sea rabia, confusión o tristeza.",
        "Escribí una carta que no necesitás enviar.",
        "Separá en dos columnas: hechos reales y reproches mentales.",
        "Elegí un recuerdo y escribí qué valor querés conservar de él.",
        "Mandale un mensaje breve a alguien de confianza.",
        "Permitite quince minutos para llorar, escribir o quedarte en silencio.",
        "Hacé una acción básica de cuidado, aunque parezca pequeña.",
        "Caminá diez minutos observando tu respiración y el entorno.",
        "Anotá qué activó el dolor hoy y qué necesitaste en ese momento.",
        "Creá un gesto simple para honrar lo vivido.",
        "Escribí tres señales pequeñas de avance desde que empezaste.",
        "Diseñá un ritual de cinco minutos que puedas repetir cuando lo necesites.",
        "Cada vez que aparezca un si hubiera, escribí: hoy no puedo cambiar eso.",
        "Reducí el día a lo esencial: agua, comida, descanso y compañía si la necesitás.",
        "Elegí una rutina pequeña para repetir mañana.",
        "Hacé un gesto de amor hacia alguien o hacia vos.",
        "Nombrá cinco cosas que ves, cuatro que sentís, tres que escuchás, dos que olés y una que agradecés.",
        "Escribí: hoy puedo empezar a perdonarme por...",
        "Decile a alguien una frase honesta sobre cómo estás.",
        "Una hora antes de dormir, escribí lo pendiente y dejalo para mañana.",
        "Escribí qué forma puede tomar ahora ese vínculo.",
        "Hacé una lista de cinco cosas que querés conservar.",
        "Escribí una carga que querés empezar a soltar.",
        "Anotá tres cosas pequeñas que te gustaría recuperar.",
        "Respondé: qué aprendí, qué necesito cuidar, qué ya no quiero repetir.",
        "Escribí una carta final con cuatro partes: agradezco, acepto, suelto y elijo."
    )

    private val dailyJournals = listOf(
        "Qué parte de esta pérdida me cuesta más nombrar.",
        "Qué me estoy exigiendo que hoy podría soltar.",
        "Qué cambia en mi cuerpo cuando respiro más lento.",
        "Qué necesita mi cuerpo que vengo ignorando.",
        "Qué emoción estoy intentando esconder.",
        "Qué palabra quedó pendiente.",
        "Qué culpa es real y cuál es castigo.",
        "Qué recuerdo quiero cuidar sin quedarme atrapado.",
        "A quién puedo dejar entrar un poco.",
        "Qué tristeza necesito permitirme.",
        "Qué básico puedo sostener hoy.",
        "Cómo se siente caminar con esto en vez de contra esto.",
        "Qué disparador reconocí hoy.",
        "Qué significa honrar sin detener mi vida.",
        "Qué avance pequeño no estoy valorando.",
        "Qué ritual podría darme calma.",
        "Qué pasado imposible sigo intentando cambiar.",
        "Qué necesito en un día malo.",
        "Qué rutina me daría un poco de suelo.",
        "Dónde todavía hay amor disponible.",
        "Qué me devuelve al presente.",
        "Qué parte de mí necesita perdón.",
        "Qué verdad puedo decir sin explicarme de más.",
        "Qué necesito soltar antes de dormir.",
        "Cómo puede transformarse este vínculo.",
        "Qué quiero conservar sin cargar dolor extra.",
        "Qué carga ya no me corresponde llevar.",
        "Qué paso pequeño puedo imaginar.",
        "Qué lugar ocupa esta experiencia en mi historia.",
        "Qué elijo para seguir."
    )

    private val dailyActions = listOf(
        "Decir en voz baja una frase honesta sobre lo que pasó.",
        "Hacer una pausa sin justificarla.",
        "Respirar tres veces antes de revisar mensajes o recuerdos.",
        "Tomar un vaso de agua lentamente.",
        "Permitir una emoción sin actuarla de inmediato.",
        "Guardar o romper la carta, según lo que te dé más paz.",
        "Elegir una frase de trato más amable hacia vos.",
        "Cuidar un objeto, foto o recuerdo durante unos minutos.",
        "Pedir compañía sin pedir soluciones.",
        "Después del momento de tristeza, hacer una acción simple de cuidado.",
        "Comer algo sencillo.",
        "Salir a caminar o abrir una ventana.",
        "Anotar el disparador en una nota.",
        "Encender una vela, rezar, agradecer o hacer silencio.",
        "Reconocer un avance en voz alta.",
        "Repetir tu ritual una vez.",
        "Volver al presente con una acción física concreta.",
        "No tomar decisiones importantes si estás desbordado.",
        "Preparar algo pequeño para mañana.",
        "Enviar o recibir una muestra simple de afecto.",
        "Nombrar el lugar donde estás y la hora del día.",
        "Bajar el tono del juicio interno.",
        "Hablar con una persona segura.",
        "Apagar pantallas unos minutos antes.",
        "Elegir una nueva forma de presencia.",
        "Guardar una enseñanza por escrito.",
        "Hacer un gesto simbólico de soltar.",
        "Planear un paso posible, no perfecto.",
        "Agradecerte por haber llegado hasta acá.",
        "Cerrar con una respiración lenta y una frase de continuidad."
    )

    private fun shortEmotionLabel(label: String): String {
        val clean = Normalizer.normalize(label.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return when {
            clean.contains("shock") || clean.contains("bloqueo") -> "Shock"
            clean.contains("culpa") -> "Culpa"
            clean.contains("tristeza") -> "Tristeza"
            clean.contains("ansiedad") || clean.contains("desborde") -> "Ansiedad"
            clean.contains("rabia") || clean.contains("enojo") || clean.contains("injusticia") -> "Rabia"
            clean.contains("aceptacion") || clean.contains("aceptación") -> "Aceptación"
            clean.contains("esperanza") || clean.contains("reconstruccion") || clean.contains("reconstrucción") -> "Esperanza"
            else -> label.take(12)
        }
    }
}