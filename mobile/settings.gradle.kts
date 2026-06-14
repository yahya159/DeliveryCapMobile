pluginManagement {
    repositories {
        google()
        mavenCentral()
                mavenLocal()
        // Add jitpack repository for com.github.jeziellago:compose-markdown
        maven (url = uri("https://jitpack.io"))

        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.sap.odata.android") {
                useModule("com.sap.cloud.android:odata-android-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        // Add jitpack repository for com.github.jeziellago:compose-markdown
        maven ( url = uri("https://jitpack.io") )
    }
}

rootProject.name = "MySAPBTPSDKProject"
include(":app")
