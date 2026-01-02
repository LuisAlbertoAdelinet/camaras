pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Fallback mirrors for 403 errors
        maven("https://jitpack.io")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Fallback mirrors for 403 errors
        maven("https://jitpack.io")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
    }
}

rootProject.name = "RtspCctv"
include(":app")
