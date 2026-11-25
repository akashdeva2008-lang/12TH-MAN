<?php
// ==========================================================
// 12THMAN BACKEND - GET BOOKINGS API
// Endpoint: GET /api/bookings/get_bookings.php?user_email=...
// ==========================================================

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

require_once __DIR__ . '/../../config/database.php';

$userEmail = trim($_GET['user_email'] ?? $_GET['email'] ?? '');
$bookingType = trim($_GET['booking_type'] ?? $_GET['type'] ?? '');

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
                CASE 
                    WHEN b.end_time IS NOT NULL AND b.end_time != '' AND b.start_time NOT LIKE '%-%' 
                    THEN CONCAT(b.start_time, ' - ', b.end_time) 
                    ELSE b.start_time 
                END AS timeSlot,
                b.venue,
                b.match_type AS matchType,
                b.status,
                b.amount,
                b.notes,
                b.created_at AS createdAt,
                b.updated_at AS updatedAt
            FROM bookings b
            JOIN users u ON b.user_id = u.id";

    $where = [];
    $params = [];

    if (!empty($userEmail)) {
        $where[] = "u.email = :email";
        $params[':email'] = $userEmail;
    }

    if (!empty($bookingType)) {
        $where[] = "b.item_type = :btype";
        $params[':btype'] = strtoupper($bookingType);
    }

    if (!empty($where)) {
        $sql .= " WHERE " . implode(" AND ", $where);
    }

    $sql .= " ORDER BY b.id DESC";

    $stmt = $db->prepare($sql);
    $stmt->execute($params);
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
