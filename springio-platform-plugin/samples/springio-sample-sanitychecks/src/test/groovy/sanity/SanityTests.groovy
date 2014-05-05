package sanity

import spock.lang.IgnoreIf
import spock.lang.Specification

class SanityTests extends Specification {

	static final int JDK6_CLASS_VERSION = 50;

	@IgnoreIf({ javaVersion != 1.7 })
	def "Check that is JDK7"() {
		expect:
		System.getProperty('java.version').startsWith('1.7')
	}

	@IgnoreIf({ javaVersion != 1.8 })
	def "Check that is JDK8"() {
		expect:
		System.getProperty('java.version').startsWith('1.8')
	}

	@IgnoreIf({ System.getProperty('spring3') })
	def "Check Spring is not 3.x"() {
		expect:
		!org.springframework.core.SpringVersion.version.startsWith("3.")
	}

	def "Check Compiled with JDK6"() {
		expect:
		compiledMajorVersion == JDK6_CLASS_VERSION
	}

	def getCompiledMajorVersion() {
		String classResourceName = Sanity.getName().replaceAll("\\.", "/") + ".class"
		InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(classResourceName)
		try {
			DataInputStream data = new DataInputStream(input)
			data.readInt()
			data.readShort() // minor
			return data.readShort();
		} finally {
			try { input.close(); } catch(Exception e) {}
		}
	}
}