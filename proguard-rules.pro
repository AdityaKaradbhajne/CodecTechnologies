# Keep Room entities
-keep class com.example.aichat.data.** { *; }

# Keep Gemini SDK
-keep class com.google.ai.client.generativeai.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
