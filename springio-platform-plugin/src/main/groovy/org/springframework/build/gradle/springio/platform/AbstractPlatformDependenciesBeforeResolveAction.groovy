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

	String resource = 'springio-dependencies'

	@Override
	public void execute(ResolvableDependencies resolvableDependencies) {
		Map<String, ModuleVersionSelector> selectors = createSelectorsFromStream(getClass().getResourceAsStream(resource))
		doExecute(resolvableDependencies, selectors)
	}

	abstract void doExecute(ResolvableDependencies resolvableDependencies, Map<String, ModuleVersionSelector> selectors)

	/**
	 * Reads the given stream, each line of which is expected to be in the format {@code group:artifact:version}, and
	 * returns a {@code Map<String, ModuleVersionSelector>} where the keys are of the form {@code group:version} and
	 * the values are ModuleVersion selectors created with {@code group}, {@code name}, and {@code version}.
	 *
	 * @param stream The stream to read the dependency information from
	 *
	 * @return The map of selectors
	 */
	private Map<String, ModuleVersionSelector> createSelectorsFromStream(InputStream stream) {
		Map<String,ModuleVersionSelector> depToSelector = [:]
		stream.eachLine { line ->
			if(line && !line.startsWith('#')) {
				def (group, name, version) = line.split(':')
				depToSelector.put("$group:$name" as String, new DefaultModuleVersionSelector(group, name, version))
			}
		}
		depToSelector
	}

	protected Set<String> getIgnoredDependencies() {
		project.rootProject.allprojects.collect { "$it.group:$it.name" as String }
	}

}
