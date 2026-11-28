<?php
// ==========================================================
// 12THMAN BACKEND - UMPIRE REGISTRATION API
// Endpoint: POST /api/umpires/register_umpire.php
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

$fullName          = trim($input['full_name'] ?? $input['name'] ?? '');
$email             = trim($input['email'] ?? '');
$phone             = trim($input['phone'] ?? '');
$password          = trim($input['password'] ?? '');
$dob               = trim($input['dob'] ?? '');
$city              = trim($input['city'] ?? 'Mumbai, MH');
$profileImage      = trim($input['profile_image'] ?? '');
$experienceYears   = max(0, (int)($input['experience_years'] ?? 1));
$certRaw           = trim($input['certification'] ?? '');
$matchesOfficiated = max(0, (int)($input['matches_officiated'] ?? 0));
$preferredFormats  = trim($input['preferred_formats'] ?? 'T20, ODI');
$availability      = trim($input['availability_status'] ?? 'Available');
$languagesKnown    = trim($input['languages_known'] ?? 'English, Hindi');
$bio               = trim($input['bio'] ?? '');

// Match DB ENUM('ICC Accredited','National Certified','State Level','Club Certified')
$certMap = [
    'icc accredited'     => 'ICC Accredited',
    'national certified' => 'National Certified',
    'national level'     => 'National Certified',
    'state level'        => 'State Level',
    'club certified'     => 'Club Certified'
];
$certification = $certMap[strtolower($certRaw)] ?? 'State Level';
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
                           VALUES (?, ?, ?, ?, 'UMPIRE', ?, ?, ?, ?, ?, NOW(), NOW())");
    $uStmt->execute([
        $fullName, $email, $phone, $hashedPassword,
        $profileImage ?: null, $city, $dob ?: null, $token, $tokenExpiry
    ]);
    $userId = (int)$db->lastInsertId();

    $umStmt = $db->prepare("INSERT INTO umpire_profiles
                            (user_id, certification, experience_years, matches_officiated, preferred_formats, languages_known, availability_status, bio, city, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())");
    $umStmt->execute([
        $userId, $certification, $experienceYears, $matchesOfficiated,
        $preferredFormats, $languagesKnown, $availabilityStatus, $bio, $city
    ]);

    $db->commit();

    http_response_code(201);
    echo json_encode([
        "status"  => "success",
        "message" => "Umpire registered successfully!",
        "data"    => [
            "id"            => $userId,
            "full_name"     => $fullName,
            "email"         => $email,
            "phone"         => $phone,
            "role"          => "UMPIRE",
            "profile_image" => $profileImage,
            "city"          => $city,
            "token"         => $token,
            "token_expiry"  => $tokenExpiry
        ]
    ]);
} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) $db->rollBack();
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Umpire registration failed: " . $e->getMessage()]);
}
