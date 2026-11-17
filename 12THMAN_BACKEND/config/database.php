<?php
// ==========================================================
// 12THMAN BACKEND - DATABASE CONFIGURATION (PDO)
// Database: 12thman_db
// Compatibility: PHP 7.4+ / PHP 8.0+ / PHP 8.2+ (XAMPP MySQL)
// ==========================================================

// Global CORS & Content-Type Headers
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Handle preflight OPTIONS requests immediately
if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

class Database {
    private string $host = "127.0.0.1";
    private string $db_name = "12thman_db";
    private string $username = "root";
    private string $password = "";
    private int $port = 3306;
    private ?PDO $conn = null;

    public function getConnection(): ?PDO {
        $this->conn = null;

        $dsn = "mysql:host=" . $this->host . ";port=" . $this->port . ";dbname=" . $this->db_name . ";charset=utf8mb4";
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ];

        try {
            $this->conn = new PDO($dsn, $this->username, $this->password, $options);
        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode([
                "status" => "error",
                "message" => "Database Connection Failed: " . $e->getMessage()
            ]);
            exit();
        }

        return $this->conn;
    }
}

/**
 * Convenience helper function to obtain PDO connection directly.
 */
function getDatabaseConnection(): PDO {
    $database = new Database();
    return $database->getConnection();
}

