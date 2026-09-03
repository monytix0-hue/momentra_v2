package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.ui.shell.empty.personal.PersonalSetupCatalog
import com.example.momentra.ui.shell.empty.personal.PersonalSetupKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalPulseFamilyTest {

    @Test
    fun mapsLifeSubtypeCodesToLifeOperations() {
        listOf("LIFE_RHYTHM", "LIFE_RECOVERY", "LIFE_MOOD", "LIFE_WELLBEING").forEach { code ->
            assertEquals(PersonalPulseFamily.LIFE_OPERATIONS, personalPulseFamilyFor(code))
        }
    }

    @Test
    fun mapsFutureSubtypeCodesToFutureBuilding() {
        listOf(
            "FUTURE_GOAL",
            "FUTURE_MILESTONE",
            "FUTURE_PROGRESS",
            "FUTURE_OPPORTUNITY",
            "FUTURE_PIVOT",
            "FUTURE_LEARNING_ACTIVITY",
        ).forEach { code ->
            assertEquals(PersonalPulseFamily.FUTURE_BUILDING, personalPulseFamilyFor(code))
        }
    }

    @Test
    fun mapsLifestyleSubtypeCodesToLifestyle() {
        listOf(
            "LIFESTYLE",
            "LIFESTYLE_EXPERIENCE",
            "LIFESTYLE_WELLBEING",
            "LIFESTYLE_DISCOVERY",
            "LIFESTYLE_CREATION",
        ).forEach { code ->
            assertEquals(PersonalPulseFamily.LIFESTYLE, personalPulseFamilyFor(code))
        }
    }

    @Test
    fun mapsRelationshipSubtypeCodesToRelationships() {
        listOf(
            "RELATIONSHIP_CONNECTION",
            "RELATIONSHIP_INTERACTION",
            "RELATIONSHIP_SUPPORT",
            "RELATIONSHIP_SHARED_EXPERIENCE",
            "RELATIONSHIP_INVESTMENT",
        ).forEach { code ->
            assertEquals(PersonalPulseFamily.RELATIONSHIPS, personalPulseFamilyFor(code))
        }
    }

    @Test
    fun setupCatalogDefaultCodesMapToExpectedFamilies() {
        val cases = listOf(
            PersonalSetupKind.LIFE_OPERATIONS to PersonalPulseFamily.LIFE_OPERATIONS,
            PersonalSetupKind.FUTURE_BUILDING to PersonalPulseFamily.FUTURE_BUILDING,
            PersonalSetupKind.LIFESTYLE to PersonalPulseFamily.LIFESTYLE,
            PersonalSetupKind.RELATIONSHIPS to PersonalPulseFamily.RELATIONSHIPS,
        )
        cases.forEach { (kind, expected) ->
            val code = PersonalSetupCatalog.forKind(kind).momentTypeCode
            assertEquals(expected, personalPulseFamilyFor(code))
        }
    }
}
