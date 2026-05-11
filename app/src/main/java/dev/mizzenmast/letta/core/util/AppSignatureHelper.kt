package dev.mizzenmast.letta.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Computes the 11-character app signature hash required by the SMS Retriever API.
 *
 * Your SMS template must end with this hash on its own line, preceded by a newline:
 * ```
 * <#> Letta verification code: 123456
 * AB12De3f79h
 * ```
 * (where `AB12De3f79h` is the value returned by [getSignatureHash])
 *
 * In production you should hard-code the hash from your release keystore.
 * Use this helper only in debug/development to discover the hash.
 *
 * NOTE: Google Play App Signing produces a DIFFERENT certificate than your local
 * keystore. If you use Play App Signing, get the hash from:
 *   Play Console → Setup → App integrity → App signing certificate → SHA-256 fingerprint
 * then convert it: take the raw SHA-256 bytes and run them through the algorithm below.
 *
 * Usage (e.g. in a debug screen or logged once on first launch):
 * ```kotlin
 * val hash = AppSignatureHelper.getSignatureHash(context)
 * Log.d("SMS", "App hash: $hash")
 * ```
 */
object AppSignatureHelper {

    private const val TAG = "AppSignatureHelper"
    private const val HASH_TYPE = "SHA-256"
    private const val NUM_HASHES = 1

    /**
     * Returns the 11-character hash string, or null if the signature cannot be read.
     * On debug builds this uses the debug keystore certificate.
     */
    fun getSignatureHash(context: Context): String? {
        return try {
            val packageName = context.packageName
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES,
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures ?: emptyArray()
            }

            signatures.mapNotNull { sig -> computeHash(packageName, sig.toByteArray()) }.firstOrNull()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get signature hash", e)
            null
        }
    }

    /**
     * Computes the SMS Retriever hash for a given [packageName] and raw [signatureBytes].
     *
     * Algorithm (from Google's SMS Retriever sample):
     *  1. SHA-256 hash the signature bytes
     *  2. Encode result as base64 (no padding, no wrap)
     *  3. Form the string: "<packageName> <base64_of_sha256>"
     *  4. SHA-256 hash that combined string
     *  5. Base64-encode the result (no padding, no wrap)
     *  6. Take the first 11 characters → this is the hash
     */
    private fun computeHash(packageName: String, signatureBytes: ByteArray): String? {
        return try {
            val md = MessageDigest.getInstance(HASH_TYPE)

            // Step 1+2: SHA-256 the cert, base64 encode
            md.update(signatureBytes)
            val sigHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP or Base64.NO_PADDING)

            // Step 3+4: SHA-256 the combined string
            val combined = "$packageName $sigHash"
            md.reset()
            md.update(combined.toByteArray(Charsets.UTF_8))
            val finalHash = md.digest()

            // Step 5+6: base64 encode, take first 11 chars
            val encoded = Base64.encodeToString(finalHash, Base64.NO_WRAP or Base64.NO_PADDING)
            encoded.substring(0, NUM_HASHES * 11).also {
                Log.i(TAG, "App SMS signature hash: $it")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "SHA-256 not available", e)
            null
        }
    }
}
