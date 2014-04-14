package org.springframework.build.gradle.springio.platform

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ModuleVersionSelection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author Rob Winch
 */
class ConfigureResolutionStrategyTask extends DefaultTask {
	Configuration configuration
	Action<DependencyResolveDetails> action

	@TaskAction
	void configure() {
		if(!action) {
			action = createDefaultActionFromStream(project,getClass().getResourceAsStream('springio-dependencies'))
		}
		configuration.resolutionStrategy.eachDependency action
	}

	/**
	 * Gets a Map<String,String> such that the key is in the format of "$group:$name" or "$group:" and the value is the
	 * version to use.
	 *
	 * @return
	 */
	private static MappingDependencyResolveDetailsAction createDefaultActionFromStream(Project project, InputStream stream) {
		Map<String,ModuleVersionSelector> depToSelector = [:]
		stream.eachLine { line ->
			if(line && !line.startsWith('#')) {
				def (group, name, version) = line.split(':')

				depToSelector.put("$group:$name".toString(), new DefaultModuleVersionSelector(group, name, version))
			}
		}
		Set<String> ignoredGroupAndNames = project.rootProject.allprojects.collect { "$it.group:$it.name" }
		new MappingDependencyResolveDetailsAction(depToSelector : depToSelector, ignoredGroupAndNames : ignoredGroupAndNames)
	}

	private static class MappingDependencyResolveDetailsAction implements Action<DependencyResolveDetails> {
		Map<String,ModuleVersionSelector> depToSelector = [:]
		/**
		 * In the format of group:name
		 */
		Set<String> ignoredGroupAndNames = []

		void execute(DependencyResolveDetails details) {
			ModuleVersionSelector requested = details.requested
			if(ignoredGroupAndNames.contains("$requested.group:$requested.name")) {
				return
			}

			ModuleVersionSelector mapping = getMapping("$requested.group:$requested.name") ?: (getMapping("$requested.group:") ?: getMapping(":$requested.name"))

			if(!mapping) {
				//logger.debug("Did NOT find mapping for $requested.group:$requested.name")
				return
			}

			details.useTarget new DefaultModuleVersionSelector( mapping.group ?: requested.group, mapping.name ?: requested.name, mapping.version ?: requested.version)
		}

		private ModuleVersionSelector getMapping(String id) {
			depToSelector[id]
		}
	}
}
