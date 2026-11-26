package com.example.twelveman.data.api

import com.example.twelveman.data.api.models.ApiResponse
import com.example.twelveman.data.api.models.AuthResponse
import com.example.twelveman.data.api.models.LoginRequest
import com.example.twelveman.data.api.models.RegisterRequest
import com.example.twelveman.data.model.Booking
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.Player
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.Tournament
import com.example.twelveman.data.model.Umpire
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    // Auth & Diagnostic
    @POST("api/auth/register.php")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login.php")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/profile.php")
    suspend fun getProfile(): Response<AuthResponse>

    @GET("test_db.php")
    suspend fun testDb(): Response<Map<String, Any>>

    // Players
    @GET("api/players/get_players.php")
    suspend fun getPlayers(): Response<ApiResponse<List<Player>>>

    // Umpires
    @GET("api/umpires/get_umpires.php")
    suspend fun getUmpires(): Response<ApiResponse<List<Umpire>>>

    // Tournaments & Matches
    @GET("api/tournaments/get_tournaments.php")
    suspend fun getTournaments(): Response<ApiResponse<List<Tournament>>>

    @POST("api/tournaments/create_tournament.php")
    suspend fun createTournament(@Body tournament: Tournament): Response<ApiResponse<Tournament>>

    @POST("api/tournaments/register_team.php")
    suspend fun registerTeam(@Body team: Team): Response<ApiResponse<Map<String, Any>>>

    @GET("api/tournaments/get_matches.php")
    suspend fun getMatches(@Query("tournament_id") tournamentId: String = ""): Response<ApiResponse<List<Match>>>

    @POST("api/tournaments/update_match.php")
    suspend fun updateMatch(@Body matchUpdate: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>

    @GET("api/tournaments/get_live_match.php")
    suspend fun getLiveMatch(@Query("match_id") matchId: String = ""): Response<ApiResponse<Match>>

    // Tournament Organizer Endpoints
    @GET("api/tournaments/get_organizer_dashboard.php")
    suspend fun getOrganizerDashboard(@Query("tournament_id") tournamentId: String = ""): Response<ApiResponse<com.example.twelveman.data.model.OrganizerDashboardData>>

    @GET("api/tournaments/get_teams.php")
    suspend fun getTournamentTeams(@Query("tournament_id") tournamentId: String = ""): Response<ApiResponse<List<Team>>>

    @POST("api/tournaments/manage_team.php")
    suspend fun manageTeam(@Body request: com.example.twelveman.data.model.ManageTeamRequest): Response<ApiResponse<Map<String, Any>>>

    @POST("api/tournaments/generate_fixtures.php")
    suspend fun generateFixtures(@Body request: com.example.twelveman.data.model.GenerateFixturesRequest): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("api/tournaments/get_standings.php")
    suspend fun getTournamentStandings(@Query("tournament_id") tournamentId: String = ""): Response<ApiResponse<List<com.example.twelveman.data.model.TeamStanding>>>

    // Bookings
    @GET("api/bookings/get_bookings.php")
    suspend fun getBookings(): Response<ApiResponse<List<Booking>>>

    @POST("api/bookings/create_booking.php")
    suspend fun createBooking(@Body booking: Booking): Response<ApiResponse<Booking>>

    @POST("api/bookings/cancel_booking.php")
    suspend fun cancelBooking(@Body body: Map<String, String>): Response<ApiResponse<Map<String, Any>>>
}
