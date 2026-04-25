# -- GateKeeper Mobile ProGuard Rules --

# Keep VpnService
-keep class com.gatekeeper.mobile.vpn.GateKeeperVpnService { *; }

# Room
-keep class com.gatekeeper.mobile.data.db.entity.** { *; }

# Retrofit models
-keep class com.gatekeeper.mobile.data.remote.dto.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# MaxMind GeoIP
-keep class com.maxmind.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
