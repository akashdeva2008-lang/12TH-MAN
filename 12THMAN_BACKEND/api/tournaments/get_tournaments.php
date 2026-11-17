<?php
// ==========================================================
// 12THMAN BACKEND - GET TOURNAMENTS API
// Endpoint: GET /api/tournaments/get_tournaments.php
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

try {
    $db = getDatabaseConnection();

    $sql = "SELECT 
                t.id AS numeric_id,
                CONCAT('T', t.id + 300) AS id,
                t.tournament_name AS name,
                t.tournament_type AS format,
                t.location,
                t.start_date AS startDate,
                t.end_date AS endDate,
                t.max_teams AS totalTeams,
                t.registered_teams_count AS registeredTeamsCount,
                t.entry_fee AS entryFee,
                t.prize_money AS prizeMoney,
                t.status,
                t.organizer_name AS organizerName,
                t.organizer_contact AS organizerContact,
                t.description
            FROM tournaments t
            ORDER BY t.id DESC";

    $stmt = $db->query($sql);
    $tournaments = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => $tournaments
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to fetch tournaments: " . $e->getMessage()
    ]);
}

