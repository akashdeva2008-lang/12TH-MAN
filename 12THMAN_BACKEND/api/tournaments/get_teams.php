<?php
// ==========================================================
// 12THMAN BACKEND - GET TEAMS API
// Endpoint: GET /api/tournaments/get_teams.php?tournament_id=T301
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

$tournamentIdRaw = trim($_GET['tournament_id'] ?? $_GET['tournamentId'] ?? '');

try {
    $db = getDatabaseConnection();

    if (!empty($tournamentIdRaw)) {
        $numericId = preg_replace('/[^0-9]/', '', $tournamentIdRaw);
        if ((int)$numericId > 300) {
            $numericId = (int)$numericId - 300;
        } else {
            $numericId = (int)$numericId;
        }

        $sql = "SELECT 
                    tm.id AS numeric_id,
                    CONCAT('TM', tm.id) AS id,
                    COALESCE(CONCAT('T', ? + 300), '') AS tournamentId,
                    tm.team_name AS teamName,
                    COALESCE(tm.short_name, UPPER(SUBSTRING(tm.team_name, 1, 3))) AS shortName,
                    tm.captain_name AS captainName,
                    tm.captain_phone AS captainPhone,
                    tm.captain_email AS captainEmail,
                    COALESCE(tm.vice_captain, '') AS viceCaptain,
                    COALESCE(tm.manager_name, '') AS managerName,
                    COALESCE(tm.description, '') AS description,
                    COALESCE(tm.city, 'Mumbai') AS city,
                    COALESCE(tm.logo_url, 'RED') AS logoUrl,
                    COALESCE(tt.status, tm.approval_status, 'APPROVED') AS approvalStatus,
                    COALESCE(tt.group_name, 'Group A') AS groupName,
                    DATE_FORMAT(tm.created_at, '%d %b %Y') AS registrationDate
                FROM teams tm
                LEFT JOIN tournament_teams tt ON tt.team_id = tm.id AND tt.tournament_id = ?
                WHERE tm.tournament_id = ? OR tt.tournament_id = ?
                GROUP BY tm.id
                ORDER BY tm.id DESC";
        $stmt = $db->prepare($sql);
        $stmt->execute([$numericId, $numericId, $numericId, $numericId]);
    } else {
        $sql = "SELECT 
                    tm.id AS numeric_id,
                    CONCAT('TM', tm.id) AS id,
                    CASE WHEN tm.tournament_id IS NOT NULL AND tm.tournament_id > 0 
                         THEN CONCAT('T', tm.tournament_id + 300) 
                         ELSE '' END AS tournamentId,
                    tm.team_name AS teamName,
                    COALESCE(tm.short_name, UPPER(SUBSTRING(tm.team_name, 1, 3))) AS shortName,
                    tm.captain_name AS captainName,
                    tm.captain_phone AS captainPhone,
                    tm.captain_email AS captainEmail,
                    COALESCE(tm.vice_captain, '') AS viceCaptain,
                    COALESCE(tm.manager_name, '') AS managerName,
                    COALESCE(tm.description, '') AS description,
                    COALESCE(tm.city, 'Mumbai') AS city,
                    COALESCE(tm.logo_url, 'RED') AS logoUrl,
                    COALESCE(tm.approval_status, 'APPROVED') AS approvalStatus,
                    'Group A' AS groupName,
                    DATE_FORMAT(tm.created_at, '%d %b %Y') AS registrationDate
                FROM teams tm
                ORDER BY tm.id DESC";
        $stmt = $db->query($sql);
    }

    $teams = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Fetch players with detailed role, jersey number, and contact
    foreach ($teams as &$t) {
        $pStmt = $db->prepare("SELECT 
                                    id,
                                    player_name AS playerName,
                                    COALESCE(player_role, 'Batsman') AS role,
                                    COALESCE(jersey_number, '') AS jerseyNumber,
                                    COALESCE(player_contact, '') AS contact,
                                    is_captain AS isCaptain,
                                    COALESCE(is_vice_captain, 0) AS isViceCaptain
                                FROM team_players 
                                WHERE team_id = ? 
                                ORDER BY is_captain DESC, is_vice_captain DESC, id ASC");
        $pStmt->execute([$t['numeric_id']]);
        $playersList = $pStmt->fetchAll(PDO::FETCH_ASSOC);

        $playerNames = [];
        $cleanPlayers = [];
        foreach ($playersList as $p) {
            $playerNames[] = $p['playerName'];
            $cleanPlayers[] = [
                'id'            => (string)$p['id'],
                'playerName'    => (string)$p['playerName'],
                'role'          => (string)$p['role'],
                'jerseyNumber'  => (string)$p['jerseyNumber'],
                'contact'       => (string)$p['contact'],
                'isCaptain'     => ((int)$p['isCaptain'] === 1),
                'isViceCaptain' => ((int)$p['isViceCaptain'] === 1)
            ];
        }

        $t['playerNames'] = $playerNames;
        $t['players'] = $cleanPlayers;
        $t['playerCount'] = count($playerNames);
    }

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => $teams
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Database error: " . $e->getMessage()
    ]);
}
