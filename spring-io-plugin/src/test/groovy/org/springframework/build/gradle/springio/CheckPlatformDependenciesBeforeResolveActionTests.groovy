package org.springframework.build.gradle.springio

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector
import org.gradle.api.internal.artifacts.ivyservice.DefaultDependencyResolveDetails
import org.gradle.testfixtures.ProjectBuilder
import org.springframework.build.gradle.springio.CheckPlatformDependenciesBeforeResolveAction;

import spock.lang.Specification

/**
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
class CheckPlatformDependenciesBeforeResolveActionTests extends Specification {
	Project parent
	Project child
	Configuration config
	CheckPlatformDependenciesBeforeResolveAction action

	def setup() {
		def projectDir = new File('.').absoluteFile
		parent = ProjectBuilder.builder().withName("parent").withProjectDir(projectDir).build()
		parent.group = 'thisprojectgroup'
		parent.version = 'nochange'

		config = parent.configurations.create('configuration')

		parent.repositories { maven { url 'src/test/resources/test-maven-repository' } }

		Configuration versionsConfiguration = parent.configurations.create('versions')

		parent.dependencies {
			versions 'test:versions:1.0.0@properties'
		}

		action = new CheckPlatformDependenciesBeforeResolveAction(project: parent, configuration: config, versionsConfiguration: versionsConfiguration)

		child = ProjectBuilder.builder().withName('child').withParent(parent).build()
		child.group = parent.group
		child.version = parent.version
	}

	def "Execution fails with unmapped direct dependency"() {
		setup:
			parent.dependencies {
				configuration "notfound:notfound:nochange"
			}
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolvedConfiguration
		then:
			thrown InvalidUserDataException
	}

	def "Execution succeeds with unmapped transitive dependency"() {
		setup:
			DependencyResolveDetails details = details('notfound:notfound:nochange')
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then: 'resolution will succeed'
			config.resolvedConfiguration
	}

	def "Action can be configured to fail with unmapped transitive dependency"() {
		setup:
			DependencyResolveDetails details = details('notfound:notfound:nochange')
			action.failOnUnmappedTransitiveDependency = true
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
			config.resolvedConfiguration
		then:
			thrown InvalidUserDataException
	}

	def "Action can be configured to succeed with unmapped direct dependency"() {
		setup:
			parent.dependencies {
				configuration "notfound:notfound:nochange"
			}
			action.failOnUnmappedDirectDependency = false
		when:
			action.execute(Mock(ResolvableDependencies))

		then: 'resolution will succeeed'
			config.resolvedConfiguration
	}

	DependencyResolveDetails details(String path) {
		String[] parts = path.split(':')
		new DefaultDependencyResolveDetails(new DefaultModuleVersionSelector(parts[0],parts[1],parts[2]))
	}
}