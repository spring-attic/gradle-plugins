## Overview
Provides additional `optional` and `provided` dependency configurations for Gradle
along with Maven POM generation support.

## Configuration
See [tags][1] to determine the
latest available version. Then configure the plugin in your project as
follows:
```groovy
buildscript {
    repositories {
        maven { url 'http://repo.springsource.org/plugins-release' }
    }
    dependencies {
        classpath 'org.springframework.build.gradle:propdeps-plugin:0.0.1'
    }
}

// ...

configure(allprojects) {
    apply plugin: 'propdeps'
    apply plugin: 'propdeps-maven'
    apply plugin: 'propdeps-idea'
    apply plugin: 'propdeps-eclipse'
}
```

## Usage

The `optional` and `provided` dependency configurations can be used in the same way
as existing configurations:

```groovy
dependencies {
	compile("commons-logging:commons-logging:1.1.1")
	optional("log4j:log4j:1.2.17")
	provided("javax.servlet:javax.servlet-api:3.0.1")
	testCompile("junit:junit:4.11")
}
```

No additional tasks are installed, however, existing `eclipse` `idea` and
`install` tasks are enhanced to support the new configurations:

```
$ gradle install
$ gradle eclipse
$ gradle idea
```

[1]: https://github.com/SpringSource/gradle-plugins/tags
