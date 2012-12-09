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
import org.gradle.api.plugins.scala.ScalaBasePlugin;
import org.gradle.api.tasks.*
import org.gradle.plugins.ide.idea.IdeaPlugin

/**
 * Plugin to allow optional and provided dependency configurations to work with the
 * standard gradle 'idea' plugin
 *
 * @author Phillip Webb
 */
class PropDepsIdeaPlugin implements Plugin<Project> {

	public void apply(Project project) {
		project.plugins.apply(PropDepsPlugin)
		project.plugins.apply(IdeaPlugin)
		project.idea.module {
			scopes.PROVIDED.plus += project.configurations.provided
			// Add a new temporary scope to ensure option dependencies are not exported
			scopes.put("OPTIONAL", [plus: [project.configurations.optional], minus: []])
		}
		// Remove the temporary OPTIONAL scope
		project.idea.module.iml {
			whenMerged { module ->
				module.dependencies.each {
					if(it.scope == "OPTIONAL") {
						it.scope = "COMPILE"
					}
				}
			}
		}
	}

}
