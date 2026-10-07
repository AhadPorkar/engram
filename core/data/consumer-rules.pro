# Backup DTOs are (de)serialised with kotlinx.serialization.
-keepclassmembers @kotlinx.serialization.Serializable class com.ahadporkar.engram.core.data.backup.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ahadporkar.engram.core.data.backup.**$$serializer { *; }
