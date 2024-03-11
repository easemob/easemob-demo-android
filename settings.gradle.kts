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
    }
}

rootProject.name = "chatuikit-android-demo"
include(":app")
include(":ease-im-kit")
project(":ease-im-kit").projectDir = File("../ChatUIKit/ease-im-kit")
 