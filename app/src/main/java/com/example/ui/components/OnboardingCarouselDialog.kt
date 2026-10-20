package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Emerald700
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class OnboardingStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val highlights: List<String>,
    val badgeLabel: String,
    val statusPill: String
)

@Composable
fun OnboardingCarouselDialog(
    onDismiss: () -> Unit,
    onAddMember: (() -> Unit)? = null,
    onAddCategory: (() -> Unit)? = null,
    onSeedCategories: (() -> Unit)? = null,
    appLanguage: String = "EN",
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val isEnglish = appLanguage.equals("EN", ignoreCase = true)

    val steps = if (isEnglish) {
        listOf(
            OnboardingStep(
                stepNumber = 1,
                title = "Voice Logging",
                subtitle = "Tap the mic and say: \"Coffee 3.50\". Amount & category are recognized instantly.",
                icon = Icons.Default.Mic,
                gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981)),
                highlights = listOf(
                    "Voice recognition in English & Greek",
                    "Instant category & amount detection",
                    "Fast entry without typing"
                ),
                badgeLabel = "1/3: Voice",
                statusPill = "✓ Fast Voice Logging"
            ),
            OnboardingStep(
                stepNumber = 2,
                title = "Household Profile",
                subtitle = "Designed for singles, couples, flatmates, or families.",
                icon = Icons.Default.Group,
                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)),
                highlights = listOf(
                    "Single role for solo tracking",
                    "Fair split for couples & roommates",
                    "Parent & children tracking for families"
                ),
                badgeLabel = "2/3: Profile",
                statusPill = "✓ Built for Everyone"
            ),
            OnboardingStep(
                stepNumber = 3,
                title = "Smart Stats & Cloud",
                subtitle = "Real-time sync across devices and a reminder at 22:00.",
                icon = Icons.Default.PieChart,
                gradientColors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8)),
                highlights = listOf(
                    "Real-time cloud sync on all devices",
                    "Monthly balance & daily average",
                    "Gentle reminder at 22:00"
                ),
                badgeLabel = "3/3: Stats & Sync",
                statusPill = "✓ Live Cloud Sync"
            )
        )
    } else {
        listOf(
            OnboardingStep(
                stepNumber = 1,
                title = "Φωνητική Καταγραφή",
                subtitle = "Πατήστε το μικρόφωνο και πείτε: «Σούπερ μάρκετ 45€». Αναγνωρίζεται αυτόματα.",
                icon = Icons.Default.Mic,
                gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981)),
                highlights = listOf(
                    "Φωνητική καταγραφή στα ελληνικά",
                    "Αυτόματη κατηγορία & ποσό",
                    "Άμεση καταχώρηση χωρίς πληκτρολόγηση"
                ),
                badgeLabel = "1/3: Φωνή",
                statusPill = "✓ Φωνητική Καταγραφή"
            ),
            OnboardingStep(
                stepNumber = 2,
                title = "Επιλογή Προφίλ",
                subtitle = "Ιδανικό για μονοπρόσωπο σπίτι, ζευγάρια, συγκατοίκους ή οικογένειες.",
                icon = Icons.Default.Group,
                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)),
                highlights = listOf(
                    "«Μονοπρόσωπο» για ατομική χρήση",
                    "Διαμοιρασμός για ζευγάρια & συγκατοίκους",
                    "Έξοδα γονέων & παιδιών για οικογένειες"
                ),
                badgeLabel = "2/3: Προφίλ",
                statusPill = "✓ Προσαρμογή για Όλους"
            ),
            OnboardingStep(
                stepNumber = 3,
                title = "Στατιστικά & Cloud Sync",
                subtitle = "Αυτόματος συγχρονισμός σε όλες τις συσκευές και υπενθύμιση στις 22:00.",
                icon = Icons.Default.PieChart,
                gradientColors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8)),
                highlights = listOf(
                    "Real-time συγχρονισμός στο Cloud",
                    "Μηνιαίος ισολογισμός & μέσο έξοδο",
                    "Βραδινή υπενθύμιση στις 22:00"
                ),
                badgeLabel = "3/3: Στατιστικά",
                statusPill = "✓ Ζωντανός Συγχρονισμός"
            )
        )
    }

    // Auto-advance every 10 seconds if user is not actively scrolling
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            delay(10000L)
            if (pagerState.currentPage < steps.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .height(535.dp) // Strictly uniform height for all steps from start to finish
                .padding(vertical = 8.dp)
                .testTag("onboarding_carousel_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row: Counter & Close/Skip Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentStep = steps[pagerState.currentPage]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = currentStep.gradientColors.first().copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = currentStep.badgeLabel,
                            color = currentStep.gradientColors.first(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("onboarding_skip_button")
                        ) {
                            Text(
                                text = if (isEnglish) "Skip" else "Παράλειψη",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp).testTag("onboarding_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isEnglish) "Close" else "Κλείσιμο",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Carousel Pager: Exactly calibrated dimensions ensuring zero clipping across all steps
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(370.dp)
                ) { pageIndex ->
                    val step = steps[pageIndex]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Illustrated Hero Icon Badge (60dp)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(colors = step.gradientColors))
                                .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Title with fixed uniform height box (44dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = step.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 21.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 3. Subtitle with fixed uniform height box (54dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = step.subtitle,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.5.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. Key Highlights Card with fixed uniform height (96dp)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                step.highlights.forEach { highlight ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(step.gradientColors.first().copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = step.gradientColors.first(),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = highlight,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 5. Interactive Action Area: Exactly 42dp across ALL steps so height is 100% constant!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (step.stepNumber == 2 && (onAddCategory != null || onSeedCategories != null)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (onAddCategory != null) {
                                        Button(
                                            onClick = onAddCategory,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = step.gradientColors.first()),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("onboarding_add_category_btn")
                                        ) {
                                            Text(
                                                text = if (isEnglish) "New Category" else "Νέα Κατηγορία",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (onSeedCategories != null) {
                                        OutlinedButton(
                                            onClick = onSeedCategories,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("onboarding_seed_categories_btn")
                                        ) {
                                            Text(
                                                text = if (isEnglish) "Load Defaults" else "Φόρτωση Βασικών",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            } else if (step.stepNumber == 2 && onAddMember != null) {
                                Button(
                                    onClick = onAddMember,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = step.gradientColors.first()),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("onboarding_add_member_btn")
                                ) {
                                    Text(
                                        text = if (isEnglish) "Set Up Members & Roles" else "Ορισμός Μελών & Ρόλων",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Subtle aesthetic status pill giving equal layout balance to Steps 1, 4, 5, 6
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = step.gradientColors.first().copy(alpha = 0.08f),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = step.gradientColors.first(),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = step.statusPill,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = step.gradientColors.first()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Pagination Section: Dots & Nav Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dots Page Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.indices.forEach { index ->
                            val isSelected = pagerState.currentPage == index
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 22.dp else 7.dp,
                                label = "dot_width"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(7.dp)
                                    .width(dotWidth)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) steps[pagerState.currentPage].gradientColors.first()
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("onboarding_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEnglish) "Back" else "Πίσω",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        val isLastPage = pagerState.currentPage == steps.size - 1
                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onDismiss()
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLastPage) Emerald700 else steps[pagerState.currentPage].gradientColors.first()
                            ),
                            modifier = Modifier
                                .weight(if (pagerState.currentPage > 0) 1.5f else 1f)
                                .height(44.dp)
                                .testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = if (isLastPage) {
                                    if (isEnglish) "Get Started!" else "Ξεκινήστε τώρα!"
                                } else {
                                    if (isEnglish) "Next" else "Επόμενο"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
