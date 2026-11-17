<?php
// ==========================================================
// 12THMAN BACKEND - USER PROFILE API
// Endpoint: GET /api/auth/profile.php
// Header: Authorization: Bearer <token>
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

// Extract token from Authorization header or query param
$token = null;
$headers = [];
if (function_exists('apache_request_headers')) {
    $headers = apache_request_headers();
}
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? $_SERVER['HTTP_AUTHORIZATION'] ?? '';

if (preg_match('/Bearer\s+(\S+)/', $authHeader, $matches)) {
    $token = $matches[1];
} elseif (!empty($_GET['token'])) {
    $token = trim($_GET['token']);
}

if (empty($token)) {
    http_response_code(401);
    echo json_encode([
        "status" => "error",
        "message" => "Authentication token required. Provide 'Authorization: Bearer <token>' header."
    ]);
    exit();
}

try {
    $db = getDatabaseConnection();

    $stmt = $db->prepare("SELECT id, full_name, email, phone, role, profile_image, city, token_expiry, created_at 
                          FROM users 
                          WHERE token = :token 
                          LIMIT 1");
    $stmt->execute([':token' => $token]);
    $user = $stmt->fetch();

    if (!$user) {
        http_response_code(401);
        echo json_encode([
            "status" => "error",
            "message" => "Invalid or expired authentication token"
        ]);
        exit();
    }

    // Check expiry if set
    if (!empty($user['token_expiry']) && strtotime($user['token_expiry']) < time()) {
        http_response_code(401);
        echo json_encode([
            "status" => "error",
            "message" => "Session expired. Please log in again."
        ]);
        exit();
    }

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "message" => "User profile retrieved successfully",
        "data" => [
            "id"            => (int)$user['id'],
            "full_name"     => $user['full_name'],
            "email"         => $user['email'],
            "phone"         => $user['phone'],
            "role"          => $user['role'],
            "profile_image" => $user['profile_image'],
            "city"          => $user['city'] ?? "Mumbai",
            "created_at"    => $user['created_at']
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Database error while fetching profile: " . $e->getMessage()
    ]);
}

