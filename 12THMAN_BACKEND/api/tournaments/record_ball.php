<?php
// ==========================================================
// 12THMAN BACKEND - RECORD BALL API (Atomic Upsert)
// Endpoint: POST /api/tournaments/record_ball.php
// ==========================================================

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

require_once __DIR__ . '/../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

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

$matchIdRaw       = trim($input['match_id'] ?? $input['id'] ?? '');
$inningsNumber    = (int)($input['innings_number'] ?? 1);
$overNumber       = (int)($input['over_number'] ?? 0);
$ballNumber       = (int)($input['ball_number'] ?? 1);
$isLegalBall      = isset($input['is_legal_ball']) ? (int)$input['is_legal_ball'] : 1;
$batsmanName      = trim($input['batsman_name'] ?? 'Striker');
$bowlerName       = trim($input['bowler_name'] ?? 'Bowler');
$runs             = (int)($input['runs'] ?? 0);
$extras           = (int)($input['extras'] ?? 0);
$extraType        = trim($input['extra_type'] ?? 'NONE');
$wicket           = !empty($input['wicket']) ? 1 : 0;
$wicketType       = trim($input['wicket_type'] ?? 'NONE');
$playerDismissed  = trim($input['player_dismissed'] ?? '');
$commentary       = trim($input['commentary'] ?? '');
$scoreA           = trim($input['score_a'] ?? $input['scoreA'] ?? '');
$scoreB           = trim($input['score_b'] ?? $input['scoreB'] ?? '');
$status           = trim($input['status'] ?? 'LIVE');
$result           = trim($input['result'] ?? '');

$numericMatchId = preg_replace('/[^0-9]/', '', $matchIdRaw);
if ((int)$numericMatchId > 500) {
    $numericMatchId = (int)$numericMatchId - 500;
} else {
    $numericMatchId = (int)$numericMatchId;
}

if ($numericMatchId <= 0) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Invalid match ID"]);
    exit();
}

try {
    $db = getDatabaseConnection();
    $db->beginTransaction();

    // 1. Get or create match_innings record
    $stmtInn = $db->prepare("SELECT id, total_runs, wickets, legal_balls, overs, extras FROM match_innings WHERE match_id = :mid AND innings_number = :inum LIMIT 1");
    $stmtInn->execute([':mid' => $numericMatchId, ':inum' => $inningsNumber]);
    $inningsRow = $stmtInn->fetch();

    if (!$inningsRow) {
        $insertInn = $db->prepare("
            INSERT INTO match_innings (match_id, innings_number, batting_team_id, bowling_team_id, batting_team_name, bowling_team_name, status)
            SELECT id, :inum, team1_id, team2_id, team1_name, team2_name, 'IN_PROGRESS'
            FROM matches WHERE id = :mid LIMIT 1
        ");
        $insertInn->execute([':inum' => $inningsNumber, ':mid' => $numericMatchId]);
        $inningsId = (int)$db->lastInsertId();
    } else {
        $inningsId = (int)$inningsRow['id'];
    }

    // 2. Perform atomic upsert on ball_by_ball to prevent duplicate ball records
    $stmtBall = $db->prepare("
        INSERT INTO ball_by_ball (
            innings_id, match_id, over_number, ball_number, is_legal_ball,
            batsman_name, bowler_name, runs, extras, extra_type,
            wicket, wicket_type, player_dismissed, commentary
        ) VALUES (
            :innings_id, :match_id, :over_number, :ball_number, :is_legal_ball,
            :batsman_name, :bowler_name, :runs, :extras, :extra_type,
            :wicket, :wicket_type, :player_dismissed, :commentary
        )
        ON DUPLICATE KEY UPDATE
            is_legal_ball = VALUES(is_legal_ball),
            batsman_name = VALUES(batsman_name),
            bowler_name = VALUES(bowler_name),
            runs = VALUES(runs),
            extras = VALUES(extras),
            extra_type = VALUES(extra_type),
            wicket = VALUES(wicket),
            wicket_type = VALUES(wicket_type),
            player_dismissed = VALUES(player_dismissed),
            commentary = VALUES(commentary),
            created_at = CURRENT_TIMESTAMP
    ");

    $stmtBall->execute([
        ':innings_id'        => $inningsId,
        ':match_id'          => $numericMatchId,
        ':over_number'       => $overNumber,
        ':ball_number'       => $ballNumber,
        ':is_legal_ball'     => $isLegalBall,
        ':batsman_name'      => $batsmanName,
        ':bowler_name'       => $bowlerName,
        ':runs'              => $runs,
        ':extras'            => $extras,
        ':extra_type'        => $extraType,
        ':wicket'            => $wicket,
        ':wicket_type'       => $wicketType,
        ':player_dismissed'  => $playerDismissed,
        ':commentary'        => $commentary
    ]);

    // 3. Recalculate innings aggregates accurately from all balls
    $stmtAgg = $db->prepare("
        SELECT 
            COALESCE(SUM(runs + extras), 0) AS total_runs,
            COALESCE(SUM(wicket), 0) AS total_wickets,
            COALESCE(SUM(is_legal_ball), 0) AS total_legal_balls,
            COALESCE(SUM(extras), 0) AS total_extras
        FROM ball_by_ball 
        WHERE innings_id = :iid
    ");
    $stmtAgg->execute([':iid' => $inningsId]);
    $agg = $stmtAgg->fetch();

    $totRuns = (int)$agg['total_runs'];
    $totWickets = (int)$agg['total_wickets'];
    $totLegal = (int)$agg['total_legal_balls'];
    $totExtras = (int)$agg['total_extras'];
    $completedOvers = intdiv($totLegal, 6);
    $remBalls = $totLegal % 6;
    $oversDecimal = (float)($completedOvers . '.' . $remBalls);

    // Update match_innings
    $stmtUpdInn = $db->prepare("
        UPDATE match_innings 
        SET total_runs = :tr, wickets = :w, legal_balls = :lb, overs = :ov, extras = :ex, status = 'IN_PROGRESS', updated_at = NOW()
        WHERE id = :iid
    ");
    $stmtUpdInn->execute([
        ':tr'  => $totRuns,
        ':w'   => $totWickets,
        ':lb'  => $totLegal,
        ':ov'  => $oversDecimal,
        ':ex'  => $totExtras,
        ':iid' => $inningsId
    ]);

    // 4. Update matches table with latest score and status
    $calculatedScore = "$totRuns/$totWickets ($oversDecimal)";
    if (empty($scoreA) && $inningsNumber == 1) {
        $scoreA = $calculatedScore;
    }
    if (empty($scoreB) && $inningsNumber == 2) {
        $scoreB = $calculatedScore;
    }

    $updateFields = ["status = :status", "updated_at = NOW()"];
    $matchParams = [':status' => $status, ':mid' => $numericMatchId];
    if ($result !== '') {
        $updateFields[] = "result = :result";
        $matchParams[':result'] = $result;
    }
    if ($scoreA !== '') {
        $updateFields[] = "score_a = :score_a";
        $matchParams[':score_a'] = $scoreA;
    }
    if ($scoreB !== '') {
        $updateFields[] = "score_b = :score_b";
        $matchParams[':score_b'] = $scoreB;
    }

    $sqlMatch = "UPDATE matches SET " . implode(", ", $updateFields) . " WHERE id = :mid";
    $stmtUpdMatch = $db->prepare($sqlMatch);
    $stmtUpdMatch->execute($matchParams);

    $db->commit();

    http_response_code(200);
    echo json_encode([
        "status"  => "success",
        "message" => "Ball recorded atomically with deduplication",
        "data"    => [
            "matchId"     => "M" . ($numericMatchId + 500),
            "scoreA"      => $scoreA,
            "scoreB"      => $scoreB,
            "overs"       => $oversDecimal,
            "totalRuns"   => $totRuns,
            "wickets"     => $totWickets,
            "status"      => $status,
            "updatedAt"   => date("Y-m-d H:i:s")
        ]
    ]);

} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) {
        $db->rollBack();
    }
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to record ball: " . $e->getMessage()
    ]);
}
