<?php
// ==========================================================
// 12THMAN BACKEND - CREATE BOOKING API (With Idempotency)
// Endpoint: POST /api/bookings/create_booking.php
// ==========================================================

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

require_once __DIR__ . '/../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

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

$bookingType = strtoupper(trim($input['bookingType'] ?? $input['booking_type'] ?? 'PLAYER'));
$itemIdRaw   = trim($input['itemId'] ?? $input['item_id'] ?? '1');
$itemName    = trim($input['itemName'] ?? $input['item_name'] ?? '');
$userEmail   = trim($input['userEmail'] ?? $input['user_email'] ?? 'akashdeva2008@gmail.com');
$date        = trim($input['date'] ?? $input['booking_date'] ?? date('d M Y'));
$timeSlot    = trim($input['timeSlot'] ?? $input['time_slot'] ?? '04:00 PM - 08:00 PM');
$venue       = trim($input['venue'] ?? 'Cricket Ground');
$matchType   = trim($input['matchType'] ?? $input['match_type'] ?? 'T20');
$amount      = (int)($input['amount'] ?? 1800);
$notes       = trim($input['notes'] ?? '');

$numericItemId = preg_replace('/[^0-9]/', '', $itemIdRaw);
if ((int)$numericItemId > 200) {
    $numericItemId = (int)$numericItemId - 200;
} elseif ((int)$numericItemId > 100) {
    $numericItemId = (int)$numericItemId - 100;
} else {
    $numericItemId = (int)$numericItemId;
}

$startTime = $timeSlot;
$endTime = null;
if (strpos($timeSlot, '-') !== false) {
    $parts = explode('-', $timeSlot);
    $startTime = trim($parts[0]);
    $endTime = trim($parts[1]);
}

try {
    $db = getDatabaseConnection();

    // Find user ID from email or fallback to 14 (akash) or 1
    $userId = 14;
    if (!empty($userEmail)) {
        $uStmt = $db->prepare("SELECT id FROM users WHERE email = ? LIMIT 1");
        $uStmt->execute([$userEmail]);
        $uRow = $uStmt->fetch();
        if ($uRow) {
            $userId = (int)$uRow['id'];
        }
    }

    // Duplicate submission prevention: check if identical booking exists within last 60 seconds
    $dupCheck = $db->prepare("
        SELECT id FROM bookings 
        WHERE user_id = :uid 
          AND item_type = :itype 
          AND item_id = :iid 
          AND booking_date = :bdate 
          AND venue = :venue 
          AND created_at >= (NOW() - INTERVAL 60 SECOND)
        LIMIT 1
    ");
    $dupCheck->execute([
        ':uid'   => $userId,
        ':itype' => $bookingType,
        ':iid'   => $numericItemId,
        ':bdate' => $date,
        ':venue' => $venue
    ]);
    $existing = $dupCheck->fetch();
    if ($existing) {
        $existingCodeId = 'B' . ($existing['id'] + 400);
        http_response_code(200);
        echo json_encode([
            "status"  => "success",
            "message" => "Booking already confirmed (Duplicate submission prevented)",
            "data"    => [
                "id"          => $existingCodeId,
                "numeric_id"  => (int)$existing['id'],
                "bookingType" => $bookingType,
                "itemId"      => $itemIdRaw,
                "itemName"    => $itemName,
                "userEmail"   => $userEmail,
                "date"        => $date,
                "timeSlot"    => $timeSlot,
                "venue"       => $venue,
                "matchType"   => $matchType,
                "status"      => "CONFIRMED",
                "amount"      => $amount,
                "notes"       => $notes
            ]
        ]);
        exit();
    }

    $sql = "INSERT INTO bookings 
            (user_id, item_type, item_id, item_name, booking_date, start_time, end_time, venue, match_type, amount, notes, status, created_at, updated_at)
            VALUES 
            (:user_id, :item_type, :item_id, :item_name, :date, :start_time, :end_time, :venue, :match_type, :amount, :notes, 'CONFIRMED', NOW(), NOW())";

    $stmt = $db->prepare($sql);
    $stmt->execute([
        ':user_id'    => $userId,
        ':item_type'  => $bookingType,
        ':item_id'    => $numericItemId,
        ':item_name'  => $itemName,
        ':date'       => $date,
        ':start_time' => $startTime,
        ':end_time'   => $endTime,
        ':venue'      => $venue,
        ':match_type' => $matchType,
        ':amount'     => $amount,
        ':notes'      => $notes
    ]);

    $insertedId = (int)$db->lastInsertId();
    $codeId = 'B' . ($insertedId + 400);

    http_response_code(201);
    echo json_encode([
        "status"  => "success",
        "message" => "Booking created successfully",
        "data"    => [
            "id"          => $codeId,
            "numeric_id"  => $insertedId,
            "bookingType" => $bookingType,
            "itemId"      => $itemIdRaw,
            "itemName"    => $itemName,
            "userEmail"   => $userEmail,
            "date"        => $date,
            "timeSlot"    => $timeSlot,
            "venue"       => $venue,
            "matchType"   => $matchType,
            "status"      => "CONFIRMED",
            "amount"      => $amount,
            "notes"       => $notes
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to create booking: " . $e->getMessage()
    ]);
}
