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

	/**
	 * Controls whether or not the build will fail when a direct dependency that does not
	 * have a Spring IO version mapping is encountered. Defaults to {@code true}.
	 */
	def failOnUnmappedDirectDependency = true

	/**
	 * Controls whether or not the build will fail when a transitive dependency that does
	 * not have a Spring IO version mapping is encountered. Defaults to {@code false}.
	 */
	def failOnUnmappedTransitiveDependency = false
}
