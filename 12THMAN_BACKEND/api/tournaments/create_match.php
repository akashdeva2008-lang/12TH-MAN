<?php
// ==========================================================
// 12THMAN BACKEND - CREATE / SCHEDULE MATCH API
// Endpoint: POST /api/tournaments/create_match.php
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

$tournamentIdRaw = trim($input['tournamentId'] ?? $input['tournament_id'] ?? 'T301');
$teamA           = trim($input['teamA'] ?? $input['team1_name'] ?? '');
$teamB           = trim($input['teamB'] ?? $input['team2_name'] ?? '');
$matchDate       = trim($input['date'] ?? $input['match_date'] ?? 'Today');
$matchTime       = trim($input['time'] ?? $input['match_time'] ?? '05:00 PM');
$venue           = trim($input['venue'] ?? 'Green Park Stadium');
$format          = trim($input['format'] ?? $input['match_type'] ?? 'T20');
$stage           = trim($input['stage'] ?? 'League Match');
$status          = trim($input['status'] ?? 'UPCOMING');

if (empty($teamA) || empty($teamB)) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Both Team A and Team B names are required."]);
    exit();
}

$numericTournamentId = (int)preg_replace('/[^0-9]/', '', $tournamentIdRaw);
if ($numericTournamentId > 300) {
    $numericTournamentId -= 300;
}
if ($numericTournamentId <= 0) {
    $numericTournamentId = 1;
}

try {
    $db = getDatabaseConnection();

    // Ensure tournament exists (fallback to first tournament if needed)
    $tStmt = $db->prepare("SELECT id FROM tournaments WHERE id = ? LIMIT 1");
    $tStmt->execute([$numericTournamentId]);
    if (!$tStmt->fetch()) {
        $firstT = $db->query("SELECT id FROM tournaments ORDER BY id ASC LIMIT 1")->fetch();
        $numericTournamentId = $firstT ? (int)$firstT['id'] : 1;
    }

    // Resolve or create team1_id and team2_id
    $resolveTeamId = function($teamName) use ($db, $numericTournamentId) {
        $stmt = $db->prepare("SELECT id FROM teams WHERE LOWER(TRIM(team_name)) = LOWER(TRIM(?)) LIMIT 1");
        $stmt->execute([$teamName]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($row) {
            return (int)$row['id'];
        }
        $short = strtoupper(substr(preg_replace('/\s+/', '', $teamName), 0, 3));
        $ins = $db->prepare("INSERT INTO teams (tournament_id, team_name, short_name, captain_name, captain_phone, city, created_at) VALUES (?, ?, ?, ?, '', 'Mumbai', NOW())");
        $ins->execute([$numericTournamentId, $teamName, $short, $teamName . ' Captain']);
        return (int)$db->lastInsertId();
    };

    $team1Id = $resolveTeamId($teamA);
    $team2Id = $resolveTeamId($teamB);

    // Next match number in tournament
    $numStmt = $db->prepare("SELECT COALESCE(MAX(match_number), 0) + 1 AS next_num FROM matches WHERE tournament_id = ?");
    $numStmt->execute([$numericTournamentId]);
    $nextNum = (int)($numStmt->fetch(PDO::FETCH_ASSOC)['next_num'] ?? 1);

    $insMatch = $db->prepare("INSERT INTO matches 
        (tournament_id, match_number, team1_id, team2_id, team1_name, team2_name, match_date, match_time, venue, match_type, stage, status, result, score_a, score_b, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Scheduled', '0/0 (0.0)', '0/0 (0.0)', NOW())");
    $insMatch->execute([
        $numericTournamentId,
        $nextNum,
        $team1Id,
        $team2Id,
        $teamA,
        $teamB,
        $matchDate,
        $matchTime,
        $venue,
        $format,
        $stage,
        $status
    ]);

    $newId = (int)$db->lastInsertId();
    $formattedId = 'M' . ($newId + 500);

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "message" => "Match scheduled successfully",
        "data" => [
            "numeric_id" => $newId,
            "id" => $formattedId,
            "tournamentId" => 'T' . ($numericTournamentId + 300),
            "tournamentNumericId" => $numericTournamentId,
            "matchNumber" => $nextNum,
            "teamA" => $teamA,
            "teamB" => $teamB,
            "date" => $matchDate,
            "time" => $matchTime,
            "venue" => $venue,
            "format" => $format,
            "stage" => $stage,
            "status" => $status,
            "result" => "Scheduled",
            "scoreA" => "0/0 (0.0)",
            "scoreB" => "0/0 (0.0)"
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Failed to schedule match: " . $e->getMessage()
    ]);
}
