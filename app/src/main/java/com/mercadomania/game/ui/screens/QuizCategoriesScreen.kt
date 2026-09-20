package com.mercadomania.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.game.GameLogic
import com.mercadomania.game.game.QuizCategory
import com.mercadomania.game.game.QuizContent
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.ScreenHeader
import com.mercadomania.game.ui.theme.BazaarPalette

/** Quiz category menu: three stalls, each with its emblem and best score. */
@Composable
fun QuizCategoriesScreen(
    quizBest: Map<String, Int>,
    onCategory: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BazaarBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            ScreenHeader(
                title = stringResource(R.string.quiz_title),
                backDescription = stringResource(R.string.cd_back),
                onBack = onBack
            )
            Text(
                text = stringResource(R.string.quiz_pick_category),
                style = MaterialTheme.typography.bodyLarge,
                color = BazaarPalette.Cream,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuizContent.categories.forEach { category ->
                    CategoryCard(
                        category = category,
                        best = quizBest[category.id]?.coerceAtLeast(0) ?: 0,
                        onClick = { onCategory(category.id) }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: QuizCategory,
    best: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = GameLogic.QUIZ_QUESTIONS_PER_CATEGORY
    val bestLabel = if (best > 0) {
        stringResource(R.string.quiz_best, best, total)
    } else {
        stringResource(R.string.quiz_not_played)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(BazaarPalette.PanelTop, BazaarPalette.PanelBottom)
                )
            )
            .border(2.dp, BazaarPalette.Gold, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClickLabel = category.title, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(62.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Assets.plateRound),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = painterResource(Assets.item(category.iconIndex)),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge,
                color = BazaarPalette.Gold
            )
            Text(
                text = category.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = BazaarPalette.Cream
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = bestLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (best > 0) BazaarPalette.GemCyan else BazaarPalette.MutedSlate
            )
        }
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .background(BazaarPalette.Gold)
        )
    }
}
