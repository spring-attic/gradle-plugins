package org.springframework.build.gradle.springio;

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector

/**
 * @author Rob Winch
 * @author Andy Wilkinson
 */
abstract class AbstractPlatformDependenciesBeforeResolveAction implements Action<ResolvableDependencies> {

	Project project

	Configuration configuration

	Configuration versionsConfiguration

	@Override
	public void execute(ResolvableDependencies resolvableDependencies) {
		doExecute(resolvableDependencies, createSelectors())
	}

	abstract void doExecute(ResolvableDependencies resolvableDependencies, Map<String, ModuleVersionSelector> selectors)

	private Map<String, ModuleVersionSelector> createSelectors() {
		if (versionsConfiguration.incoming.files.empty) {
			throw new InvalidUserDataException("At least one properties file must be a dependency of the $versionsConfiguration.name configuration")
		}
		Map<String, ModuleVersionSelector> selectors = [:]
		versionsConfiguration.incoming.files.each {
			Properties properties = new Properties()
			it.withInputStream { is ->
				properties.load(is)
			}
			properties.each { key, version ->
				def (group, name) = key.split(':')
				selectors.put("$group:$name" as String, new DefaultModuleVersionSelector(group, name, version))
			}
		}
		selectors
	}

	protected Set<String> getIgnoredDependencies() {
		project.rootProject.allprojects.collect { "$it.group:$it.name" as String }
	}
}
