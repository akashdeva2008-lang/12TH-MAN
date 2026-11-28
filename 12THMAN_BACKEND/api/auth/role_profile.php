<?php
// ==========================================================
// 12THMAN BACKEND - GET / UPDATE ROLE PROFILE API
// Endpoint: GET / POST /api/auth/role_profile.php
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$headers = function_exists('getallheaders') ? getallheaders() : [];
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? $_SERVER['HTTP_AUTHORIZATION'] ?? '';
$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    $token = $matches[1];
}

try {
    $db = getDatabaseConnection();
    $userId = (int)($_GET['user_id'] ?? 0);

    if (!empty($token)) {
        $tStmt = $db->prepare("SELECT id, full_name, email, phone, role, city, dob, profile_image FROM users WHERE token = ? LIMIT 1");
        $tStmt->execute([$token]);
        $user = $tStmt->fetch(PDO::FETCH_ASSOC);
    } elseif ($userId > 0) {
        $uStmt = $db->prepare("SELECT id, full_name, email, phone, role, city, dob, profile_image FROM users WHERE id = ? LIMIT 1");
        $uStmt->execute([$userId]);
        $user = $uStmt->fetch(PDO::FETCH_ASSOC);
    } else {
        $user = null;
    }

    if (!$user) {
        http_response_code(401);
        echo json_encode(["status" => "error", "message" => "Unauthorized or user not found."]);
        exit();
    }

    $uid = (int)$user['id'];
    $role = strtoupper($user['role']);

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
        $fullName = trim($input['full_name'] ?? $user['full_name']);
        $city     = trim($input['city'] ?? $user['city']);
        $phone    = trim($input['phone'] ?? $user['phone']);
        $bio      = trim($input['bio'] ?? '');
        $avail    = trim($input['availability_status'] ?? 'Available');

        $db->prepare("UPDATE users SET full_name = ?, city = ?, phone = ?, updated_at = NOW() WHERE id = ?")
           ->execute([$fullName, $city, $phone, $uid]);

        if ($role === 'PLAYER') {
            $playingRole = trim($input['playing_role'] ?? 'Batsman');
            $batting     = trim($input['batting_style'] ?? 'Right Hand');
            $bowling     = trim($input['bowling_style'] ?? 'Right Arm Fast');
            $exp         = trim($input['experience'] ?? '1+ Years');
            $format      = trim($input['preferred_format'] ?? 'T20');

            $db->prepare("UPDATE player_profiles SET playing_role = ?, batting_style = ?, bowling_style = ?, experience = ?, preferred_format = ?, bio = ?, city = ?, availability_status = ?, updated_at = NOW() WHERE user_id = ?")
               ->execute([$playingRole, $batting, $bowling, $exp, $format, $bio, $city, $avail, $uid]);
        } elseif ($role === 'UMPIRE') {
            $cert      = trim($input['certification'] ?? 'State Level');
            $expYears  = (int)($input['experience_years'] ?? 1);
            $matches   = (int)($input['matches_officiated'] ?? 0);
            $formats   = trim($input['preferred_formats'] ?? 'T20, ODI');
            $languages = trim($input['languages_known'] ?? 'English, Hindi');

            $db->prepare("UPDATE umpire_profiles SET certification = ?, experience_years = ?, matches_officiated = ?, preferred_formats = ?, languages_known = ?, bio = ?, city = ?, availability_status = ?, updated_at = NOW() WHERE user_id = ?")
               ->execute([$cert, $expYears, $matches, $formats, $languages, $bio, $city, $avail, $uid]);
        }

        echo json_encode(["status" => "success", "message" => "Profile updated successfully."]);
        exit();
    }

    $profileData = null;
    if ($role === 'PLAYER') {
        $pStmt = $db->prepare("SELECT * FROM player_profiles WHERE user_id = ? LIMIT 1");
        $pStmt->execute([$uid]);
        $profileData = $pStmt->fetch(PDO::FETCH_ASSOC);
    } elseif ($role === 'UMPIRE') {
        $umStmt = $db->prepare("SELECT * FROM umpire_profiles WHERE user_id = ? LIMIT 1");
        $umStmt->execute([$uid]);
        $profileData = $umStmt->fetch(PDO::FETCH_ASSOC);
    }

    echo json_encode([
        "status" => "success",
        "data"   => [
            "user"    => $user,
            "profile" => $profileData
        ]
    ]);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
}
