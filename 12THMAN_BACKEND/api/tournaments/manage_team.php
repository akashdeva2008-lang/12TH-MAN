<?php
// ==========================================================
// 12THMAN BACKEND - MANAGE TEAM API
// Endpoint: POST /api/tournaments/manage_team.php
// Actions: add_team, create_team, update_team, remove_team, delete_team, update_status
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

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

$action          = trim($input['action'] ?? 'add_team');
$tournamentIdRaw = trim($input['tournamentId'] ?? $input['tournament_id'] ?? '');
$teamIdRaw       = trim($input['teamId'] ?? $input['team_id'] ?? $input['id'] ?? '');
$teamName        = trim($input['teamName'] ?? $input['team_name'] ?? '');
$shortName       = trim($input['shortName'] ?? $input['short_name'] ?? '');
$captainName     = trim($input['captainName'] ?? $input['captain_name'] ?? '');
$captainPhone    = trim($input['captainPhone'] ?? $input['captain_phone'] ?? '');
$captainEmail    = trim($input['captainEmail'] ?? $input['captain_email'] ?? 'captain@12thman.com');
$viceCaptain     = trim($input['viceCaptain'] ?? $input['vice_captain'] ?? '');
$managerName     = trim($input['managerName'] ?? $input['manager_name'] ?? '');
$description     = trim($input['description'] ?? '');
$city            = trim($input['city'] ?? 'Mumbai');
$logoUrl         = trim($input['logoUrl'] ?? $input['logo_url'] ?? 'RED');
$status          = trim($input['status'] ?? 'APPROVED');
$groupName       = trim($input['groupName'] ?? 'Group A');
$players         = $input['players'] ?? $input['playerNames'] ?? [];

if (empty($shortName) && !empty($teamName)) {
    $parts = preg_split('/\s+/', $teamName);
    if (count($parts) > 1) {
        $shortName = strtoupper(substr($parts[0], 0, 1) . substr($parts[1], 0, 2));
    } else {
        $shortName = strtoupper(substr($teamName, 0, 3));
    }
}

$numericTournamentId = null;
if (!empty($tournamentIdRaw)) {
    $tId = (int)preg_replace('/[^0-9]/', '', $tournamentIdRaw);
    if ($tId > 300) {
        $tId -= 300;
    }
    if ($tId > 0) {
        $numericTournamentId = $tId;
    }
}

$numericTeamId = 0;
if (!empty($teamIdRaw)) {
    $numericTeamId = (int)preg_replace('/[^0-9]/', '', $teamIdRaw);
}

try {
    $db = getDatabaseConnection();

    switch ($action) {
        case 'add_team':
        case 'create_team':
            if (empty($teamName)) {
                http_response_code(400);
                echo json_encode(["status" => "error", "message" => "Team name is required."]);
                exit();
            }
            if (empty($captainName)) {
                http_response_code(400);
                echo json_encode(["status" => "error", "message" => "Captain name is required."]);
                exit();
            }

            $db->beginTransaction();

            // Check if team already exists by name or by specific numeric ID
            $existingTeam = null;
            if ($numericTeamId > 0) {
                $idCheck = $db->prepare("SELECT id FROM teams WHERE id = ? LIMIT 1");
                $idCheck->execute([$numericTeamId]);
                $existingTeam = $idCheck->fetch(PDO::FETCH_ASSOC);
            }
            if (!$existingTeam) {
                $nameCheck = $db->prepare("SELECT id FROM teams WHERE LOWER(TRIM(team_name)) = LOWER(TRIM(?)) LIMIT 1");
                $nameCheck->execute([$teamName]);
                $existingTeam = $nameCheck->fetch(PDO::FETCH_ASSOC);
            }

            if ($existingTeam) {
                // Safely update existing team without duplicate-key errors
                $targetId = (int)$existingTeam['id'];
                $upStmt = $db->prepare("UPDATE teams SET 
                                            team_name = ?,
                                            short_name = ?,
                                            captain_name = ?,
                                            captain_phone = ?,
                                            captain_email = ?,
                                            vice_captain = ?,
                                            manager_name = ?,
                                            description = ?,
                                            city = ?,
                                            logo_url = ?,
                                            approval_status = ?
                                        WHERE id = ?");
                $upStmt->execute([$teamName, $shortName, $captainName, $captainPhone, $captainEmail,
                                  $viceCaptain, $managerName, $description, $city, $logoUrl, $status, $targetId]);
                $newTeamId = $targetId;
            } else {
                // Insert new master team
                $stmt = $db->prepare("INSERT INTO teams (tournament_id, team_name, short_name, captain_name, captain_phone, captain_email, 
                                                        vice_captain, manager_name, description, city, logo_url, approval_status, created_at)
                                      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())");
                $stmt->execute([$numericTournamentId, $teamName, $shortName, $captainName, $captainPhone, $captainEmail,
                                $viceCaptain, $managerName, $description, $city, $logoUrl, $status]);
                $newTeamId = (int)$db->lastInsertId();
            }

            // Link to tournament_teams if tournament is specified
            if ($numericTournamentId !== null && $numericTournamentId > 0) {
                $ttCheck = $db->prepare("SELECT id FROM tournament_teams WHERE tournament_id = ? AND team_id = ?");
                $ttCheck->execute([$numericTournamentId, $newTeamId]);
                if (!$ttCheck->fetch()) {
                    $ttStmt = $db->prepare("INSERT INTO tournament_teams (tournament_id, team_id, status, group_name, created_at) 
                                            VALUES (?, ?, ?, ?, NOW())");
                    $ttStmt->execute([$numericTournamentId, $newTeamId, $status, $groupName]);

                    $upT = $db->prepare("UPDATE tournaments SET registered_teams_count = registered_teams_count + 1 WHERE id = ?");
                    $upT->execute([$numericTournamentId]);
                }
            }

            // Insert or refresh squad players
            if (is_array($players) && !empty($players)) {
                // Clear old squad records for this team to keep list clean
                $delPlayers = $db->prepare("DELETE FROM team_players WHERE team_id = ?");
                $delPlayers->execute([$newTeamId]);

                $pStmt = $db->prepare("INSERT INTO team_players (team_id, player_name, player_role, jersey_number, player_contact, is_captain, is_vice_captain, created_at) 
                                       VALUES (?, ?, ?, ?, ?, ?, ?, NOW())");

                $captainFound = false;

                foreach ($players as $idx => $p) {
                    if (is_array($p)) {
                        $pName = trim($p['playerName'] ?? $p['name'] ?? '');
                        $pRole = trim($p['role'] ?? 'Batsman');
                        $pJersey = trim($p['jerseyNumber'] ?? '');
                        $pContact = trim($p['contact'] ?? '');
                        $isCap = (!empty($p['isCaptain']) && $p['isCaptain'] !== 'false') ? 1 : 0;
                        $isVc = (!empty($p['isViceCaptain']) && $p['isViceCaptain'] !== 'false') ? 1 : 0;
                    } else {
                        $pName = trim((string)$p);
                        $pRole = 'Batsman';
                        $pJersey = '';
                        $pContact = '';
                        $isCap = 0;
                        $isVc = 0;
                    }

                    if ($isCap === 1) {
                        $captainFound = true;
                    }

                    if (!empty($pName)) {
                        $pStmt->execute([$newTeamId, $pName, $pRole, $pJersey, $pContact, $isCap, $isVc]);
                    }
                }

                // If captain was not explicitly flagged in squad, flag or insert captain
                if (!$captainFound && !empty($captainName)) {
                    $flagCap = $db->prepare("UPDATE team_players SET is_captain = 1 WHERE team_id = ? AND LOWER(TRIM(player_name)) = LOWER(TRIM(?)) LIMIT 1");
                    $flagCap->execute([$newTeamId, $captainName]);
                    if ($flagCap->rowCount() === 0) {
                        $pStmt->execute([$newTeamId, $captainName, 'Batsman', '7', $captainPhone, 1, 0]);
                    }
                }
            } else {
                // If no squad passed, ensure at least the captain is in squad
                $delPlayers = $db->prepare("DELETE FROM team_players WHERE team_id = ?");
                $delPlayers->execute([$newTeamId]);

                $pStmt = $db->prepare("INSERT INTO team_players (team_id, player_name, player_role, jersey_number, player_contact, is_captain, is_vice_captain, created_at) 
                                       VALUES (?, ?, 'Batsman', '7', ?, 1, 0, NOW())");
                $pStmt->execute([$newTeamId, $captainName, $captainPhone]);
            }

            $db->commit();

            http_response_code(200);
            echo json_encode([
                "status"  => "success",
                "message" => "Team '$teamName' saved successfully with squad players",
                "data"    => [
                    "id"           => "TM" . $newTeamId,
                    "numeric_id"   => $newTeamId,
                    "teamName"     => $teamName,
                    "shortName"    => $shortName,
                    "captainName"  => $captainName,
                    "logoUrl"      => $logoUrl,
                    "tournamentId" => $tournamentIdRaw
                ]
            ]);
            break;

        case 'update_team':
            if ($numericTeamId <= 0) {
                // Try to find team by name if ID was missing
                $nameCheck = $db->prepare("SELECT id FROM teams WHERE LOWER(TRIM(team_name)) = LOWER(TRIM(?)) LIMIT 1");
                $nameCheck->execute([$teamName]);
                $f = $nameCheck->fetch(PDO::FETCH_ASSOC);
                if ($f) {
                    $numericTeamId = (int)$f['id'];
                }
            }

            if ($numericTeamId <= 0) {
                http_response_code(400);
                echo json_encode(["status" => "error", "message" => "Valid team ID required for update."]);
                exit();
            }

            $db->beginTransaction();

            $upStmt = $db->prepare("UPDATE teams SET 
                                        team_name = ?,
                                        short_name = ?,
                                        captain_name = ?,
                                        captain_phone = ?,
                                        captain_email = ?,
                                        vice_captain = ?,
                                        manager_name = ?,
                                        description = ?,
                                        city = ?,
                                        logo_url = ?
                                    WHERE id = ?");
            $upStmt->execute([$teamName, $shortName, $captainName, $captainPhone, $captainEmail,
                              $viceCaptain, $managerName, $description, $city, $logoUrl, $numericTeamId]);

            // Replace squad players
            if (is_array($players) && !empty($players)) {
                $delPlayers = $db->prepare("DELETE FROM team_players WHERE team_id = ?");
                $delPlayers->execute([$numericTeamId]);

                $pStmt = $db->prepare("INSERT INTO team_players (team_id, player_name, player_role, jersey_number, player_contact, is_captain, is_vice_captain, created_at) 
                                       VALUES (?, ?, ?, ?, ?, ?, ?, NOW())");
                foreach ($players as $idx => $p) {
                    if (is_array($p)) {
                        $pName = trim($p['playerName'] ?? $p['name'] ?? '');
                        $pRole = trim($p['role'] ?? 'Batsman');
                        $pJersey = trim($p['jerseyNumber'] ?? '');
                        $pContact = trim($p['contact'] ?? '');
                        $isCap = (!empty($p['isCaptain']) && $p['isCaptain'] !== 'false') ? 1 : 0;
                        $isVc = (!empty($p['isViceCaptain']) && $p['isViceCaptain'] !== 'false') ? 1 : 0;
                    } else {
                        $pName = trim((string)$p);
                        $pRole = 'Batsman';
                        $pJersey = '';
                        $pContact = '';
                        $isCap = ($idx === 0) ? 1 : 0;
                        $isVc = 0;
                    }

                    if (!empty($pName)) {
                        $pStmt->execute([$numericTeamId, $pName, $pRole, $pJersey, $pContact, $isCap, $isVc]);
                    }
                }
            }

            $db->commit();

            http_response_code(200);
            echo json_encode([
                "status"  => "success",
                "message" => "Team '$teamName' updated successfully",
                "data"    => [
                    "id"           => "TM" . $numericTeamId,
                    "numeric_id"   => $numericTeamId,
                    "teamName"     => $teamName,
                    "shortName"    => $shortName,
                    "captainName"  => $captainName,
                    "logoUrl"      => $logoUrl,
                    "tournamentId" => $tournamentIdRaw
                ]
            ]);
            break;

        case 'remove_team':
        case 'delete_team':
            if ($numericTeamId <= 0) {
                http_response_code(400);
                echo json_encode(["status" => "error", "message" => "Valid team ID required."]);
                exit();
            }

            $db->beginTransaction();

            // Check if matches have already been completed with this team
            $mCheck = $db->prepare("SELECT id FROM matches WHERE (team1_id = ? OR team2_id = ?) AND status = 'COMPLETED' LIMIT 1");
            $mCheck->execute([$numericTeamId, $numericTeamId]);
            if ($mCheck->fetch()) {
                $db->rollBack();
                http_response_code(400);
                echo json_encode(["status" => "error", "message" => "Cannot delete team: completed match records exist for this team."]);
                exit();
            }

            // Delete upcoming or scheduled matches referencing this team
            $delMatches = $db->prepare("DELETE FROM matches WHERE (team1_id = ? OR team2_id = ?) AND status != 'COMPLETED'");
            $delMatches->execute([$numericTeamId, $numericTeamId]);

            // Remove tournament link
            $delLink = $db->prepare("DELETE FROM tournament_teams WHERE team_id = ?");
            $delLink->execute([$numericTeamId]);

            // Remove players
            $delP = $db->prepare("DELETE FROM team_players WHERE team_id = ?");
            $delP->execute([$numericTeamId]);

            // Remove master team
            $delTeam = $db->prepare("DELETE FROM teams WHERE id = ?");
            $delTeam->execute([$numericTeamId]);

            $db->commit();

            http_response_code(200);
            echo json_encode(["status" => "success", "message" => "Team deleted successfully."]);
            break;

        case 'update_status':
            if ($numericTeamId <= 0) {
                http_response_code(400);
                echo json_encode(["status" => "error", "message" => "Valid team ID required."]);
                exit();
            }

            $upMaster = $db->prepare("UPDATE teams SET approval_status = ? WHERE id = ?");
            $upMaster->execute([$status, $numericTeamId]);

            if ($numericTournamentId !== null) {
                $upStatus = $db->prepare("UPDATE tournament_teams SET status = ? WHERE tournament_id = ? AND team_id = ?");
                $upStatus->execute([$status, $numericTournamentId, $numericTeamId]);
            }

            http_response_code(200);
            echo json_encode(["status" => "success", "message" => "Team status updated to $status"]);
            break;

        default:
            http_response_code(400);
            echo json_encode(["status" => "error", "message" => "Unknown action: $action"]);
            break;
    }

} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) {
        $db->rollBack();
    }
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
}
