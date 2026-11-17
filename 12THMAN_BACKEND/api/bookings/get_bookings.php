<?php
// ==========================================================
// 12THMAN BACKEND - GET BOOKINGS API
// Endpoint: GET /api/bookings/get_bookings.php
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

try {
    $db = getDatabaseConnection();

    $sql = "SELECT 
                b.id AS numeric_id,
                CONCAT('B', b.id + 400) AS id,
                b.item_type AS bookingType,
                b.item_id AS itemId,
                b.item_name AS itemName,
                u.email AS userEmail,
                b.booking_date AS date,
                b.start_time AS timeSlot,
                b.venue,
                b.match_type AS matchType,
                b.status,
                b.amount,
                b.notes
            FROM bookings b
            JOIN users u ON b.user_id = u.id
            ORDER BY b.id DESC";

    $stmt = $db->query($sql);
    $bookings = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => $bookings
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to fetch bookings: " . $e->getMessage()
    ]);
}

