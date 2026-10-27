package com.example.twelveman.data.local

import com.example.twelveman.data.model.*

object MockData {
    val players = mutableListOf(
        Player(
            id = "P101",
            name = "Virat Sharma",
            role = "Batsman",
            rating = 4.9,
            matchesPlayed = 85,
            runsScored = 3420,
            wicketsTaken = 12,
            pricePerMatch = 1800,
            location = "Mumbai, MH",
            availabilityStatus = "Available",
            bio = "Aggressive top-order batsman with 10+ years of corporate & club cricket experience.",
            phone = "+91 98765 43210",
            email = "virat.sharma@cricket.com"
        ),
        Player(
            id = "P102",
            name = "Jasprit Patel",
            role = "Bowler",
            rating = 4.8,
            matchesPlayed = 72,
            runsScored = 450,
            wicketsTaken = 145,
            pricePerMatch = 2000,
            location = "Ahmedabad, GJ",
            availabilityStatus = "Available",
            bio = "Pace bowler specializing in death overs, yorkers, and cutter variations.",
            phone = "+91 98765 43211",
            email = "jasprit.p@cricket.com"
        ),
        Player(
            id = "P103",
            name = "Hardik Verma",
            role = "All-Rounder",
            rating = 4.7,
            matchesPlayed = 94,
            runsScored = 2100,
            wicketsTaken = 98,
            pricePerMatch = 2200,
            location = "Bengaluru, KA",
            availabilityStatus = "Available",
            bio = "Hard-hitting middle-order power hitter and reliable seam bowler.",
            phone = "+91 98765 43212",
            email = "hardik.v@cricket.com"
        ),
        Player(
            id = "P104",
            name = "Rishabh Singh",
            role = "Wicket Keeper",
            rating = 4.6,
            matchesPlayed = 64,
            runsScored = 1890,
            wicketsTaken = 0,
            pricePerMatch = 1600,
            location = "Delhi, NCR",
            availabilityStatus = "Booked",
            bio = "Dynamic wicket-keeper batsman with quick stumpings and explosive batting.",
            phone = "+91 98765 43213",
            email = "rishabh.s@cricket.com"
        ),
        Player(
            id = "P105",
            name = "Ravindra Yadav",
            role = "All-Rounder",
            rating = 4.9,
            matchesPlayed = 110,
            runsScored = 2800,
            wicketsTaken = 130,
            pricePerMatch = 2500,
            location = "Chennai, TN",
            availabilityStatus = "Available",
            bio = "Left-arm spin bowler and lower-order finisher. Exceptional fielder.",
            phone = "+91 98765 43214",
            email = "ravindra.y@cricket.com"
        )
    )

    val umpires = mutableListOf(
        Umpire(
            id = "U201",
            name = "Suresh Menon",
            experienceYears = 12,
            matchesOfficiated = 240,
            rating = 4.9,
            certificationLevel = "ICC Accredited",
            pricePerMatch = 3000,
            location = "Mumbai, MH",
            availabilityStatus = "Available",
            bio = "Senior ICC certified umpire with extensive experience in A-division and T20 tournaments.",
            phone = "+91 91234 56789"
        ),
        Umpire(
            id = "U202",
            name = "Rajesh Gupta",
            experienceYears = 8,
            matchesOfficiated = 150,
            rating = 4.7,
            certificationLevel = "National Certified",
            pricePerMatch = 2200,
            location = "Delhi, NCR",
            availabilityStatus = "Available",
            bio = "BCCI level-2 certified umpire with sharp decision-making and match management skills.",
            phone = "+91 91234 56790"
        ),
        Umpire(
            id = "U203",
            name = "Anil Kumar",
            experienceYears = 5,
            matchesOfficiated = 95,
            rating = 4.6,
            certificationLevel = "State Level",
            pricePerMatch = 1500,
            location = "Bengaluru, KA",
            availabilityStatus = "Booked",
            bio = "State panel umpire specializing in corporate cups and box cricket leagues.",
            phone = "+91 91234 56791"
        )
    )

    val tournaments = mutableListOf(
        Tournament(
            id = "T301",
            name = "Premier Corporate T20 Cup 2026",
            format = "T20",
            location = "Wankhede Stadium Turf, Mumbai",
            startDate = "15 Mar 2026",
            endDate = "22 Mar 2026",
            totalTeams = 16,
            registeredTeamsCount = 12,
            entryFee = 15000,
            prizeMoney = 150000,
            status = "UPCOMING",
            organizerName = "Mumbai Cricket Club",
            organizerContact = "+91 98200 11223",
            description = "Grand T20 tournament open for corporate and private clubs with live streaming and official umpires."
        ),
        Tournament(
            id = "T302",
            name = "Bengaluru Night Box Premier League",
            format = "Box Cricket",
            location = "PlayArena Turf, Sarjapur",
            startDate = "01 Mar 2026",
            endDate = "05 Mar 2026",
            totalTeams = 12,
            registeredTeamsCount = 10,
            entryFee = 5000,
            prizeMoney = 50000,
            status = "ONGOING",
            organizerName = "Silicon Turf Sports",
            organizerContact = "+91 98450 99887",
            description = "8-a-side floodlight box cricket tournament with high stakes and exciting prizes."
        ),
        Tournament(
            id = "T303",
            name = "Delhi Summer ODI Challenge",
            format = "ODI",
            location = "Feroz Shah Grounds, New Delhi",
            startDate = "10 Feb 2026",
            endDate = "20 Feb 2026",
            totalTeams = 8,
            registeredTeamsCount = 8,
            entryFee = 20000,
            prizeMoney = 200000,
            status = "COMPLETED",
            organizerName = "Capital Cricket Association",
            organizerContact = "+91 98110 44556",
            description = "50-over leather ball tournament featuring top regional club teams."
        )
    )

    val bookings = mutableListOf(
        Booking(
            id = "B401",
            bookingType = "PLAYER",
            itemId = "P101",
            itemName = "Virat Sharma (Batsman)",
            userEmail = "user@example.com",
            date = "18 Mar 2026",
            timeSlot = "04:00 PM - 08:00 PM",
            venue = "MCC Turf Ground, Chembur",
            matchType = "T20",
            status = "CONFIRMED",
            amount = 1800,
            notes = "Need middle-order batsman for semi-final match."
        ),
        Booking(
            id = "B402",
            bookingType = "UMPIRE",
            itemId = "U201",
            itemName = "Suresh Menon (ICC Accredited)",
            userEmail = "user@example.com",
            date = "20 Mar 2026",
            timeSlot = "09:00 AM - 01:00 PM",
            venue = "Gymkhana Ground, Marine Lines",
            matchType = "T20",
            status = "PENDING",
            amount = 3000,
            notes = "Official final match officiating required."
        )
    )

    val matches = mutableListOf(
        Match(
            id = "M501",
            tournamentId = "T301",
            matchNumber = 1,
            teamA = "Mumbai Warriors",
            teamB = "Strikers XI",
            date = "15 Mar 2026",
            time = "10:00 AM",
            venue = "Wankhede Pitch A",
            stage = "Group Stage",
            status = "UPCOMING",
            result = "Scheduled",
            scoreA = "-",
            scoreB = "-"
        ),
        Match(
            id = "M502",
            tournamentId = "T301",
            matchNumber = 2,
            teamA = "Royal Super Kings",
            teamB = "Titan Chargers",
            date = "15 Mar 2026",
            time = "02:00 PM",
            venue = "Wankhede Pitch B",
            stage = "Group Stage",
            status = "UPCOMING",
            result = "Scheduled",
            scoreA = "-",
            scoreB = "-"
        ),
        Match(
            id = "M503",
            tournamentId = "T302",
            matchNumber = 5,
            teamA = "Bangalore Blasters",
            teamB = "Techie Eleven",
            date = "02 Mar 2026",
            time = "07:00 PM",
            venue = "PlayArena Arena 1",
            stage = "Quarter Final",
            status = "COMPLETED",
            result = "Bangalore Blasters won by 14 runs",
            scoreA = "98/4 (8.0)",
            scoreB = "84/6 (8.0)"
        )
    )

    val currentUser = User(
        id = "U100",
        name = "Rahul Dravid",
        email = "user@example.com",
        phone = "+91 99000 11222",
        role = "ORGANIZER"
    )
}