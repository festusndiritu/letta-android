package dev.mizzenmast.letta.data.remote

import dev.mizzenmast.letta.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.accessToken
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        // On 401 — attempt token refresh once
        if (response.code == 401) {
            val refreshToken = tokenStore.refreshToken ?: return response

            response.close()

            val newTokens = runBlocking {
                try {
                    // We can't inject AuthApiService here (circular dep) so we
                    // make a raw OkHttp call for the refresh
                    val refreshResponse = chain.proceed(
                        chain.request().newBuilder()
                            .url(chain.request().url.newBuilder()
                                .encodedPath("/auth/refresh")
                                .build())
                            .post(
                                """{"refresh_token":"$refreshToken"}"""
                                    .toRequestBody(
                                        "application/json"
                                            .toMediaTypeOrNull()
                                    )
                            )
                            .build()
                    )
                    if (refreshResponse.isSuccessful) {
                        val body = refreshResponse.body.string()
                        refreshResponse.close()
                        body
                    } else {
                        refreshResponse.close()
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            if (newTokens != null) {
                // Parse new tokens and retry
                try {
                    val accessToken = Regex(""""access_token"\s*:\s*"([^"]+)"""")
                        .find(newTokens)?.groupValues?.get(1)
                    val newRefreshToken = Regex(""""refresh_token"\s*:\s*"([^"]+)"""")
                        .find(newTokens)?.groupValues?.get(1)

                    if (accessToken != null && newRefreshToken != null) {
                        tokenStore.accessToken = accessToken
                        tokenStore.refreshToken = newRefreshToken

                        return chain.proceed(
                            chain.request().newBuilder()
                                .header("Authorization", "Bearer $accessToken")
                                .build()
                        )
                    }
                } catch (_: Exception) { }
            }

            // Refresh failed — clear tokens so app redirects to login
            tokenStore.clear()
        }

        return response
    }
}