package dev.mizzenmast.letta.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.mizzenmast.letta.BuildConfig
import dev.mizzenmast.letta.data.remote.AuthInterceptor
import dev.mizzenmast.letta.data.remote.api.AuthApiService
import dev.mizzenmast.letta.data.remote.api.ConversationApiService
import dev.mizzenmast.letta.data.remote.api.MediaApiService
import dev.mizzenmast.letta.data.remote.api.MetaApiService
import dev.mizzenmast.letta.data.remote.api.StatusApiService
import dev.mizzenmast.letta.data.remote.api.CallApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            // Use HEADERS instead of BODY to avoid logging sensitive data
                            level = HttpLoggingInterceptor.Level.HEADERS
                        }
                    )
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideConversationApiService(retrofit: Retrofit): ConversationApiService =
        retrofit.create(ConversationApiService::class.java)

    @Provides
    @Singleton
    fun provideMediaApiService(retrofit: Retrofit): MediaApiService =
        retrofit.create(MediaApiService::class.java)

    @Provides
    @Singleton
    fun provideMetaApiService(retrofit: Retrofit): MetaApiService =
        retrofit.create(MetaApiService::class.java)

    @Provides
    @Singleton
    fun provideStatusApiService(retrofit: Retrofit): StatusApiService =
        retrofit.create(StatusApiService::class.java)

    @Provides
    @Singleton
    fun provideCallApiService(retrofit: Retrofit): CallApiService =
        retrofit.create(CallApiService::class.java)
}