<?php
// ==========================================================
// 12THMAN BACKEND - USER LOGIN API
// Endpoint: POST /api/auth/login.php
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

$email    = trim($input['email'] ?? '');
$password = trim($input['password'] ?? '');

// Validate inputs
if (empty($email) || empty($password)) {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Email and password are required"
    ]);
    exit();
}

try {
    $db = getDatabaseConnection();

    // Search actual users table
    $stmt = $db->prepare("SELECT id, full_name, email, phone, password, role, profile_image, city 
                          FROM users 
                          WHERE email = :email 
                          LIMIT 1");
    $stmt->execute([':email' => $email]);
    $user = $stmt->fetch();

    if (!$user) {
        http_response_code(401);
        echo json_encode([
            "status" => "error",
            "message" => "Invalid email or password"
        ]);
        exit();
    }

    // Verify hashed password
    if (!password_verify($password, $user['password'])) {
        http_response_code(401);
        echo json_encode([
            "status" => "error",
            "message" => "Invalid email or password"
        ]);
        exit();
    }

    // Generate fresh secure auth token
    $token = bin2hex(random_bytes(32));
    $tokenExpiry = date('Y-m-d H:i:s', strtotime('+30 days'));

    // Update token in database
    $updateStmt = $db->prepare("UPDATE users SET token = :token, token_expiry = :token_expiry, updated_at = NOW() WHERE id = :id");
    $updateStmt->execute([
        ':token'        => $token,
        ':token_expiry' => $tokenExpiry,
        ':id'           => $user['id']
    ]);

    // Return authenticated user data (excluding password)
    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "message" => "Login successful",
        "data" => [
            "id"            => (int)$user['id'],
            "full_name"     => $user['full_name'],
            "email"         => $user['email'],
            "phone"         => $user['phone'],
            "role"          => $user['role'],
            "profile_image" => $user['profile_image'],
            "city"          => $user['city'] ?? "Mumbai",
            "token"         => $token,
            "token_expiry"  => $tokenExpiry
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Database error during login: " . $e->getMessage()
    ]);
}

