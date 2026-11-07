pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Huawei Maven repo: Wear Engine SDK + HMF tasks
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

rootProject.name = "eDompet"
include(":app")
