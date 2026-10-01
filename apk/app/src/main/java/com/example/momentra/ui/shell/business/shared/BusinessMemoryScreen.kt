package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.BusinessMemoryPayloadDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.life.BusinessLifeLensChips
import com.example.momentra.ui.shell.business.life.components.CompanyLifeColors
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.Instant
import java.time.ZoneId

@Composable
fun BusinessMemoryScreen(
    momentId: String?,
    refreshToken: Long,
    momentTypeCode: String?,
    companyId: String?,
    onRecordLearning: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<BusinessMemoryPayloadDto?>(null) }
    var shownCompany by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var lens by remember(momentTypeCode) { mutableStateOf(businessLifeLens(momentTypeCode)) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(companyId) {
        if (!companyId.isNullOrBlank() && shownCompany != null && companyId != shownCompany) {
            payload = null
            error = null
            shownCompany = companyId
        }
    }

    LaunchedEffect(momentTypeCode) {
        lens = businessLifeLens(momentTypeCode)
    }

    LaunchedEffect(refreshToken, momentId, companyId, retry) {
        if (momentId.isNullOrBlank()) {
            loading = false
            payload = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        if (payload == null) {
            BusinessTabDataCache.peekMemory(momentId)?.memory?.let { payload = it }
        }
        loading = payload == null
        error = null
        repository.getMemory(momentId).fold(
            onSuccess = { facet ->
                val incoming = facet.companyId
                if (!companyId.isNullOrBlank() && !incoming.isNullOrBlank() && incoming != companyId) return@fold
                if (!shownCompany.isNullOrBlank() && !incoming.isNullOrBlank() && incoming != shownCompany && companyId.isNullOrBlank()) {
                    return@fold
                }
                payload = facet.payload
                shownCompany = incoming ?: companyId
                val previous = BusinessTabDataCache.peekMemory(momentId)
                BusinessTabDataCache.putMemory(
                    momentId,
                    BusinessTabDataCache.MemoryTab(
                        memory = facet.payload,
                        pulse = previous?.pulse,
                        finance = previous?.finance,
                        life = previous?.life,
                    ),
                )
            },
            onFailure = { error = it.message },
        )
        loading = false
    }

    if (loading && payload == null) {
        Box(modifier.fillMaxSize().background(CompanyLifeColors.Bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CompanyLifeColors.IndigoSolid)
        }
        return
    }

    val facts = payload?.items.orEmpty().map { it.toMemoryFact() }
    val presentation = buildBusinessMemoryPresentation(facts, Instant.now(), zone)
    val worth = filterWorthRemembering(presentation.worthRemembering, lens)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompanyLifeColors.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "Company memory",
            color = CompanyLifeColors.Text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        if (error != null) {
            Text(error ?: "", color = Color(0xFFF87171), fontSize = 13.sp, fontFamily = PlusJakartaSans)
            Text(
                "Try again",
                color = CompanyLifeColors.Indigo,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CompanyLifeColors.Card)
                    .clickable { retry += 1 }
                    .padding(10.dp),
            )
        }
        BusinessLifeLensChips(selected = lens, onSelect = { lens = it })
        MemoryCard("Memory") {
            Text(presentation.heroPeriod, color = CompanyLifeColors.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            Text(presentation.heroSentence, color = CompanyLifeColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
        MemoryCard("Worth remembering") {
            if (worth.isEmpty()) {
                Text(BUSINESS_MEMORY_WORTH_EMPTY, color = CompanyLifeColors.Secondary, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            } else {
                worth.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.title, color = CompanyLifeColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        val meta = listOf(item.familyLabel, item.dateLabel).filter { !it.isNullOrBlank() }.joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(meta, color = CompanyLifeColors.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }
        }
        presentation.pattern?.let { pattern ->
            MemoryCard("Pattern worth knowing") {
                Text(pattern.title, color = CompanyLifeColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text("See evidence", color = CompanyLifeColors.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                pattern.evidence.forEach { title ->
                    Text(title, color = CompanyLifeColors.Text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                }
            }
        }
        MemoryCard("What worked / What didn't") {
            Text("What worked", color = CompanyLifeColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            if (presentation.worked.isEmpty()) {
                Text(BUSINESS_MEMORY_WORKED_EMPTY, color = CompanyLifeColors.Secondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            } else {
                presentation.worked.forEach { title ->
                    Text(title, color = CompanyLifeColors.Text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                }
            }
            Text("What didn't", color = CompanyLifeColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            if (presentation.didnt.isEmpty()) {
                Text(BUSINESS_MEMORY_DIDNT_EMPTY, color = CompanyLifeColors.Secondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            } else {
                presentation.didnt.forEach { title ->
                    Text(title, color = CompanyLifeColors.Text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                }
            }
        }
        presentation.thenNow?.let { comparison ->
            MemoryCard("Then → Now") {
                Text("Then", color = CompanyLifeColors.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                Text(comparison.thenTitle, color = CompanyLifeColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text(comparison.thenDate, color = CompanyLifeColors.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                Text("Now", color = CompanyLifeColors.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                Text(comparison.nowTitle, color = CompanyLifeColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text(comparison.nowDate, color = CompanyLifeColors.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
        Text(
            "Record learning",
            color = CompanyLifeColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CompanyLifeColors.IndigoSolid)
                .clickable(onClick = onRecordLearning)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun MemoryCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CompanyLifeColors.Card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = CompanyLifeColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        content()
    }
}

private fun Map<String, Any?>.toMemoryFact(): BusinessMemoryItemFact {
    fun str(key: String): String? {
        val value = this[key] ?: return null
        val text = value.toString().trim()
        if (text.isEmpty() || text == "null") return null
        return text
    }
    return BusinessMemoryItemFact(
        title = str("title").orEmpty(),
        body = str("body"),
        occurredAt = str("occurredAt"),
        memoryType = str("memoryType"),
        businessFamily = str("businessFamily"),
    )
}
