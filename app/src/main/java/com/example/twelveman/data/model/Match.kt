package com.example.twelveman.data.model

import java.util.Locale

data class BatterScore(
    val name: String = "",
    var runs: Int = 0,
    var balls: Int = 0,
    var fours: Int = 0,
    var sixes: Int = 0,
    var isStriker: Boolean = false,
    var isOut: Boolean = false,
    var dismissalType: String = "not out", // e.g., "b Bumrah", "c Rohit b Shami", "run out (Kohli)", "not out"
    var bowlerName: String = "",
    var fielderName: String = ""
) {
    val strikeRate: Double
        get() = if (balls > 0) (runs.toDouble() * 100.0 / balls) else 0.0

    val strikeRateDisplay: String
        get() = String.format(Locale.US, "%.1f", strikeRate)
}

data class BowlerScore(
    val name: String = "",
    var legalBalls: Int = 0,
    var maidens: Int = 0,
    var runsConceded: Int = 0,
    var wickets: Int = 0,
    var wides: Int = 0,
    var noBalls: Int = 0
) {
    val oversDisplay: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val economy: Double
        get() = if (legalBalls > 0) (runsConceded.toDouble() * 6.0 / legalBalls) else 0.0

    val economyDisplay: String
        get() = String.format(Locale.US, "%.2f", economy)
}

data class FallOfWicket(
    val wicketNumber: Int = 0,
    val runs: Int = 0,
    val overs: String = "",
    val batsmanName: String = ""
)

data class ExtrasBreakdown(
    var wides: Int = 0,
    var noBalls: Int = 0,
    var byes: Int = 0,
    var legByes: Int = 0,
    var penalty: Int = 0
) {
    val total: Int
        get() = wides + noBalls + byes + legByes + penalty
}

data class InningsScorecard(
    val battingList: MutableList<BatterScore> = mutableListOf(),
    val bowlingList: MutableList<BowlerScore> = mutableListOf(),
    val fallOfWickets: MutableList<FallOfWicket> = mutableListOf(),
    var extras: ExtrasBreakdown = ExtrasBreakdown(),
    val currentOverDeliveries: MutableList<String> = mutableListOf()
) {
    fun deepCopy(): InningsScorecard {
        val copy = InningsScorecard()
        copy.battingList.addAll(this.battingList.map { it.copy() })
        copy.bowlingList.addAll(this.bowlingList.map { it.copy() })
        copy.fallOfWickets.addAll(this.fallOfWickets.map { it.copy() })
        copy.extras = this.extras.copy()
        copy.currentOverDeliveries.addAll(this.currentOverDeliveries)
        return copy
    }
}

data class MatchStateSnapshot(
    val runsA: Int = 0,
    val wicketsA: Int = 0,
    val legalBallsA: Int = 0,
    val oversA: Double = 0.0,
    val runsB: Int = 0,
    val wicketsB: Int = 0,
    val legalBallsB: Int = 0,
    val oversB: Double = 0.0,
    val currentInnings: Int = 1,
    val scoreA: String = "",
    val scoreB: String = "",
    val strikerName: String = "",
    val strikerRuns: Int = 0,
    val strikerBalls: Int = 0,
    val nonStrikerName: String = "",
    val nonStrikerRuns: Int = 0,
    val nonStrikerBalls: Int = 0,
    val bowlerName: String = "",
    val bowlerOvers: Double = 0.0,
    val bowlerRunsConceded: Int = 0,
    val bowlerWickets: Int = 0,
    val previousBowlerName: String = "",
    val partnershipRuns: Int = 0,
    val partnershipBalls: Int = 0,
    val extrasWide: Int = 0,
    val extrasNoBall: Int = 0,
    val extrasBye: Int = 0,
    val extrasLegBye: Int = 0,
    val extrasPenalty: Int = 0,
    val target: Int = 0,
    val innings1Snapshot: InningsScorecard? = null,
    val innings2Snapshot: InningsScorecard? = null
)

data class Match(
    val id: String = "",
    val tournamentId: String = "",
    val matchNumber: Int = 0,
    val teamA: String = "",
    val teamB: String = "",
    val date: String = "",
    val time: String = "",
    val venue: String = "",
    val stage: String = "", // "Group Stage", "Semi Final", "Final", "League Match"
    var status: String = "UPCOMING", // "UPCOMING", "LIVE", "COMPLETED"
    var result: String = "",
    var scoreA: String = "0/0 (0.0)",
    var scoreB: String = "0/0 (0.0)",
    var runsA: Int = 0,
    var wicketsA: Int = 0,
    var legalBallsA: Int = 0,
    var oversA: Double = 0.0,
    var runsB: Int = 0,
    var wicketsB: Int = 0,
    var legalBallsB: Int = 0,
    var oversB: Double = 0.0,
    var currentInnings: Int = 1, // 1 for Team A batting, 2 for Team B batting
    val commentary: MutableList<String> = mutableListOf("Match scheduled. Ready for live scoring."),
    val historyStack: MutableList<MatchStateSnapshot> = mutableListOf(),
    var format: String = "T20",
    var totalOvers: Int = 20,
    var ballType: String = "Leather Ball",
    var tossWinner: String = "",
    var tossDecision: String = "", // "BAT" or "BOWL"
    var battingTeam: String = "",
    var bowlingTeam: String = "",
    var strikerName: String = "",
    var strikerRuns: Int = 0,
    var strikerBalls: Int = 0,
    var nonStrikerName: String = "",
    var nonStrikerRuns: Int = 0,
    var nonStrikerBalls: Int = 0,
    var bowlerName: String = "",
    var previousBowlerName: String = "",
    var bowlerOvers: Double = 0.0,
    var bowlerRunsConceded: Int = 0,
    var bowlerWickets: Int = 0,
    var extrasWide: Int = 0,
    var extrasNoBall: Int = 0,
    var extrasBye: Int = 0,
    var extrasLegBye: Int = 0,
    var extrasPenalty: Int = 0,
    var partnershipRuns: Int = 0,
    var partnershipBalls: Int = 0,
    var target: Int = 0,
    var innings1: InningsScorecard = InningsScorecard(),
    var innings2: InningsScorecard = InningsScorecard()
) {
    val activeInnings: InningsScorecard
        get() = if (currentInnings == 1) innings1 else innings2

    val activeLegalBalls: Int
        get() = if (currentInnings == 1) legalBallsA else legalBallsB

    val activeRuns: Int
        get() = if (currentInnings == 1) runsA else runsB

    val activeWickets: Int
        get() = if (currentInnings == 1) wicketsA else wicketsB

    val activeOversDisplay: String
        get() = "${activeLegalBalls / 6}.${activeLegalBalls % 6}"

    val oversADisplay: String
        get() = "${legalBallsA / 6}.${legalBallsA % 6}"

    val oversBDisplay: String
        get() = "${legalBallsB / 6}.${legalBallsB % 6}"

    fun getStrikerScore(): BatterScore {
        val inn = activeInnings
        var found = inn.battingList.firstOrNull { it.name.equals(strikerName, ignoreCase = true) }
        if (found == null && strikerName.isNotBlank()) {
            found = BatterScore(name = strikerName, isStriker = true)
            inn.battingList.add(found)
        }
        return found ?: BatterScore(name = strikerName)
    }

    fun getNonStrikerScore(): BatterScore {
        val inn = activeInnings
        var found = inn.battingList.firstOrNull { it.name.equals(nonStrikerName, ignoreCase = true) }
        if (found == null && nonStrikerName.isNotBlank()) {
            found = BatterScore(name = nonStrikerName, isStriker = false)
            inn.battingList.add(found)
        }
        return found ?: BatterScore(name = nonStrikerName)
    }

    fun getCurrentBowlerScore(): BowlerScore {
        val inn = activeInnings
        var found = inn.bowlingList.firstOrNull { it.name.equals(bowlerName, ignoreCase = true) }
        if (found == null && bowlerName.isNotBlank()) {
            found = BowlerScore(name = bowlerName)
            inn.bowlingList.add(found)
        }
        return found ?: BowlerScore(name = bowlerName)
    }
}
