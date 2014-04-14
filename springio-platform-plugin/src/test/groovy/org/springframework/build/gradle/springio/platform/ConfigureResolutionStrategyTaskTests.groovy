package org.springframework.build.gradle.springio.platform

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector
import org.gradle.api.internal.artifacts.ivyservice.DefaultDependencyResolveDetails
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 *
 * @author Rob Winch
 */
class ConfigureResolutionStrategyTaskTests extends Specification {
	Project parent
	Project child
	Configuration config
	ConfigureResolutionStrategyTask task

	def setup() {
		parent = ProjectBuilder.builder().withName("parent").build()
		parent.group = 'thisprojectgroup'
		parent.version = 'nochange'

		config = parent.configurations.create('configuration')
		task = parent.tasks.create(name: SpringioPlatformPlugin.CONFIG_RESOLUTION_STRATEGY_TASK_NAME, type: ConfigureResolutionStrategyTask) {
			configuration = config
		}

		child = ProjectBuilder.builder().withName('child').withParent(parent).build()
		child.group = parent.group
		child.version = parent.version
	}

	def "configure adds Action as ResolutionStrategy Rule"() {
		setup:
			task.action = Mock(Action)
			DependencyResolveDetails details = details('org.springframework:spring-core:3.2.0.RELEASE')
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			1 * task.action.execute(details)
	}

	def "configure defaults Action"() {
		setup:
			DependencyResolveDetails details = details('org.springframework:spring-core:3.2.0.RELEASE')
		when:
			task.configure()
		then: 'Is of correct type'
			task.action instanceof ConfigureResolutionStrategyTask.MappingDependencyResolveDetailsAction
		and: 'Sanity check of the Mapping (detailed checks are done with testTask resource)'
			task.action.depToSelector['org.springframework:']
		and: 'Ignore these projects'
			task.action.ignoredGroupAndNames == ['thisprojectgroup:parent','thisprojectgroup:child'] as Set
	}

	def "default Action ignores these projects"() {
		setup:
			DependencyResolveDetails details = details('thisprojectgroup:child:nochange')
			task.action = defaultTestAction()
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'nochange'
			details.target.name == 'child'
			details.target.group == 'thisprojectgroup'
	}

	def "default Action supports group only"() {
		setup:
			DependencyResolveDetails details = details('grouponly:notdefined:changeme')
			task.action = defaultTestAction()
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'grouponlyversion'
			details.target.name == 'notdefined'
			details.target.group == 'grouponly'
	}

	def "default Action supports name only"() {
		setup:
			DependencyResolveDetails details = details('notdefined:nameonly:changeme')
			task.action = defaultTestAction()
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'nameonlyversion'
			details.target.name == 'nameonly'
			details.target.group == 'notdefined'
	}

	def "default Action supports name and group"() {
		setup:
			DependencyResolveDetails details = details('standardgroup:standardname:changeme')
			task.action = defaultTestAction()
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'standardversion'
			details.target.name == 'standardname'
			details.target.group == 'standardgroup'
	}

	def "default Action prioritizes both group and name highest"() {
		setup:
			DependencyResolveDetails details = details('prioritygroup:priorityname:changeme')
			task.action = defaultTestAction()
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'prioritygroupandnameversion'
			details.target.name == 'priorityname'
			details.target.group == 'prioritygroup'
	}

	def "default Action prioritizes group only second highest"() {
		setup:
			DependencyResolveDetails details = details('priority2group:notfound:changeme')
			task.action = defaultTestAction()
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'priority2groupversion'
			details.target.name == 'notfound'
			details.target.group == 'priority2group'
	}

	def "default Action prioritizes name only second highest"() {
		setup:
			DependencyResolveDetails details = details('notfound:priority3name:changeme')
			task.action = defaultTestAction()
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'priority3nameversion'
			details.target.name == 'priority3name'
			details.target.group == 'notfound'
	}

	def "default Action supports notfound"() {
		setup:
			DependencyResolveDetails details = details('notfound:notfound:nochange')
			task.action = defaultTestAction()
		when:
			task.configure()
			config.resolutionStrategy.dependencyResolveRule.execute(details)
		then:
			details.target.version == 'nochange'
			details.target.name == 'notfound'
			details.target.group == 'notfound'
	}

	Action<DependencyResolveDetails> defaultTestAction() {
		ConfigureResolutionStrategyTask.createDefaultActionFromStream(parent,getClass().getResourceAsStream('test-springio-dependencies'))
	}

	DependencyResolveDetails details(String path) {
		String[] parts = path.split(':')
		new DefaultDependencyResolveDetails(new DefaultModuleVersionSelector(parts[0],parts[1],parts[2]))
	}
}