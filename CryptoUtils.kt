package com.example.pcunlocker

import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    // Secret Key aur Payload se HMAC-SHA256 signature generate karta hai
    fun generateHmacSha256(data: String, secretKey: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), algorithm)
        mac.init(keySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { String.format(Locale.US, "%02x", it) }
    }
}