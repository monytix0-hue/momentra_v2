package com.example.momentra.ui.shell.empty.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans

data class GroupDraftPerson(
    val name: String,
    val roleCode: String,
    val roleLabel: String,
    val avatarRes: Int,
    val isOrganizer: Boolean = roleCode == "ORGANIZER" || roleCode == "CO_ORGANIZER",
    val avatarUri: String? = null,
    val useInitials: Boolean = false,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
)

@Composable
fun GroupSetupStepper(
    activeStep: Int,
    palette: GroupTypePalette,
    modifier: Modifier = Modifier,
) {
    val steps = listOf(
        Triple(R.drawable.ges_step_compass, "Type", 0),
        Triple(R.drawable.ges_step_check, "Details", 1),
        Triple(R.drawable.ges_step_check, "People", 2),
        Triple(R.drawable.ges_step_rocket, "Activate", 3),
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        steps.forEach { (icon, label, index) ->
            GroupStepperItem(
                iconRes = icon,
                label = label,
                state = when {
                    index < activeStep -> GroupStepState.COMPLETED
                    index == activeStep -> GroupStepState.CURRENT
                    else -> GroupStepState.UPCOMING
                },
                palette = palette,
                upcomingIconColor = if (index == 3) GroupSetupTheme.TextSecondary else palette.accent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private enum class GroupStepState { COMPLETED, CURRENT, UPCOMING }

@Composable
private fun GroupStepperItem(
    iconRes: Int,
    label: String,
    state: GroupStepState,
    palette: GroupTypePalette,
    upcomingIconColor: Color,
    modifier: Modifier = Modifier,
) {
    val active = state != GroupStepState.UPCOMING
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(
                    elevation = if (state == GroupStepState.CURRENT) 7.dp else 0.dp,
                    shape = CircleShape,
                    ambientColor = palette.stepGlow,
                    spotColor = palette.stepGlow,
                )
                .clip(CircleShape)
                .background(
                    when (state) {
                        GroupStepState.CURRENT -> palette.accent
                        GroupStepState.COMPLETED -> palette.accent.copy(alpha = 0.2f)
                        GroupStepState.UPCOMING -> GroupSetupTheme.StepInactiveBg
                    },
                )
                .border(
                    width = 2.dp,
                    color = when (state) {
                        GroupStepState.CURRENT, GroupStepState.COMPLETED -> palette.accent
                        GroupStepState.UPCOMING -> GroupSetupTheme.Border
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(
                    when (state) {
                        GroupStepState.CURRENT -> GroupSetupTheme.CtaText
                        GroupStepState.COMPLETED -> palette.accent
                        GroupStepState.UPCOMING -> upcomingIconColor
                    },
                ),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            color = if (active) palette.accent else GroupSetupTheme.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun GroupSetupSectionHeader(
    stepLabel: String,
    title: String,
    palette: GroupTypePalette,
    subtitle: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stepLabel,
                color = palette.accent.copy(alpha = 0.12f),
                fontSize = if (stepLabel.length <= 2) 40.sp else 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                title.uppercase(),
                color = palette.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        if (subtitle != null) {
            Text(
                subtitle,
                color = GroupSetupTheme.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun GroupSetupHero(
    title: String,
    subtitle: String,
    accent: Color,
    @androidx.annotation.DrawableRes iconRes: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                            ),
                        )
                    },
            )
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(56.dp))
                    .background(Color(0xFF161B26))
                    .border(1.dp, Color(0x33FFE1BF), RoundedCornerShape(56.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                color = accent,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
            )
            Text(
                subtitle,
                color = accent.copy(alpha = 0.42f),
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
fun GroupTypeCardGrid(
    types: List<GroupTypeOption>,
    selectedCode: String,
    onSelect: (GroupTypeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        types.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { option ->
                    GroupTypeCard(
                        option = option,
                        selected = option.code == selectedCode,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GroupTypeCard(
    option: GroupTypeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor = if (selected) option.selectedBorder else option.unselectedBorder
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .height(132.dp)
            .shadow(
                elevation = if (selected && option.selectedShadow != null) 10.dp else 0.dp,
                shape = shape,
                ambientColor = option.selectedShadow ?: Color.Transparent,
                spotColor = option.selectedShadow ?: Color.Transparent,
            )
            .background(option.cardGradient, shape)
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GroupSetupTheme.TypeIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(option.iconRes),
                null,
                Modifier.size(22.dp),
            )
        }
        Text(
            option.label,
            color = GroupSetupTheme.CtaText,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            fontFamily = PlusJakartaSans,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GroupDetailsCard(
    fields: List<Triple<Int, String, Pair<String, (String) -> Unit>>>,
    palette: GroupTypePalette,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, GroupSetupTheme.Border, RoundedCornerShape(20.dp))
            .background(GroupSetupTheme.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fields.forEach { (iconRes, label, valuePair) ->
            GroupDetailField(iconRes, label, valuePair.first, valuePair.second, palette)
        }
    }
}

@Composable
fun GroupDetailField(
    iconRes: Int,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    palette: GroupTypePalette,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GroupSetupTheme.IconSurface),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(iconRes),
                null,
                Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(palette.accent),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), color = GroupSetupTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = GroupSetupTheme.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                ),
                cursorBrush = SolidColor(palette.accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun GroupPeopleCard(
    people: List<GroupDraftPerson>,
    palette: GroupTypePalette,
    onInvite: () -> Unit,
    onRemove: ((GroupDraftPerson) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, GroupSetupTheme.Border, RoundedCornerShape(20.dp))
            .background(GroupSetupTheme.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        people.forEachIndexed { index, person ->
            if (index > 0) {
                HorizontalDivider(color = GroupSetupTheme.Border, thickness = 1.dp)
            }
            GroupPersonRow(
                person = person,
                palette = palette,
                onRemove = if (person.roleCode != "ORGANIZER" && onRemove != null) {
                    { onRemove(person) }
                } else {
                    null
                },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GroupSetupTheme.StepInactiveBg)
                .dashedBorder(1.dp, palette.accent, RoundedCornerShape(14.dp))
                .clickable(onClick = onInvite)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(R.drawable.ges_icon_add_people),
                    null,
                    Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(palette.accent),
                )
                Text("Add People", color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
fun GroupPersonRow(
    person: GroupDraftPerson,
    palette: GroupTypePalette,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupContactAvatar(
            name = person.name,
            photoUri = person.avatarUri,
            avatarRes = if (person.useInitials) null else person.avatarRes,
            size = 44.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(person.name, color = GroupSetupTheme.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (person.isOrganizer) palette.organizerRoleBg else GroupSetupTheme.MemberRoleBg)
                    .border(
                        1.dp,
                        if (person.isOrganizer) palette.organizerRoleBorder else GroupSetupTheme.MemberRoleBorder,
                        RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (person.isOrganizer) {
                    Image(
                        painterResource(R.drawable.ges_icon_role_organizer),
                        null,
                        Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(palette.accent),
                    )
                } else {
                    Image(
                        painterResource(R.drawable.ges_icon_role_member),
                        null,
                        Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(GroupSetupTheme.TextSecondary),
                    )
                }
                Text(
                    person.roleLabel,
                    color = if (person.isOrganizer) palette.accent else GroupSetupTheme.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GroupSetupTheme.IconSurface)
                    .clickable(onClick = onRemove)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Remove ${person.name}"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.ges_icon_x_circle),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(GroupSetupTheme.TextSecondary),
                )
            }
        }
    }
}

@Composable
fun GroupReviewCard(
    typeLabel: String,
    title: String,
    dates: String,
    memberSummary: String,
    ctaLabel: String,
    palette: GroupTypePalette,
    submitting: Boolean,
    onActivate: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, GroupSetupTheme.Border, RoundedCornerShape(20.dp))
            .background(GroupSetupTheme.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Experience Details".uppercase(), color = GroupSetupTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text("$typeLabel • $title", color = GroupSetupTheme.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            Text(dates, color = palette.accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        HorizontalDivider(color = GroupSetupTheme.Border)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Group Members".uppercase(), color = GroupSetupTheme.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(memberSummary, color = GroupSetupTheme.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 20.sp, fontFamily = PlusJakartaSans)
        }
        if (error != null) {
            Text(error, color = Color(0xFFFF8A80), fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = palette.stepGlow, spotColor = palette.stepGlow)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.accentGradient)
                .testTag(MaestroIds.GROUP_SETUP_SUBMIT)
                .semantics {
                    role = Role.Button
                    contentDescription = ctaLabel
                }
                .clickable(enabled = !submitting, onClick = onActivate),
            contentAlignment = Alignment.Center,
        ) {
            if (submitting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = GroupSetupTheme.CtaText, strokeWidth = 2.dp)
            } else {
                Text(ctaLabel, color = GroupSetupTheme.CtaText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

fun defaultGroupPeople(@Suppress("UNUSED_PARAMETER") code: String): List<GroupDraftPerson> = listOf(
    GroupDraftPerson(
        name = "You",
        roleCode = "ORGANIZER",
        roleLabel = "Organizer",
        avatarRes = R.drawable.ges_avatar_1,
        isOrganizer = true,
        useInitials = true,
    ),
)

fun buildMemberSummary(people: List<GroupDraftPerson>): String {
    val names = people.joinToString(", ") { it.name }
    return "$names (${people.size} members)"
}

@Composable
fun GroupContactAvatar(
    name: String,
    photoUri: String?,
    avatarRes: Int? = null,
    size: Dp,
) {
    val shape = RoundedCornerShape(size / 2)
    val context = LocalContext.current
    val photo = remember(photoUri) {
        if (photoUri.isNullOrBlank()) null
        else runCatching {
            context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    when {
        photo != null -> Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size).clip(shape),
            contentScale = ContentScale.Crop,
        )
        avatarRes != null -> Image(
            painter = painterResource(avatarRes),
            contentDescription = null,
            modifier = Modifier.size(size).clip(shape),
            contentScale = ContentScale.Crop,
        )
        else -> Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(GroupSetupTheme.IconSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                color = GroupSetupTheme.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

private fun Modifier.dashedBorder(width: Dp, color: Color, shape: RoundedCornerShape): Modifier = this.then(
    Modifier.drawBehind {
        val stroke = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(14.dp.toPx()),
            style = stroke,
        )
    },
)
