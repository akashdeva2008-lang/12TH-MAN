package com.example.twelveman.data.api.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "PLAYER"
)

data class PlayerRegistrationRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("dob") val dob: String,
    @SerializedName("city") val city: String,
    @SerializedName("profile_image") val profileImage: String = "",
    @SerializedName("playing_role") val playingRole: String,
    @SerializedName("batting_style") val battingStyle: String,
    @SerializedName("bowling_style") val bowlingStyle: String,
    @SerializedName("experience") val experience: String,
    @SerializedName("preferred_format") val preferredFormat: String,
    @SerializedName("availability_status") val availabilityStatus: String,
    @SerializedName("bio") val bio: String
)

data class UmpireRegistrationRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("dob") val dob: String,
    @SerializedName("city") val city: String,
    @SerializedName("profile_image") val profileImage: String = "",
    @SerializedName("experience_years") val experienceYears: Int,
    @SerializedName("certification") val certification: String,
    @SerializedName("matches_officiated") val matchesOfficiated: Int,
    @SerializedName("preferred_formats") val preferredFormats: String,
    @SerializedName("availability_status") val availabilityStatus: String,
    @SerializedName("languages_known") val languagesKnown: String,
    @SerializedName("bio") val bio: String
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponseData(
    @SerializedName("id") val id: Int,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String,
    @SerializedName("profile_image") val profileImage: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("token_expiry") val tokenExpiry: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AuthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: AuthResponseData?,
    @SerializedName("errors") val errors: List<String>? = null
)

