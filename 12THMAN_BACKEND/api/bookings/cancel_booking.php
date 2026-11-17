<?php
// ==========================================================
// 12THMAN BACKEND - CANCEL BOOKING API
// Endpoint: POST /api/bookings/cancel_booking.php
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

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

$bookingIdRaw = trim($input['id'] ?? $input['bookingId'] ?? $input['booking_id'] ?? '');

$numericBookingId = preg_replace('/[^0-9]/', '', $bookingIdRaw);
if ((int)$numericBookingId > 400) {
    $numericBookingId = (int)$numericBookingId - 400;
} else {
    $numericBookingId = (int)$numericBookingId;
}

try {
    $db = getDatabaseConnection();

    $stmt = $db->prepare("UPDATE bookings SET status = 'CANCELLED', updated_at = NOW() WHERE id = ?");
    $stmt->execute([$numericBookingId]);

    http_response_code(200);
    echo json_encode([
        "status"  => "success",
        "message" => "Booking cancelled successfully"
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to cancel booking: " . $e->getMessage()
    ]);
}

