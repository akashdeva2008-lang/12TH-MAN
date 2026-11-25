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

$name                 = trim($input['name'] ?? $input['tournament_name'] ?? '');
$format               = trim($input['format'] ?? $input['tournament_type'] ?? 'T20');
$tournamentFormat     = trim($input['tournamentFormat'] ?? $input['tournament_format'] ?? 'KNOCKOUT');
$location             = trim($input['location'] ?? 'Mumbai');
$groundName           = trim($input['groundName'] ?? $input['ground_name'] ?? 'Cricket Ground');
$startDate            = trim($input['startDate'] ?? $input['start_date'] ?? date('d M Y'));
$endDate              = trim($input['endDate'] ?? $input['end_date'] ?? date('d M Y', strtotime('+7 days')));
$registrationDeadline = trim($input['registrationDeadline'] ?? $input['registration_deadline'] ?? '');
$totalTeams           = (int)($input['totalTeams'] ?? $input['max_teams'] ?? 16);
$entryFee             = (int)($input['entryFee'] ?? $input['entry_fee'] ?? 10000);
$prizeMoney           = (int)($input['prizeMoney'] ?? $input['prize_money'] ?? 100000);
$firstPrize           = trim($input['firstPrize'] ?? $input['first_prize'] ?? '₹50,000');
$secondPrize          = trim($input['secondPrize'] ?? $input['second_prize'] ?? '₹25,000');
$thirdPrize           = trim($input['thirdPrize'] ?? $input['third_prize'] ?? '₹10,000');
$rules                = trim($input['rules'] ?? 'Standard ICC / MCA T20 cricket tournament rules apply.');
$overs                = (int)($input['overs'] ?? 20);
$isDraft              = (int)($input['isDraft'] ?? $input['is_draft'] ?? 0);
$organizerName        = trim($input['organizerName'] ?? $input['organizer_name'] ?? 'Tournament Organizer');
$organizerContact     = trim($input['organizerContact'] ?? $input['organizer_contact'] ?? '');
$description          = trim($input['description'] ?? '');

if (empty($name) || empty($location)) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Tournament name and location are required."]);
    exit();
}

try {
    $db = getDatabaseConnection();
    $organizerId = 1;

    $sql = "INSERT INTO tournaments 
            (organizer_id, tournament_name, tournament_type, tournament_format, location, ground_name, start_date, end_date, 
             registration_deadline, entry_fee, prize_money, first_prize, second_prize, third_prize, rules, overs, is_draft,
             max_teams, registered_teams_count, status, organizer_name, organizer_contact, description, created_at, updated_at)
            VALUES 
            (:organizer_id, :name, :format, :tournament_format, :location, :ground_name, :start_date, :end_date, 
             :deadline, :entry_fee, :prize_money, :first_prize, :second_prize, :third_prize, :rules, :overs, :is_draft,
             :max_teams, 0, :status, :organizer_name, :organizer_contact, :description, NOW(), NOW())";

    $status = ($isDraft === 1) ? 'UPCOMING' : 'UPCOMING';

    $stmt = $db->prepare($sql);
    $stmt->execute([
        ':organizer_id'       => $organizerId,
        ':name'               => $name,
        ':format'             => $format,
        ':tournament_format'  => $tournamentFormat,
        ':location'           => $location,
        ':ground_name'        => $groundName,
        ':start_date'         => $startDate,
        ':end_date'           => $endDate,
        ':deadline'           => $registrationDeadline,
        ':entry_fee'          => $entryFee,
        ':prize_money'        => $prizeMoney,
        ':first_prize'        => $firstPrize,
        ':second_prize'       => $secondPrize,
        ':third_prize'        => $thirdPrize,
        ':rules'              => $rules,
        ':overs'              => $overs,
        ':is_draft'           => $isDraft,
        ':max_teams'          => $totalTeams,
        ':status'             => $status,
        ':organizer_name'     => $organizerName,
        ':organizer_contact'  => $organizerContact,
        ':description'        => $description
    ]);

    $insertedId = (int)$db->lastInsertId();
    $codeId = 'T' . ($insertedId + 300);

    http_response_code(201);
    echo json_encode([
        "status"  => "success",
        "message" => ($isDraft === 1) ? "Tournament saved as draft" : "Tournament published successfully",
        "data"    => [
            "id"                   => $codeId,
            "numeric_id"           => $insertedId,
            "name"                 => $name,
            "format"               => $format,
            "tournamentFormat"     => $tournamentFormat,
            "location"             => $location,
            "groundName"           => $groundName,
            "startDate"            => $startDate,
            "endDate"              => $endDate,
            "totalTeams"           => $totalTeams,
            "registeredTeamsCount" => 0,
            "entryFee"             => $entryFee,
            "prizeMoney"           => $prizeMoney,
            "firstPrize"           => $firstPrize,
            "secondPrize"          => $secondPrize,
            "thirdPrize"           => $thirdPrize,
            "rules"                => $rules,
            "overs"                => $overs,
            "isDraft"              => $isDraft,
            "status"               => $status,
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
