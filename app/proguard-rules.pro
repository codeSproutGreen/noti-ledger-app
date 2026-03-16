# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.notiledger.data.model.** { *; }
-keep class com.notiledger.data.backup.BackupData { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose
-dontwarn androidx.compose.**
