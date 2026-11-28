<?php
// ==========================================================
// 12THMAN BACKEND - UPDATE MATCH API (Live Scoring / Result)
// Endpoint: POST /api/tournaments/update_match.php
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

$matchIdRaw = trim($input['id'] ?? $input['match_id'] ?? '');
$status     = trim($input['status'] ?? 'LIVE');
$result     = trim($input['result'] ?? '');
$scoreA     = trim($input['scoreA'] ?? $input['score_a'] ?? '');
$scoreB     = trim($input['scoreB'] ?? $input['score_b'] ?? '');
$totalOvers = isset($input['totalOvers']) ? (int)$input['totalOvers'] : (isset($input['overs']) ? (int)$input['overs'] : 0);

$numericMatchId = preg_replace('/[^0-9]/', '', $matchIdRaw);
if ((int)$numericMatchId > 500) {
    $numericMatchId = (int)$numericMatchId - 500;
} else {
    $numericMatchId = (int)$numericMatchId;
}

if ($numericMatchId <= 0) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Invalid match ID"]);
    exit();
}

try {
    $db = getDatabaseConnection();

    $updateFields = ["status = :status", "updated_at = NOW()"];
    $params = [':status' => $status, ':id' => $numericMatchId];
    if ($result !== '') {
        $updateFields[] = "result = :result";
        $params[':result'] = $result;
    }
    if ($scoreA !== '') {
        $updateFields[] = "score_a = :score_a";
        $params[':score_a'] = $scoreA;
    }
    if ($scoreB !== '') {
        $updateFields[] = "score_b = :score_b";
        $params[':score_b'] = $scoreB;
    }
    if ($totalOvers > 0) {
        $updateFields[] = "overs = :overs";
        $params[':overs'] = $totalOvers;
    }

    $sql = "UPDATE matches SET " . implode(", ", $updateFields) . " WHERE id = :id";
    $stmt = $db->prepare($sql);
    $stmt->execute($params);

    http_response_code(200);
    echo json_encode([
        "status"  => "success",
        "message" => "Match updated successfully",
        "data"    => [
            "id"        => "M" . ($numericMatchId + 500),
            "status"    => $status,
            "updatedAt" => date("Y-m-d H:i:s")
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to update match: " . $e->getMessage()
    ]);
}
