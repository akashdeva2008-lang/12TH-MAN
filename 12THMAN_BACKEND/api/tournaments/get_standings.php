<?php
// ==========================================================
// 12THMAN BACKEND - TOURNAMENT STANDINGS & LIVE POINTS TABLE API
// Endpoint: GET /api/tournaments/get_standings.php?tournament_id=T301
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

$tournamentIdRaw = trim($_GET['tournament_id'] ?? $_GET['tournamentId'] ?? '');

$numericTournamentId = 1;
if (!empty($tournamentIdRaw)) {
    $numericTournamentId = (int)preg_replace('/[^0-9]/', '', $tournamentIdRaw);
    if ($numericTournamentId > 300) {
        $numericTournamentId -= 300;
    }
}

try {
    $db = getDatabaseConnection();

    // 1. Fetch all registered teams in tournament
    $tStmt = $db->prepare("SELECT tm.id, tm.team_name, COALESCE(tt.group_name, 'Group A') AS group_name
                           FROM teams tm
                           LEFT JOIN tournament_teams tt ON tt.team_id = tm.id AND tt.tournament_id = ?
                           WHERE tm.tournament_id = ? OR tt.tournament_id = ?
                           GROUP BY tm.id
                           ORDER BY tm.id ASC");
    $tStmt->execute([$numericTournamentId, $numericTournamentId, $numericTournamentId]);
    $teams = $tStmt->fetchAll();

    // Initialize standings map
    $standings = [];
    foreach ($teams as $t) {
        $tId = (int)$t['id'];
        $standings[$tId] = [
            "teamId"       => "TM" . $tId,
            "numeric_id"   => $tId,
            "teamName"     => $t['team_name'],
            "groupName"    => $t['group_name'],
            "played"       => 0,
            "won"          => 0,
            "lost"         => 0,
            "tied"         => 0,
            "noResult"     => 0,
            "points"       => 0,
            "runsScored"   => 0,
            "oversFaced"   => 0.0,
            "runsConceded" => 0,
            "oversBowled"  => 0.0,
            "nrr"          => 0.000,
            "form"         => []
        ];
    }

    // 2. Fetch completed matches for this tournament
    $mStmt = $db->prepare("SELECT id, team1_id, team2_id, team1_name, team2_name, status, winner_id, result, score_a, score_b 
                           FROM matches 
                           WHERE tournament_id = ? AND status = 'COMPLETED'");
    $mStmt->execute([$numericTournamentId]);
    $matches = $mStmt->fetchAll();

    // Helper to parse score string e.g. "178/4 (20.0)" -> runs=178, overs=20.0
    function parseScoreString($str) {
        $runs = 0;
        $overs = 20.0;
        if (preg_match('/(\d+)\/\d+\s*\((\d+\.?\d*)\)/', $str, $matches)) {
            $runs = (int)$matches[1];
            $overs = (float)$matches[2];
        } elseif (preg_match('/(\d+)\s*runs/i', $str, $matches)) {
            $runs = (int)$matches[1];
        }
        if ($overs <= 0) $overs = 20.0;
        return ['runs' => $runs, 'overs' => $overs];
    }

    // Convert cricket overs e.g. 19.3 overs to actual decimal overs: 19 + 3/6 = 19.5
    function toDecimalOvers($ov) {
        $fullOvers = floor($ov);
        $balls = round(($ov - $fullOvers) * 10);
        return $fullOvers + ($balls / 6.0);
    }

    foreach ($matches as $m) {
        $t1 = (int)$m['team1_id'];
        $t2 = (int)$m['team2_id'];

        if (!isset($standings[$t1]) || !isset($standings[$t2])) {
            continue;
        }

        $standings[$t1]['played']++;
        $standings[$t2]['played']++;

        $scoreA = parseScoreString($m['score_a']);
        $scoreB = parseScoreString($m['score_b']);

        $standings[$t1]['runsScored'] += $scoreA['runs'];
        $standings[$t1]['oversFaced'] += toDecimalOvers($scoreA['overs']);
        $standings[$t1]['runsConceded'] += $scoreB['runs'];
        $standings[$t1]['oversBowled'] += toDecimalOvers($scoreB['overs']);

        $standings[$t2]['runsScored'] += $scoreB['runs'];
        $standings[$t2]['oversFaced'] += toDecimalOvers($scoreB['overs']);
        $standings[$t2]['runsConceded'] += $scoreA['runs'];
        $standings[$t2]['oversBowled'] += toDecimalOvers($scoreA['overs']);

        $winnerId = (int)($m['winner_id'] ?? 0);
        $resultText = strtolower($m['result'] ?? '');

        if ($winnerId == $t1 || strpos($resultText, strtolower($standings[$t1]['teamName'])) !== false) {
            $standings[$t1]['won']++;
            $standings[$t1]['points'] += 2;
            $standings[$t1]['form'][] = "W";
            $standings[$t2]['lost']++;
            $standings[$t2]['form'][] = "L";
        } elseif ($winnerId == $t2 || strpos($resultText, strtolower($standings[$t2]['teamName'])) !== false) {
            $standings[$t2]['won']++;
            $standings[$t2]['points'] += 2;
            $standings[$t2]['form'][] = "W";
            $standings[$t1]['lost']++;
            $standings[$t1]['form'][] = "L";
        } elseif (strpos($resultText, 'tied') !== false) {
            $standings[$t1]['tied']++;
            $standings[$t1]['points'] += 1;
            $standings[$t1]['form'][] = "T";
            $standings[$t2]['tied']++;
            $standings[$t2]['points'] += 1;
            $standings[$t2]['form'][] = "T";
        } else {
            $standings[$t1]['noResult']++;
            $standings[$t1]['points'] += 1;
            $standings[$t1]['form'][] = "NR";
            $standings[$t2]['noResult']++;
            $standings[$t2]['points'] += 1;
            $standings[$t2]['form'][] = "NR";
        }
    }

    // 3. Compute Net Run Rate (NRR)
    foreach ($standings as &$st) {
        if ($st['played'] > 0 && $st['oversFaced'] > 0 && $st['oversBowled'] > 0) {
            $forRate = $st['runsScored'] / $st['oversFaced'];
            $againstRate = $st['runsConceded'] / $st['oversBowled'];
            $st['nrr'] = round($forRate - $againstRate, 3);
        } else {
            $st['nrr'] = 0.000;
        }
    }

    // 4. Sort Standings: Points DESC, NRR DESC, Won DESC
    $sortedStandings = array_values($standings);
    usort($sortedStandings, function ($a, $b) {
        if ($b['points'] != $a['points']) {
            return $b['points'] <=> $a['points'];
        }
        if ($b['nrr'] != $a['nrr']) {
            return $b['nrr'] <=> $a['nrr'];
        }
        return $b['won'] <=> $a['won'];
    });

    // Add position rank
    foreach ($sortedStandings as $idx => &$row) {
        $row['position'] = $idx + 1;
    }

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => $sortedStandings
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Standings error: " . $e->getMessage()]);
}
