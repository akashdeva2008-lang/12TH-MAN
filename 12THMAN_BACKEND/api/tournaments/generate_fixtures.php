<?php
// ==========================================================
// 12THMAN BACKEND - AUTOMATIC FIXTURE GENERATOR API
// Endpoint: POST /api/tournaments/generate_fixtures.php
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Method not allowed. Use POST."]);
    exit();
}

$rawInput = file_get_contents('php://input');
$input = json_decode($rawInput, true);
if (!is_array($input)) {
    $input = $_POST;
}

$tournamentIdRaw = trim($input['tournamentId'] ?? $input['tournament_id'] ?? '');
$format          = strtoupper(trim($input['format'] ?? 'KNOCKOUT')); // KNOCKOUT, ROUND_ROBIN, LEAGUE, GROUP_KNOCKOUT
$venue           = trim($input['venue'] ?? $input['ground'] ?? 'Wankhede Cricket Ground');
$startDate       = trim($input['startDate'] ?? $input['start_date'] ?? date('d M Y'));
$overs           = (int)($input['overs'] ?? 20);
$umpireName      = trim($input['umpire'] ?? 'Official Umpire');
$scorerName      = trim($input['scorer'] ?? 'Official Scorer');
$clearExisting   = (bool)($input['clearExisting'] ?? true);

$numericTournamentId = (int)preg_replace('/[^0-9]/', '', $tournamentIdRaw);
if ($numericTournamentId > 300) {
    $numericTournamentId -= 300;
}

if ($numericTournamentId <= 0) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Valid Tournament ID is required."]);
    exit();
}

try {
    $db = getDatabaseConnection();

    // 1. Fetch approved registered teams for this tournament
    $tStmt = $db->prepare("SELECT tm.id, tm.team_name, COALESCE(tt.group_name, 'Group A') AS group_name
                           FROM teams tm
                           LEFT JOIN tournament_teams tt ON tt.team_id = tm.id AND tt.tournament_id = ?
                           WHERE (tm.tournament_id = ? OR tt.tournament_id = ?)
                           AND COALESCE(tt.status, tm.approval_status, 'APPROVED') = 'APPROVED'
                           GROUP BY tm.id
                           ORDER BY tm.id ASC");
    $tStmt->execute([$numericTournamentId, $numericTournamentId, $numericTournamentId]);
    $teams = $tStmt->fetchAll();

    $teamCount = count($teams);
    if ($teamCount < 2) {
        http_response_code(400);
        echo json_encode([
            "status"  => "error",
            "message" => "At least 2 approved teams are required to generate fixtures. Currently registered: $teamCount"
        ]);
        exit();
    }

    $db->beginTransaction();

    // Optional: Clear existing fixtures for fresh regeneration
    if ($clearExisting) {
        $del = $db->prepare("DELETE FROM matches WHERE tournament_id = ? AND status = 'UPCOMING'");
        $del->execute([$numericTournamentId]);
    }

    // Get current max match number
    $maxNumStmt = $db->prepare("SELECT COALESCE(MAX(match_number), 0) AS max_num FROM matches WHERE tournament_id = ?");
    $maxNumStmt->execute([$numericTournamentId]);
    $matchNumber = (int)$maxNumStmt->fetch()['max_num'];

    $generatedMatches = [];
    $times = ["09:00 AM", "01:30 PM", "05:30 PM", "08:00 PM"];
    $timeIdx = 0;
    $dayOffset = 0;

    $baseDate = strtotime($startDate) ?: time();

    // 2. Fixture Generation based on format
    if ($format === 'ROUND_ROBIN' || $format === 'LEAGUE') {
        // Round Robin: Every team plays every other team once
        for ($i = 0; $i < $teamCount; $i++) {
            for ($j = $i + 1; $j < $teamCount; $j++) {
                $matchNumber++;
                $teamA = $teams[$i];
                $teamB = $teams[$j];

                $matchDate = date('d M Y', strtotime("+$dayOffset days", $baseDate));
                $matchTime = $times[$timeIdx % count($times)];
                $timeIdx++;
                if ($timeIdx % 2 == 0) {
                    $dayOffset++;
                }

                $stage = "League Match";

                $ins = $db->prepare("INSERT INTO matches 
                    (tournament_id, match_number, team1_id, team2_id, team1_name, team2_name, match_date, match_time, venue, match_type, stage, status, umpire_name, scorer_name, overs, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'T20', ?, 'UPCOMING', ?, ?, ?, NOW())");
                $ins->execute([
                    $numericTournamentId, $matchNumber, $teamA['id'], $teamB['id'],
                    $teamA['team_name'], $teamB['team_name'], $matchDate, $matchTime, $venue, $stage, $umpireName, $scorerName, $overs
                ]);
                $newId = (int)$db->lastInsertId();

                $generatedMatches[] = [
                    "id"          => "M" . ($newId + 500),
                    "matchNumber" => $matchNumber,
                    "teamA"       => $teamA['team_name'],
                    "teamB"       => $teamB['team_name'],
                    "date"        => $matchDate,
                    "time"        => $matchTime,
                    "venue"       => $venue,
                    "stage"       => $stage,
                    "status"      => "UPCOMING"
                ];
            }
        }
    } else {
        // KNOCKOUT format (Default)
        // Handle powers of 2 and Byes
        $shuffled = $teams;
        // Seed teams
        $k = 0;
        $roundTeams = count($shuffled);
        
        // Stage label
        $stageName = "Round 1";
        if ($roundTeams <= 2) $stageName = "Final";
        elseif ($roundTeams <= 4) $stageName = "Semi Final";
        elseif ($roundTeams <= 8) $stageName = "Quarter Final";

        for ($i = 0; $i < $roundTeams; $i += 2) {
            if ($i + 1 < $roundTeams) {
                $matchNumber++;
                $teamA = $shuffled[$i];
                $teamB = $shuffled[$i + 1];

                $matchDate = date('d M Y', strtotime("+$dayOffset days", $baseDate));
                $matchTime = $times[$timeIdx % count($times)];
                $timeIdx++;
                if ($timeIdx % 2 == 0) $dayOffset++;

                $ins = $db->prepare("INSERT INTO matches 
                    (tournament_id, match_number, team1_id, team2_id, team1_name, team2_name, match_date, match_time, venue, match_type, stage, status, umpire_name, scorer_name, overs, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'T20', ?, 'UPCOMING', ?, ?, ?, NOW())");
                $ins->execute([
                    $numericTournamentId, $matchNumber, $teamA['id'], $teamB['id'],
                    $teamA['team_name'], $teamB['team_name'], $matchDate, $matchTime, $venue, $stageName, $umpireName, $scorerName, $overs
                ]);
                $newId = (int)$db->lastInsertId();

                $generatedMatches[] = [
                    "id"          => "M" . ($newId + 500),
                    "matchNumber" => $matchNumber,
                    "teamA"       => $teamA['team_name'],
                    "teamB"       => $teamB['team_name'],
                    "date"        => $matchDate,
                    "time"        => $matchTime,
                    "venue"       => $venue,
                    "stage"       => $stageName,
                    "status"      => "UPCOMING"
                ];
            } else {
                // Odd team gets a BYE to the next round
                $byeTeam = $shuffled[$i];
                // Record a note or auto-qualification
            }
        }
    }

    // Update tournament status to ONGOING if currently UPCOMING
    $db->prepare("UPDATE tournaments SET status = 'ONGOING', tournament_format = ? WHERE id = ? AND status = 'UPCOMING'")
       ->execute([$format, $numericTournamentId]);

    $db->commit();

    http_response_code(201);
    echo json_encode([
        "status"  => "success",
        "message" => "Successfully generated " . count($generatedMatches) . " fixtures for $format format.",
        "data"    => $generatedMatches
    ]);

} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) {
        $db->rollBack();
    }
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Fixture generation error: " . $e->getMessage()]);
}
