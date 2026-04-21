# Keep Moshi-reflected DTOs stable when release shrinking is on.
-keep class com.hastakala.shop.network.ai.provider.** { *; }
-keep class com.hastakala.shop.network.ai.model.** { *; }
-keep class com.hastakala.shop.network.ai.AiManagerImpl$PriceSuggestionPayload { *; }
-keep class com.hastakala.shop.data.repository.BackupPayload { *; }
-keep class com.hastakala.shop.data.local.Product { *; }
-keep class com.hastakala.shop.data.local.Variant { *; }
-keep class com.hastakala.shop.data.local.Sale { *; }

# Tink pulls in errorprone annotations that are not needed at runtime.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
