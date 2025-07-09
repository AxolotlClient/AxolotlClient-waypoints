plugins {
	id("fabric-loom")
	id("ploceus")
	id("io.github.p03w.machete")
}

val minecraftVersion = "1.8.9"
val feather = "1.8.9+build.27"
val osl = "0.16.3"

group = project.property("maven_group")!!
version = "${project.property("version")}+$minecraftVersion"
base.archivesName = project.property("archives_base_name").toString()

loom {
	accessWidenerPath.set(file("src/main/resources/axolotlclient-waypoints.accesswidener"))

	mods {
		create("axolotlclient") {
			sourceSet("main")
		}
		create("axolotlclient-test") {
			sourceSet("test")
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:$minecraftVersion")
	mappings("net.ornithemc:feather:$feather")

	modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader")}")

	modImplementation("io.github.axolotlclient:AxolotlClient-config:${project.property("config")}+$minecraftVersion") {
		exclude(group = "org.lwjgl")
		exclude(group = "org.slf4j")
	}
	include("io.github.axolotlclient:AxolotlClient-config:${project.property("config")}+$minecraftVersion}")
	modImplementation("io.github.axolotlclient.AxolotlClient-config:AxolotlClientConfig-common:${project.property("config")}")

	ploceus.dependOsl(osl)

	modImplementation("com.terraformersmc:modmenu:0.3.1+mc1.8.9")

	implementation(include(project(path = ":common", configuration = "shadow"))!!)

	val lwjglVersion = "3.3.5"
	api("org.lwjgl:lwjgl-nanovg:$lwjglVersion")
	runtimeOnly("org.lwjgl:lwjgl-nanovg:${lwjglVersion}:natives-linux")
	runtimeOnly("org.lwjgl:lwjgl-nanovg:${lwjglVersion}:natives-linux-arm64")
	runtimeOnly("org.lwjgl:lwjgl-nanovg:${lwjglVersion}:natives-windows")
	runtimeOnly("org.lwjgl:lwjgl-nanovg:${lwjglVersion}:natives-windows-arm64")
	runtimeOnly("org.lwjgl:lwjgl-nanovg:${lwjglVersion}:natives-macos")
	runtimeOnly("org.lwjgl:lwjgl-nanovg:${lwjglVersion}:natives-macos-arm64")

	/*include("org.apache.logging.log4j:log4j-slf4j-impl:2.0-beta9") {
		exclude(group = "org.apache.logging.log4j", module = "log4j-api")
		exclude(group = "org.apache.logging.log4j", module = "log4j-core")
	}
	implementation(include("org.slf4j:slf4j-api:1.7.36")!!)
	localRuntime("org.slf4j:slf4j-jdk14:1.7.36")*/

	//compileOnly("org.lwjgl:lwjgl-glfw:${lwjglVersion}")

	modCompileOnly("io.github.moehreag:legacy-lwjgl3:${project.property("legacy_lwgjl3")}")
	modLocalRuntime("io.github.moehreag:legacy-lwjgl3:${project.property("legacy_lwgjl3")}:all-remapped")

	implementation(include("org.joml:joml:1.10.8")!!)
}

configurations.configureEach {
	resolutionStrategy {
		dependencySubstitution {
			substitute(module("io.netty:netty-all:4.0.23.Final")).using(module("io.netty:netty-all:4.1.9.Final"))
		}
		force("io.netty:netty-all:4.1.9.Final")
	}
}

tasks.processResources {
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.runClient {
	if (project.property("native_glfw") == "true") {
		val glfwPath = project.properties.getOrDefault("native_glfw_path", "/usr/lib/libglfw.so")
		jvmArgs("-Dorg.lwjgl.glfw.libname=$glfwPath")
	}
	classpath(sourceSets.getByName("test").runtimeClasspath)
	jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks.withType(JavaCompile::class).configureEach {
	options.encoding = "UTF-8"

	if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_18)) {
		options.release = 17
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

// Configure the maven publication
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = base.archivesName.get()
			from(components["java"])
		}
	}

	repositories {
		maven {
			name = "owlMaven"
			val repository = if (project.version.toString().contains("beta") || project.version.toString()
					.contains("alpha")
			) "snapshots" else "releases"
			url = uri("https://moehreag.duckdns.org/maven/$repository")
			credentials(PasswordCredentials::class)
			authentication {
				create<BasicAuthentication>("basic")
			}
		}
	}
}

tasks.modrinth {
	dependsOn(tasks.getByName("optimizeOutputsOfRemapJar"))
}

modrinth {
	token = System.getenv("MODRINTH_TOKEN")
	projectId = "p2rxzX0q"
	versionNumber = "${project.version}"
	versionType = "release"
	uploadFile = tasks.remapJar.get()
	gameVersions.set(listOf(minecraftVersion))
	loaders.set(listOf("fabric", "quilt"))
	additionalFiles.set(listOf(tasks.remapSourcesJar))
	dependencies {
		required.project("osl")
		required.project("moehreag-legacy-lwjgl3")
	}

	// Changelog fetching: Credit LambdAurora.
	// https://github.com/LambdAurora/LambDynamicLights/blob/1ef85f486084873b5d97b8a08df72f57859a3295/build.gradle#L145
	// License: MIT
	val changelogText = file("../CHANGELOG.md").readText()
	val regexVersion =
		((project.version) as String).split("+")[0].replace("\\.".toRegex(), "\\.").replace("\\+".toRegex(), "+")
	val changelogRegex = "###? ${regexVersion}\\n\\n(( *- .+\\n)+)".toRegex()
	val matcher = changelogRegex.find(changelogText)

	if (matcher != null) {
		var changelogContent = matcher.groups[1]?.value

		val changelogLines = changelogText.split("\n")
		val linkRefRegex = "^\\[([A-z0-9 _\\-/+.]+)]: ".toRegex()
		for (line in changelogLines.reversed()) {
			if ((linkRefRegex.matches(line)))
				changelogContent += "\n" + line
			else break
		}
		changelog = changelogContent
	} else {
		afterEvaluate {
			tasks.modrinth.configure { isEnabled = false }
		}
	}
}
