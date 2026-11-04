package com.wisdomtower.academy.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wisdomtower.academy.ui.theme.wisdomPressClickable
import com.wisdomtower.academy.ui.theme.wisdomScreenEntrance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.HomeBannerRepository
import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.ui.auth.SignInPill
import com.wisdomtower.academy.ui.study.HubGateDialog
import com.wisdomtower.academy.ui.packages.ENTRANCE_BASE_PATH
import com.wisdomtower.academy.ui.packages.ENTRANCE_IMAGE_ASSET
import com.wisdomtower.academy.ui.packages.OpenCard
import com.wisdomtower.academy.ui.packages.OpenCardLayout
import com.wisdomtower.academy.ui.packages.openGrid
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.OutfitFontFamily
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentFuchsia
import com.wisdomtower.academy.ui.theme.WisdomAccentIndigo
import com.wisdomtower.academy.ui.theme.WisdomAccentOrange
import com.wisdomtower.academy.ui.theme.WisdomAccentPurple
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomAccentViolet
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import com.wisdomtower.academy.ui.theme.wisdomReveal
import com.wisdomtower.academy.ui.theme.wisdomHeaderParallax
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ProgramItem(
    val id: String,
    val name: String,
    val path: String,
    val assetImage: String,
    val webImageFallback: String,
    val accentColor: Color
)

data class ResourceGuide(
    val name: String,
    val path: String,
    val icon: ImageVector,
    val accentColor: Color
)

// Cards in the order of the website home / academy page (LandingPathways.tsx, 2026-10-11):
// ECE, Freshman, Grade 9, 10, 11, 12 (independent cards, no combined "Grade 9-12"), Entrance Questions,
// Entrance Questions, Remedial, UAT, COC, GAT, Exit Exam. Grade pictures are the website package images.
//
// ECE: the website card links to /academy/special-packages/electrical-computer-engineering, a page that is not a
// catalog package, so the app router sent it to the website (online only). The native ECE package screen
// (path .../sem-1) already shows BOTH semesters, every course and the six hub cards without a connection,
// so the card opens that. Name and picture (ece.jpg, kept in step with the website by `copySpecialPackageImages`)
// are unchanged.
//
// D3 (day mode): HOME_PROGRAMS and OTHER_RESOURCES are now GETTERS (`get() = listOf(...)`), not values built once at class
// load. The accent colours are mode-aware tokens, and a list built once would keep the colours of the mode the app started
// in, so switching Dark / Day would not recolour the tiles until a restart. Same items, same order, same callers.
val HOME_PROGRAMS: List<ProgramItem>
    get() = listOf(
        ProgramItem(
            id = "ece",
            name = "ECE Engineering",
            path = "/academy/special-packages/electrical-computer-engineering/sem-1",
            assetImage = "file:///android_asset/images/special-packages/ece.jpg",
            webImageFallback = "https://wisdom-tower-academy.live/images/special-packages/ece.jpg",
            accentColor = WisdomAccentViolet
        ),
        ProgramItem(
            id = "freshman",
            name = "Freshman",
            path = "/academy/freshman",
            assetImage = "file:///android_asset/images/packages/freshman_00241b.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/freshman_00241b.jpeg",
            accentColor = WisdomAccentPurple
        ),
        ProgramItem(
            id = "grade-9",
            name = "Grade 9",
            path = "/academy/grades/9",
            assetImage = "file:///android_asset/images/packages/grade-9_67df27.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/grade-9_67df27.jpeg",
            accentColor = WisdomAccentSky
        ),
        ProgramItem(
            id = "grade-10",
            name = "Grade 10",
            path = "/academy/grades/10",
            assetImage = "file:///android_asset/images/packages/grade-10_156767.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/grade-10_156767.jpeg",
            accentColor = WisdomAccentViolet
        ),
        ProgramItem(
            id = "grade-11",
            name = "Grade 11",
            path = "/academy/grades/11",
            assetImage = "file:///android_asset/images/packages/grade-11_6309fc.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/grade-11_6309fc.jpeg",
            accentColor = WisdomAccentAmber
        ),
        ProgramItem(
            id = "grade-12",
            name = "Grade 12",
            path = "/academy/grades/12",
            assetImage = "file:///android_asset/images/packages/grade-12_f1ddef.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/grade-12_f1ddef.jpeg",
            accentColor = WisdomAccentEmerald
        ),
        ProgramItem(
            id = "entrance-questions",
            name = "Entrance Questions",
            path = ENTRANCE_BASE_PATH,
            assetImage = ENTRANCE_IMAGE_ASSET,
            webImageFallback = "https://wisdom-tower-academy.live/images/Entrance%20questions.jpeg",
            accentColor = WisdomCyan
        ),
        ProgramItem(
            id = "remedial",
            name = "Remedial",
            path = "/academy/remedial",
            assetImage = "file:///android_asset/images/packages/remedial.jpg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/remedial.jpg",
            accentColor = WisdomAccentAmber
        ),
        ProgramItem(
            id = "uat",
            name = "UAT",
            path = "/academy/uat",
            assetImage = "file:///android_asset/images/packages/uat_56b257.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/uat_56b257.jpeg",
            accentColor = WisdomAccentEmerald
        ),
        ProgramItem(
            id = "coc",
            name = "COC",
            path = "/academy/coc",
            assetImage = "file:///android_asset/images/packages/coc_e44a09.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/coc_e44a09.jpeg",
            accentColor = WisdomAccentIndigo
        ),
        ProgramItem(
            id = "gat",
            name = "GAT",
            path = "/academy/gat",
            assetImage = "file:///android_asset/images/packages/gat_46ddb1.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/gat_46ddb1.jpeg",
            accentColor = WisdomAccentRose
        ),
        ProgramItem(
            id = "exit-exam",
            name = "Exit Exam",
            path = "/academy/exit-exam",
            assetImage = "file:///android_asset/images/packages/exit-exam_c32a43.jpeg",
            webImageFallback = "https://wisdom-tower-academy.live/images/packages/exit-exam_c32a43.jpeg",
            accentColor = WisdomAccentFuchsia
        ),
    )

val OTHER_RESOURCES: List<ResourceGuide>
    get() = listOf(
        ResourceGuide("Success Stories", "/academy/success-stories", Icons.Default.EmojiEvents, WisdomAccentAmber),
        ResourceGuide("Study Techniques", "/academy/study-techniques", Icons.Default.Lightbulb, WisdomCyan),
        ResourceGuide("Campus Life", "/academy/campus-life", Icons.Default.Park, WisdomAccentSky),
        ResourceGuide("Universities Directory", "/academy/universities", Icons.Default.Apartment, WisdomAccentViolet),
        ResourceGuide("Departments Guide", "/academy/departments", Icons.AutoMirrored.Filled.MenuBook, WisdomAccentOrange),
        ResourceGuide("Scholarships Guide", "/academy/scholarships", Icons.Default.School, WisdomAccentRose)
    )

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean = false,
    userName: String? = null,
    onNavigateToUrl: (String) -> Unit
) {
    // D3: the resource list is read once per composition (it is a getter now), so both tiles of a row come from one list.
    val resources = OTHER_RESOURCES
    val programs = HOME_PROGRAMS
    var comingSoonProgram by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState() // V10 header
    val context = LocalContext.current
    var bannerImage by remember(context) {
        mutableStateOf<Any?>(HomeBannerRepository.cachedOrFallback(context))
    }
    LaunchedEffect(context) {
        HomeBannerRepository.refresh(context) { image -> bannerImage = image }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Atmospheric radial background
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().wisdomScreenEntrance(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Signed out only: one row, "New here? Create a free account" + "Sign in".
            //    Signed in (2026-10-22, owner order): no line and no "My Learning" button here any more; the
            //    "Welcome back, <name>" greeting now lives inside the welcome card below (animated).
            //    The big "Wisdom Tower Academy" title and the "Enter Academy" button were removed on purpose (2026-10-13).
            if (!isLoggedIn) {
                item(key = "hero_section") {
                    HeroSection(
                        onSignIn = { onNavigateToUrl("/login") },
                        onSignUp = { onNavigateToUrl("/signup") }
                    )
                }
            }

            // 2. Welcome card: the shared card layout (picture, title, Open). Signed in, the title is the animated
            //    "Welcome back, <name>" (replays every time Home opens); signed out it stays "Open Learning".
            //    M11 slice 6: rises in once like the program cards below it (wisdomReveal, docs/UI_MOTION_PLAN.md).
            item(key = "welcome_card") {
                Box(modifier = Modifier.wisdomHeaderParallax(listState, "welcome_card").wisdomReveal("home_welcome_card", 0)) {
                    if (isLoggedIn) {
                        OpenCardLayout(
                            image = bannerImage,
                            subtitle = null,
                            onClick = { onNavigateToUrl("/learning") },
                            titleSlot = { WelcomeTitle(name = userName?.takeIf { it.isNotBlank() } ?: "Student") }
                        )
                    } else {
                        OpenCard(
                            title = "Open Learning",
                            image = bannerImage,
                            onClick = { onNavigateToUrl("/learning") }
                        )
                    }
                }
            }

            // 3. Program cards: the shared OpenCard, two per row (each row already rises in through openGrid)
            openGrid("programs", programs) { program ->
                OpenCard(program.name, program.assetImage) {
                    if (program.id == "gat" || program.id == "exit-exam") {
                        comingSoonProgram = program.name
                    } else {
                        onNavigateToUrl(program.path)
                    }
                }
            }

            // 4. "Other resources" heading & 2-column grid (2026-10-14: heavy 26 sp heading, big pill tiles, 18 sp titles)
            item(key = "resources_header") {
                Text(
                    text = "Other resources",
                    fontFamily = OutfitFontFamily,
                    fontSize = 26.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    color = WisdomTextPrimary,
                    modifier = Modifier
                        .wisdomReveal("home_resources_header", 0)
                        .padding(top = 10.dp, bottom = 2.dp)
                )
            }

            items(
                count = (resources.size + 1) / 2,
                key = { rowIndex -> "resource_row_$rowIndex" }
            ) { rowIndex ->
                val first = resources[rowIndex * 2]
                val second = resources.getOrNull(rowIndex * 2 + 1)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ResourceTile(
                            resource = first,
                            index = rowIndex * 2,
                            onClick = { onNavigateToUrl(first.path) }
                        )
                    }
                    if (second != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            ResourceTile(
                                resource = second,
                                index = rowIndex * 2 + 1,
                                onClick = { onNavigateToUrl(second.path) }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Bottom breathing space above bottom bar
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        comingSoonProgram?.let { programName ->
            HubGateDialog(
                badge = ContentAccess.COMING_SOON_TITLE,
                title = ContentAccess.COMING_SOON_TITLE,
                hubName = programName,
                message = ContentAccess.COMING_SOON_BODY,
                buttonText = ContentAccess.COMING_SOON_BUTTON,
                onClose = { comingSoonProgram = null }
            )
        }
    }
}

/**
 * Animated title of the welcome card (signed in): "Welcome back," rises and fades in, then the student's name
 * follows a moment later, bold and cyan. It replays every time Home is opened (the effect runs each time the
 * card enters the screen, and again if the name changes). Replaces the small "Welcome back, name" line and the
 * "My Learning" button that used to sit above the card (owner order 2026-10-22); the whole card still opens Learning.
 */
@Composable
private fun WelcomeTitle(name: String) {
    val first = remember { Animatable(0f) }
    val second = remember { Animatable(0f) }
    LaunchedEffect(name) {
        first.snapTo(0f)
        second.snapTo(0f)
        launch { first.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        delay(280)
        second.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Welcome back,",
            fontFamily = OutfitFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = WisdomInk,
            modifier = Modifier.graphicsLayer {
                alpha = first.value
                translationY = (1f - first.value) * 28f
            }
        )
        Text(
            text = name,
            fontFamily = OutfitFontFamily,
            fontSize = 26.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Black,
            color = WisdomCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer {
                val v = second.value
                alpha = v
                translationY = (1f - v) * 28f
                scaleX = 0.92f + 0.08f * v
                scaleY = 0.92f + 0.08f * v
            }
        )
    }
}

/**
 * Signed-out line at the top of Home: ONE line, "New here? Create a free account" on the left, "Sign in" button on the right.
 * (Signed in, nothing is shown here; the greeting is inside the welcome card.)
 * M11 slice 6: the line fades up first (wisdomReveal, index 0).
 * S3 part 2: "Create a free account" is a button for TalkBack with a 48 dp touch target.
 */
@Composable
private fun HeroSection(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wisdomReveal("home_hero", 0),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "New here? ",
                color = WisdomMuted,
                fontSize = 12.sp
            )
            Text(
                text = "Create a free account",
                color = WisdomCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .wisdomPressClickable(role = Role.Button, onClick = onSignUp)
            )
        }
        SignInPill(onClick = onSignIn) // S5-signin: the shared Sign in pill
    }
}

/**
 * "Other resources" tile (2026-10-14, iOS style): big rounded glass tile tinted with the guide's colour, a round icon,
 * a heavy 18 sp title (two lines reserved so both tiles of a row are equally tall), and a round arrow. It rises in
 * softly when it first appears and presses in like the login button. Titles are the website's, unchanged.
 * S3 part 2: the tile is ONE button for TalkBack (role Button); the icons are decorative, so it is read as
 * "<title>, button".
 * D3 (day mode): the title, the arrow and the arrow circle use [WisdomInk] (white at night, deep navy by day) instead of
 * Color.White, so night is unchanged.
 */
@Composable
private fun ResourceTile(
    resource: ResourceGuide,
    index: Int,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "resourceTilePress"
    )
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 6) * 60L)
        shown = true
    }
    val enter by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "resourceTileEnter"
    )
    val accent = resource.accentColor
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter
                translationY = (1f - enter) * 32f
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), WisdomGlassFill)))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.4f)), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f))
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.5f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resource.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(WisdomInk.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, WisdomBorderWhite), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = WisdomInk,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = resource.name,
            fontFamily = OutfitFontFamily,
            color = WisdomInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 22.sp,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
