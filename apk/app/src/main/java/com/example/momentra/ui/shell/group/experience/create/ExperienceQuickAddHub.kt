package com.example.momentra.ui.shell.group.experience.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma 584:17037 / 584:17136 — Experience Quick Add hub. */
@Composable
fun ExperienceQuickAddHub(
    theme: ExperienceActiveTheme,
    momentTitle: String?,
    hasActiveMoment: Boolean,
    onClose: () -> Unit,
    onTile: (ExperienceQuickAddKind) -> Unit,
    onCreateMoment: () -> Unit = {},
    onJoinCode: (String) -> Unit = {},
    capabilities: List<String>? = null,
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf("") }
    val tiles = experienceHubTiles(theme.includesVendor).filter {
        val q = search.trim().lowercase()
        q.isEmpty() || it.label().lowercase().contains(q)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Quick Add", color = Color(0xFFF7F5F2), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text("Bring your experience to life", color = theme.muted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF171618))
                    .border(1.dp, Color(0xFF403C40), RoundedCornerShape(16.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = theme.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(momentTitle ?: theme.typeLabel, theme.accent, solid = true, darkText = theme.darkText)
            Chip("Shared Experience", Color(0xFF14B8A6), solid = false, darkText = theme.darkText)
            Chip("Planning Stage", Color(0xFFA855F7), solid = false, darkText = theme.darkText)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.heroGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Bring your experience to life", color = Color(0xFF14121B), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(
                if (hasActiveMoment) "Add people, plans, money, memories and decisions." else "Select or create a moment first.",
                color = Color(0xFF14121B),
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Image(
                    painter = painterResource(theme.hubHeroRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 180.dp, height = 120.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF171618))
                .border(1.dp, Color(0xFF403C40), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                decorationBox = { inner ->
                    if (search.isEmpty()) {
                        Text("Search actions...", color = theme.muted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                    }
                    inner()
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tiles.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { kind ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(104.dp)
                                .alpha(if (hasActiveMoment) 1f else 0.45f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(theme.accent.copy(alpha = 0.25f), theme.accentSolid.copy(alpha = 0.15f)),
                                    ),
                                )
                                .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                                .then(if (hasActiveMoment) Modifier.clickable { onTile(kind) } else Modifier)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(kind.emoji(), fontSize = 22.sp)
                            Text(kind.label(), color = theme.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        }
                    }
                    repeat(3 - row.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (!hasActiveMoment) {
            Text(
                "Create a new moment",
                color = theme.accentLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onCreateMoment),
            )
        }
    }
}

@Composable
private fun Chip(label: String, tint: Color, solid: Boolean, darkText: Color) {
    Text(
        label,
        color = if (solid) darkText else tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (solid) tint else tint.copy(alpha = 0.15f))
            .border(1.dp, if (solid) Color.Transparent else tint.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

