package com.example.pcunlocker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class UnlockClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    // Signal bhejne ka main function
    suspend fun sendUnlockRequest(ip: String, secretKey: String): Result<String> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val payloadData = "$ip:$timestamp:unlock"
        val signature = CryptoUtils.generateHmacSha256(payloadData, secretKey)

        val json = JSONObject().apply {
            put("action", "unlock")
            put("timestamp", timestamp)
            put("signature", signature)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("http://$ip:5000/unlock")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("PC Unlocked Successfully!")
                } else {
                    Result.failure(IOException("Server Error: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Connection Failed ($ip:5000): ${e.localizedMessage}"))
        }
    }
}