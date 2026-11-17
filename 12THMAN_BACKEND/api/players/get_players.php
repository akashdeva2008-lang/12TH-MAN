<?php
// ==========================================================
// 12THMAN BACKEND - GET PLAYERS API
// Endpoint: GET /api/players/get_players.php
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

try {
    $db = getDatabaseConnection();

    $sql = "SELECT 
                p.id AS numeric_id,
                CONCAT('P', p.id + 100) AS id,
                u.full_name AS name,
                p.playing_role AS role,
                CAST(p.rating AS DOUBLE) AS rating,
                p.matches_played AS matchesPlayed,
                p.runs AS runsScored,
                p.wickets AS wicketsTaken,
                p.price_per_match AS pricePerMatch,
                p.city AS location,
                p.availability_status AS availabilityStatus,
                p.bio,
                u.phone,
                u.email
            FROM player_profiles p
            JOIN users u ON p.user_id = u.id
            ORDER BY p.rating DESC";

    $stmt = $db->query($sql);
    $players = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => $players
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to fetch players: " . $e->getMessage()
    ]);
}

