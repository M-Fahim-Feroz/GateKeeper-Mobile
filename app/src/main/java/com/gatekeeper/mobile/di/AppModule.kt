package com.gatekeeper.mobile.di

import android.content.Context
import androidx.room.Room
import com.gatekeeper.mobile.data.db.AppDatabase
import com.gatekeeper.mobile.data.db.dao.ConnectionLogDao
import com.gatekeeper.mobile.data.db.dao.DnsBlocklistDao
import com.gatekeeper.mobile.data.db.dao.FirewallRuleDao
import com.gatekeeper.mobile.data.db.dao.IpRuleDao
import com.gatekeeper.mobile.data.db.dao.ThreatFeedDao
import com.gatekeeper.mobile.data.remote.AiApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // â”€â”€ Database â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gatekeeper.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideFirewallRuleDao(db: AppDatabase): FirewallRuleDao = db.firewallRuleDao()
    @Provides fun provideDnsBlocklistDao(db: AppDatabase): DnsBlocklistDao = db.dnsBlocklistDao()
    @Provides fun provideConnectionLogDao(db: AppDatabase): ConnectionLogDao = db.connectionLogDao()
    @Provides fun provideIpRuleDao(db: AppDatabase): IpRuleDao = db.ipRuleDao()
    @Provides fun provideThreatFeedDao(db: AppDatabase): ThreatFeedDao = db.threatFeedDao()

    // â”€â”€ Network â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Provides
    @Singleton
    fun provideOkHttpClient(settingsRepository: com.gatekeeper.mobile.data.repository.SettingsRepository): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                var request = chain.request()
                // Synchronously read the dynamically configured IP address
                val dynamicIp: String? = kotlinx.coroutines.runBlocking { 
                    settingsRepository.backendIpFlow.firstOrNull()
                }

                if (!dynamicIp.isNullOrBlank()) {
                    try {
                        val newHost = okhttp3.HttpUrl.Builder()
                            .scheme("http")
                            .host(dynamicIp) // e.g. "192.168.1.5"
                            .port(8888)
                            .build()

                        val newUrl = request.url.newBuilder()
                            .scheme(newHost.scheme)
                            .host(newHost.host)
                            .port(newHost.port)
                            .build()

                        request = request.newBuilder()
                            .url(newUrl)
                            .build()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                chain.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        // Base URL will be overridden per-request based on user's server config.
        // Default to 10.0.2.2 (Android Emulator host loopback)
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8888/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAiApiService(retrofit: Retrofit): AiApiService {
        return retrofit.create(AiApiService::class.java)
    }
}
