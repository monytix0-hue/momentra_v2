package com.example.momentra.ui.shell

import com.example.momentra.data.api.ApiResultException
import com.example.momentra.data.repository.MeGateway
import com.example.momentra.domain.AppContext
import com.example.momentra.domain.AuthPhase
import com.example.momentra.domain.BottomDestination
import com.example.momentra.domain.CompanySummary
import com.example.momentra.domain.MomentExperienceKind
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.ShellContentState
import com.example.momentra.domain.ShellIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Shell state tests use fakes only — never shipped at runtime. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun authPhaseAuthenticatedExists() {
        assertEquals(AuthPhase.Authenticated, AuthPhase.Authenticated)
    }

    @Test
    fun contextSelectionKeepsIdentity() = runTest {
        val vm = AppShellViewModel(FakeMeGateway())
        vm.bindIdentity(ShellIdentity("u1", "Ada", "a@b.c", "fb"))
        advanceUntilIdle()
        vm.selectContext(AppContext.GROUP)
        advanceUntilIdle()
        assertEquals(AppContext.GROUP, vm.state.value.selectedContext)
        vm.selectContext(AppContext.PERSONAL)
        advanceUntilIdle()
        assertEquals(AppContext.PERSONAL, vm.state.value.selectedContext)
        assertEquals("u1", vm.state.value.identity?.userId)
    }

    @Test
    fun preservesTabPerContext() = runTest {
        val vm = AppShellViewModel(FakeMeGateway())
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        vm.selectBottomDestination(BottomDestination.LIFE)
        vm.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        vm.selectBottomDestination(BottomDestination.MOMENTS)
        vm.selectContext(AppContext.PERSONAL)
        advanceUntilIdle()
        assertEquals(BottomDestination.LIFE, vm.state.value.bottomDestination)
    }

    @Test
    fun companySelectionClearsMoment() = runTest {
        val companies = listOf(
            CompanySummary("c1", "Acme"),
            CompanySummary("c2", "Beta"),
        )
        val vm = AppShellViewModel(
            FakeMeGateway(
                companies = companies,
                businessMoments = listOf(
                    MomentSummary("m1", "Launch", "ACTIVE", companyId = "c2"),
                    MomentSummary("mOther", "Other Co", "ACTIVE", companyId = "c1"),
                ),
            ),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        vm.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        vm.selectCompany(companies[1])
        advanceUntilIdle()
        assertEquals("c2", vm.state.value.selectedCompany?.companyId)
        assertEquals(listOf("m1"), vm.state.value.moments.map { it.momentId })
        assertEquals("m1", vm.state.value.selectedMomentId)
    }

    @Test
    fun personalFirstMomentState() = runTest {
        val vm = AppShellViewModel(FakeMeGateway())
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        assertTrue(vm.state.value.contextContent is ShellContentState.Empty)
        assertEquals(MomentExperienceKind.FIRST_MOMENT, vm.state.value.momentExperience)
        assertFalse(vm.state.value.showMomentSwitcher)
    }

    @Test
    fun personalBetweenMomentsWithHistory() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(
                personalMoments = listOf(
                    MomentSummary("m1", "Goa", "COMPLETED"),
                    MomentSummary("m2", "Move", "CANCELLED"),
                ),
            ),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        assertEquals(MomentExperienceKind.BETWEEN_MOMENTS, vm.state.value.momentExperience)
        assertTrue(vm.state.value.contextContent is ShellContentState.Empty)
        assertEquals(2, vm.state.value.moments.size)
        assertFalse(vm.state.value.showMomentSwitcher)
    }

    @Test
    fun personalActiveShowsSwitcher() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(personalMoments = listOf(MomentSummary("m1", "Now", "ACTIVE"))),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        assertEquals(MomentExperienceKind.ACTIVE, vm.state.value.momentExperience)
        assertTrue(vm.state.value.contextContent is ShellContentState.Ready)
        assertTrue(vm.state.value.showMomentSwitcher)
        assertEquals("m1", vm.state.value.selectedMomentId)
    }

    @Test
    fun selectMomentUpdatesSelection() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(
                personalMoments = listOf(
                    MomentSummary("m1", "Alpha", "ACTIVE"),
                    MomentSummary("m2", "Beta", "ACTIVE"),
                ),
            ),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        vm.selectMoment("m2")
        assertEquals("m2", vm.state.value.selectedMomentId)
        assertEquals("Beta", vm.state.value.selectedMomentTitle)
    }

    @Test
    fun groupNoActiveMoment() = runTest {
        val vm = AppShellViewModel(FakeMeGateway(groupMoments = emptyList()))
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        vm.selectContext(AppContext.GROUP)
        advanceUntilIdle()
        assertEquals(MomentExperienceKind.FIRST_MOMENT, vm.state.value.momentExperience)
        assertFalse(vm.state.value.showMomentSwitcher)
    }

    @Test
    fun groupHistoryExists() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(groupMoments = listOf(MomentSummary("g1", "Trip", "COMPLETED"))),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        vm.selectContext(AppContext.GROUP)
        advanceUntilIdle()
        assertEquals(MomentExperienceKind.BETWEEN_MOMENTS, vm.state.value.momentExperience)
        assertFalse(vm.state.value.showMomentSwitcher)
    }

    @Test
    fun groupActiveShowsSwitcher() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(groupMoments = listOf(MomentSummary("g1", "Trip", "ACTIVE"))),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        vm.selectContext(AppContext.GROUP)
        advanceUntilIdle()
        assertTrue(vm.state.value.showMomentSwitcher)
        assertEquals(MomentExperienceKind.ACTIVE, vm.state.value.momentExperience)
    }

    @Test
    fun businessNoCompanyRemainsSetup() = runTest {
        val vm = AppShellViewModel(FakeMeGateway(companies = emptyList()))
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        vm.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        assertTrue(vm.state.value.contextContent is ShellContentState.Empty)
        assertNull(vm.state.value.selectedCompany)
        assertFalse(vm.state.value.showMomentSwitcher)
    }

    @Test
    fun businessCompanyNoActiveMoment() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(
                companies = listOf(CompanySummary("c1", "Acme")),
                businessMoments = emptyList(),
            ),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        vm.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        assertEquals("c1", vm.state.value.selectedCompany?.companyId)
        assertEquals(MomentExperienceKind.FIRST_MOMENT, vm.state.value.momentExperience)
        assertFalse(vm.state.value.showMomentSwitcher)
    }

    @Test
    fun businessCompanyWithHistory() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(
                companies = listOf(CompanySummary("c1", "Acme")),
                businessMoments = listOf(MomentSummary("b1", "Launch", "COMPLETED")),
            ),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        vm.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        assertEquals(MomentExperienceKind.BETWEEN_MOMENTS, vm.state.value.momentExperience)
        assertFalse(vm.state.value.showMomentSwitcher)
    }

    @Test
    fun personalErrorIsNotEmpty() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(bootstrapError = ApiResultException.Network()),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        assertTrue(vm.state.value.contextContent is ShellContentState.Offline)
        assertEquals(MomentExperienceKind.ERROR, vm.state.value.momentExperience)
    }

    @Test
    fun life360OpenDismissPreservesShellSelection() = runTest {
        val vm = AppShellViewModel(
            FakeMeGateway(companies = listOf(CompanySummary("c1", "Acme"))),
        )
        vm.bindIdentity(ShellIdentity("u1", "Ada", null, null))
        advanceUntilIdle()
        vm.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        vm.selectCompany(CompanySummary("c1", "Acme"))
        vm.selectBottomDestination(BottomDestination.LIFE)
        val beforeContext = vm.state.value.selectedContext
        val beforeCompany = vm.state.value.selectedCompany?.companyId
        val beforeMoment = vm.state.value.selectedMomentId
        val beforeTab = vm.state.value.bottomDestination

        vm.openLife360(true)
        assertTrue(vm.state.value.life360Open)
        assertFalse(vm.state.value.profileOpen)
        assertEquals(beforeContext, vm.state.value.selectedContext)
        assertEquals(beforeCompany, vm.state.value.selectedCompany?.companyId)
        assertEquals(beforeMoment, vm.state.value.selectedMomentId)
        assertEquals(beforeTab, vm.state.value.bottomDestination)

        vm.openProfile(true)
        assertTrue(vm.state.value.profileOpen)
        assertFalse(vm.state.value.life360Open)

        vm.openLife360(true)
        vm.openLife360(false)
        assertFalse(vm.state.value.life360Open)
        assertEquals(beforeContext, vm.state.value.selectedContext)
        assertEquals(beforeCompany, vm.state.value.selectedCompany?.companyId)
        assertEquals(beforeMoment, vm.state.value.selectedMomentId)
        assertEquals(beforeTab, vm.state.value.bottomDestination)
    }

    @Test
    fun circleShowsEmptyComingSoonNotDeferred() = runTest {
        val vm = AppShellViewModel(FakeMeGateway())
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        vm.selectContext(AppContext.CIRCLE)
        advanceUntilIdle()
        assertTrue(vm.state.value.contextContent is ShellContentState.Empty)
        assertTrue(vm.state.value.moments.isEmpty())
        assertFalse(vm.state.value.showMomentSwitcher)
        assertEquals(AppContext.CIRCLE, vm.state.value.selectedContext)
    }

    @Test
    fun circleDoesNotLeakOtherContextMoments() = runTest {
        val personal = listOf(MomentSummary("p1", "Personal A", "ACTIVE"))
        val group = listOf(MomentSummary("g1", "Group A", "ACTIVE"))
        val vm = AppShellViewModel(
            FakeMeGateway(personalMoments = personal, groupMoments = group),
        )
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        vm.selectContext(AppContext.PERSONAL)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.moments.size)
        vm.selectContext(AppContext.CIRCLE)
        advanceUntilIdle()
        assertTrue(vm.state.value.moments.isEmpty())
        assertTrue(vm.state.value.contextContent is ShellContentState.Empty)
        vm.selectContext(AppContext.PERSONAL)
        advanceUntilIdle()
        assertEquals("p1", vm.state.value.moments.firstOrNull()?.momentId)
    }

    @Test
    fun forbiddenDoesNotClearIdentity() = runTest {
        val vm = AppShellViewModel(FakeMeGateway(bootstrapError = ApiResultException.Forbidden()))
        vm.bindIdentity(ShellIdentity("u1", "Ada", null, null))
        advanceUntilIdle()
        assertTrue(vm.state.value.contextContent is ShellContentState.Forbidden)
        assertEquals("u1", vm.state.value.identity?.userId)
    }

    @Test
    fun bottomNavRemainsUsableWhileEmpty() = runTest {
        val vm = AppShellViewModel(FakeMeGateway())
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        vm.selectBottomDestination(BottomDestination.MOMENTS)
        vm.selectBottomDestination(BottomDestination.LIFE)
        vm.selectBottomDestination(BottomDestination.CREATE)
        assertEquals(BottomDestination.CREATE, vm.state.value.bottomDestination)
        assertTrue(vm.state.value.contextContent is ShellContentState.Empty)
    }

    @Test
    fun logoutClearsShellState() = runTest {
        val vm = AppShellViewModel(FakeMeGateway(companies = listOf(CompanySummary("c1", "Acme"))))
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        vm.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        vm.clearForLogout()
        assertNull(vm.state.value.identity)
        assertNull(vm.state.value.selectedCompany)
        assertEquals(AppContext.PERSONAL, vm.state.value.selectedContext)
    }

    @Test
    fun accountSwitchIsolation() = runTest {
        val vm = AppShellViewModel(FakeMeGateway(userId = "userA", companies = listOf(CompanySummary("cA", "A Co"))))
        vm.bindIdentity(ShellIdentity("userA", "A", null, null))
        vm.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        vm.clearForLogout()
        val vmB = AppShellViewModel(FakeMeGateway(userId = "userB", companies = emptyList()))
        vmB.bindIdentity(ShellIdentity("userB", "B", null, null))
        vmB.selectContext(AppContext.BUSINESS)
        advanceUntilIdle()
        assertEquals("userB", vmB.state.value.identity?.userId)
        assertTrue(vmB.state.value.companies.isEmpty())
        assertNull(vmB.state.value.selectedCompany)
    }
    @Test
    fun momentSwitchBumpsScopedRefreshWithoutClearingIdentity() = runTest {
        val moments = listOf(
            MomentSummary("m1", "Goal A", "ACTIVE", "LIFE_RHYTHM", null),
            MomentSummary("m2", "Goal B", "ACTIVE", "LIFE_RHYTHM", null),
        )
        val gateway = CountingMeGateway(personalMoments = moments)
        val vm = AppShellViewModel(gateway)
        vm.bindIdentity(ShellIdentity("u1", null, null, null))
        advanceUntilIdle()
        val bootCallsAfterBind = gateway.bootstrapCalls
        val tokenBefore = vm.state.value.personalTabRefreshToken
        vm.selectMoment("m2")
        advanceUntilIdle()
        assertEquals("m2", vm.state.value.selectedMomentId)
        assertTrue(vm.state.value.personalTabRefreshToken > tokenBefore)
        // Scoped refresh must not force another /v1/me bootstrap fan-out.
        assertEquals(bootCallsAfterBind, gateway.bootstrapCalls)
    }
}

private class CountingMeGateway(
    personalMoments: List<MomentSummary> = emptyList(),
) : FakeMeGateway(personalMoments = personalMoments) {
    var bootstrapCalls = 0
        private set

    override suspend fun getBootstrap(): Result<com.example.momentra.domain.ShellBootstrap> {
        bootstrapCalls += 1
        return super.getBootstrap()
    }
}

private open class FakeMeGateway(
    private val userId: String = "u1",
    private val companies: List<CompanySummary> = emptyList(),
    private val personalMoments: List<MomentSummary> = emptyList(),
    private val groupMoments: List<MomentSummary> = emptyList(),
    private val businessMoments: List<MomentSummary> = emptyList(),
    private val hasLife360: Boolean = false,
    private val bootstrapError: Throwable? = null,
) : MeGateway {
    override suspend fun getMe(): Result<ShellIdentity> =
        Result.success(ShellIdentity(userId, null, null, null))

    override suspend fun getBootstrap(): Result<com.example.momentra.domain.ShellBootstrap> {
        if (bootstrapError != null) return Result.failure(bootstrapError)
        return Result.success(
            com.example.momentra.domain.ShellBootstrap(
                identity = ShellIdentity(userId, null, null, null),
                supportedContexts = listOf(
                    AppContext.PERSONAL,
                    AppContext.GROUP,
                    AppContext.BUSINESS,
                    AppContext.CIRCLE,
                ),
                currentlySelectedContext = AppContext.PERSONAL,
                personalMoments = personalMoments,
                groupMoments = groupMoments,
                businessMoments = businessMoments,
                companies = companies,
                selectedCompany = companies.firstOrNull(),
                capabilities = emptyList(),
                roles = listOf("USER"),
                preferencesTimezone = "UTC",
                preferencesLocale = null,
                featureFlags = emptyMap(),
            ),
        )
    }

    override fun cachedBootstrap(userId: String): com.example.momentra.domain.ShellBootstrap? = null

    override fun isBootstrapCacheFresh(userId: String, maxAgeMs: Long): Boolean = false

    override fun clearBootstrapCache(userId: String?) {}

    override suspend fun listCompanies(): Result<List<CompanySummary>> =
        Result.success(companies)

    override suspend fun listGroupMomentCount(): Result<Int> =
        Result.success(groupMoments.size.coerceAtMost(1))

    override suspend fun listPersonalMoments(limit: Int): Result<List<MomentSummary>> =
        Result.success(personalMoments.take(limit))

    override suspend fun listGroupMoments(limit: Int): Result<List<MomentSummary>> =
        Result.success(groupMoments.take(limit))

    override suspend fun listBusinessMoments(limit: Int): Result<List<MomentSummary>> =
        Result.success(businessMoments.take(limit))

    override suspend fun hasLife360(): Result<Boolean> =
        Result.success(hasLife360)
}
