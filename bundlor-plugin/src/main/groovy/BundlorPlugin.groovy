import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

/**
 * Contribute a 'bundlor' task capable of creating an OSGi manifest. Task is tied
 * to the lifecycle by having the 'jar' task depend on 'bundlor'.
 *
 * @author Chris Beams
 * @author Luke Taylor
 * @see http://www.springsource.org/bundlor
 * @see http://static.springsource.org/s2-bundlor/1.0.x/user-guide/html/ch04s02.html
 */
public class BundlorPlugin implements Plugin<Project> {

    public void apply(Project project) {

        project.configurations { bundlorconf }
        project.dependencies {
            bundlorconf 'com.springsource.bundlor:com.springsource.bundlor.ant:1.0.0.RELEASE',
                    'com.springsource.bundlor:com.springsource.bundlor:1.0.0.RELEASE',
                    'com.springsource.bundlor:com.springsource.bundlor.blint:1.0.0.RELEASE'
        }

        project.tasks.create("bundlor") {

            ext {
                dependsOn project.compileJava
                group = 'Build'
                description = 'Generates an OSGi-compatibile MANIFEST.MF file.'

                enabled = true
                failOnWarnings = true
                bundleName = null
                bundleVersion = project.version
                bundleVendor = 'SpringSource'
                bundleSymbolicName = null
                bundleManifestVersion = '2'
                importPackage = []
                importTemplate = []
                manifestTemplate = null

                outputDir = new File("${project.buildDir}/bundlor")
            }

            def manifest = new File("${outputDir}/META-INF/MANIFEST.MF")

            // inform gradle what directory this task writes so that
            // it can be removed when issuing `gradle cleanBundlor`
            outputs.dir outputDir

            // incremental build configuration
            //   if the manifest output file already exists, the bundlor
            //   task will be skipped *unless* any of the following are true
            //   * manifestTemplate or other task properties have been changed
            //   * main classpath dependencies have been changed
            //   * main java sources for this project have been modified
            outputs.files manifest
            /* TODO ask gradle team how to do this such that inputs are lazy
            inputs.property 'bundleName', bundleName
            inputs.property 'bundleVersion', bundleVersion
            inputs.property 'bundleVendor', bundleVendor
            inputs.property 'bundleSymbolicName', bundleSymbolicName
            inputs.property 'bundleManifestVersion', bundleManifestVersion
            inputs.property 'manifestTemplate', manifestTemplate
            inputs.property 'importTemplate', importTemplate
            */
            inputs.files project.sourceSets.main.runtimeClasspath

            // the bundlor manifest should be evaluated as part of the jar task's
            // incremental build
            project.jar {
                dependsOn 'bundlor'
                inputs.files manifest
            }

            project.jar.manifest.from manifest

            doFirst {
                if (bundleName == null)
                    bundleName = project.description

                project.ant.taskdef(
                    resource: 'com/springsource/bundlor/ant/antlib.xml',
                    classpath: project.configurations.bundlorconf.asPath)

                // the bundlor ant task writes directly to standard out
                // redirect it to INFO level logging, which gradle will
                // deal with gracefully
                logging.captureStandardOutput(LogLevel.INFO)

                // the ant task will throw unless this dir exists
                if (!outputDir.isDirectory())
                    outputDir.mkdir()


                // execute the ant task, and write out the manifest file
                project.ant.bundlor(
                        enabled: enabled,
                        inputPath: project.sourceSets.main.output.classesDir,
                        outputPath: outputDir,
                        bundleVersion: bundleVersion,
                        failOnWarnings: failOnWarnings) {
                    if (manifestTemplate == null) {
                        assert bundleSymbolicName != null
                        assert bundleVendor != null
                        assert bundleName != null
                        manifestTemplate = """\
                            Bundle-Vendor: ${bundleVendor}
                            Bundle-Version: ${bundleVersion}
                            Bundle-Name: ${bundleName}
                            Bundle-ManifestVersion: ${bundleManifestVersion}
                            Bundle-SymbolicName: ${bundleSymbolicName}
                        """.stripIndent()
                        if (!importPackage.isEmpty()) {
                            manifestTemplate += "Import-Package: "
                            importPackage.each { entry ->
                                manifestTemplate += "\n " + entry
                                if (entry != importPackage.last()) {
                                    manifestTemplate += ','
                                }
                                else {
                                    manifestTemplate += '\n'
                                }
                            }
                        }
                        if (!importTemplate.isEmpty()) {
                            manifestTemplate += "Import-Template: "
                            importTemplate.each { entry ->
                                manifestTemplate += "\n " + entry
                                if (entry != importTemplate.last()) {
                                    manifestTemplate += ','
                                }
                            }
                        }
                        logger.info('Using generated bundlor manifest template:')
                    } else {
                        logger.info('Using explicit bundlor manifest template:')
                    }
                    logger.info('-------------------------------------------------')
                    logger.info(manifestTemplate)
                    logger.info('-------------------------------------------------')
                    manifestTemplate(manifestTemplate)
                }
            }
        }
    }
}
