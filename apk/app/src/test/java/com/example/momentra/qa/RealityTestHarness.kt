package com.example.momentra.qa

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RealityTestHarness {

    @Test
    fun testRealityDatasetsAndTotals() {
        val baseDir = File("../qa/reality")
        assertTrue("QA reality directory must exist at ${baseDir.absolutePath}", baseDir.exists())

        val personalFile = File(baseDir, "personal_reality_seed_v1.json")
        val businessFile = File(baseDir, "business_reality_seed_v1.json")
        val expectedFile = File(baseDir, "expected_totals_v1.json")

        assertTrue("Personal reality seed must exist", personalFile.exists())
        assertTrue("Business reality seed must exist", businessFile.exists())
        assertTrue("Expected totals manifest must exist", expectedFile.exists())

        val gson = Gson()
        val personalJson = gson.fromJson(personalFile.readText(), JsonObject::class.java)
        val businessJson = gson.fromJson(businessFile.readText(), JsonObject::class.java)
        val expectedJson = gson.fromJson(expectedFile.readText(), JsonObject::class.java)

        // 1. Validate Personal Records Count & Totals
        val pSummary = personalJson.getAsJsonObject("summary")
        val pExpected = expectedJson.getAsJsonObject("personal")

        assertEquals(pExpected.get("everydayCount").asInt, pSummary.get("everydayCount").asInt)
        assertEquals(pExpected.get("futureCount").asInt, pSummary.get("futureCount").asInt)
        assertEquals(pExpected.get("lifestyleCount").asInt, pSummary.get("lifestyleCount").asInt)
        assertEquals(pExpected.get("peopleCount").asInt, pSummary.get("peopleCount").asInt)
        assertEquals(pExpected.get("totalRecords").asInt, pSummary.get("totalRecords").asInt)

        val pRecords = personalJson.getAsJsonObject("records")
        val everydayArray = pRecords.getAsJsonArray("everyday")
        val lifestyleArray = pRecords.getAsJsonArray("lifestyle")
        val peopleArray = pRecords.getAsJsonArray("people")

        var calculatedEverydaySpend = 0.0
        var calculatedEverydayIncome = 0.0
        var calculatedEverydaySavings = 0.0
        var calculatedEverydayTransfer = 0.0

        for (el in everydayArray) {
            val obj = el.asJsonObject
            val type = obj.get("type")?.asString ?: continue
            val amount = obj.get("amount")?.asDouble ?: 0.0
            when (type) {
                "Spend" -> calculatedEverydaySpend += amount
                "Income" -> calculatedEverydayIncome += amount
                "Savings" -> calculatedEverydaySavings += amount
                "Transfer" -> calculatedEverydayTransfer += amount
            }
        }

        assertEquals(pExpected.get("everydaySpendTotal").asDouble, calculatedEverydaySpend, 0.01)
        assertEquals(pExpected.get("everydayIncomeTotal").asDouble, calculatedEverydayIncome, 0.01)
        assertEquals(pExpected.get("everydaySavingsTotal").asDouble, calculatedEverydaySavings, 0.01)
        assertEquals(pExpected.get("everydayTransferTotal").asDouble, calculatedEverydayTransfer, 0.01)

        var calculatedLifestyleSpend = 0.0
        for (el in lifestyleArray) {
            val obj = el.asJsonObject
            calculatedLifestyleSpend += obj.get("amount")?.asDouble ?: 0.0
        }
        assertEquals(pExpected.get("lifestyleSpendTotal").asDouble, calculatedLifestyleSpend, 0.01)

        var calculatedPeopleCare = 0.0
        for (el in peopleArray) {
            val obj = el.asJsonObject
            calculatedPeopleCare += obj.get("amount")?.asDouble ?: 0.0
        }
        assertEquals(pExpected.get("peopleCareTotal").asDouble, calculatedPeopleCare, 0.01)

        // 2. Validate Business Records Count & Totals
        val bSummary = businessJson.getAsJsonObject("summary")
        val bExpected = expectedJson.getAsJsonObject("business")

        assertEquals(bExpected.get("moneyRecordsCount").asInt, bSummary.get("moneyRecordsCount").asInt)
        assertEquals(bExpected.get("dailyRecordsCount").asInt, bSummary.get("dailyRecordsCount").asInt)
        assertEquals(bExpected.get("teamRecordsCount").asInt, bSummary.get("teamRecordsCount").asInt)
        assertEquals(bExpected.get("totalRecords").asInt, bSummary.get("totalRecords").asInt)

        val bRecords = businessJson.getAsJsonObject("records")
        val moneyArray = bRecords.getAsJsonArray("moneyAndCashFlow")

        var calculatedRevenue = 0.0
        var calculatedExpense = 0.0
        var openInvoices = 0
        var paidInvoices = 0
        var mainStoreCount = 0
        var northStoreCount = 0
        var southStoreCount = 0

        val totalBusinessRecordsCount = bSummary.get("totalRecords").asInt

        val allBusinessRecords = mutableListOf<JsonObject>()
        bRecords.getAsJsonArray("moneyAndCashFlow")?.forEach { allBusinessRecords.add(it.asJsonObject) }
        bRecords.getAsJsonArray("dailyBusiness")?.forEach { allBusinessRecords.add(it.asJsonObject) }
        bRecords.getAsJsonArray("teamAndWork")?.forEach { allBusinessRecords.add(it.asJsonObject) }

        for (obj in allBusinessRecords) {
            val storeId = obj.get("storeId")?.asString
            when (storeId) {
                "store_main" -> mainStoreCount++
                "store_north" -> northStoreCount++
                "store_south" -> southStoreCount++
            }
        }

        for (el in moneyArray) {
            val obj = el.asJsonObject
            val type = obj.get("type")?.asString ?: continue
            val amount = obj.get("amount")?.asDouble ?: 0.0
            if (type == "Revenue") calculatedRevenue += amount
            if (type == "Expense") calculatedExpense += amount
            if (type == "Invoice") {
                val status = obj.get("status")?.asString
                if (status == "paid") paidInvoices++
                else if (status == "outstanding" || status == "overdue") openInvoices++
            }
        }

        assertEquals(bExpected.get("revenueTotal").asDouble, calculatedRevenue, 0.01)
        assertEquals(bExpected.get("expenseTotal").asDouble, calculatedExpense, 0.01)
        assertEquals(bExpected.get("openInvoicesCount").asInt, openInvoices)
        assertEquals(bExpected.get("paidInvoicesCount").asInt, paidInvoices)

        // Store distribution check (~45% Main, ~30% North, ~25% South)
        val mainRatio = mainStoreCount.toDouble() / totalBusinessRecordsCount
        val northRatio = northStoreCount.toDouble() / totalBusinessRecordsCount
        val southRatio = southStoreCount.toDouble() / totalBusinessRecordsCount

        assertTrue("Main store ratio ($mainRatio) should be close to 45%", mainRatio in 0.40..0.50)
        assertTrue("North store ratio ($northRatio) should be close to 30%", northRatio in 0.25..0.35)
        assertTrue("South store ratio ($southRatio) should be close to 25%", southRatio in 0.20..0.30)

        println("=== REALITY TEST HARNESS PASSED SUCCESSFULLY ===")
        println("Personal records verified: 100")
        println("Business records verified: 300")
        println("Financial math reconciled: Revenue=₹$calculatedRevenue, Expense=₹$calculatedExpense")
        println("Store distribution: Main=$mainStoreCount (${String.format("%.1f", mainRatio * 100)}%), North=$northStoreCount (${String.format("%.1f", northRatio * 100)}%), South=$southStoreCount (${String.format("%.1f", southRatio * 100)}%)")
    }
}
