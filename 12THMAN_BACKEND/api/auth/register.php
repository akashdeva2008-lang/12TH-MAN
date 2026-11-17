<?php
// ==========================================================
// 12THMAN BACKEND - USER REGISTRATION API
// Endpoint: POST /api/auth/register.php
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        "status" => "error",
        "message" => "Method not allowed. Use POST."
    ]);
    exit();
}

// Parse request payload (JSON or form-urlencoded)
$rawInput = file_get_contents('php://input');
$input = json_decode($rawInput, true);
if (!is_array($input)) {
    $input = $_POST;
}

// Extract & sanitize parameters
$fullName = trim($input['full_name'] ?? $input['name'] ?? '');
$email    = trim($input['email'] ?? '');
$phone    = trim($input['phone'] ?? '');
$password = trim($input['password'] ?? '');
$rawRole  = strtoupper(trim($input['role'] ?? 'PLAYER'));

// Validate role enum
$validRoles = ['PLAYER', 'UMPIRE', 'ORGANIZER', 'SCORER'];
$role = in_array($rawRole, $validRoles) ? $rawRole : 'PLAYER';

// Validation: Required fields
$errors = [];
if (empty($fullName)) {
    $errors[] = "Full name is required";
}
if (empty($email)) {
    $errors[] = "Email address is required";
} elseif (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    $errors[] = "Valid email address is required";
}
if (empty($phone)) {
    $errors[] = "Phone number is required";
}
if (empty($password)) {
    $errors[] = "Password is required";
} elseif (strlen($password) < 6) {
    $errors[] = "Password must be at least 6 characters long";
}

if (!empty($errors)) {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => implode(". ", $errors),
        "errors" => $errors
    ]);
    exit();
}

try {
    $db = getDatabaseConnection();

    // Check duplicate email
    $checkStmt = $db->prepare("SELECT id FROM users WHERE email = ? LIMIT 1");
    $checkStmt->execute([$email]);
    if ($checkStmt->fetch()) {
        http_response_code(409); // Conflict
        echo json_encode([
            "status" => "error",
            "message" => "Email already registered"
        ]);
        exit();
    }

    // Secure password hash
    $hashedPassword = password_hash($password, PASSWORD_BCRYPT);

    // Generate secure auth token
    $token = bin2hex(random_bytes(32));
    $tokenExpiry = date('Y-m-d H:i:s', strtotime('+30 days'));

    // Insert user into users table
    $insertSql = "INSERT INTO users (full_name, email, phone, password, role, token, token_expiry, created_at, updated_at) 
                  VALUES (:full_name, :email, :phone, :password, :role, :token, :token_expiry, NOW(), NOW())";
    $insertStmt = $db->prepare($insertSql);
    $insertStmt->execute([
        ':full_name'    => $fullName,
        ':email'        => $email,
        ':phone'        => $phone,
        ':password'     => $hashedPassword,
        ':role'         => $role,
        ':token'        => $token,
        ':token_expiry' => $tokenExpiry
    ]);

    $userId = (int)$db->lastInsertId();

    // Optionally create default role profile if applicable
    if ($role === 'PLAYER') {
        try {
            $profStmt = $db->prepare("INSERT INTO player_profiles (user_id, playing_role, bio, city, availability_status) 
                                      VALUES (?, 'Batsman', 'Cricket player registered on 12THMAN', 'Mumbai, MH', 'Available')");
            $profStmt->execute([$userId]);
        } catch (Exception $e) {
            // Non-fatal if profile exists or fails
        }
    } elseif ($role === 'UMPIRE') {
        try {
            $profStmt = $db->prepare("INSERT INTO umpire_profiles (user_id, certification, bio, city, availability_status) 
                                      VALUES (?, 'State Level', 'Cricket umpire registered on 12THMAN', 'Mumbai, MH', 'Available')");
            $profStmt->execute([$userId]);
        } catch (Exception $e) {
            // Non-fatal
        }
    }

    // Return success response
    http_response_code(201); // 201 Created
    echo json_encode([
        "status" => "success",
        "message" => "User registered successfully",
        "data" => [
            "id"           => $userId,
            "full_name"    => $fullName,
            "email"        => $email,
            "phone"        => $phone,
            "role"         => $role,
            "token"        => $token,
            "token_expiry" => $tokenExpiry
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Database error during registration: " . $e->getMessage()
    ]);
}

