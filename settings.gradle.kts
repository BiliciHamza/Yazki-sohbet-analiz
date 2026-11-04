pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Yazki"

include(":app")

// Android'den bağımsız, saf Kotlin çekirdek modülleri.
// Testleri emülatörsüz, saniyeler içinde çalışır.
include(":core:parser")   // WhatsApp dışa aktarım dosyasını okur ve ayrıştırır
include(":core:turkish")  // Türkçe metin katmanı: küçük harf, kahkaha, sayaçlar
include(":core:analysis") // Metrik motoru: ayrıştırılmış sohbetten istatistik üretir
