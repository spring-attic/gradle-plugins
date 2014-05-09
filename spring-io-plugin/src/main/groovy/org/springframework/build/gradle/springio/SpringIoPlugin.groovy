package org.springframework.build.gradle.springio

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

/**
 * @author Rob Winch
 * @author Andy Wilkinson
 */
class SpringIoPlugin implements Plugin<Project> {
	static String CHECK_TASK_NAME = 'springIoCheck'
	static String TEST_TASK_NAME = 'springIoTest'
	static String INCOMPLETE_EXCLUDES_TASK_NAME = 'springIoIncompleteExcludesCheck'
	static String ALTERNATIVE_DEPENDENCIES_TASK_NAME = 'springIoAlternativeDependenciesCheck'
	static String CHECK_DEPENDENCY_VERSION_MAPPING_TASK_NAME = 'springIoDependencyVersionMappingCheck'

	@Override
	void apply(Project project) {
		project.plugins.withType(JavaPlugin) {
			applyJavaProject(project)
		}
	}

	def applyJavaProject(Project project) {
		Configuration springioTestRuntimeConfig = project.configurations.create('springIoTestRuntime', {
			extendsFrom project.configurations.testRuntime
		})

		Configuration springioVersionsConfig = project.configurations.create('springIoVersions')

		springioTestRuntimeConfig.incoming.beforeResolve(
			new MapPlatformDependenciesBeforeResolveAction(project: project, configuration: springioTestRuntimeConfig,
					versionsConfiguration: springioVersionsConfig))

		Task springioTest = project.tasks.create(TEST_TASK_NAME)

		['Jdk7','Jdk8'].each { jdk ->
			maybeCreateJdkTest(project, springioTestRuntimeConfig, jdk, springioTest)
		}

		Task incompleteExcludesCheck = project.tasks.create(INCOMPLETE_EXCLUDES_TASK_NAME, IncompleteExcludesTask)
		Task alternativeDependenciesCheck = project.tasks.create(ALTERNATIVE_DEPENDENCIES_TASK_NAME, AlternativeDependenciesTask)
		DependencyVersionMappingCheckTask dependencyVersionMappingCheck =
			project.tasks.create(CHECK_DEPENDENCY_VERSION_MAPPING_TASK_NAME, DependencyVersionMappingCheckTask)
		dependencyVersionMappingCheck.versionsConfiguration = springioVersionsConfig

		project.tasks.create(CHECK_TASK_NAME) {
			dependsOn dependencyVersionMappingCheck
			dependsOn springioTest
			dependsOn incompleteExcludesCheck
			dependsOn alternativeDependenciesCheck
		}
	}

	private void maybeCreateJdkTest(Project project, Configuration springioTestRuntimeConfig, String jdk, Task springioTest) {
		def whichJdk = "${jdk.toUpperCase()}_HOME"
		if(!project.hasProperty(whichJdk)) {
			return
		}
		def jdkHome = project."${whichJdk}"
		def exec = new File(jdkHome, createRelativeJavaExec(isWindows()))
		if(!exec.exists()) {
			throw new IllegalStateException("The path $exec does not exist! Please ensure to define a valid JDK home as a command-line argument using -P${whichJdk}=<path>")
		}

		Test springioJdkTest = project.tasks.create("springIo${jdk}Test", Test)
		project.configure(springioJdkTest) {
			classpath = project.sourceSets.test.output + project.sourceSets.main.output + springioTestRuntimeConfig
			reports {
				html.destination = project.file("$project.buildDir/reports/spring-io-${jdk.toLowerCase()}-tests/")
				junitXml.destination = project.file("$project.buildDir/spring-io-${jdk.toLowerCase()}-test-results/")
			}
			executable exec
		}
		springioTest.dependsOn springioJdkTest
	}

	static createRelativeJavaExec(boolean isWindows) {
		isWindows ? '/bin/java.exe' : '/bin/java'
	}

	static isWindows() {
		File.pathSeparatorChar == ';'
	}
}
