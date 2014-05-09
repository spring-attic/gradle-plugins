package org.springframework.build.gradle.springio

import com.google.common.io.Files

import org.gradle.api.*
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.springframework.build.gradle.springio.AlternativeDependenciesTask;
import org.springframework.build.gradle.springio.IncompleteExcludesTask;
import org.springframework.build.gradle.springio.SpringIoPlugin;

import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 *
 * @author Rob Winch
 */
class SpringIoPluginTests extends Specification {
	Project project
	@AutoCleanup("deleteDir")
	File jdkHome = Files.createTempDir()
	File java

	def setup() {
		project = ProjectBuilder.builder().build()
		java = new File(jdkHome,'bin/java')
		java.parentFile.mkdirs()
		java.createNewFile()
	}

	def "Sets up does not fail on non Java project"() {
		when:
			project.apply plugin: SpringIoPlugin
		then:
			!project.configurations.hasProperty('springIoTestRuntime')
	}

	def "Works for Groovy project"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: GroovyPlugin
		then: 'Plugin performs configurations'
			project.configurations.springIoTestRuntime
	}

	def "Sets up configurations.springioTestRuntime"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			project.configurations.springIoTestRuntime
		and:
			project.configurations.springIoTestRuntime.extendsFrom.contains(project.configurations.testRuntime)
	}

	def "Sets up springIoVersions configuration"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			project.configurations.springIoVersions
	}

	def "Creates springIoCheck Task"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			Task sioc = project.tasks.findByName(SpringIoPlugin.CHECK_TASK_NAME)
			sioc
			sioc.taskDependencies.getDependencies(sioc) == [versionMappingCheckTask, springioTestTask, alternativeDependenciesTask, incompleteExcludesTask] as Set
	}

	def "Creates springIoTest Task"() {
		setup:
			setupJdks()
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			Task siot = springioTestTask
			siot
			siot.taskDependencies.getDependencies(siot) == [jdk7TestTask, jdk8TestTask] as Set
	}

	def "Creates springIoJDK7Test Task"() {
		setup:
			setupJdks()
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			jdk7TestTask
			jdk7TestTask.executable == java.absolutePath
	}

	def "Does not create springIoJdk7Test if JDK7_HOME missing"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			!jdk7TestTask
	}

	def "Creates springIoJDK8Test Task"() {
		setup:
			setupJdks()
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			jdk8TestTask
			jdk8TestTask.executable == java.absolutePath
	}

	def "Does not create springIoJdk8Test if JDK8_HOME missing"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			!jdk7TestTask
	}

	def "Creates springIoIncompleteExcludes Task"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			incompleteExcludesTask instanceof IncompleteExcludesTask
	}

	def "Creates springIoAlternativeDependencies Task"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			alternativeDependenciesTask instanceof AlternativeDependenciesTask
	}

	def "Creates springIoDependencyVersionMappingCheck task"() {
		when:
			project.apply plugin: SpringIoPlugin
			project.apply plugin: JavaPlugin
		then:
			versionMappingCheckTask
	}

	def 'gh-36: finds java exec on Windows'() {
		expect:
			SpringIoPlugin.createRelativeJavaExec(true) == '/bin/java.exe'
	}

	def 'gh-36: finds java exec on non Windows'() {
		expect:
		SpringIoPlugin.createRelativeJavaExec(false) == '/bin/java'
	}

	def getAlternativeDependenciesTask() {
		project.tasks.findByName(SpringIoPlugin.ALTERNATIVE_DEPENDENCIES_TASK_NAME)
	}

	def getIncompleteExcludesTask() {
		project.tasks.findByName(SpringIoPlugin.INCOMPLETE_EXCLUDES_TASK_NAME)
	}

	def getSpringioTestTask() {
		project.tasks.findByName(SpringIoPlugin.TEST_TASK_NAME)
	}

	def getJdk7TestTask() {
		project.tasks.findByName('springIoJdk7Test')
	}

	def getJdk8TestTask() {
		project.tasks.findByName('springIoJdk8Test')
	}

	def getVersionMappingCheckTask() {
		project.tasks.findByName('springIoDependencyVersionMappingCheck')
	}

	def setupJdks() {
		project.ext.JDK7_HOME = jdkHome
		project.ext.JDK8_HOME = jdkHome
	}
}