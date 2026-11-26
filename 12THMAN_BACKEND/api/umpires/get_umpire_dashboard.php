<?php
// ==========================================================
// 12THMAN BACKEND - GET UMPIRE DASHBOARD API
// Endpoint: GET /api/umpires/get_umpire_dashboard.php
// ==========================================================

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

require_once __DIR__ . '/../../config/database.php';

try {
    $db = getDatabaseConnection();

    // 1. Fetch all umpires
    $sqlUmpires = "SELECT 
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

    $stmtUmpires = $db->query($sqlUmpires);
    $umpires = $stmtUmpires->fetchAll();

    // 2. Fetch umpire bookings
    $sqlBookings = "SELECT 
                        id,
                        item_id AS itemId,
                        item_name AS itemName,
                        booking_date AS date,
                        start_time AS timeSlot,
                        venue,
                        match_type AS matchType,
                        status,
                        amount,
                        notes,
                        'PAID' AS paymentStatus,
                        'UMPIRE' AS bookingType
                    FROM bookings 
                    WHERE item_type = 'UMPIRE'
                    ORDER BY id DESC";
    $stmtBookings = $db->query($sqlBookings);
    $bookings = $stmtBookings->fetchAll();

    // 3. Aggregate Stats
    $totalUmpires = count($umpires);
    $availableCount = 0;
    $certifiedCount = 0;
    foreach ($umpires as $u) {
        if (strcasecmp($u['availabilityStatus'] ?? '', 'Available') === 0) {
            $availableCount++;
        }
        if (!empty($u['certificationLevel'])) {
            $certifiedCount++;
        }
    }

    $activeBookingsCount = 0;
    foreach ($bookings as $b) {
        if (in_array(strtoupper($b['status'] ?? ''), ['CONFIRMED', 'PENDING'])) {
            $activeBookingsCount++;
        }
    }

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => [
            "stats" => [
                "totalUmpires"        => $totalUmpires,
                "availableUmpires"    => $availableCount,
                "activeBookings"      => $activeBookingsCount,
                "certifiedOfficials"  => $certifiedCount,
                "averageRating"       => 4.8
            ],
            "umpires"  => $umpires,
            "bookings" => $bookings
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to load umpire dashboard: " . $e->getMessage()
    ]);
}
