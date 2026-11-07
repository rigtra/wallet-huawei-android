plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// ~/Documents is synced by iCloud Drive, which makes "BuildConfig 2.java"-style conflict copies
// of build output and breaks/slows builds. iCloud skips folders ending in ".nosync".
allprojects {
    layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("build.nosync/${project.name}"))
}
