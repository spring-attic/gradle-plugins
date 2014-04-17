package org.springframework.build.gradle.springio.platform

import org.gradle.api.Action;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ResolutionStrategy

/**
 * @author Andy Wilkinson
 */
class SpringioPlatformExtension {

	/**
	 * The action to be run against each dependency in the {@code springioTestRuntime}
	 * configuration. May be {@code null} in which case the default action will be used.
	 * 
	 * @see ResolutionStrategy#eachDependency
	 */
	def dependencyResolutionAction
}
