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
import org.springframework.build.gradle.springio.AbstractPlatformDependenciesBeforeResolveAction;
import org.springframework.build.gradle.springio.MapPlatformDependenciesBeforeResolveAction;

import spock.lang.Specification

/**
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
class MapPlatformDependenciesBeforeResolveActionTests extends Specification {
	Project parent
	Project child
	Configuration config
	AbstractPlatformDependenciesBeforeResolveAction action

	def setup() {
		parent = ProjectBuilder.builder().withName("parent").build()
		parent.group = 'thisprojectgroup'
		parent.version = 'nochange'

		config = parent.configurations.create('configuration')

		Configuration versionsConfiguration = parent.configurations.create('versions')
		def versionsFile = new File('src/test/resources/org/springframework/build/gradle/springio/test-spring-io-dependencies.properties').getAbsoluteFile()
		def files = parent.files(versionsFile)
		parent.dependencies {
			versions files
		}

		action = new MapPlatformDependenciesBeforeResolveAction(project: parent, configuration: config,
			versionsConfiguration: versionsConfiguration)

		child = ProjectBuilder.builder().withName('child').withParent(parent).build()
		child.group = parent.group
		child.version = parent.version
	}

	def "Action ignores these projects"() {
		setup:
			DependencyResolveDetails details = details('thisprojectgroup:child:nochange')
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.group == 'thisprojectgroup'
			details.target.name == 'child'
			details.target.version == 'nochange'
	}

	def "Action supplies mapped version"() {
		setup:
			DependencyResolveDetails details = details('standardgroup:standardname:changeme')
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.group == 'standardgroup'
			details.target.name == 'standardname'
			details.target.version == 'standardversion'
	}

	def "Action leaves unmapped dependency unchanged"() {
		setup:
			DependencyResolveDetails details = details('something:unknown:1.0')
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.group == 'something'
			details.target.name == 'unknown'
			details.target.version == '1.0'
	}

	def "Action's versions can be overridden"() {
		setup:
			def versionsFile = new File('src/test/resources/org/springframework/build/gradle/springio/override-spring-io-dependencies.properties').getAbsoluteFile()
			def files = parent.files(versionsFile)
			parent.dependencies {
				versions files
			}
			DependencyResolveDetails details = details('standardgroup:standardname:changeme')
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.group == 'standardgroup'
			details.target.name == 'standardname'
			details.target.version == 'overriddenversion'
	}

	DependencyResolveDetails details(String path) {
		String[] parts = path.split(':')
		new DefaultDependencyResolveDetails(new DefaultModuleVersionSelector(parts[0],parts[1],parts[2]))
	}
}