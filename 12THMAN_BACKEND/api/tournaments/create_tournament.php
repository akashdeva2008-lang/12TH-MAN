<?php
// ==========================================================
// 12THMAN BACKEND - CREATE TOURNAMENT API
// Endpoint: POST /api/tournaments/create_tournament.php
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

$name             = trim($input['name'] ?? $input['tournament_name'] ?? '');
$format           = trim($input['format'] ?? $input['tournament_type'] ?? 'T20');
$location         = trim($input['location'] ?? '');
$startDate        = trim($input['startDate'] ?? $input['start_date'] ?? '');
$endDate          = trim($input['endDate'] ?? $input['end_date'] ?? '');
$totalTeams       = (int)($input['totalTeams'] ?? $input['max_teams'] ?? 16);
$entryFee         = (int)($input['entryFee'] ?? $input['entry_fee'] ?? 10000);
$prizeMoney       = (int)($input['prizeMoney'] ?? $input['prize_money'] ?? 100000);
$organizerName    = trim($input['organizerName'] ?? $input['organizer_name'] ?? 'Organizer');
$organizerContact = trim($input['organizerContact'] ?? $input['organizer_contact'] ?? '');
$description      = trim($input['description'] ?? '');

if (empty($name) || empty($location)) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Tournament name and location are required."]);
    exit();
}

try {
    $db = getDatabaseConnection();

    // Default organizer_id to 1 (Rahul Dravid) if not specified
    $organizerId = 1;

    $sql = "INSERT INTO tournaments 
            (organizer_id, tournament_name, tournament_type, location, start_date, end_date, 
             entry_fee, prize_money, max_teams, registered_teams_count, status, organizer_name, 
             organizer_contact, description, created_at, updated_at)
            VALUES 
            (:organizer_id, :name, :format, :location, :start_date, :end_date, 
             :entry_fee, :prize_money, :max_teams, 1, 'UPCOMING', :organizer_name, 
             :organizer_contact, :description, NOW(), NOW())";

    $stmt = $db->prepare($sql);
    $stmt->execute([
        ':organizer_id'     => $organizerId,
        ':name'             => $name,
        ':format'           => $format,
        ':location'         => $location,
        ':start_date'       => $startDate,
        ':end_date'         => $endDate,
        ':entry_fee'        => $entryFee,
        ':prize_money'      => $prizeMoney,
        ':max_teams'        => $totalTeams,
        ':organizer_name'   => $organizerName,
        ':organizer_contact'=> $organizerContact,
        ':description'      => $description
    ]);

    $insertedId = (int)$db->lastInsertId();
    $codeId = 'T' . ($insertedId + 300);

    http_response_code(201);
    echo json_encode([
        "status"  => "success",
        "message" => "Tournament created successfully",
        "data"    => [
            "id"                   => $codeId,
            "numeric_id"           => $insertedId,
            "name"                 => $name,
            "format"               => $format,
            "location"             => $location,
            "startDate"            => $startDate,
            "endDate"              => $endDate,
            "totalTeams"           => $totalTeams,
            "registeredTeamsCount" => 1,
            "entryFee"             => $entryFee,
            "prizeMoney"           => $prizeMoney,
            "status"               => "UPCOMING",
            "organizerName"        => $organizerName,
            "organizerContact"     => $organizerContact,
            "description"          => $description
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Failed to create tournament: " . $e->getMessage()
    ]);
}

