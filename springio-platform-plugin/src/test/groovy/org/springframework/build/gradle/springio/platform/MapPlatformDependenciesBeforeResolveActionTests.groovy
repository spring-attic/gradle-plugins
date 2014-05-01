package org.springframework.build.gradle.springio.platform

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector
import org.gradle.api.internal.artifacts.ivyservice.DefaultDependencyResolveDetails
import org.gradle.testfixtures.ProjectBuilder

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
		action = new MapPlatformDependenciesBeforeResolveAction(project: parent, configuration: config, resource: 'test-springio-dependencies')

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

	DependencyResolveDetails details(String path) {
		String[] parts = path.split(':')
		new DefaultDependencyResolveDetails(new DefaultModuleVersionSelector(parts[0],parts[1],parts[2]))
	}
}