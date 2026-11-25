<?php
// ==========================================================
// 12THMAN BACKEND - TOURNAMENT ORGANIZER MIGRATION
// ==========================================================

require_once __DIR__ . '/../config/database.php';

try {
    $db = getDatabaseConnection();
    echo "Running Tournament Organizer Database Migration...\n";

    // Helper function to check if column exists
    function addColumnIfNotExists($db, $table, $column, $definition) {
        $check = $db->query("SHOW COLUMNS FROM `$table` LIKE '$column'");
        if ($check->rowCount() == 0) {
            $db->exec("ALTER TABLE `$table` ADD COLUMN `$column` $definition");
            echo "Added `$column` to `$table`.\n";
        } else {
            echo "Column `$column` already exists in `$table`.\n";
        }
    }

    // 1. Update tournaments table
    addColumnIfNotExists($db, 'tournaments', 'rules', 'TEXT DEFAULT NULL');
    addColumnIfNotExists($db, 'tournaments', 'first_prize', 'VARCHAR(100) DEFAULT NULL');
    addColumnIfNotExists($db, 'tournaments', 'second_prize', 'VARCHAR(100) DEFAULT NULL');
    addColumnIfNotExists($db, 'tournaments', 'third_prize', 'VARCHAR(100) DEFAULT NULL');
    addColumnIfNotExists($db, 'tournaments', 'ground_name', 'VARCHAR(150) DEFAULT NULL');
    addColumnIfNotExists($db, 'tournaments', 'registration_deadline', 'VARCHAR(50) DEFAULT NULL');
    addColumnIfNotExists($db, 'tournaments', 'tournament_format', 'VARCHAR(50) DEFAULT "KNOCKOUT"');
    addColumnIfNotExists($db, 'tournaments', 'overs', 'INT UNSIGNED DEFAULT 20');
    addColumnIfNotExists($db, 'tournaments', 'is_draft', 'TINYINT(1) DEFAULT 0');
    addColumnIfNotExists($db, 'tournaments', 'logo_url', 'VARCHAR(255) DEFAULT NULL');

    // 2. Update teams table
    addColumnIfNotExists($db, 'teams', 'logo_url', 'VARCHAR(255) DEFAULT NULL');
    addColumnIfNotExists($db, 'teams', 'vice_captain', 'VARCHAR(120) DEFAULT NULL');
    addColumnIfNotExists($db, 'teams', 'manager_name', 'VARCHAR(120) DEFAULT NULL');
    addColumnIfNotExists($db, 'teams', 'description', 'TEXT DEFAULT NULL');
    addColumnIfNotExists($db, 'teams', 'approval_status', 'ENUM("PENDING", "APPROVED", "REJECTED") DEFAULT "APPROVED"');

    // 3. Update matches table
    addColumnIfNotExists($db, 'matches', 'umpire_name', 'VARCHAR(120) DEFAULT "Official Umpire"');
    addColumnIfNotExists($db, 'matches', 'scorer_name', 'VARCHAR(120) DEFAULT "Official Scorer"');
    addColumnIfNotExists($db, 'matches', 'overs', 'INT UNSIGNED DEFAULT 20');

    // 4. Create tournament_teams junction table for multi-tournament team participation
    $db->exec("CREATE TABLE IF NOT EXISTS `tournament_teams` (
      `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
      `tournament_id` INT UNSIGNED NOT NULL,
      `team_id` INT UNSIGNED NOT NULL,
      `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED',
      `group_name` VARCHAR(50) DEFAULT 'Group A',
      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE KEY `uk_tourn_team_link` (`tournament_id`, `team_id`),
      FOREIGN KEY (`tournament_id`) REFERENCES `tournaments` (`id`) ON DELETE CASCADE,
      FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
    echo "Verified table `tournament_teams`.\n";

    // 5. Populate tournament_teams with existing teams
    $db->exec("INSERT IGNORE INTO `tournament_teams` (tournament_id, team_id, status, created_at)
               SELECT tournament_id, id, 'APPROVED', created_at FROM teams WHERE tournament_id IS NOT NULL;");
    echo "Populated `tournament_teams` from existing records.\n";

    echo "Migration completed successfully!\n";

} catch (Exception $e) {
    echo "Error during migration: " . $e->getMessage() . "\n";
    exit(1);
}
