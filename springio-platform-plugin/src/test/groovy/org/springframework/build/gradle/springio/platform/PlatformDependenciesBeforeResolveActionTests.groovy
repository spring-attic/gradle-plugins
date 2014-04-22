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
class PlatformDependenciesBeforeResolveActionTests extends Specification {
	Project parent
	Project child
	Configuration config
	PlatformDependenciesBeforeResolveAction action

	def setup() {
		parent = ProjectBuilder.builder().withName("parent").build()
		parent.extensions.create("springioPlatform", SpringioPlatformExtension)
		parent.group = 'thisprojectgroup'
		parent.version = 'nochange'

		config = parent.configurations.create('configuration')
		action = new PlatformDependenciesBeforeResolveAction(project: parent, configuration: config)

		child = ProjectBuilder.builder().withName('child').withParent(parent).build()
		child.group = parent.group
		child.version = parent.version
	}

	def "execute adds Action as ResolutionStrategy Rule"() {
		setup:
			def resolutionAction = Mock(Action)
			parent.springioPlatform {
				dependencyResolutionAction = resolutionAction
			}
			DependencyResolveDetails details = details('org.springframework:spring-core:3.2.0.RELEASE')
		when:
		    action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			1 * resolutionAction.execute(details)
	}

	def "default dependency resolution action ignores these projects"() {
		setup:
			DependencyResolveDetails details = details('thisprojectgroup:child:nochange')
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'nochange'
			details.target.name == 'child'
			details.target.group == 'thisprojectgroup'
	}

	def "default dependency resolution action supports group only"() {
		setup:
			DependencyResolveDetails details = details('grouponly:notdefined:changeme')
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'grouponlyversion'
			details.target.name == 'notdefined'
			details.target.group == 'grouponly'
	}

	def "default dependency resolution action supports name only"() {
		setup:
			DependencyResolveDetails details = details('notdefined:nameonly:changeme')
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'nameonlyversion'
			details.target.name == 'nameonly'
			details.target.group == 'notdefined'
	}

	def "default dependency resolution action supports name and group"() {
		setup:
			DependencyResolveDetails details = details('standardgroup:standardname:changeme')
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'standardversion'
			details.target.name == 'standardname'
			details.target.group == 'standardgroup'
	}

	def "default dependency resolution action prioritizes both group and name highest"() {
		setup:
			DependencyResolveDetails details = details('prioritygroup:priorityname:changeme')
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'prioritygroupandnameversion'
			details.target.name == 'priorityname'
			details.target.group == 'prioritygroup'
	}

	def "default dependency resolution action prioritizes group only second highest"() {
		setup:
			DependencyResolveDetails details = details('priority2group:notfound:changeme')
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'priority2groupversion'
			details.target.name == 'notfound'
			details.target.group == 'priority2group'
	}

	def "default dependency resolution action prioritizes name only second highest"() {
		setup:
			DependencyResolveDetails details = details('notfound:priority3name:changeme')
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'priority3nameversion'
	}
	
	def "default dependency resolution action fails with unmapped direct dependency"() {
		setup:
			parent.dependencies {
				configuration "notfound:notfound:nochange"
			}
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolvedConfiguration
		then:
			thrown InvalidUserDataException
	}

	def "default dependency resolution action succeeds with unmapped transitive dependency"() {
		setup:
			DependencyResolveDetails details = details('notfound:notfound:nochange')
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then: 'resolution will succeed'
			config.resolvedConfiguration
	}

	def "default dependency resolution action can be configured to fail with unmapped transitive dependency"() {
		setup:
			DependencyResolveDetails details = details('notfound:notfound:nochange')
			configureDefaultDependencyResolutionAction(parent, config)
			parent.springioPlatform {
				failOnUnmappedTransitiveDependency = true
			}
		when:
			action.execute(Mock(ResolvableDependencies))
			config.resolutionStrategy.dependencyResolveRule.execute(details)
			config.resolvedConfiguration
		then:
			thrown InvalidUserDataException
	}

	def "default dependency resolution action can be configured to succeed with unmapped direct dependency"() {
		setup:
			parent.dependencies {
				configuration "notfound:notfound:nochange"
			}
			parent.springioPlatform {
				failOnUnmappedDirectDependency = false
			}
			configureDefaultDependencyResolutionAction(parent, config)
		when:
			action.execute(Mock(ResolvableDependencies))

		then: 'resolution will succeeed'
			config.resolvedConfiguration
	}
	
	void configureDefaultDependencyResolutionAction(Project project, Configuration configuration) {
		project.springioPlatform {
			dependencyResolutionAction = PlatformDependenciesBeforeResolveAction.createDefaultActionFromStream(parent, configuration, getClass().getResourceAsStream('test-springio-dependencies'))
		}
	}

	DependencyResolveDetails details(String path) {
		String[] parts = path.split(':')
		new DefaultDependencyResolveDetails(new DefaultModuleVersionSelector(parts[0],parts[1],parts[2]))
	}
}