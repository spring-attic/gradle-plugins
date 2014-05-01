package org.springframework.build.gradle.springio.platform

import com.google.common.io.Files
import org.gradle.api.*
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 *
 * @author Rob Winch
 */
class SpringioPlatformPluginTests extends Specification {
	Project project
	@AutoCleanup("deleteDir")
	File jdkHome = Files.createTempDir()

	def setup() {
		project = ProjectBuilder.builder().build()
		def java = new File(jdkHome,'bin/java')
		java.parentFile.mkdirs()
		java.createNewFile()
	}

	def "Sets up does not fail on non Java project"() {
		when:
			project.apply plugin: SpringioPlatformPlugin
		then:
			!project.configurations.hasProperty('springioTestRuntime')
	}

	def "Works for Groovy project"() {
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: GroovyPlugin
		then: 'Plugin performs configurations'
			project.configurations.springioTestRuntime
	}

	def "Sets up configurations.springioTestRuntime"() {
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			project.configurations.springioTestRuntime
		and:
			project.configurations.springioTestRuntime.extendsFrom.contains(project.configurations.testRuntime)
	}

	def "Creates springioCheck Task"() {
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			Task sioc = project.tasks.findByName(SpringioPlatformPlugin.CHECK_TASK_NAME)
			sioc
			sioc.taskDependencies.getDependencies(sioc) == [versionMappingCheckTask, springioTestTask, alternativeDependenciesTask, incompleteExcludesTask] as Set
	}

	def "Creates springioTest Task"() {
		setup:
			setupJdks()
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			Task siot = springioTestTask
			siot
			siot.taskDependencies.getDependencies(siot) == [jdk7TestTask, jdk8TestTask] as Set
	}

	def "Creates springioJDK7Test Task"() {
		setup:
			setupJdks()
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			jdk7TestTask
	}

	def "Does not create springioJDK7Test if JDK7_HOME missing Task"() {
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			!jdk7TestTask
	}

	def "Creates springioJDK8Test Task"() {
		setup:
			setupJdks()
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			jdk8TestTask
	}

	def "Does not create springioJDK8Test if JDK8_HOME missing Task"() {
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			!jdk7TestTask
	}

	def "Creates springioIncompleteExcludes Task"() {
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			incompleteExcludesTask instanceof IncompleteExcludesTask
	}

	def "Creates springioAlternativeDependencies Task"() {
		when:
			project.apply plugin: SpringioPlatformPlugin
			project.apply plugin: JavaPlugin
		then:
			alternativeDependenciesTask instanceof AlternativeDependenciesTask
	}

	def "Creates springioDependencyVersionMappingCheck task"() {
		when:
		project.apply plugin: SpringioPlatformPlugin
		project.apply plugin: JavaPlugin
	then:
		versionMappingCheckTask
	}

	def getAlternativeDependenciesTask() {
		project.tasks.findByName(SpringioPlatformPlugin.ALTERNATIVE_DEPENDENCIES_TASK_NAME)
	}

	def getIncompleteExcludesTask() {
		project.tasks.findByName(SpringioPlatformPlugin.INCOMPLETE_EXCLUDES_TASK_NAME)
	}

	def getSpringioTestTask() {
		project.tasks.findByName(SpringioPlatformPlugin.TEST_TASK_NAME)
	}

	def getJdk7TestTask() {
		project.tasks.findByName('springioJDK7Test')
	}

	def getJdk8TestTask() {
		project.tasks.findByName('springioJDK8Test')
	}

	def getVersionMappingCheckTask() {
		project.tasks.findByName('springioDependencyVersionMappingCheck')
	}

	def setupJdks() {
		project.ext.JDK7_HOME = jdkHome
		project.ext.JDK8_HOME = jdkHome
	}
}