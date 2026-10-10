package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Amber700
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
    val badgeLabel: String
)

@Composable
fun OnboardingCarouselDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 6 })

    // Steps matching exactly user instructions:
    val steps = listOf(
        OnboardingStep(
            stepNumber = 1,
            title = "Δημιουργήθηκε το εξοδολόγιό σας",
            subtitle = "Το οικιακό σας εξοδολόγιο δημιουργήθηκε αυτόματα και είναι συνδεδεμένο με το cloud. Όλα τα δεδομένα σας παραμένουν ασφαλή και προσβάσιμα από κάθε συσκευή σας.",
            icon = Icons.Default.Home,
            gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981)),
            highlights = listOf(
                "Αυτόματη ασφαλής αποθήκευση στο Cloud",
                "Καθαρή έναρξη χωρίς παλιά δεδομένα",
                "Προσωπικό ή οικογενειακό εξοδολόγιο"
            ),
            badgeLabel = "Βήμα 1: Εξοδολόγιο"
        ),
        OnboardingStep(
            stepNumber = 2,
            title = "Καταχωρήστε τις κατηγορίες των εξόδων σας",
            subtitle = "Ορίστε τις δικές σας κατηγορίες εξόδων (π.χ. Σούπερ Μάρκετ, Φαγητό, Λογαριασμοί) με μηνιαίο προϋπολογισμό, χρώματα και εικονίδια για άμεσο έλεγχο.",
            icon = Icons.Default.Category,
            gradientColors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8)),
            highlights = listOf(
                "Μηνιαίος στόχος προϋπολογισμού ανά κατηγορία",
                "Επιλογή εξατομικευμένου χρώματος & εικονιδίου",
                "Λέξεις-κλειδιά για έξυπνη φωνητική καταγραφή"
            ),
            badgeLabel = "Βήμα 2: Κατηγορίες"
        ),
        OnboardingStep(
            stepNumber = 3,
            title = "Καταχωρήστε τα μέλη τα οποία αφορά το εξοδολόγιο",
            subtitle = "Προσθέστε τα μέλη της οικογένειας ή του σπιτιού σας ώστε να γνωρίζετε ποιος πλήρωσε τι και να μοιράζετε τα έξοδα ισόποσα ή εξατομικευμένα.",
            icon = Icons.Default.Group,
            gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)),
            highlights = listOf(
                "Προσθήκη μελών με ονόματα & ρόλους",
                "Χρωματιστά avatars για γρήγορη αναγνώριση",
                "Διαχωρισμός εξόδων: Κοινό οικογένειας ή συγκεκριμένα μέλη"
            ),
            badgeLabel = "Βήμα 3: Μέλη"
        ),
        OnboardingStep(
            stepNumber = 4,
            title = "Ξεκινήστε να καταχωρείτε έσοδα και έξοδα γραπτώς ή μέσω φωνής",
            subtitle = "Καταχωρήστε συναλλαγές σε δευτερόλεπτα! Χρησιμοποιήστε το κουμπί «+» ή πατήστε το μικρόφωνο και πείτε στα Ελληνικά «Σκλαβενίτης 45 ευρώ».",
            icon = Icons.Default.Mic,
            gradientColors = listOf(Color(0xFFD97706), Color(0xFFFBBF24)),
            highlights = listOf(
                "Έξυπνη ελληνική φωνητική αναγνώριση",
                "Αυτόματη ταξινόμηση σε κατηγορία & μέλος",
                "Γρήγορη χειροκίνητη πληκτρολόγηση"
            ),
            badgeLabel = "Βήμα 4: Έσοδα & Έξοδα"
        ),
        OnboardingStep(
            stepNumber = 5,
            title = "Παρακολουθήστε τα στατιστικά στην καρτέλα στατιστικά",
            subtitle = "Αποκτήστε πλήρη έλεγχο των οικονομικών σας με διαγράμματα εξόδων ανά κατηγορία, κατανομή ανά μέλος, μηνιαία σύγκριση και αποταμιευτικούς στόχους.",
            icon = Icons.Default.PieChart,
            gradientColors = listOf(Color(0xFF2563EB), Color(0xFF60A5FA)),
            highlights = listOf(
                "Γραφήματα δαπανών και ποσοστά ανά κατηγορία",
                "Έλεγχος υπέρβασης μηνιαίου budget",
                "Σύγκριση εσόδων - εξόδων και υπόλοιπο"
            ),
            badgeLabel = "Βήμα 5: Στατιστικά"
        ),
        OnboardingStep(
            stepNumber = 6,
            title = "Αναβαθμίστε σε pro για πρόσβαση και άλλων μελών στο εξοδολόγιό σας ή για την προσθήκη περισσότερων εξοδολογίων",
            subtitle = "Ξεκλειδώστε τη λειτουργία Pro για να συνεργάζεστε σε πραγματικό χρόνο με την οικογένειά σας από διαφορετικά κινητά, να διαχειρίζεστε πολλαπλά εξοδολόγια και να εξάγετε Excel.",
            icon = Icons.Default.Star,
            gradientColors = listOf(Color(0xFFB45309), Color(0xFFF59E0B)),
            highlights = listOf(
                "Ζωντανός συγχρονισμός πολλών συσκευών μέσω κωδικού",
                "Διαχείριση πολλαπλών ανεξάρτητων εξοδολογίων",
                "Εξαγωγή αναλυτικών αναφορών σε Excel / CSV"
            ),
            badgeLabel = "Βήμα 6: Pro Δυνατότητες"
        )
    )

    // Auto-advance every 10 seconds (10000ms) if user is not actively scrolling (Requirement 3)
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            delay(10000L)
            if (pagerState.currentPage < steps.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                // Loop back to start after last slide
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
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("onboarding_carousel_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                text = "Παράλειψη",
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
                                contentDescription = "Κλείσιμο",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Carousel Pager: Equal height across all cards (Requirement 2)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                ) { pageIndex ->
                    val step = steps[pageIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Illustrated Hero Icon Badge
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(colors = step.gradientColors)
                                )
                                .border(
                                    width = 3.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Title with fixed uniform height box so 2-line vs 1-line titles don't shift height
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = step.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle with fixed uniform height box so variations don't shift cards
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = step.subtitle,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                maxLines = 4
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Key Highlights Cards with equal minimum height
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                step.highlights.forEach { highlight ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(step.gradientColors.first().copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = step.gradientColors.first(),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = highlight,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dots Page Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "dot_width")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) steps[pagerState.currentPage].gradientColors.first()
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                .testTag("onboarding_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Πίσω",
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
                            .testTag("onboarding_next_button")
                    ) {
                        Text(
                            text = if (isLastPage) "Ξεκινήστε τώρα!" else "Επόμενο",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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
