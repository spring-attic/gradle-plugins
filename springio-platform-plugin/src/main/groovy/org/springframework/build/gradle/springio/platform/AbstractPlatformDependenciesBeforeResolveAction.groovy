package org.springframework.build.gradle.springio.platform;

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyResolveDetails
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

	String resource = 'springio-dependencies.properties'

	@Override
	public void execute(ResolvableDependencies resolvableDependencies) {
		Properties properties = new Properties()
		Map<String, ModuleVersionSelector> selectors
		getClass().getResource(resource).withInputStream { is ->
			properties.load(is)
		}
		doExecute(resolvableDependencies, createSelectorsFromProperties(properties))
	}

	abstract void doExecute(ResolvableDependencies resolvableDependencies, Map<String, ModuleVersionSelector> selectors)

	/**
	 * Uses the given Properties of the form {@code group:name=version} and returns a
	 * {@code Map<String, ModuleVersionSelector>} where the keys are of the form {@code group:version} and the values
	 * are ModuleVersion selectors created with {@code group}, {@code name}, and {@code version}.
	 *
	 * @param Properties The dependency information
	 *
	 * @return The map of selectors
	 */
	private Map<String, ModuleVersionSelector> createSelectorsFromProperties(Properties properties) {
		Map<String,ModuleVersionSelector> depToSelector = [:]
		properties.each { key, version ->
			def (group, name) = key.split(':')
			depToSelector.put("$group:$name" as String, new DefaultModuleVersionSelector(group, name, version))
		}
		depToSelector
	}

	protected Set<String> getIgnoredDependencies() {
		project.rootProject.allprojects.collect { "$it.group:$it.name" as String }
	}

}
