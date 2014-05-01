import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;

abstract class AbstractDependencyResolveDetailsAction implements Action<DependencyResolveDetails> {

	Map<String,ModuleVersionSelector> dependencyToSelector = [:]

	/**
	 * In the form {@code group:name}
	 */
	Set<String> ignoredDependencies = []

	void execute(DependencyResolveDetails details) {
		if (!isIgnoredDependency(details)) {
			ModuleVersionSelector selector = getSelector(details)
			execute(details, selector)
		}
	}

	protected abstract void execute(DependencyResolveDetails details, ModuleVersionSelector springIoMapping)

	private boolean isIgnoredDependency(DependencyResolveDetails details) {
		ignoredDependencies.contains(getId(details))
	}

	protected ModuleVersionSelector getSelector(DependencyResolveDetails details) {
		dependencyToSelector[getId(details)]
	}

	private String getId(DependencyResolveDetails details) {
		"$details.requested.group:$details.requested.name"
	}
}