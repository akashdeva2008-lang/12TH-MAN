package com.example.twelveman

import com.example.twelveman.data.model.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicOversInningsTest {

    @Test
    fun testMaxLegalBallsForAllFormats() {
        val formatOvers = mapOf(
            5 to 30,
            10 to 60,
            15 to 90,
            20 to 120,
            25 to 150,
            50 to 300,
            8 to 48
        )

        for ((overs, expectedBalls) in formatOvers) {
            val match = Match(id = "M_TEST_$overs", totalOvers = overs)
            assertEquals("Expected $expectedBalls legal balls for $overs overs", expectedBalls, match.maxLegalBalls)
        }
    }

    @Test
    fun testFirstAndSecondInningsCompletionThresholds() {
        val oversList = listOf(5, 10, 15, 20, 25, 50, 7)
        for (overs in oversList) {
            val maxBalls = overs * 6
            val match = Match(
                id = "M_LIMIT_$overs",
                teamA = "Alpha",
                teamB = "Beta",
                totalOvers = overs,
                currentInnings = 1
            )

            match.legalBallsA = maxBalls - 1
            assertFalse("Innings 1 should not be complete before $maxBalls balls", match.isFirstInningsOverLimitReached)
            assertEquals(1, match.remainingLegalBalls)

            match.legalBallsA = maxBalls
            assertTrue("Innings 1 should complete at $maxBalls balls", match.isFirstInningsOverLimitReached)
            assertEquals(0, match.remainingLegalBalls)

            // Transition to Innings 2
            match.currentInnings = 2
            match.legalBallsB = maxBalls - 3
            assertFalse("Innings 2 should not be complete with 3 balls left", match.isSecondInningsOverLimitReached)
            assertEquals(3, match.remainingLegalBalls)

            match.legalBallsB = maxBalls
            assertTrue("Innings 2 should complete at $maxBalls balls", match.isSecondInningsOverLimitReached)
            assertEquals(0, match.remainingLegalBalls)
        }
    }
}
