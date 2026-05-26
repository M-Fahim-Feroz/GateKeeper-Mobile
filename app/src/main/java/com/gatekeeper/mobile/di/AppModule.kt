package com.gatekeeper.mobile.di

import android.content.Context
import androidx.room.Room
import com.gatekeeper.mobile.data.db.AppDatabase
import com.gatekeeper.mobile.data.db.dao.ConnectionLogDao
import com.gatekeeper.mobile.data.db.dao.DnsBlocklistDao
import com.gatekeeper.mobile.data.db.dao.FirewallRuleDao
import com.gatekeeper.mobile.data.db.dao.IpRuleDao
import com.gatekeeper.mobile.data.db.dao.ThreatFeedDao
import com.gatekeeper.mobile.data.db.dao.KnownNetworkDao
import com.gatekeeper.mobile.data.db.dao.SensorLogDao
import com.gatekeeper.mobile.data.db.dao.SecurityAlertDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS known_networks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    ssid TEXT NOT NULL,
                    bssid TEXT NOT NULL,
                    securityType TEXT NOT NULL,
                    firstSeenAt INTEGER NOT NULL,
                    lastSeenAt INTEGER NOT NULL,
                    isTrusted INTEGER NOT NULL
                )
            """)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sensor_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    packageName TEXT NOT NULL,
                    appName TEXT NOT NULL,
                    sensorType TEXT NOT NULL,
                    startedAt INTEGER NOT NULL,
                    durationMs INTEGER NOT NULL,
                    isBackground INTEGER NOT NULL
                )
            """)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS security_alerts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    packageName TEXT,
                    timestamp INTEGER NOT NULL,
                    isResolved INTEGER NOT NULL
                )
            """)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS blocklist_subscriptions (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    url TEXT NOT NULL,
                    type TEXT NOT NULL,
                    isEnabled INTEGER NOT NULL DEFAULT 1,
                    lastRefreshedAt INTEGER NOT NULL DEFAULT 0,
                    domainCount INTEGER NOT NULL DEFAULT 0
                )
            """)
        }
    }

    // v7: Add blockWhenScreenOff column to firewall_rules (F8 per-app screen-off blocking)
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE firewall_rules ADD COLUMN blockWhenScreenOff INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE connection_logs ADD COLUMN isSystemEvent INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE connection_logs ADD COLUMN systemEventReason TEXT")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE firewall_rules ADD COLUMN blockScheduleEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE firewall_rules ADD COLUMN blockStartMinutes INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE firewall_rules ADD COLUMN blockEndMinutes INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gatekeeper.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .build()
    }

    @Provides fun provideFirewallRuleDao(db: AppDatabase): FirewallRuleDao = db.firewallRuleDao()
    @Provides fun provideDnsBlocklistDao(db: AppDatabase): DnsBlocklistDao = db.dnsBlocklistDao()
    @Provides fun provideConnectionLogDao(db: AppDatabase): ConnectionLogDao = db.connectionLogDao()
    @Provides fun provideIpRuleDao(db: AppDatabase): IpRuleDao = db.ipRuleDao()
    @Provides fun provideThreatFeedDao(db: AppDatabase): ThreatFeedDao = db.threatFeedDao()
    @Provides fun provideKnownNetworkDao(db: AppDatabase): KnownNetworkDao = db.knownNetworkDao()
    @Provides fun provideSensorLogDao(db: AppDatabase): SensorLogDao = db.sensorLogDao()
    @Provides fun provideSecurityAlertDao(db: AppDatabase): SecurityAlertDao = db.securityAlertDao()
    @Provides fun provideBlocklistSubscriptionDao(db: AppDatabase): com.gatekeeper.mobile.data.db.dao.BlocklistSubscriptionDao = db.blocklistSubscriptionDao()

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
