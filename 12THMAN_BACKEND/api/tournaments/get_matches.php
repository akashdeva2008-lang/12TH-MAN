<?php
// ==========================================================
// 12THMAN BACKEND - GET MATCHES API
// Endpoint: GET /api/tournaments/get_matches.php?tournament_id=T301
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

$tournamentIdRaw = trim($_GET['tournament_id'] ?? $_GET['tournamentId'] ?? '');

try {
    $db = getDatabaseConnection();

    $sql = "SELECT 
                m.id AS numeric_id,
                CONCAT('M', m.id + 500) AS id,
                CONCAT('T', m.tournament_id + 300) AS tournamentId,
                m.tournament_id AS tournamentNumericId,
                m.match_number AS matchNumber,
                m.team1_name AS teamA,
                m.team2_name AS teamB,
                m.match_date AS date,
                m.match_time AS time,
                m.venue,
                m.stage,
                m.status,
                m.result,
                m.score_a AS scoreA,
                m.score_b AS scoreB
            FROM matches m";

    if (!empty($tournamentIdRaw)) {
        $numericTournamentId = preg_replace('/[^0-9]/', '', $tournamentIdRaw);
        if ((int)$numericTournamentId > 300) {
            $numericTournamentId = (int)$numericTournamentId - 300;
        } else {
            $numericTournamentId = (int)$numericTournamentId;
        }

        $sql .= " WHERE m.tournament_id = :tid";
        $stmt = $db->prepare($sql);
        $stmt->execute([':tid' => $numericTournamentId]);
    } else {
        $stmt = $db->query($sql);
    }

    $matches = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => $matches
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to fetch matches: " . $e->getMessage()
    ]);
}

