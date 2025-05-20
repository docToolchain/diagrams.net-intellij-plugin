import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    // Java support
    //id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.1.21"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij.platform") version "2.6.0"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.2.1"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()

        // Needed when I download EAP versions which are only available on Maven.
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1638#issuecomment-2151527333
        jetbrainsRuntime()
    }

}
dependencies {
    intellijPlatform {
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1693
        intellijIdeaCommunity(properties("platformVersion"), useInstaller = false)

        // Needed when I download EAP versions which are only available on Maven.
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1638#issuecomment-2151527333
        jetbrainsRuntime()
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {

    pluginConfiguration {
        description = provider {
            File(projectDir, "README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        }

        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    publishing {
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(if ("true" == environment("PRE_RELEASE").getOrElse("false")) "eap" else "default"))
    }

    pluginVerification {
        failureLevel = listOf(VerifyPluginTask.FailureLevel.INVALID_PLUGIN, VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS, VerifyPluginTask.FailureLevel.NOT_DYNAMIC)
        freeArgs = listOf("-mute", "TemplateWordInPluginId")
        ides {
            // recommended()
            ides( properties("pluginVerifierIdeVersions").get().split(',') )
        }
    }

}

tasks.jar {
    doFirst{
        //check if needed draw.io submodule is initialized
        if (!File(projectDir, "src/webview/drawio/src").exists()) {
            throw GradleException("please init subprojects by execution 'git submodule update --init'")
        }
    }

    // Handle duplicates strategy
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from("src/webview/drawio/src/main/webapp") {
        include("**/*")
        exclude("index.html")
        into("assets")
    }
    from("src/webview") {
        include("index.html")
        into("assets")
    }
    from("src/webview/zenuml") {
        include("**/*")
        // Exclude build artifacts and files already in resources
        exclude("index.html", "dist/**", "node_modules/**", "src/**")
        into("assets/zenuml")
    }
}

// Custom task to build ZenUML web view and copy to resources
tasks.register("buildZenUMLWebView") {
    description = "Build the ZenUML web view with npm and copy to resources"
    group = "build"

    doLast {
        // Set working directory to the ZenUML web view
        val zenUmlDir = File(projectDir, "src/webview/zenuml")

        // Run npm install
        exec {
            workingDir = zenUmlDir
            commandLine("npm", "install")
        }

        // Run npm build
        exec {
            workingDir = zenUmlDir
            commandLine("npm", "run", "build")
        }

        // Copy the built files to resources
        copy {
            from("${zenUmlDir}/dist")
            into("src/main/resources/assets/zenuml")
        }

        println("ZenUML web view built and copied to resources")
    }
}

// Make jar depend on buildZenUMLWebView
tasks.jar {
    dependsOn("buildZenUMLWebView")
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks.test {
    //useJUnitPlatform()
}
