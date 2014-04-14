package org.springframework.build.gradle.springio.platform

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

/**
 *
 * @author Rob Winch
 */
class ConfigureTestJdkTask extends DefaultTask {
	Test testTask
	String jdkVersion

	@TaskAction
	void configure() {
		def whichJdk = "${jdkVersion}_HOME"
		if(!project.hasProperty(whichJdk)) {
			throw new IllegalStateException("The property ${whichJdk} is not defined. Please ensure to define a valid JDK home as a commandline argument using -P${whichJdk}=<path> or as a property within your Gradle script.")
		}
		def jdkHome = project."${whichJdk}"
		def exec = project.file(jdkHome + '/bin/java')
		if(!exec.exists()) {
			throw new IllegalStateException("The path $exec does not exist! Please ensure to define a valid JDK home as a commandline argument using -P${whichJdk}=<path> or as a property within your Gradle script.")
		}
		testTask.executable = exec
	}
}
