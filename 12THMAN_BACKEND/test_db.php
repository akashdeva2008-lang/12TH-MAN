<?php
// ==========================================================
// 12THMAN BACKEND - DATABASE DIAGNOSTIC TEST SCRIPT
// Test: Connection, Database Selection, and users Table Query
// ==========================================================

require_once __DIR__ . '/config/database.php';

try {
    $db = getDatabaseConnection();
    
    // Test 1: Check Database Name
    $stmt = $db->query("SELECT DATABASE() AS current_db");
    $dbName = $stmt->fetchColumn();

    // Test 2: Count Users Table
    $stmt = $db->query("SELECT COUNT(*) AS user_count FROM users");
    $userCount = (int)$stmt->fetchColumn();

    // Test 3: Fetch Sample Users
    $stmt = $db->query("SELECT id, full_name, email, role, created_at FROM users ORDER BY id ASC LIMIT 5");
    $sampleUsers = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "message" => "MySQL connection verified successfully!",
        "database" => $dbName,
        "total_users" => $userCount,
        "sample_users" => $sampleUsers,
        "php_version" => phpversion()
    ], JSON_PRETTY_PRINT);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Database check failed: " . $e->getMessage()
    ], JSON_PRETTY_PRINT);
}

