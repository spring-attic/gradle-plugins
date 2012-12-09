/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.build.gradle.propdep

import org.gradle.api.*
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer
import org.gradle.api.artifacts.maven.MavenPom
import org.gradle.api.artifacts.maven.PomFilterContainer
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.*

/**
 * Plugin to allow optional and provided dependency configurations to work with the
 * standard gradle 'maven' plugin
 *
 * @author Phillip Webb
 */
class PropDepsMavenPlugin implements Plugin<Project> {

	public void apply(Project project) {
		project.plugins.apply(PropDepsPlugin)
		project.plugins.apply(MavenPlugin)

		Conf2ScopeMappingContainer scopeMappings = project.conf2ScopeMappings
		scopeMappings.addMapping(MavenPlugin.COMPILE_PRIORITY + 1,
			project.configurations.getByName("provided"), Conf2ScopeMappingContainer.PROVIDED)

		// Add a temporary new optional scope
		scopeMappings.addMapping(MavenPlugin.COMPILE_PRIORITY + 2,
			project.configurations.getByName("optional"), "optional")

		// Add a hook to replace the optional scope
		project.tasks.withType(Upload).each{ applyToUploadTask(project, it) }
	}

	private void applyToUploadTask(Project project, Upload upload) {
		upload.repositories.withType(PomFilterContainer).each{ applyToPom(project, it) }
	}

	private void applyToPom(Project project, PomFilterContainer pomContainer) {
		pomContainer.pom.whenConfigured{ MavenPom pom ->
			pom.dependencies.findAll{ it.scope == "optional" }.each {
				it.scope = "compile"
				it.optional = true
			}
		}
	}
}
