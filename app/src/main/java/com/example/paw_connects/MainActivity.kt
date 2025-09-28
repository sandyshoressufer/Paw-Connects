package com.example.paw_connects

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

// ---------------- Navigation routes ----------------
object Routes {
    const val SWIPE = "swipe"
    const val MATCHES = "matches"
    const val RECIPE = "recipe"
    const val LEARN = "learn"   // etiqueta "Learn" (quiz de seguros)
    const val PROFILE = "profile"
    const val CHAT = "chat"
}

// ---------------- Activity ----------------
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PawConnectsApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PawConnectsApp(appVm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val logoId = drawableId("pawconnect_logo")
                    if (logoId != 0) {
                        Image(
                            painter = painterResource(logoId),
                            contentDescription = "PawConnects Logo",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("PawConnects", fontWeight = FontWeight.Bold)
                }
            })
        },
        bottomBar = { BottomNavBar(nav) }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.SWIPE,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.SWIPE)   { SwipeScreen(appVm) }
            composable(Routes.MATCHES) { MatchesScreen(appVm, onOpenChat = { nav.navigate(Routes.CHAT) }) }
            composable(Routes.RECIPE)  { RecipeScreen(appVm) }
            composable(Routes.LEARN)   { LearnScreen() }      // Quiz dentro de "Learn"
            composable(Routes.PROFILE) { ProfileScreen(appVm) }
            composable(Routes.CHAT)    { ChatScreen() }       // Bella ↔ Buddy
        }
    }
}

@Composable
fun BottomNavBar(nav: NavHostController) {
    val items = listOf(
        Routes.SWIPE to "Swipe",
        Routes.MATCHES to "Matches",
        Routes.RECIPE to "Batch",
        Routes.LEARN to "Learn",     // SOLO "Learn"
        Routes.PROFILE to "Profile",
        Routes.CHAT to "Chat"
    )
    NavigationBar {
        val current by nav.currentBackStackEntryAsState()
        val currentRoute = current?.destination?.route
        items.forEach { (route, label) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { nav.navigate(route) },
                icon = {},
                label = { Text(label) }
            )
        }
    }
}

// ---------------- utils ----------------
@Composable
fun drawableId(name: String): Int {
    val ctx = LocalContext.current
    return remember(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName) }
}

// ---------------- models ----------------
enum class Sex { MALE, FEMALE }
enum class ActivityTag { RUNNING, BEACH, HIKING, PARK, AGILITY }

data class Dog(
    val id: Int,
    val name: String,
    val ageYears: Int,
    val weightLb: Double,
    val sex: Sex,
    val neutered: Boolean,
    val breed: String,
    val allergies: List<String>,
    val activities: Set<ActivityTag>,
    val imageKey: String = "" // drawable name without extension
)
data class Match(val myDog: Dog, val otherDog: Dog)
data class RecipePlan(
    val kcalPerDay: Int,
    val ozPerDay: Double,
    val ozPerMeal: Double,
    val totalBatchLb30d: Double,
    val perIngredientOz: Map<String, Double>,
    val notes: String,
)
data class ChatMessage(val from: String, val text: String, val time: String)
data class QuizQ(val q: String, val options: List<String>, val correctIdx: Int, val expl: String)

// ---------------- ViewModel ----------------
class AppViewModel : ViewModel() {
    // ---- SWIPABLE PROFILES (other users): Bastian, Chester, Bella, Julia ----
    private val bastian = Dog(
        id = 101, name = "Bastian", ageYears = 5, weightLb = 75.0,
        sex = Sex.MALE, neutered = true, breed = "Goldendoodle",
        allergies = emptyList(),
        activities = setOf(ActivityTag.RUNNING, ActivityTag.BEACH),
        imageKey = "bastian"
    )
    private val chester = Dog(
        id = 102, name = "Chester", ageYears = 3, weightLb = 10.0,
        sex = Sex.MALE, neutered = false, breed = "Mini Goldendoodle",
        allergies = emptyList(),
        activities = setOf(ActivityTag.BEACH, ActivityTag.PARK),
        imageKey = "chester"
    )
    private val bella = Dog(
        id = 103, name = "Bella", ageYears = 4, weightLb = 50.0,
        sex = Sex.FEMALE, neutered = true, breed = "Mixed",
        allergies = emptyList(),
        activities = setOf(ActivityTag.PARK, ActivityTag.RUNNING),
        imageKey = "bella"
    )
    private val julia = Dog(
        id = 104, name = "Julia", ageYears = 6, weightLb = 80.0,
        sex = Sex.FEMALE, neutered = true, breed = "Boxer",
        allergies = emptyList(),
        activities = setOf(ActivityTag.PARK),
        imageKey = "julia"
    )

    // ---- YOUR ACTIVE DOG (who swipes) ----
    private val myDogMe = Dog(
        id = 1, name = "Buddy", ageYears = 4, weightLb = 55.0,
        sex = Sex.MALE, neutered = true, breed = "Mixed",
        allergies = emptyList(),
        activities = setOf(ActivityTag.PARK, ActivityTag.RUNNING),
        imageKey = "" // no photo -> shows initial
    )

    var myDogs = mutableStateListOf(myDogMe)
        private set

    var activeDogId by mutableStateOf(myDogMe.id)
        private set

    // Swipe order
    var otherDogs = mutableStateListOf(bastian, chester, bella, julia)
        private set

    var currentIndex by mutableStateOf(0)
        private set

    var matches = mutableStateListOf<Match>()
        private set

    fun setActiveDog(id: Int) { activeDogId = id }

    fun likeCurrentDog() {
        val myDog = myDogs.first { it.id == activeDogId }
        val other = otherDogs.getOrNull(currentIndex) ?: return
        if (myDog.activities.intersect(other.activities).isNotEmpty()) {
            matches.add(Match(myDog, other))
        }
        currentIndex++
    }
    fun skipCurrentDog() { currentIndex++ }
}

// ---------------- Swipe ----------------
@Composable
fun SwipeScreen(vm: AppViewModel) {
    val dog = vm.otherDogs.getOrNull(vm.currentIndex)
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Active dog: " + vm.myDogs.first { it.id == vm.activeDogId }.name,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        if (dog == null) {
            Text("No more profiles today. Go to Matches or create a batch recipe!")
        } else {
            SwipeCard(
                dog = dog,
                onSwipedRight = { vm.likeCurrentDog() },
                onSwipedLeft = { vm.skipCurrentDog() }
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { vm.skipCurrentDog() }) { Text("👎 Skip") }
                Button(onClick = { vm.likeCurrentDog() }) { Text("👍 Like") }
            }
        }
    }
}

@Composable
fun SwipeCard(
    dog: Dog,
    onSwipedRight: () -> Unit,
    onSwipedLeft: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val threshold = 300f
    val watermarkId = drawableId("pawconnect_logo")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(dog.id) {
                detectDragGestures(
                    onDragEnd = {
                        val x = offsetX.value
                        when {
                            x > threshold -> { onSwipedRight(); scope.launch { offsetX.snapTo(0f) } }
                            x < -threshold -> { onSwipedLeft(); scope.launch { offsetX.snapTo(0f) } }
                            else -> scope.launch { offsetX.animateTo(0f, animationSpec = tween(200)) }
                        }
                    }
                ) { _, drag ->
                    scope.launch { offsetX.snapTo(offsetX.value + drag.x) }
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5D6)) // amarillo suave
    ) {
        // Marca de agua (logo medioclaro, sin borde)
        Box(Modifier.fillMaxSize()) {
            if (watermarkId != 0) {
                Image(
                    painter = painterResource(watermarkId),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(220.dp)
                        .graphicsLayer(alpha = 0.08f) // opacidad baja
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val resId = drawableId(dog.imageKey)
                if (resId != 0) {
                    Image(
                        painter = painterResource(resId),
                        contentDescription = dog.name,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(60.dp))
                            .background(Color(0xFFE0E0E0))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(60.dp))
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) { Text(dog.name.take(1), fontSize = 48.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                Text("${dog.name} · ${dog.breed}", style = MaterialTheme.typography.titleLarge)
                Text("${dog.ageYears} yrs · ${dog.weightLb} lb · ${if (dog.neutered) "neutered" else "intact"}")
                Text("Allergies: ${if (dog.allergies.isEmpty()) "—" else dog.allergies.joinToString()}")
                Text("Activities: ${dog.activities.joinToString { it.name.lowercase().replaceFirstChar(Char::uppercase) }}")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Swipe right to match if you share activities",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ---------------- Matches ----------------
@Composable
fun MatchesScreen(vm: AppViewModel, onOpenChat: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Matches", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        if (vm.matches.isEmpty()) {
            Text("No matches yet. Try swiping.")
        } else {
            vm.matches.forEach { m ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${m.myDog.name} ❤ ${m.otherDog.name}", fontWeight = FontWeight.Bold)
                            Text("Shared activities: ${m.myDog.activities.intersect(m.otherDog.activities).joinToString { it.name }}")
                        }
                        Button(onClick = onOpenChat) { Text("Open chat") }
                    }
                }
            }
        }
    }
}

// ---------------- Recipe / Batch (lb/oz) ----------------
private fun gToOz(g: Int): Double = g / 28.3495
private fun round1(x: Double) = round(x * 10.0) / 10.0

fun generateRecipeFor30Days(dog: Dog): RecipePlan {
    val weightKg = dog.weightLb * 0.45359237
    val factor = if (ActivityTag.RUNNING in dog.activities) 1.9 else 1.6
    val rer = 70.0 * weightKg.pow(0.75)
    val kcalDay = (rer * factor).roundToInt()

    val kcalTurkey = 170
    val kcalSweet = 86
    val kcalCarrot = 41
    val kcalBroccoli = 34

    val base = mutableMapOf(
        "turkey" to 60.0,
        "sweet_potato" to 30.0,
        "carrot" to 5.0,
        "broccoli" to 5.0
    )
    fun isAllergic(key: String): Boolean = when (key) {
        "turkey" -> "turkey" in dog.allergies
        "sweet_potato" -> "sweet_potato" in dog.allergies || "sweet potato" in dog.allergies
        "carrot" -> "carrot" in dog.allergies
        "broccoli" -> "broccoli" in dog.allergies
        else -> false
    }

    base.keys.filter { isAllergic(it) }.forEach { base.remove(it) }
    val sum = base.values.sum()
    val normalized = base.mapValues { it.value / sum }

    val kcalPer100g =
        (normalized["turkey"] ?: 0.0) * kcalTurkey +
                (normalized["sweet_potato"] ?: 0.0) * kcalSweet +
                (normalized["carrot"] ?: 0.0) * kcalCarrot +
                (normalized["broccoli"] ?: 0.0) * kcalBroccoli

    val gramsDay = (kcalDay / (kcalPer100g / 100.0)).roundToInt()
    val gramsMeal = (gramsDay / 2.0).roundToInt()

    val ozDay = gToOz(gramsDay)
    val ozMeal = gToOz(gramsMeal)
    val totalOz30 = ozDay * 30
    val totalLb30 = totalOz30 / 16.0

    val mixGrams30 = normalized.mapValues { (it.value * (gramsDay * 30)).roundToInt() }
    val perIngOz = mixGrams30.mapValues { round1(gToOz(it.value)) }

    val notes = buildString {
        append("Note: Educational estimate for demo. Consult your veterinarian for a clinical plan. ")
        val excl = listOf("turkey","sweet_potato","carrot","broccoli").filter { isAllergic(it) }
        if (excl.isNotEmpty()) append("Excluded for allergies: ${excl.joinToString()}.")
    }

    return RecipePlan(
        kcalPerDay = kcalDay,
        ozPerDay = round1(ozDay),
        ozPerMeal = round1(ozMeal),
        totalBatchLb30d = round1(totalLb30),
        perIngredientOz = perIngOz,
        notes = notes
    )
}

@Composable
fun RecipeScreen(vm: AppViewModel) {
    val dog = vm.myDogs.first { it.id == vm.activeDogId }
    var plan by remember { mutableStateOf<RecipePlan?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("30-day Batch", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("Dog: ${dog.name} (${dog.weightLb} lb, ${dog.ageYears} yrs)")
        Text("Allergies: ${if (dog.allergies.isEmpty()) "—" else dog.allergies.joinToString()}")
        Text("Activities: ${dog.activities.joinToString { it.name }}")
        Spacer(Modifier.height(12.dp))
        Button(onClick = { plan = generateRecipeFor30Days(dog) }) { Text("GENERATE BATCH") }
        Spacer(Modifier.height(12.dp))
        plan?.let { p ->
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 80.dp)
            ) {
                Text("Calories/day: ${p.kcalPerDay}")
                Text("Ounces/day: ${p.ozPerDay}")
                Text("Ounces per meal (2/day): ${p.ozPerMeal}")
                Text("Total batch (30d): ${p.totalBatchLb30d} lb")
                Spacer(Modifier.height(8.dp))
                Text("Ingredient breakdown (30d):", fontWeight = FontWeight.Bold)
                p.perIngredientOz.forEach { (k, v) ->
                    val label = when (k) {
                        "turkey" -> "Ground turkey 93/7"
                        "sweet_potato" -> "Sweet potato"
                        "carrot" -> "Carrot"
                        "broccoli" -> "Broccoli"
                        else -> k
                    }
                    val lb = floor(v / 16.0).toInt()
                    val oz = round1(v - lb * 16)
                    Text("• $label: ${if (lb>0) "$lb lb " else ""}$oz oz")
                }
                Spacer(Modifier.height(8.dp))
                Text(p.notes, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// ---------------- Learn (quiz de seguros) ----------------
@Composable
fun LearnScreen() {
    val questions = remember {
        listOf(
            QuizQ(
                "When should you buy pet insurance?",
                listOf("After the pet is already sick", "At adoption or before the first vet visit", "Never"),
                1,
                "Buying early avoids pre-existing condition exclusions and is usually cheaper."
            ),
            QuizQ(
                "What does an accident plan cover?",
                listOf("Only vaccines", "Injuries from accidents (e.g., broken leg)", "Everything including grooming"),
                1,
                "Accident plans cover sudden injuries; wellness/routine care is usually separate."
            ),
            QuizQ(
                "What affects your premium?",
                listOf("Breed, age, and ZIP code", "Collar color", "Number of app likes"),
                0,
                "Real risk factors include genetics/breed, age, and local veterinary cost index."
            )
        )
    }

    var idx by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(-1) }
    var score by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Learn", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        val q = questions[idx]
        Text(q.q, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        q.options.forEachIndexed { i, opt ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == i, onClick = { selected = i })
                    Spacer(Modifier.width(8.dp))
                    Text(opt)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (selected == q.correctIdx) score++
                selected = -1
                if (idx < questions.lastIndex) idx++
            },
            enabled = selected != -1 && idx < questions.lastIndex
        ) { Text("Next") }

        if (selected != -1) {
            Spacer(Modifier.height(8.dp))
            Text("Why: ${q.expl}", fontSize = 12.sp, color = Color.Gray)
        }

        if (idx == questions.lastIndex) {
            Spacer(Modifier.height(16.dp))
            Text("Score: $score/${questions.size}", fontWeight = FontWeight.Bold)
            Text("Takeaway: Buy early, know coverage, avoid exclusions.")
        }
    }
}

// ---------------- Profile ----------------
@Composable
fun ProfileScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My dogs", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        vm.myDogs.forEach { d ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = vm.activeDogId == d.id, onClick = { vm.setActiveDog(d.id) })
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${d.name} (${d.breed})", fontWeight = FontWeight.Bold)
                        Text("${d.ageYears} yrs · ${d.weightLb} lb · ${if (d.neutered) "neutered" else "intact"}")
                        Text("Allergies: ${if (d.allergies.isEmpty()) "—" else d.allergies.joinToString()}")
                        Text("Act: ${d.activities.joinToString { it.name }}")
                    }
                    Button(onClick = { /* future edit */ }) { Text("Edit") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Note: In this MVP, data lives in memory (not persisted).", fontSize = 12.sp, color = Color.Gray)
    }
}

// ---------------- Chat (Bella ↔ Buddy, Key Biscayne) ----------------
@Composable
fun ChatScreen() {
    val msgs = listOf(
        ChatMessage("Bella's human", "Hi! Bella loved your running post. Would Buddy like a beach meetup?", "9:02 AM"),
        ChatMessage("Buddy's human", "Yes! Key Biscayne works great. How about Hobie Beach?", "9:03 AM"),
        ChatMessage("Bella's human", "Perfect. Next Sunday 9:00 AM? We'll bring water and treats.", "9:05 AM"),
        ChatMessage("Buddy's human", "Done. See you next Sunday at Hobie Beach, Key Biscayne, 9:00 AM. 🐾", "9:06 AM")
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chat: Bella ↔ Buddy", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        msgs.forEach { m ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(m.from, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp)); Text(m.text)
                    Spacer(Modifier.height(4.dp)); Text(m.time, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("(Input disabled in demo)", fontSize = 12.sp, color = Color.Gray)
    }
}
