<?php
// ==========================================================
// 12THMAN BACKEND - GET UMPIRES API
// Endpoint: GET /api/umpires/get_umpires.php
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

try {
    $db = getDatabaseConnection();

    $sql = "SELECT 
                up.id AS numeric_id,
                CONCAT('U', up.id + 200) AS id,
                u.full_name AS name,
                up.experience_years AS experienceYears,
                up.matches_officiated AS matchesOfficiated,
                CAST(up.rating AS DOUBLE) AS rating,
                up.certification AS certificationLevel,
                up.match_fee AS pricePerMatch,
                up.city AS location,
                up.availability_status AS availabilityStatus,
                up.bio,
                u.phone
            FROM umpire_profiles up
            JOIN users u ON up.user_id = u.id
            ORDER BY up.rating DESC";

    $stmt = $db->query($sql);
    $umpires = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => $umpires
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to fetch umpires: " . $e->getMessage()
    ]);
}

