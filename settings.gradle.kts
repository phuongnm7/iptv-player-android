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
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials(PasswordCredentials::class)
        }
    }
}

rootProject.name = "IPTV Player"
include(":app")
