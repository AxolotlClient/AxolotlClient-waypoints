plugins {
	id("java")
	id("com.gradleup.shadow")
}

group = project.property("maven_group").toString()+"."+project.property("archives_base_name").toString()
base.archivesName.set(project.property("archives_base_name").toString()+"-common")

dependencies {
	compileOnly("net.fabricmc:fabric-loader:${project.property("fabric_loader")}")
	compileOnly("org.jetbrains:annotations:24.0.0")

	// take the oldest version just to build against
	testRuntimeOnly(compileOnly("io.github.axolotlclient:AxolotlClient-config:${project.property("config")}+1.8.9") {
		isTransitive = false
	})
	testRuntimeOnly(testCompileOnly(compileOnly("io.github.axolotlclient.AxolotlClient-config:AxolotlClientConfig-common:${project.property("config")}")!!)!!)

	testImplementation(compileOnly("com.google.code.gson:gson:2.10")!!)

	compileOnly("org.joml:joml:1.10.8")
	compileOnly("org.slf4j:slf4j-api:1.7.36")
}

tasks.jar {
	enabled = false
}

tasks.build {
	dependsOn(tasks.shadowJar)
}

tasks.processResources {
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType(JavaCompile::class).configureEach {
	options.encoding = "UTF-8"

	if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_18)) {
		options.release = 17
	}
}

tasks.withType(AbstractArchiveTask::class).configureEach {
	isPreserveFileTimestamps = false
	isReproducibleFileOrder = true
}

tasks.shadowJar {
	archiveClassifier.set("")
	mergeServiceFiles()

	append("../LICENSE")
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

publishing {
	publications {
		create("shadow", MavenPublication::class) {
			artifactId = base.archivesName.get()
			from(components["shadow"])
		}
	}
	repositories {
		maven {
			name = "owlMaven"
			val repository = if(project.version.toString().contains("beta") || project.version.toString().contains("alpha")) "snapshots" else "releases"
			url = uri("https://moehreag.duckdns.org/maven/$repository")
			credentials(PasswordCredentials::class)
			authentication {
				create<BasicAuthentication>("basic")
			}
		}
	}
}

afterEvaluate {
	tasks.modrinth.configure {enabled = false}
}
