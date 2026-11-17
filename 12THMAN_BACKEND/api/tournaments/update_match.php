<?php
// ==========================================================
// 12THMAN BACKEND - UPDATE MATCH API (Live Scoring / Result)
// Endpoint: POST /api/tournaments/update_match.php
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

$matchIdRaw = trim($input['id'] ?? $input['match_id'] ?? '');
$status     = trim($input['status'] ?? 'LIVE');
$result     = trim($input['result'] ?? '');
$scoreA     = trim($input['scoreA'] ?? $input['score_a'] ?? '');
$scoreB     = trim($input['scoreB'] ?? $input['score_b'] ?? '');

$numericMatchId = preg_replace('/[^0-9]/', '', $matchIdRaw);
if ((int)$numericMatchId > 500) {
    $numericMatchId = (int)$numericMatchId - 500;
} else {
    $numericMatchId = (int)$numericMatchId;
}

try {
    $db = getDatabaseConnection();

    $sql = "UPDATE matches 
            SET status = :status, 
                result = IF(:result != '', :result, result), 
                score_a = IF(:score_a != '', :score_a, score_a), 
                score_b = IF(:score_b != '', :score_b, score_b),
                updated_at = NOW()
            WHERE id = :id";

    $stmt = $db->prepare($sql);
    $stmt->execute([
        ':status'  => $status,
        ':result'  => $result,
        ':score_a' => $scoreA,
        ':score_b' => $scoreB,
        ':id'      => $numericMatchId
    ]);

    http_response_code(200);
    echo json_encode([
        "status"  => "success",
        "message" => "Match updated successfully"
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to update match: " . $e->getMessage()
    ]);
}

