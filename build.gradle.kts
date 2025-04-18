import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    //id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "1.17.4"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.2.1"
    // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("groovy")
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
}
dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
    // mandatory dependencies for using Spock
    testImplementation ("org.codehaus.groovy:groovy-all:3.0.23")
    testImplementation ("org.spockframework:spock-core:2.3-groovy-4.0") {
        exclude("org.codehaus.groovy", "groovy-xml")
    }

}
// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    downloadSources.set(properties("platformDownloadSources").toBoolean())
    updateSinceUntilBuild.set(false) // don't write information of current IntelliJ build into plugin.xml, instead use information from patchPluginXml

//  Plugin Dependencies:
//  https://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_dependencies.html
//
//  setPlugins("java")
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
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

tasks {
    // Set the compatibility versions to 11
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<KotlinCompile>(it) {
            kotlinOptions.jvmTarget = "17"
        }
    }

    withType<Detekt> {
        jvmTarget = "17"
    }
    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set((properties("pluginSinceBuild")))
        // untilBuild(pluginUntilBuild) --> don't set "untilBuild" to allow new versions to use existing plugin without changes until breaking API changes are known

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            provider {
                File(projectDir, "README.md").readText().lines().run {
                    val start = "<!-- Plugin description -->"
                    val end = "<!-- Plugin description end -->"

                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end))
                }.joinToString("\n").run { markdownToHTML(this) }
            }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(
            provider {
                with(changelog) {
                    renderItem(
                        (getOrNull(properties("pluginVersion")) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }
        )
    }

    runPluginVerifier {
        ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
        // Version 1.364 seems has to have a problem due to https://youtrack.jetbrains.com/issue/MP-6388
        // Version 1.365 has a problem as I can't disable the check for the 'intellij' in the plugin ID verification
        verifierVersion.set("1.307")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // if release is marked as a pre-release in the GitHub release, push it to EAP
        channels.set(listOf(if ("true" == System.getenv("PRE_RELEASE")) "EAP" else "default"))
    }
    changelog {
        version.set(properties("pluginVersion"))
        header.set(provider { "[${project.version}] - ${date()}" })
    }
}

tasks.test {
    //useJUnitPlatform()
}
