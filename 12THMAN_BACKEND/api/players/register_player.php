<?php
// ==========================================================
// 12THMAN BACKEND - PLAYER REGISTRATION API
// Endpoint: POST /api/players/register_player.php
// ==========================================================

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

$fullName        = trim($input['full_name'] ?? $input['name'] ?? '');
$email           = trim($input['email'] ?? '');
$phone           = trim($input['phone'] ?? '');
$password        = trim($input['password'] ?? '');
$dob             = trim($input['dob'] ?? '');
$city            = trim($input['city'] ?? 'Mumbai, MH');
$profileImage    = trim($input['profile_image'] ?? '');
$playingRoleRaw  = trim($input['playing_role'] ?? 'Batsman');
$battingStyle    = trim($input['batting_style'] ?? 'Right Hand');
$bowlingStyle    = trim($input['bowling_style'] ?? 'Right Arm Fast');
$experience      = trim($input['experience'] ?? '1+ Years');
$preferredFormat = trim($input['preferred_format'] ?? 'T20');
$availability    = trim($input['availability_status'] ?? 'Available');
$bio             = trim($input['bio'] ?? '');

// Map UI label "Wicketkeeper" -> DB ENUM "Wicket Keeper"
$roleMap = [
    'batsman'       => 'Batsman',
    'bowler'        => 'Bowler',
    'all-rounder'   => 'All-Rounder',
    'all rounder'   => 'All-Rounder',
    'wicketkeeper'  => 'Wicket Keeper',
    'wicket keeper' => 'Wicket Keeper'
];
$playingRole = $roleMap[strtolower($playingRoleRaw)] ?? 'Batsman';
$availMap = ['available' => 'Available', 'booked' => 'Booked', 'unavailable' => 'Unavailable'];
$availabilityStatus = $availMap[strtolower($availability)] ?? 'Available';

$errors = [];
if (empty($fullName)) $errors[] = "Full name is required";
if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) $errors[] = "Valid email address is required";
if (empty($phone)) $errors[] = "Phone number is required";
if (strlen($password) < 6) $errors[] = "Password must be at least 6 characters";

if (!empty($errors)) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => implode(". ", $errors)]);
    exit();
}

try {
    $db = getDatabaseConnection();

    $check = $db->prepare("SELECT id FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(?)) LIMIT 1");
    $check->execute([$email]);
    if ($check->fetch()) {
        http_response_code(409);
        echo json_encode(["status" => "error", "message" => "An account with this email is already registered."]);
        exit();
    }

    $hashedPassword = password_hash($password, PASSWORD_BCRYPT);
    $token = bin2hex(random_bytes(32));
    $tokenExpiry = date('Y-m-d H:i:s', strtotime('+30 days'));

    $db->beginTransaction();

    $uStmt = $db->prepare("INSERT INTO users (full_name, email, phone, password, role, profile_image, city, dob, token, token_expiry, created_at, updated_at)
                           VALUES (?, ?, ?, ?, 'PLAYER', ?, ?, ?, ?, ?, NOW(), NOW())");
    $uStmt->execute([
        $fullName, $email, $phone, $hashedPassword,
        $profileImage ?: null, $city, $dob ?: null, $token, $tokenExpiry
    ]);
    $userId = (int)$db->lastInsertId();

    $pStmt = $db->prepare("INSERT INTO player_profiles
                           (user_id, playing_role, batting_style, bowling_style, experience, preferred_format, bio, city, availability_status, created_at, updated_at)
                           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())");
    $pStmt->execute([
        $userId, $playingRole, $battingStyle, $bowlingStyle,
        $experience, $preferredFormat, $bio, $city, $availabilityStatus
    ]);

    $db->commit();

    http_response_code(201);
    echo json_encode([
        "status"  => "success",
        "message" => "Player registered successfully!",
        "data"    => [
            "id"            => $userId,
            "full_name"     => $fullName,
            "email"         => $email,
            "phone"         => $phone,
            "role"          => "PLAYER",
            "profile_image" => $profileImage,
            "city"          => $city,
            "token"         => $token,
            "token_expiry"  => $tokenExpiry
        ]
    ]);
} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) $db->rollBack();
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Player registration failed: " . $e->getMessage()]);
}
