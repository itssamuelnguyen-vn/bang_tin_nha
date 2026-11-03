# R8 rules for the release build. Room, WorkManager, Glance, OkHttp and kotlinx.serialization ship their own
# consumer rules, so nothing app-specific is needed yet. Add rules here only when a release build fails or crashes.

# Keep line numbers so crash stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
