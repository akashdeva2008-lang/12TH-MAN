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
                COALESCE(t.tournament_format, 'KNOCKOUT') AS tournamentFormat,
                t.location,
                COALESCE(t.ground_name, 'Main Ground') AS groundName,
                t.start_date AS startDate,
                t.end_date AS endDate,
                COALESCE(t.registration_deadline, '') AS registrationDeadline,
                t.max_teams AS totalTeams,
                t.registered_teams_count AS registeredTeamsCount,
                t.entry_fee AS entryFee,
                t.prize_money AS prizeMoney,
                COALESCE(t.first_prize, '₹50,000') AS firstPrize,
                COALESCE(t.second_prize, '₹25,000') AS secondPrize,
                COALESCE(t.third_prize, '₹10,000') AS thirdPrize,
                COALESCE(t.rules, '') AS rules,
                COALESCE(t.overs, 20) AS overs,
                COALESCE(t.is_draft, 0) AS isDraft,
                t.status,
                t.organizer_name AS organizerName,
                t.organizer_contact AS organizerContact,
                t.description,
                t.logo_url AS logoUrl
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
