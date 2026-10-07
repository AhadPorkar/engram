# Type-safe navigation: route classes are serialised with kotlinx.serialization.
-keepclassmembers @kotlinx.serialization.Serializable class com.ahadporkar.engram.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ahadporkar.engram.**$$serializer { *; }
-keepnames @kotlinx.serialization.Serializable class com.ahadporkar.engram.**

# WorkManager instantiates workers reflectively.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
