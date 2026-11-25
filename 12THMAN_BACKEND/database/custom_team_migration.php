<?php
// ==========================================================
// 12THMAN BACKEND - CUSTOM TEAM FEATURE MIGRATION
// ==========================================================

require_once __DIR__ . '/../config/database.php';

try {
    $db = getDatabaseConnection();
    echo "Running Custom Teams Database Migration...\n";

    function addColIfNotExists($db, $table, $column, $definition) {
        $check = $db->query("SHOW COLUMNS FROM `$table` LIKE '$column'");
        if ($check->rowCount() == 0) {
            $db->exec("ALTER TABLE `$table` ADD COLUMN `$column` $definition");
            echo "Added `$column` to `$table`.\n";
        } else {
            echo "Column `$column` already exists in `$table`.\n";
        }
    }

    // 1. Make tournament_id in teams table nullable so users can create their own teams without a tournament
    try {
        $db->exec("ALTER TABLE `teams` MODIFY `tournament_id` INT UNSIGNED NULL");
        echo "Made `teams.tournament_id` nullable.\n";
    } catch (Exception $e) {
        echo "tournament_id alter note: " . $e->getMessage() . "\n";
    }

    // 2. Add columns to teams
    addColIfNotExists($db, 'teams', 'short_name', 'VARCHAR(15) DEFAULT NULL');
    addColIfNotExists($db, 'teams', 'owner_id', 'INT UNSIGNED DEFAULT 1');

    // 3. Add columns to team_players for role, jersey number, and contact
    addColIfNotExists($db, 'team_players', 'player_role', 'VARCHAR(50) DEFAULT "Batsman"');
    addColIfNotExists($db, 'team_players', 'jersey_number', 'VARCHAR(10) DEFAULT NULL');
    addColIfNotExists($db, 'team_players', 'player_contact', 'VARCHAR(30) DEFAULT NULL');
    addColIfNotExists($db, 'team_players', 'is_vice_captain', 'TINYINT(1) DEFAULT 0');

    echo "Migration completed successfully!\n";

} catch (Exception $e) {
    echo "Error during migration: " . $e->getMessage() . "\n";
    exit(1);
}
