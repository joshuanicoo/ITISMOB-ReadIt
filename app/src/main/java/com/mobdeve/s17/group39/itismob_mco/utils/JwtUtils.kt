package com.mobdeve.s17.group39.itismob_mco.utils

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object JwtUtils {

    private const val SECRET_KEY = "your-secret-key-change-in-production-mobile-book-app-2024"
    private const val ALGORITHM = "HmacSHA256"
    private val gson = Gson()

    data class JwtPayload(
        val userId: String,
        val email: String,
        val username: String,
        val iat: Long, // issued at
        val exp: Long  // expiration
    )

    fun createToken(userId: String, email: String, username: String): String {
        val header = mapOf(
            "alg" to "HS256",
            "typ" to "JWT"
        )

        val now = System.currentTimeMillis() / 1000
        val expiration = now + (24 * 60 * 60) // 24 hours

        val payload = JwtPayload(
            userId = userId,
            email = email,
            username = username,
            iat = now,
            exp = expiration
        )

        val headerBase64 = base64Encode(gson.toJson(header).toByteArray())
        val payloadBase64 = base64Encode(gson.toJson(payload).toByteArray())

        val signature = createSignature("$headerBase64.$payloadBase64")

        return "$headerBase64.$payloadBase64.$signature"
    }

    fun verifyToken(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            val signature = createSignature("${parts[0]}.${parts[1]}")
            if (signature != parts[2]) return false

            val payloadJson = String(base64Decode(parts[1]))
            val payload = gson.fromJson(payloadJson, JwtPayload::class.java)

            // Check if token is expired
            val now = System.currentTimeMillis() / 1000
            payload.exp > now

        } catch (e: Exception) {
            Log.e("JwtUtils", "Token verification failed", e)
            false
        }
    }

    fun getPayload(token: String): JwtPayload? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payloadJson = String(base64Decode(parts[1]))
            gson.fromJson(payloadJson, JwtPayload::class.java)
        } catch (e: Exception) {
            Log.e("JwtUtils", "Failed to get payload", e)
            null
        }
    }

    fun getUserIdFromToken(token: String): String? {
        return getPayload(token)?.userId
    }

    fun getEmailFromToken(token: String): String? {
        return getPayload(token)?.email
    }

    fun getUsernameFromToken(token: String): String? {
        return getPayload(token)?.username
    }

    private fun createSignature(data: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)
        mac.init(secretKeySpec)
        val signature = mac.doFinal(data.toByteArray())
        return base64Encode(signature)
    }

    private fun base64Encode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun base64Decode(base64String: String): ByteArray {
        return Base64.decode(base64String, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}