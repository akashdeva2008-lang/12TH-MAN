<?php
// ==========================================================
// 12THMAN BACKEND - REGISTER TEAM API
// Endpoint: POST /api/tournaments/register_team.php
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
$teamName        = trim($input['teamName'] ?? $input['team_name'] ?? '');
$captainName     = trim($input['captainName'] ?? $input['captain_name'] ?? '');
$captainPhone    = trim($input['captainPhone'] ?? $input['captain_phone'] ?? '');
$captainEmail    = trim($input['captainEmail'] ?? $input['captain_email'] ?? '');
$players         = $input['playerNames'] ?? $input['players'] ?? [];

if (empty($tournamentIdRaw) || empty($teamName) || empty($captainName)) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Tournament ID, Team Name, and Captain Name are required."]);
    exit();
}

// Convert "T301" -> 1 if needed
$numericTournamentId = preg_replace('/[^0-9]/', '', $tournamentIdRaw);
if ((int)$numericTournamentId > 300) {
    $numericTournamentId = (int)$numericTournamentId - 300;
} else {
    $numericTournamentId = (int)$numericTournamentId;
}

try {
    $db = getDatabaseConnection();

    // Verify tournament exists
    $tCheck = $db->prepare("SELECT id FROM tournaments WHERE id = ?");
    $tCheck->execute([$numericTournamentId]);
    if (!$tCheck->fetch()) {
        http_response_code(404);
        echo json_encode(["status" => "error", "message" => "Tournament not found."]);
        exit();
    }

    $db->beginTransaction();

    // Insert team
    $stmt = $db->prepare("INSERT INTO teams (tournament_id, team_name, captain_name, captain_phone, captain_email, city, created_at) 
                          VALUES (?, ?, ?, ?, ?, 'Mumbai', NOW())");
    $stmt->execute([$numericTournamentId, $teamName, $captainName, $captainPhone, $captainEmail]);
    $teamId = (int)$db->lastInsertId();

    // Insert players
    if (is_array($players)) {
        $pStmt = $db->prepare("INSERT INTO team_players (team_id, player_name, is_captain, created_at) VALUES (?, ?, ?, NOW())");
        foreach ($players as $idx => $pName) {
            $isCap = ($idx === 0) ? 1 : 0;
            $pStmt->execute([$teamId, trim($pName), $isCap]);
        }
    }

    // Increment registered_teams_count
    $upStmt = $db->prepare("UPDATE tournaments SET registered_teams_count = registered_teams_count + 1 WHERE id = ?");
    $upStmt->execute([$numericTournamentId]);

    $db->commit();

    http_response_code(201);
    echo json_encode([
        "status"  => "success",
        "message" => "Team registered successfully",
        "data"    => [
            "id"           => "TM" . $teamId,
            "tournamentId" => $tournamentIdRaw,
            "teamName"     => $teamName,
            "captainName"  => $captainName
        ]
    ]);

} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) {
        $db->rollBack();
    }
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to register team: " . $e->getMessage()
    ]);
}

