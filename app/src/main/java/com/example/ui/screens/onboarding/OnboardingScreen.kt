package com.example.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.BinderViewModel
import kotlinx.coroutines.launch

data class OnboardingStep(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val iconColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: BinderViewModel,
    currentLanguage: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        OnboardingStep(
            titleRes = R.string.onboarding_title_1,
            descRes = R.string.onboarding_desc_1,
            icon = Icons.Default.School,
            iconColor = MaterialTheme.colorScheme.primary
        ),
        OnboardingStep(
            titleRes = R.string.onboarding_title_2,
            descRes = R.string.onboarding_desc_2,
            icon = Icons.Default.FolderSpecial,
            iconColor = Color(0xFF0D9488)
        ),
        OnboardingStep(
            titleRes = R.string.onboarding_title_3,
            descRes = R.string.onboarding_desc_3,
            icon = Icons.Default.TrendingUp,
            iconColor = Color(0xFFEA580C)
        ),
        OnboardingStep(
            titleRes = R.string.onboarding_title_4,
            descRes = R.string.onboarding_desc_4,
            icon = Icons.Default.Archive,
            iconColor = Color(0xFF10B981)
        )
    )

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar with language toggles and skip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentLanguage == "fr",
                    onClick = { viewModel.setLanguage("fr") },
                    label = { Text("Français") }
                )
                FilterChip(
                    selected = currentLanguage == "ar",
                    onClick = { viewModel.setLanguage("ar") },
                    label = { Text("العربية") }
                )
            }

            TextButton(
                onClick = {
                    viewModel.completeOnboarding()
                    onFinish()
                }
            ) {
                Text(stringResource(R.string.action_skip))
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(3f)
        ) { page ->
            val step = steps[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = step.iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = step.iconColor,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(step.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(step.descRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // Pager indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(steps.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Next or Get Started Button
        val isLastPage = pagerState.currentPage == steps.size - 1

        Button(
            onClick = {
                if (isLastPage) {
                    viewModel.completeOnboarding()
                    onFinish()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_onboarding_action")
        ) {
            Text(
                text = if (isLastPage) stringResource(R.string.action_start) else "Suivant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
