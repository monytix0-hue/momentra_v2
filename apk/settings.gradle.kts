// When local.properties is synced across Mac/Linux, sdk.dir often points at a
// missing path. Rewrite it to a usable SDK before AGP resolves dependencies.
run {
    val localProps = file("local.properties")
    fun sdkDirFromLocal(): File? {
        if (!localProps.exists()) return null
        val raw = localProps.readLines()
            .firstOrNull { it.trim().startsWith("sdk.dir=") }
            ?.substringAfter("sdk.dir=")
            ?.trim()
            ?.trim('"')
            ?: return null
        return file(raw.replace("\\\\", "\\"))
    }
    val configured = sdkDirFromLocal()
    if (configured == null || !configured.isDirectory) {
        val fallback = listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            "${System.getProperty("user.home")}/Android/Sdk",
            "${System.getProperty("user.home")}/Library/Android/sdk",
        ).map { file(it) }.firstOrNull { it.isDirectory }
        if (fallback != null) {
            val path = fallback.absolutePath.replace("\\", "\\\\")
            val lines = if (localProps.exists()) {
                localProps.readLines().toMutableList()
            } else {
                mutableListOf()
            }
            val idx = lines.indexOfFirst { it.trim().startsWith("sdk.dir=") }
            if (idx >= 0) lines[idx] = "sdk.dir=$path" else lines += "sdk.dir=$path"
            localProps.writeText(lines.joinToString("\n", postfix = "\n"))
            println("settings: repaired local.properties sdk.dir -> $path")
        }
    }
}

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

rootProject.name = "momentra"
include(":app")
include(":openapi-generated")
project(":openapi-generated").projectDir = file("openapi-generated")
