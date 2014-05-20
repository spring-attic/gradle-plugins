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
		def projectDir = new File('.').absoluteFile
		parent = ProjectBuilder.builder().withName("parent").withProjectDir(projectDir).build()
		parent.group = 'thisprojectgroup'
		parent.version = 'nochange'

		parent.repositories { maven { url 'src/test/resources/test-maven-repository' } }
		parent.repositories { flatDir { dirs 'src/test/resources/test-flat-repository' } }

		config = parent.configurations.create('configuration')

		Configuration versionsConfiguration = parent.configurations.create('versions')
		parent.dependencies {
			versions 'test:versions:1.0.0@properties'
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

	def "Action's versions can be overridden with Maven repository artifact"() {
		setup:
			parent.dependencies {
				versions "test:override-versions:1.0.0@properties"
			}
			DependencyResolveDetails details1 = details('standardgroup:standardname:changeme')
			DependencyResolveDetails details2 = details('standardgroup:standardothername:changeme')
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details1)
			config.resolutionStrategy.dependencyResolveRule.execute(details2)
		then:
			details1.target.group == 'standardgroup'
			details1.target.name == 'standardname'
			details1.target.version == 'overriddenversion'
			details2.target.group == 'standardgroup'
			details2.target.name == 'standardothername'
			details2.target.version == 'standardversion'

	}

	def "Action's versions can be overridden with flat directory repository artifact"() {
		setup:
			parent.dependencies {
				versions ':flat-versions@properties'
			}
			DependencyResolveDetails details1 = details('standardgroup:standardname:changeme')
			DependencyResolveDetails details2 = details('standardgroup:standardothername:changeme')
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details1)
			config.resolutionStrategy.dependencyResolveRule.execute(details2)
		then:
			details1.target.group == 'standardgroup'
			details1.target.name == 'standardname'
			details1.target.version == 'flatversion'
			details2.target.group == 'standardgroup'
			details2.target.name == 'standardothername'
			details2.target.version == 'standardversion'

	}

	DependencyResolveDetails details(String path) {
		String[] parts = path.split(':')
		new DefaultDependencyResolveDetails(new DefaultModuleVersionSelector(parts[0],parts[1],parts[2]))
	}
}