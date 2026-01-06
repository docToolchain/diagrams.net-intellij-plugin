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
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij.platform") version "2.10.5"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.5.0"
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

    // NanoHTTPD for MCP HTTP server (lightweight, works well in IntelliJ plugins)
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // Gson for JSON serialization (used by MCP server)
    implementation("com.google.code.gson:gson:2.10.1")

    // Jackson for browser communication (diagram editor)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")

    // JUnit 4 for unit testing (required by IntelliJ Platform test framework)
    testImplementation("junit:junit:4.13.2")
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
            // Configure IDE versions for verification - required on CI
            val ideVersions = properties("pluginVerifierIdeVersions").get()
            if (ideVersions.isNotBlank()) {
                ides( ideVersions.split(',').map { it.trim() }.filter { it.isNotEmpty() } )
            }
            // Note: If empty, verifyPlugin task is skipped via onlyIf condition below
        }
    }

}

// Skip plugin verification if no IDE versions configured (ARM64/Apple Silicon local builds)
tasks.named<VerifyPluginTask>("verifyPlugin") {
    onlyIf {
        properties("pluginVerifierIdeVersions").get().isNotBlank()
    }
}

tasks.jar {
    doFirst{
        //check if needed draw.io submodule is initialized
        if (!File(projectDir, "src/webview/drawio/src").exists()) {
            throw GradleException("please init subprojects by execution 'git submodule update --init'")
        }
    }
    from("src/webview/drawio/src/main/webapp") {
        include("**/*")
        exclude("index.html")
        into("assets")
    }
    from("src/webview") {
        include("index.html")
        into("assets")
    }
}

// Use JVM Toolchain to ensure consistent Java/Kotlin compilation target
// This ensures both Java and Kotlin compile to the same JVM target
kotlin {
    jvmToolchain(17)
}

// Keep explicit Java configuration for clarity and compatibility
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    // Use IntelliJ Platform test runner (JUnit 4 based)
    // Note: useJUnitPlatform() conflicts with IntelliJ Platform's test framework
}

// TEMPORARY: Workaround for JCEF out-of-process bug (IJPL-184288) causing blank canvas
// Remove this once JetBrains fixes the issue
tasks.named<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runIde") {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Dide.browser.jcef.out-of-process.enabled=false")
    }
}

// Task to run an external IDE installation with isolated test configuration
// Usage: ./gradlew runTestIde [-PtestIdePath=/path/to/IDE.app] [-PtestConfigName=my-config] [-PcleanConfig=true]
// Each IDE type gets its own config directory by default (e.g., test-ide-idea, test-ide-pycharm)
tasks.register<Exec>("runTestIde") {
    group = "intellij platform"
    description = "Run an external IDE with isolated test configuration and the built plugin"
    dependsOn("buildPlugin")

    // This task is not compatible with configuration cache because it needs
    // to evaluate properties at execution time (e.g., testIdePath from command line)
    notCompatibleWithConfigurationCache("Task properties must be evaluated at execution time")

    // Store providers for lazy evaluation
    val testIdePathProp = properties("testIdePath")
    val testConfigNameProp = properties("testConfigName")
    val cleanConfigProp = properties("cleanConfig")
    val detachedProp = properties("detached")

    doFirst {
        val userHome = System.getProperty("user.home")
        val isMacOS = System.getProperty("os.name").lowercase().contains("mac")

        // Configurable properties with defaults - check both user and system Applications
        val defaultIdePath = if (isMacOS) {
            // Prefer user's Applications folder, fall back to system Applications
            val userApp = File("$userHome/Applications/IntelliJ IDEA.app")
            val systemApp = File("/Applications/IntelliJ IDEA.app")
            when {
                userApp.exists() -> userApp.absolutePath
                systemApp.exists() -> systemApp.absolutePath
                else -> "$userHome/Applications/IntelliJ IDEA.app"  // Default for error message
            }
        } else "/opt/idea"
        val testIdePath = testIdePathProp.getOrElse(defaultIdePath)
        val cleanConfig = cleanConfigProp.getOrElse("false").toBoolean()
        val detached = detachedProp.getOrElse("true").toBoolean()

        // Validate IDE exists
        val ideFile = File(testIdePath)
        if (!ideFile.exists()) {
            throw GradleException("""
                IDE not found at: $testIdePath
                Set -PtestIdePath=/path/to/IDE.app
                Example: -PtestIdePath=$userHome/Applications/PyCharm.app
            """.trimIndent())
        }

        // Find IDE binary and determine env var prefix
        // Each JetBrains IDE uses its own prefix: IDEA, PYCHARM, WEBIDE, GOLAND, etc.
        data class IdeInfo(val binary: String, val envPrefix: String, val ideName: String)
        val ideInfo: IdeInfo = if (isMacOS) {
            val macosDir = File(testIdePath, "Contents/MacOS")
            val ideMap = mapOf(
                "idea" to Pair("IDEA", "idea"),
                "pycharm" to Pair("PYCHARM", "pycharm"),
                "webstorm" to Pair("WEBIDE", "webstorm"),
                "goland" to Pair("GOLAND", "goland"),
                "clion" to Pair("CLION", "clion"),
                "rider" to Pair("RIDER", "rider"),
                "phpstorm" to Pair("PHPSTORM", "phpstorm"),
                "rubymine" to Pair("RUBYMINE", "rubymine"),
                "datagrip" to Pair("DATAGRIP", "datagrip")
            )
            val found = ideMap.entries.firstOrNull { File(macosDir, it.key).exists() }
            if (found != null) {
                IdeInfo(File(macosDir, found.key).absolutePath, found.value.first, found.value.second)
            } else {
                IdeInfo(File(macosDir, "idea").absolutePath, "IDEA", "idea")
            }
        } else {
            IdeInfo(File(testIdePath, "bin/idea.sh").absolutePath, "IDEA", "idea")
        }
        val ideBinary = ideInfo.binary
        val envPrefix = ideInfo.envPrefix
        val ideName = ideInfo.ideName

        // Config directory defaults to IDE-specific name (e.g., test-ide-pycharm, test-ide-webstorm)
        val testConfigName = testConfigNameProp.getOrElse("test-ide-$ideName")

        // Paths - each IDE type gets its own isolated directory
        val configDir = layout.buildDirectory.dir(testConfigName).get().asFile
        val pluginsDir = File(configDir, "plugins")
        val ideaProperties = File(configDir, "idea.properties")
        val ideaVmOptions = File(configDir, "idea.vmoptions")

        // Clean config if requested
        if (cleanConfig && configDir.exists()) {
            println("Cleaning test config directory: $configDir")
            configDir.deleteRecursively()
        }

        // Create config directories
        listOf("config", "system", "log", "plugins").forEach {
            File(configDir, it).mkdirs()
        }

        // Clear plugin cache in system directory to force fresh plugin loading
        val pluginCache = File(configDir, "system/plugins")
        if (pluginCache.exists()) {
            println("Clearing plugin cache...")
            pluginCache.deleteRecursively()
        }

        // Create idea.properties with FULLY isolated paths
        // All paths are under build/<config-name>/ - NO interference with default IDE
        ideaProperties.writeText("""
            idea.config.path=${configDir.absolutePath}/config
            idea.system.path=${configDir.absolutePath}/system
            idea.log.path=${configDir.absolutePath}/log
            idea.plugins.path=${configDir.absolutePath}/plugins
            idea.scratch.path=${configDir.absolutePath}/scratches
            idea.initially.ask.config=true
        """.trimIndent() + "\n")

        // Create options directory and configure to NOT reopen last project
        val optionsDir = File(configDir, "config/options")
        optionsDir.mkdirs()
        File(optionsDir, "ide.general.xml").writeText("""
            <application>
              <component name="GeneralSettings">
                <option name="reopenLastProject" value="false" />
                <option name="showTipsOnStartup" value="false" />
                <option name="confirmExit" value="false" />
              </component>
            </application>
        """.trimIndent() + "\n")

        // Create idea.vmoptions with JCEF workaround AND path overrides
        // Using -D flags ensures these take precedence over default paths
        ideaVmOptions.writeText("""
            -Xmx4096m
            -Dide.browser.jcef.out-of-process.enabled=false
            -Didea.config.path=${configDir.absolutePath}/config
            -Didea.system.path=${configDir.absolutePath}/system
            -Didea.log.path=${configDir.absolutePath}/log
            -Didea.plugins.path=${configDir.absolutePath}/plugins
        """.trimIndent() + "\n")

        // Clean plugins directory completely to avoid conflicts with marketplace version
        pluginsDir.listFiles()?.forEach { it.deleteRecursively() }

        // Install built plugin
        val pluginZip = fileTree(layout.buildDirectory.dir("distributions")) {
            include("*.zip")
        }.files.firstOrNull()

        if (pluginZip != null) {
            println("Installing plugin from: ${pluginZip.name}")
            copy {
                from(zipTree(pluginZip))
                into(pluginsDir)
            }
            // Verify installation
            val installedPlugin = File(pluginsDir, "diagrams.net-intellij-plugin")
            if (installedPlugin.exists()) {
                println("Plugin installed to: ${installedPlugin.absolutePath}")
            }
        } else {
            throw GradleException("No plugin ZIP found in build/distributions/ - run buildPlugin first")
        }

        // Create disabled_plugins.txt to prevent loading marketplace plugin from user's config
        // This disables the plugin by ID in any OTHER location (bundled, user config, etc.)
        // Our local plugin in idea.plugins.path will still load as it's explicitly in our plugins dir
        val disabledPlugins = File(configDir, "config/disabled_plugins.txt")
        disabledPlugins.parentFile.mkdirs()
        // Note: We list plugins to disable from OTHER locations here
        // The marketplace plugin has same ID but is in a different location
        disabledPlugins.writeText("")  // Empty for now - isolation via paths should work

        // Create a launcher script that properly exports environment variables
        // This ensures they're inherited by the macOS app bundle
        // Use IDE-specific env var prefix (IDEA, PYCHARM, WEBIDE, etc.)
        // The script launches the IDE detached so Gradle can complete (allows parallel runs)
        val launcherScript = File(configDir, "launch.sh")
        val launchCommand = if (detached) {
            """
            # Launch detached so Gradle completes and releases lock (enables parallel IDE runs)
            nohup "$ideBinary" > /dev/null 2>&1 &
            echo "IDE launched in background (PID: ${'$'}!)"
            """.trimIndent()
        } else {
            """
            # Launch synchronously (for debugging)
            exec "$ideBinary"
            """.trimIndent()
        }
        launcherScript.writeText("""
            #!/bin/bash
            # Clear any inherited MCP port env var for clean test environment
            unset DIAGRAMS_NET_MCP_PORT_CURRENT
            export ${envPrefix}_PROPERTIES="${ideaProperties.absolutePath}"
            export ${envPrefix}_VM_OPTIONS="${ideaVmOptions.absolutePath}"
            $launchCommand
        """.trimIndent() + "\n")
        launcherScript.setExecutable(true)
        println("Using env prefix: ${envPrefix}_PROPERTIES, ${envPrefix}_VM_OPTIONS")

        println("")
        println("=== Test IDE Configuration (Fully Isolated) ===")
        println("IDE:      $testIdePath")
        println("Base dir: $configDir")
        println("  config:   ${configDir.absolutePath}/config")
        println("  system:   ${configDir.absolutePath}/system")
        println("  plugins:  ${configDir.absolutePath}/plugins")
        println("  log:      ${configDir.absolutePath}/log")
        println("JCEF workaround: -Dide.browser.jcef.out-of-process.enabled=false")
        println("Launcher:  ${launcherScript.absolutePath}")
        println("")
        println("This test IDE is FULLY ISOLATED from your default IDE installation.")
        println("================================================")

        // Set command line at execution time (after configDir is determined)
        commandLine("bash", File(configDir, "launch.sh").absolutePath)
    }
}
