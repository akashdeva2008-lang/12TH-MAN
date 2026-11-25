-- ==========================================================
-- 12THMAN CRICKET ECOSYSTEM & TOURNAMENT MANAGEMENT PLATFORM
-- Complete Production Database Schema & Seed Data
-- Database Name: 12thman_db
-- Compatibility: MySQL 5.7+ / MySQL 8.0+ / MariaDB 10.4+
-- ==========================================================

CREATE DATABASE IF NOT EXISTS `12thman_db`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;


  

USE `12thman_db`;

-- Drop tables in reverse FK dependency order
DROP TABLE IF EXISTS `ball_by_ball`;
DROP TABLE IF EXISTS `match_innings`;
DROP TABLE IF EXISTS `team_players`;
DROP TABLE IF EXISTS `matches`;
DROP TABLE IF EXISTS `bookings`;
DROP TABLE IF EXISTS `teams`;
DROP TABLE IF EXISTS `tournaments`;
DROP TABLE IF EXISTS `umpire_profiles`;
DROP TABLE IF EXISTS `player_profiles`;
DROP TABLE IF EXISTS `user_notifications`;
DROP TABLE IF EXISTS `login_attempts`;
DROP TABLE IF EXISTS `users`;

-- ----------------------------------------------------------
-- 1. USERS TABLE
-- ----------------------------------------------------------
CREATE TABLE `users` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `full_name` VARCHAR(120) NOT NULL,
  `email` VARCHAR(150) NOT NULL UNIQUE,
  `phone` VARCHAR(25) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `role` ENUM('PLAYER', 'UMPIRE', 'ORGANIZER', 'SCORER') NOT NULL DEFAULT 'PLAYER',
  `profile_image` VARCHAR(255) DEFAULT NULL,
  `city` VARCHAR(100) DEFAULT 'Mumbai',
  `token` VARCHAR(255) DEFAULT NULL,
  `token_expiry` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_users_role` (`role`),
  INDEX `idx_users_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 2. PLAYER PROFILES TABLE
-- ----------------------------------------------------------
CREATE TABLE `player_profiles` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NOT NULL UNIQUE,
  `playing_role` ENUM('Batsman', 'Bowler', 'All-Rounder', 'Wicket Keeper') NOT NULL DEFAULT 'Batsman',
  `batting_style` VARCHAR(50) DEFAULT 'Right-Handed',
  `bowling_style` VARCHAR(50) DEFAULT 'Right-Arm Fast',
  `experience` VARCHAR(50) DEFAULT '5+ Years',
  `matches_played` INT UNSIGNED NOT NULL DEFAULT 0,
  `runs` INT UNSIGNED NOT NULL DEFAULT 0,
  `wickets` INT UNSIGNED NOT NULL DEFAULT 0,
  `price_per_match` INT UNSIGNED NOT NULL DEFAULT 1500,
  `rating` DECIMAL(3, 1) NOT NULL DEFAULT 4.5,
  `bio` TEXT DEFAULT NULL,
  `city` VARCHAR(100) DEFAULT 'Mumbai, MH',
  `availability_status` ENUM('Available', 'Booked', 'Unavailable') NOT NULL DEFAULT 'Available',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  INDEX `idx_player_role` (`playing_role`),
  INDEX `idx_player_city` (`city`),
  INDEX `idx_player_avail` (`availability_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 3. UMPIRE PROFILES TABLE
-- ----------------------------------------------------------
CREATE TABLE `umpire_profiles` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NOT NULL UNIQUE,
  `certification` ENUM('ICC Accredited', 'National Certified', 'State Level', 'Club Certified') NOT NULL DEFAULT 'State Level',
  `experience_years` INT UNSIGNED NOT NULL DEFAULT 5,
  `matches_officiated` INT UNSIGNED NOT NULL DEFAULT 50,
  `rating` DECIMAL(3, 1) NOT NULL DEFAULT 4.7,
  `match_fee` INT UNSIGNED NOT NULL DEFAULT 2000,
  `availability_status` ENUM('Available', 'Booked', 'Unavailable') NOT NULL DEFAULT 'Available',
  `bio` TEXT DEFAULT NULL,
  `city` VARCHAR(100) DEFAULT 'Mumbai, MH',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  INDEX `idx_umpire_cert` (`certification`),
  INDEX `idx_umpire_city` (`city`),
  INDEX `idx_umpire_avail` (`availability_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 4. TOURNAMENTS TABLE
-- ----------------------------------------------------------
CREATE TABLE `tournaments` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `organizer_id` INT UNSIGNED NOT NULL,
  `tournament_name` VARCHAR(180) NOT NULL,
  `tournament_type` ENUM('T20', 'ODI', 'Box Cricket', 'Test') NOT NULL DEFAULT 'T20',
  `location` VARCHAR(255) NOT NULL,
  `start_date` VARCHAR(50) NOT NULL,
  `end_date` VARCHAR(50) NOT NULL,
  `entry_fee` INT UNSIGNED NOT NULL DEFAULT 10000,
  `prize_money` INT UNSIGNED NOT NULL DEFAULT 100000,
  `max_teams` INT UNSIGNED NOT NULL DEFAULT 16,
  `registered_teams_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `status` ENUM('UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'UPCOMING',
  `organizer_name` VARCHAR(120) DEFAULT NULL,
  `organizer_contact` VARCHAR(50) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`organizer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  INDEX `idx_tournament_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 5. TEAMS TABLE
-- ----------------------------------------------------------
CREATE TABLE `teams` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `tournament_id` INT UNSIGNED NOT NULL,
  `team_name` VARCHAR(120) NOT NULL,
  `captain_id` INT UNSIGNED DEFAULT NULL,
  `captain_name` VARCHAR(120) NOT NULL,
  `captain_phone` VARCHAR(25) NOT NULL,
  `captain_email` VARCHAR(150) DEFAULT NULL,
  `city` VARCHAR(100) DEFAULT 'Mumbai',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`tournament_id`) REFERENCES `tournaments` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`captain_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  INDEX `idx_team_tournament` (`tournament_id`),
  UNIQUE KEY `uk_tourn_team` (`tournament_id`, `team_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 6. TEAM PLAYERS TABLE
-- ----------------------------------------------------------
CREATE TABLE `team_players` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `team_id` INT UNSIGNED NOT NULL,
  `player_id` INT UNSIGNED DEFAULT NULL,
  `player_name` VARCHAR(120) NOT NULL,
  `is_captain` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`player_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  INDEX `idx_team_player` (`team_id`, `player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 7. MATCHES TABLE
-- ----------------------------------------------------------
CREATE TABLE `matches` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `tournament_id` INT UNSIGNED NOT NULL,
  `match_number` INT UNSIGNED NOT NULL DEFAULT 1,
  `team1_id` INT UNSIGNED NOT NULL,
  `team2_id` INT UNSIGNED NOT NULL,
  `team1_name` VARCHAR(120) NOT NULL,
  `team2_name` VARCHAR(120) NOT NULL,
  `match_date` VARCHAR(50) NOT NULL,
  `match_time` VARCHAR(50) NOT NULL,
  `venue` VARCHAR(255) NOT NULL,
  `match_type` VARCHAR(50) NOT NULL DEFAULT 'T20',
  `stage` VARCHAR(80) NOT NULL DEFAULT 'Group Stage',
  `status` ENUM('UPCOMING', 'LIVE', 'COMPLETED', 'ABANDONED') NOT NULL DEFAULT 'UPCOMING',
  `toss_winner_id` INT UNSIGNED DEFAULT NULL,
  `toss_decision` ENUM('BAT', 'BOWL') DEFAULT NULL,
  `winner_id` INT UNSIGNED DEFAULT NULL,
  `result` VARCHAR(255) NOT NULL DEFAULT 'Scheduled',
  `score_a` VARCHAR(50) NOT NULL DEFAULT '0/0 (0.0)',
  `score_b` VARCHAR(50) NOT NULL DEFAULT '0/0 (0.0)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`tournament_id`) REFERENCES `tournaments` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`team1_id`) REFERENCES `teams` (`id`) ON DELETE RESTRICT,
  FOREIGN KEY (`team2_id`) REFERENCES `teams` (`id`) ON DELETE RESTRICT,
  INDEX `idx_match_tournament` (`tournament_id`),
  INDEX `idx_match_status` (`status`),
  UNIQUE KEY `uk_tourn_match` (`tournament_id`, `match_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 8. BOOKINGS TABLE
-- ----------------------------------------------------------
CREATE TABLE `bookings` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NOT NULL,
  `item_type` ENUM('PLAYER', 'UMPIRE') NOT NULL,
  `item_id` INT UNSIGNED NOT NULL,
  `item_name` VARCHAR(150) NOT NULL,
  `booking_date` VARCHAR(50) NOT NULL,
  `start_time` VARCHAR(50) NOT NULL,
  `end_time` VARCHAR(50) DEFAULT NULL,
  `venue` VARCHAR(255) NOT NULL,
  `match_type` VARCHAR(50) DEFAULT 'T20',
  `amount` INT UNSIGNED NOT NULL DEFAULT 0,
  `notes` TEXT DEFAULT NULL,
  `status` ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'PENDING',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  INDEX `idx_booking_user` (`user_id`),
  INDEX `idx_booking_item` (`item_type`, `item_id`),
  INDEX `idx_booking_date` (`booking_date`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 9. MATCH INNINGS TABLE
-- ----------------------------------------------------------
CREATE TABLE `match_innings` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `match_id` INT UNSIGNED NOT NULL,
  `innings_number` TINYINT UNSIGNED NOT NULL DEFAULT 1,
  `batting_team_id` INT UNSIGNED NOT NULL,
  `bowling_team_id` INT UNSIGNED NOT NULL,
  `batting_team_name` VARCHAR(120) NOT NULL,
  `bowling_team_name` VARCHAR(120) NOT NULL,
  `total_runs` INT UNSIGNED NOT NULL DEFAULT 0,
  `wickets` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `legal_balls` INT UNSIGNED NOT NULL DEFAULT 0,
  `overs` DECIMAL(4, 1) NOT NULL DEFAULT 0.0,
  `extras` INT UNSIGNED NOT NULL DEFAULT 0,
  `status` ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_match_innings` (`match_id`, `innings_number`),
  FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE CASCADE,
  INDEX `idx_innings_match` (`match_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 10. BALL BY BALL TABLE
-- ----------------------------------------------------------
CREATE TABLE `ball_by_ball` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `innings_id` INT UNSIGNED NOT NULL,
  `match_id` INT UNSIGNED NOT NULL,
  `over_number` INT UNSIGNED NOT NULL DEFAULT 0,
  `ball_number` INT UNSIGNED NOT NULL DEFAULT 1,
  `is_legal_ball` TINYINT(1) NOT NULL DEFAULT 1,
  `batsman_id` INT UNSIGNED DEFAULT NULL,
  `batsman_name` VARCHAR(120) DEFAULT 'Striker',
  `bowler_id` INT UNSIGNED DEFAULT NULL,
  `bowler_name` VARCHAR(120) DEFAULT 'Bowler',
  `runs` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `extras` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `extra_type` ENUM('NONE', 'WIDE', 'NO_BALL', 'BYE', 'LEG_BYE') NOT NULL DEFAULT 'NONE',
  `wicket` TINYINT(1) NOT NULL DEFAULT 0,
  `wicket_type` ENUM('NONE', 'BOWLED', 'CAUGHT', 'LBW', 'RUN_OUT', 'STUMPED', 'HIT_WICKET') NOT NULL DEFAULT 'NONE',
  `player_dismissed` VARCHAR(120) DEFAULT NULL,
  `commentary` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`innings_id`) REFERENCES `match_innings` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE CASCADE,
  INDEX `idx_ball_innings` (`innings_id`),
  INDEX `idx_ball_match` (`match_id`),
  UNIQUE KEY `uk_innings_over_ball` (`innings_id`, `over_number`, `ball_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 11. USER NOTIFICATIONS TABLE
-- ----------------------------------------------------------
CREATE TABLE `user_notifications` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `message` TEXT NOT NULL,
  `is_read` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  INDEX `idx_user_notif` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 12. LOGIN ATTEMPTS (Rate limiting / Brute force defense)
-- ----------------------------------------------------------
CREATE TABLE `login_attempts` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `ip_address` VARCHAR(45) NOT NULL,
  `email` VARCHAR(150) NOT NULL,
  `attempts` INT UNSIGNED NOT NULL DEFAULT 1,
  `last_attempt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_login_ip` (`ip_address`, `email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================================
-- SEED DATA (Synchronized with Android Application State)
-- Default Password for all seed users: password123
-- ==========================================================

-- 1. Users
INSERT INTO `users` (`id`, `full_name`, `email`, `phone`, `password`, `role`, `profile_image`, `city`, `token`) VALUES
(1, 'Rahul Dravid', 'user@example.com', '+91 99000 11222', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'ORGANIZER', 'organizer_1.png', 'Mumbai, MH', 'token_organizer_user_100'),
(2, 'Virat Sharma', 'virat.sharma@cricket.com', '+91 98765 43210', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'PLAYER', 'player_101.png', 'Mumbai, MH', 'token_player_virat'),
(3, 'Jasprit Patel', 'jasprit.p@cricket.com', '+91 98765 43211', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'PLAYER', 'player_102.png', 'Ahmedabad, GJ', 'token_player_jasprit'),
(4, 'Hardik Verma', 'hardik.v@cricket.com', '+91 98765 43212', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'PLAYER', 'player_103.png', 'Bengaluru, KA', 'token_player_hardik'),
(5, 'Rishabh Singh', 'rishabh.s@cricket.com', '+91 98765 43213', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'PLAYER', 'player_104.png', 'Delhi, NCR', 'token_player_rishabh'),
(6, 'Ravindra Yadav', 'ravindra.y@cricket.com', '+91 98765 43214', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'PLAYER', 'player_105.png', 'Chennai, TN', 'token_player_ravindra'),
(7, 'Suresh Menon', 'suresh.menon@cricket.com', '+91 91234 56789', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'UMPIRE', 'umpire_201.png', 'Mumbai, MH', 'token_umpire_suresh'),
(8, 'Rajesh Gupta', 'rajesh.gupta@cricket.com', '+91 91234 56790', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'UMPIRE', 'umpire_202.png', 'Delhi, NCR', 'token_umpire_rajesh'),
(9, 'Anil Kumar', 'anil.kumar@cricket.com', '+91 91234 56791', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'UMPIRE', 'umpire_203.png', 'Bengaluru, KA', 'token_umpire_anil'),
(10, 'Official Scorer', 'scorer@cricket.com', '+91 91234 99999', '$2y$10$6HuOv74uEYNdS.L/2uoPqOX/aAwA1PLl6pCIV8EPiPiQJtUThUUgG', 'SCORER', 'scorer_default.png', 'Mumbai, MH', 'token_scorer_official');

-- 2. Player Profiles
INSERT INTO `player_profiles` (`id`, `user_id`, `playing_role`, `batting_style`, `bowling_style`, `experience`, `matches_played`, `runs`, `wickets`, `price_per_match`, `rating`, `bio`, `city`, `availability_status`) VALUES
(1, 2, 'Batsman', 'Right-Handed', 'Right-Arm Medium', '10+ Years', 85, 3420, 12, 1800, 4.9, 'Aggressive top-order batsman with 10+ years of corporate & club cricket experience.', 'Mumbai, MH', 'Available'),
(2, 3, 'Bowler', 'Right-Handed', 'Right-Arm Fast', '7+ Years', 72, 450, 145, 2000, 4.8, 'Pace bowler specializing in death overs, yorkers, and cutter variations.', 'Ahmedabad, GJ', 'Available'),
(3, 4, 'All-Rounder', 'Right-Handed', 'Right-Arm Medium Fast', '9+ Years', 94, 2100, 98, 2200, 4.7, 'Hard-hitting middle-order power hitter and reliable seam bowler.', 'Bengaluru, KA', 'Available'),
(4, 5, 'Wicket Keeper', 'Left-Handed', 'None', '6+ Years', 64, 1890, 0, 1600, 4.6, 'Dynamic wicket-keeper batsman with quick stumpings and explosive batting.', 'Delhi, NCR', 'Booked'),
(5, 6, 'All-Rounder', 'Left-Handed', 'Slow Left-Arm Orthodox', '11+ Years', 110, 2800, 130, 2500, 4.9, 'Left-arm spin bowler and lower-order finisher. Exceptional fielder.', 'Chennai, TN', 'Available');

-- 3. Umpire Profiles
INSERT INTO `umpire_profiles` (`id`, `user_id`, `certification`, `experience_years`, `matches_officiated`, `rating`, `match_fee`, `availability_status`, `bio`, `city`) VALUES
(1, 7, 'ICC Accredited', 12, 240, 4.9, 3000, 'Available', 'Senior ICC certified umpire with extensive experience in A-division and T20 tournaments.', 'Mumbai, MH'),
(2, 8, 'National Certified', 8, 150, 4.7, 2200, 'Available', 'BCCI level-2 certified umpire with sharp decision-making and match management skills.', 'Delhi, NCR'),
(3, 9, 'State Level', 5, 95, 4.6, 1500, 'Booked', 'State panel umpire specializing in corporate cups and box cricket leagues.', 'Bengaluru, KA');

-- 4. Tournaments
INSERT INTO `tournaments` (`id`, `organizer_id`, `tournament_name`, `tournament_type`, `location`, `start_date`, `end_date`, `entry_fee`, `prize_money`, `max_teams`, `registered_teams_count`, `status`, `organizer_name`, `organizer_contact`, `description`) VALUES
(1, 1, 'Premier Corporate T20 Cup 2026', 'T20', 'Wankhede Stadium Turf, Mumbai', '15 Mar 2026', '22 Mar 2026', 15000, 150000, 16, 12, 'UPCOMING', 'Mumbai Cricket Club', '+91 98200 11223', 'Grand T20 tournament open for corporate and private clubs with live streaming and official umpires.'),
(2, 1, 'Bengaluru Night Box Premier League', 'Box Cricket', 'PlayArena Turf, Sarjapur', '01 Mar 2026', '05 Mar 2026', 5000, 50000, 12, 10, 'ONGOING', 'Silicon Turf Sports', '+91 98450 99887', '8-a-side floodlight box cricket tournament with high stakes and exciting prizes.'),
(3, 1, 'Delhi Summer ODI Challenge', 'ODI', 'Feroz Shah Grounds, New Delhi', '10 Feb 2026', '20 Feb 2026', 20000, 200000, 8, 8, 'COMPLETED', 'Capital Cricket Association', '+91 98110 44556', '50-over leather ball tournament featuring top regional club teams.');

-- 5. Teams
INSERT INTO `teams` (`id`, `tournament_id`, `team_name`, `captain_id`, `captain_name`, `captain_phone`, `captain_email`, `city`) VALUES
(1, 1, 'Mumbai Warriors', 2, 'Virat Sharma', '+91 98765 43210', 'virat.sharma@cricket.com', 'Mumbai'),
(2, 1, 'Strikers XI', NULL, 'Rohan Mehta', '+91 98222 33445', 'rohan@strikers.com', 'Mumbai'),
(3, 1, 'Royal Super Kings', 4, 'Hardik Verma', '+91 98765 43212', 'hardik.v@cricket.com', 'Bengaluru'),
(4, 1, 'Titan Chargers', NULL, 'Kunal Kapoor', '+91 98111 22334', 'kunal@chargers.com', 'Pune'),
(5, 2, 'Bangalore Blasters', NULL, 'Sunil Gowda', '+91 98455 66778', 'sunil@blasters.com', 'Bengaluru'),
(6, 2, 'Techie Eleven', NULL, 'Arun Prakash', '+91 98455 11223', 'arun@techie11.com', 'Bengaluru');

-- 6. Team Players
INSERT INTO `team_players` (`team_id`, `player_id`, `player_name`, `is_captain`) VALUES
(1, 2, 'Virat Sharma', 1),
(1, 3, 'Jasprit Patel', 0),
(1, 6, 'Ravindra Yadav', 0),
(2, NULL, 'Rohan Mehta', 1),
(2, 5, 'Rishabh Singh', 0),
(3, 4, 'Hardik Verma', 1),
(5, NULL, 'Sunil Gowda', 1),
(6, NULL, 'Arun Prakash', 1);

-- 7. Matches
INSERT INTO `matches` (`id`, `tournament_id`, `match_number`, `team1_id`, `team2_id`, `team1_name`, `team2_name`, `match_date`, `match_time`, `venue`, `match_type`, `stage`, `status`, `toss_winner_id`, `toss_decision`, `winner_id`, `result`, `score_a`, `score_b`) VALUES
(1, 1, 1, 1, 2, 'Mumbai Warriors', 'Strikers XI', '15 Mar 2026', '10:00 AM', 'Wankhede Pitch A', 'T20', 'Group Stage', 'UPCOMING', NULL, NULL, NULL, 'Scheduled', '0/0 (0.0)', '0/0 (0.0)'),
(2, 1, 2, 3, 4, 'Royal Super Kings', 'Titan Chargers', '15 Mar 2026', '02:00 PM', 'Wankhede Pitch B', 'T20', 'Group Stage', 'UPCOMING', NULL, NULL, NULL, 'Scheduled', '0/0 (0.0)', '0/0 (0.0)'),
(3, 2, 5, 5, 6, 'Bangalore Blasters', 'Techie Eleven', '02 Mar 2026', '07:00 PM', 'PlayArena Arena 1', 'Box Cricket', 'Quarter Final', 'COMPLETED', 5, 'BAT', 5, 'Bangalore Blasters won by 14 runs', '98/4 (8.0)', '84/6 (8.0)');

-- 8. Bookings
INSERT INTO `bookings` (`id`, `user_id`, `item_type`, `item_id`, `item_name`, `booking_date`, `start_time`, `end_time`, `venue`, `match_type`, `amount`, `notes`, `status`) VALUES
(1, 1, 'PLAYER', 2, 'Virat Sharma (Batsman)', '18 Mar 2026', '04:00 PM', '08:00 PM', 'MCC Turf Ground, Chembur', 'T20', 1800, 'Need middle-order batsman for semi-final match.', 'CONFIRMED'),
(2, 1, 'UMPIRE', 7, 'Suresh Menon (ICC Accredited)', '20 Mar 2026', '09:00 AM', '01:00 PM', 'Gymkhana Ground, Marine Lines', 'T20', 3000, 'Official final match officiating required.', 'PENDING');

-- 9. Match Innings (for Match 1 default setup, and Match 3 completed setup)
INSERT INTO `match_innings` (`id`, `match_id`, `innings_number`, `batting_team_id`, `bowling_team_id`, `batting_team_name`, `bowling_team_name`, `total_runs`, `wickets`, `legal_balls`, `overs`, `extras`, `status`) VALUES
(1, 1, 1, 1, 2, 'Mumbai Warriors', 'Strikers XI', 0, 0, 0, 0.0, 0, 'NOT_STARTED'),
(2, 1, 2, 2, 1, 'Strikers XI', 'Mumbai Warriors', 0, 0, 0, 0.0, 0, 'NOT_STARTED'),
(3, 3, 1, 5, 6, 'Bangalore Blasters', 'Techie Eleven', 98, 4, 48, 8.0, 6, 'COMPLETED'),
(4, 3, 2, 6, 5, 'Techie Eleven', 'Bangalore Blasters', 84, 6, 48, 8.0, 4, 'COMPLETED');

-- 10. Sample Ball-by-ball for completed match 3
INSERT INTO `ball_by_ball` (`innings_id`, `match_id`, `over_number`, `ball_number`, `is_legal_ball`, `batsman_name`, `bowler_name`, `runs`, `extras`, `extra_type`, `wicket`, `wicket_type`, `player_dismissed`, `commentary`) VALUES
(3, 3, 0, 1, 1, 'Sunil Gowda', 'Arun Prakash', 1, 0, 'NONE', 0, 'NONE', NULL, 'Innings 1: 1 run taken towards mid-wicket.'),
(3, 3, 0, 2, 1, 'Sunil Gowda', 'Arun Prakash', 4, 0, 'NONE', 0, 'NONE', NULL, 'Innings 1: BOUNDARY! Cracked through covers for 4.'),
(3, 3, 0, 3, 1, 'Sunil Gowda', 'Arun Prakash', 6, 0, 'NONE', 0, 'NONE', NULL, 'Innings 1: SIX! Loosened arms and lofted over long-on.'),
(3, 3, 0, 4, 1, 'Sunil Gowda', 'Arun Prakash', 0, 0, 'NONE', 1, 'BOWLED', 'Sunil Gowda', 'Innings 1: WICKET (BOWLED)! Stumps shattered by a full yorker.');

-- 11. User Notifications
INSERT INTO `user_notifications` (`user_id`, `title`, `message`, `is_read`) VALUES
(1, 'Booking Confirmed', 'Your booking with Virat Sharma on 18 Mar 2026 has been confirmed.', 0),
(1, 'Tournament Starting Soon', 'Premier Corporate T20 Cup 2026 begins on 15 Mar 2026.', 0),
(2, 'New Booking Request', 'You have been booked by Rahul Dravid for MCC Turf Ground match on 18 Mar 2026.', 1);
