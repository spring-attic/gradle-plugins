package org.springframework.build.gradle.springio.platform

import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.ResolvableDependencies

class CheckPlatformDependenciesBeforeResolveAction extends AbstractPlatformDependenciesBeforeResolveAction {

	boolean failOnUnmappedDirectDependency = true

	boolean failOnUnmappedTransitiveDependency = false

	@Override
	public void doExecute(ResolvableDependencies resolvableDependencies, Map<String, ModuleVersionSelector> selectors) {
		CheckingDependencyResolveDetailsAction checkingAction = new CheckingDependencyResolveDetailsAction(
			dependencyToSelector: selectors, configuration: configuration, ignoredDependencies: ignoredDependencies)

		configuration.resolutionStrategy.eachDependency checkingAction
		configuration.incoming.afterResolve {
			String message
			if (failOnUnmappedDirectDependency && checkingAction.unmappedDirectDependencies) {
				message = "The following direct dependencies do not have Spring IO versions: \n" + checkingAction.unmappedDirectDependencies.collect { "    - $it.group:$it.name" }.join("\n")
			}

			if (failOnUnmappedTransitiveDependency && checkingAction.unmappedTransitiveDependencies) {
				message = message ? message + ". " : ""
				message += "The following transitive dependencies do not have Spring IO versions: \n" + checkingAction.unmappedTransitiveDependencies.collect { "    - $it.group:$it.name" }.join("\n")
			}

			if (message) {
				message += "\nPlease refer to the plugin's README for further instructions: https://github.com/spring-projects/gradle-plugins/tree/master/springio-platform-plugin#dealing-with-unmapped-dependencies"
				throw new InvalidUserDataException(message)
			}
		}
	}

	private static class CheckingDependencyResolveDetailsAction extends AbstractDependencyResolveDetailsAction {

		Configuration configuration

		List<ModuleVersionSelector> unmappedDirectDependencies = []

		List<ModuleVersionSelector> unmappedTransitiveDependencies = []

		void execute(DependencyResolveDetails details, ModuleVersionSelector springIoMapping) {
			ModuleVersionSelector requested = details.requested
			if(!springIoMapping) {
				if (isDirectDependency(requested)) {
					unmappedDirectDependencies << requested
				} else {
					unmappedTransitiveDependencies << requested
				}
			} else {
				details.useTarget springIoMapping
			}
		}

		private boolean isDirectDependency(ModuleVersionSelector selector) {
			for (Dependency dependency: configuration.allDependencies) {
				if (dependency.group == selector.group && dependency.name == selector.name) {
					return true
				}
			}
			return false
		}
	}

}
