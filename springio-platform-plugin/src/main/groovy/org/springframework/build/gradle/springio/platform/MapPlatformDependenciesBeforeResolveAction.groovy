package org.springframework.build.gradle.springio.platform

import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.ResolvableDependencies;

class MapPlatformDependenciesBeforeResolveAction extends AbstractPlatformDependenciesBeforeResolveAction {

	@Override
	public void doExecute(ResolvableDependencies resolvableDependencies, Map<String, ModuleVersionSelector> selectors) {
		MappingDependencyResolveDetailsAction action = new MappingDependencyResolveDetailsAction(
			dependencyToSelector: selectors, ignoredDependencies: ignoredDependencies)
		configuration.resolutionStrategy.eachDependency action
	}

	private static class MappingDependencyResolveDetailsAction extends AbstractDependencyResolveDetailsAction {
		void execute(DependencyResolveDetails details, ModuleVersionSelector springIoMapping) {
			if(springIoMapping) {
				details.useTarget springIoMapping
			}
		}
	}
}
