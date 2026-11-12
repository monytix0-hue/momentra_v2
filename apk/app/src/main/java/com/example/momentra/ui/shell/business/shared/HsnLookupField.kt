package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.HsnLookupResultDto
import com.example.momentra.data.repository.BusinessSliceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Advisory HSN search against `GET /business/gst/hsn`. Applying a hit fills code + optional
 * rate hint; the operator can still override the rate on the master.
 */
@Composable
fun HsnLookupHints(
    query: String,
    onPick: (code: String, rateHint: String?) -> Unit,
    accent: androidx.compose.ui.graphics.Color,
    captionStyle: androidx.compose.ui.text.TextStyle,
    titleStyle: androidx.compose.ui.text.TextStyle,
) {
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<HsnLookupResultDto>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val digits = remember(query) { query.filter { it.isDigit() } }

    LaunchedEffect(digits) {
        searchJob?.cancel()
        if (digits.length < 2 && query.trim().length < 3) {
            results = emptyList()
            return@LaunchedEffect
        }
        searchJob = scope.launch {
            delay(280)
            val q = digits.ifBlank { query.trim() }
            results = repo.searchHsn(q, limit = 6).getOrNull()?.results.orEmpty()
        }
    }

    if (results.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("HSN suggestions · advisory rate", style = captionStyle.copy(fontSize = 10.sp, color = accent))
        results.take(5).forEach { hit ->
            val rate = hit.gstRate?.takeIf { it.isNotBlank() }
            Text(
                buildString {
                    append(hit.code)
                    append(" · ")
                    append(hit.description.take(48))
                    rate?.let { append(" · $it%") }
                },
                style = titleStyle.copy(fontSize = 12.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(hit.code, rate) }
                    .padding(vertical = 4.dp),
            )
        }
    }
}
