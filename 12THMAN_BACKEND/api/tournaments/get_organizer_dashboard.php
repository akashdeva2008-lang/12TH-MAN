<?php
// ==========================================================
// 12THMAN BACKEND - GET ORGANIZER DASHBOARD API
// Endpoint: GET /api/tournaments/get_organizer_dashboard.php?tournament_id=T301
// ==========================================================

require_once __DIR__ . '/../../config/database.php';

$tournamentIdRaw = trim($_GET['tournament_id'] ?? $_GET['tournamentId'] ?? '');

try {
    $db = getDatabaseConnection();

    // If no tournament ID provided, fetch the most recent tournament
    if (empty($tournamentIdRaw)) {
        $stmt = $db->query("SELECT id FROM tournaments ORDER BY id DESC LIMIT 1");
        $row = $stmt->fetch();
        if ($row) {
            $numericId = (int)$row['id'];
        } else {
            http_response_code(404);
            echo json_encode(["status" => "error", "message" => "No tournaments found"]);
            exit();
        }
    } else {
        $numericId = preg_replace('/[^0-9]/', '', $tournamentIdRaw);
        if ((int)$numericId > 300) {
            $numericId = (int)$numericId - 300;
        } else {
            $numericId = (int)$numericId;
        }
    }

    // 1. Fetch Tournament Details
    $tStmt = $db->prepare("SELECT 
            t.id AS numeric_id,
            CONCAT('T', t.id + 300) AS id,
            t.tournament_name AS name,
            t.tournament_type AS format,
            t.tournament_format AS tournamentFormat,
            t.location,
            t.ground_name AS groundName,
            t.start_date AS startDate,
            t.end_date AS endDate,
            t.registration_deadline AS registrationDeadline,
            t.max_teams AS totalTeams,
            t.registered_teams_count AS registeredTeamsCount,
            t.entry_fee AS entryFee,
            t.prize_money AS prizeMoney,
            t.first_prize AS firstPrize,
            t.second_prize AS secondPrize,
            t.third_prize AS thirdPrize,
            t.rules,
            t.overs,
            t.is_draft AS isDraft,
            t.status,
            t.organizer_name AS organizerName,
            t.organizer_contact AS organizerContact,
            t.description,
            t.logo_url AS logoUrl
        FROM tournaments t
        WHERE t.id = ?");
    $tStmt->execute([$numericId]);
    $tournament = $tStmt->fetch();

    if (!$tournament) {
        http_response_code(404);
        echo json_encode(["status" => "error", "message" => "Tournament not found"]);
        exit();
    }

    // 2. Fetch Match Statistics
    $mStats = $db->prepare("SELECT 
            COUNT(*) AS total_matches,
            SUM(CASE WHEN status = 'UPCOMING' THEN 1 ELSE 0 END) AS upcoming_matches,
            SUM(CASE WHEN status = 'LIVE' THEN 1 ELSE 0 END) AS live_matches,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_matches
        FROM matches WHERE tournament_id = ?");
    $mStats->execute([$numericId]);
    $stats = $mStats->fetch();

    $totalMatches = (int)($stats['total_matches'] ?? 0);
    $completedMatches = (int)($stats['completed_matches'] ?? 0);
    $progressPercent = $totalMatches > 0 ? round(($completedMatches / $totalMatches) * 100) : 0;

    // 3. Fetch Registered Teams (via tournament_teams or teams table)
    $teamStmt = $db->prepare("SELECT 
            tm.id AS numeric_id,
            CONCAT('TM', tm.id) AS id,
            CONCAT('T', ? + 300) AS tournamentId,
            tm.team_name AS teamName,
            tm.captain_name AS captainName,
            tm.captain_phone AS captainPhone,
            tm.captain_email AS captainEmail,
            tm.vice_captain AS viceCaptain,
            tm.manager_name AS managerName,
            tm.city,
            tm.logo_url AS logoUrl,
            COALESCE(tt.status, tm.approval_status, 'APPROVED') AS approvalStatus,
            COALESCE(tt.group_name, 'Group A') AS groupName,
            (SELECT COUNT(*) FROM team_players tp WHERE tp.team_id = tm.id) AS playerCount,
            DATE_FORMAT(tm.created_at, '%d %b %Y') AS registrationDate
        FROM teams tm
        LEFT JOIN tournament_teams tt ON tt.team_id = tm.id AND tt.tournament_id = ?
        WHERE tm.tournament_id = ? OR tt.tournament_id = ?
        GROUP BY tm.id
        ORDER BY tm.id ASC");
    $teamStmt->execute([$numericId, $numericId, $numericId, $numericId]);
    $teams = $teamStmt->fetchAll();

    // 4. Fetch Recent Results / Matches
    $matchStmt = $db->prepare("SELECT 
            m.id AS numeric_id,
            CONCAT('M', m.id + 500) AS id,
            CONCAT('T', m.tournament_id + 300) AS tournamentId,
            m.match_number AS matchNumber,
            m.team1_name AS teamA,
            m.team2_name AS teamB,
            m.match_date AS date,
            m.match_time AS time,
            m.venue,
            m.stage,
            m.status,
            m.result,
            m.score_a AS scoreA,
            m.score_b AS scoreB
        FROM matches m
        WHERE m.tournament_id = ?
        ORDER BY m.match_number ASC");
    $matchStmt->execute([$numericId]);
    $matches = $matchStmt->fetchAll();

    // 5. Response output
    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "data"   => [
            "tournament" => $tournament,
            "stats"      => [
                "registeredTeams"   => count($teams),
                "maxTeams"          => (int)$tournament['totalTeams'],
                "totalMatches"      => $totalMatches,
                "upcomingMatches"   => (int)($stats['upcoming_matches'] ?? 0),
                "liveMatches"       => (int)($stats['live_matches'] ?? 0),
                "completedMatches"  => $completedMatches,
                "progressPercent"   => $progressPercent
            ],
            "teams"      => $teams,
            "matches"    => $matches
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Dashboard error: " . $e->getMessage()]);
}
