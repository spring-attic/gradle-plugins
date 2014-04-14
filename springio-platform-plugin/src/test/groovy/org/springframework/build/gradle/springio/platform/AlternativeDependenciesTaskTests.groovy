package org.springframework.build.gradle.springio.platform

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 *
 * @author Rob Winch
 */
class AlternativeDependenciesTaskTests extends Specification {
	Project parent
	Task task

	def setup() {
		parent = ProjectBuilder.builder().withName("parent").build()
		parent.apply plugin: JavaPlugin
		task = parent.tasks.create(name: SpringioPlatformPlugin.ALTERNATIVE_DEPENDENCIES_TASK_NAME, type: AlternativeDependenciesTask)
	}

	def "defaults alternatives"() {
		when:
			task.check()
		then:
			task.alternatives
		and: 'Ensure one of the defaults was correct'
			task.alternatives['org.apache.geronimo.specs:geronimo-jta_1.1_spec'] == 'javax.transaction:javax.transaction-api'
	}

	def "fails with alternative dependencies"() {
		setup:
			parent.dependencies {
				compile 'asm:asm:3.3.1'
			}
			task.alternaives = ['asm:asm' : 'Please use some alternative']
		when:
			task.check()
		then:
			thrown(IllegalStateException)
	}

	def "default ignores testCompile"() {
		setup:
			parent.dependencies {
				testCompile 'cglib:cglib:3.3.1'
			}
			task.alternaives = ['cglib:cglib' : 'Please use some alternative']
		when:
			task.check()
		then: 'No error since many testing frameworks (i.e. mocking frameworks) require cglib'
			noExceptionThrown()
	}
}