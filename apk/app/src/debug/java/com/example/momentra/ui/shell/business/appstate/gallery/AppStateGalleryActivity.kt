package com.example.momentra.ui.shell.business.appstate.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.momentra.domain.MomentSummary
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateAccent
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateBody
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateHubBody
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateLoadingBody
import com.example.momentra.ui.shell.business.appstate.BusinessAppStatePhase
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateScreen
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateSpecs
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateTokens
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateVertical
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateViewAllSheet
import com.example.momentra.ui.shell.momentswitcher.MomentSwitcherDirectory
import com.example.momentra.ui.shell.momentswitcher.businessMomentSwitcherSpec
import com.example.momentra.ui.shell.momentswitcher.groupMomentSwitcherSpec
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * Debug-only gallery for the Figma "App States · Shared (all Business verticals)" kit.
 *
 * Not part of the shipping surface: it lives in `src/debug` and is only reachable through an
 * adb intent, so the 17 variants and the 5 directory states can be screenshot-compared against
 * Figma without signing in.
 *
 * adb shell am start -n com.monytix.app/.ui.shell.business.appstate.gallery.AppStateGalleryActivity
 */
class AppStateGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startPage = intent.getIntExtra("page", 0).coerceIn(0, variants.lastIndex)
        setContent {
            Box(Modifier.fillMaxSize().background(BusinessAppStateTokens.Page)) {
                GalleryPager(startPage)
            }
        }
    }
}

private enum class Variant(val label: String) {
    PulseEmpty("Pulse · Empty"),
    PulseLoading("Pulse · Loading"),
    PulseError("Pulse · Error"),
    MomentsEmpty("Moments · Empty"),
    MomentsError("Moments · Error"),
    LifeEmpty("Life · Empty"),
    LifeError("Life · Error"),
    MemoryEmpty("Memory · Empty"),
    MemoryError("Memory · Error"),
    HubEmpty("Hub · Empty"),
    HubError("Hub · Error"),
    NoCompany("NoCompany · Setup"),
    Offline("Offline · Error"),
    ViewAll("ViewAll · Empty"),
    DirectoryOngoing("Directory · Ongoing"),
    DirectoryCompleted("Directory · Completed"),
    DirectoryEmpty("Directory · Empty"),
    DirectorySearchEmpty("Directory · SearchEmpty"),
    DirectoryLoadError("Directory · LoadError"),
    GroupDirectoryOngoing("Group directory · Ongoing"),
    GroupDirectoryEmpty("Group directory · Empty"),
}

private val variants = Variant.entries

@Composable
private fun GalleryPager(startPage: Int) {
    val pager = rememberPagerState(initialPage = startPage) { variants.size }
    var filter by remember { mutableStateOf("All") }
    val accent = BusinessAppStateAccent.Emerald

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "${variants[pager.currentPage].label}  (${pager.currentPage + 1}/${variants.size})",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
        )
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                when (val variant = variants[index]) {
                    Variant.PulseEmpty -> Body(BusinessAppStateScreen.Pulse, BusinessAppStatePhase.Empty)
                    Variant.PulseLoading -> BusinessAppStateLoadingBody("Loading Pulse…", accent)
                    Variant.PulseError -> Body(BusinessAppStateScreen.Pulse, BusinessAppStatePhase.Error)
                    Variant.MomentsEmpty -> Body(BusinessAppStateScreen.Moments, BusinessAppStatePhase.Empty)
                    Variant.MomentsError -> Body(BusinessAppStateScreen.Moments, BusinessAppStatePhase.Error)
                    Variant.LifeEmpty -> Body(BusinessAppStateScreen.Life, BusinessAppStatePhase.Empty)
                    Variant.LifeError -> Body(BusinessAppStateScreen.Life, BusinessAppStatePhase.Error)
                    Variant.MemoryEmpty -> Body(BusinessAppStateScreen.Memory, BusinessAppStatePhase.Empty)
                    Variant.MemoryError -> Body(BusinessAppStateScreen.Memory, BusinessAppStatePhase.Error)
                    Variant.HubEmpty -> BusinessAppStateHubBody(accent, BusinessAppStateVertical.Trading, {}, {})
                    Variant.HubError -> Body(BusinessAppStateScreen.Hub, BusinessAppStatePhase.Error)
                    Variant.NoCompany -> Body(BusinessAppStateScreen.NoCompany, BusinessAppStatePhase.Setup)
                    Variant.Offline -> Body(BusinessAppStateScreen.Offline, BusinessAppStatePhase.Error)
                    Variant.ViewAll -> BusinessAppStateViewAllSheet(
                        accent = accent,
                        selectedFilter = filter,
                        onSelectFilter = { filter = it },
                        onClose = {},
                        onShowAll = {},
                    )
                    Variant.DirectoryOngoing -> Directory(
                        moments = listOf(
                            MomentSummary("1", "Northwind Traders", "ACTIVE", "TRADING_WHOLESALE", participantCount = 3),
                            MomentSummary("2", "Retail Floor", "ACTIVE", "RETAIL_STORE", participantCount = 2),
                        ),
                        completed = emptyList(),
                    )
                    Variant.DirectoryCompleted -> Directory(
                        moments = listOf(
                            MomentSummary("1", "Northwind Traders", "ACTIVE", "TRADING_WHOLESALE", participantCount = 3),
                        ),
                        completed = listOf(
                            MomentSummary("3", "Festive Push", "COMPLETED", "CLIENT_CAMPAIGN", participantCount = 4),
                        ),
                        completedTab = true,
                    )
                    Variant.DirectoryEmpty -> Directory(moments = emptyList(), completed = emptyList())
                    Variant.DirectorySearchEmpty -> Directory(
                        moments = listOf(
                            MomentSummary("1", "Northwind Traders", "ACTIVE", "TRADING_WHOLESALE", participantCount = 3),
                        ),
                        completed = emptyList(),
                        // Seeds a query that matches nothing so the SearchEmpty card is deterministic.
                        initialQuery = "zzz",
                    )
                    Variant.DirectoryLoadError -> Directory(
                        moments = listOf(
                            MomentSummary("1", "Northwind Traders", "ACTIVE", "TRADING_WHOLESALE", participantCount = 3),
                        ),
                        completed = emptyList(),
                        completedTab = true,
                        completedError = "boom",
                    )
                    Variant.GroupDirectoryOngoing -> Directory(
                        moments = listOf(
                            MomentSummary("1", "Ananya & Rohan", "ACTIVE", "WEDDING", participantCount = 6),
                            MomentSummary("2", "Goa Weekend", "ACTIVE", "TRIP", participantCount = 4),
                        ),
                        completed = emptyList(),
                        business = false,
                    )
                    Variant.GroupDirectoryEmpty -> Directory(
                        moments = emptyList(),
                        completed = emptyList(),
                        business = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun Body(screen: BusinessAppStateScreen, phase: BusinessAppStatePhase) {
    Box(Modifier.fillMaxSize().background(BusinessAppStateTokens.Page)) {
        BusinessAppStateBody(
            spec = BusinessAppStateSpecs.spec(screen, phase),
            accent = BusinessAppStateAccent.Emerald,
            onPrimary = {},
            onSecondary = {},
        )
    }
}

@Composable
private fun Directory(
    moments: List<MomentSummary>,
    completed: List<MomentSummary>,
    completedTab: Boolean = false,
    completedError: String? = null,
    initialQuery: String = "",
    business: Boolean = true,
) {
    Box(Modifier.fillMaxSize()) {
        MomentSwitcherDirectory(
            spec = if (business) {
                businessMomentSwitcherSpec("Northwind Traders")
            } else {
                groupMomentSwitcherSpec()
            },
            moments = moments,
            selectedMomentId = moments.firstOrNull()?.momentId,
            visible = true,
            onDismiss = {},
            onSelectMoment = {},
            onSelectCompletedMoment = {},
            onOpenStory = {},
            onCreateMoment = {},
            initialCompletedTab = completedTab,
            // The gallery owns loading so the states are deterministic without a network.
            completedMoments = completed,
            completedLoading = false,
            completedError = completedError,
            initialQuery = initialQuery,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
