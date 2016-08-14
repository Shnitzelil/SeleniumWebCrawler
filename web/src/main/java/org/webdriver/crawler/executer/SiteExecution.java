/**
 * 
 */
package org.webdriver.crawler.executer;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Took from https://github.com/saucelabs/parallel-test-examples/blob/master/java/junit/src/main/java/com/saucelabs/junit/Parallelized.java
 * 
 * @author ganak
 */
public class SiteExecution extends Suite {

	private static final int AWAIT_TERMINATION_TIMEOUT = 
			Integer.getInteger("parallelizedExecutionAwaitTerminationTimeout", 720);
    private static final int maximumThreads =
            Integer.getInteger("parallelizedExecutionMaximumThreads", 0);
	private static final TimeUnit delayDurationUnit =
			getDelayDurationUnit(TimeUnit.SECONDS);
	private static final int delayDurationValue =
			Integer.getInteger("parallelizedExecutionDelayDuration", 15);

	private static TimeUnit getDelayDurationUnit(TimeUnit defaultTimeUnit) {
		String parallelizedExecutionDelayDurationUnit = System.getProperty("parallelizedExecutionDelayDurationUnit", defaultTimeUnit.name()).toUpperCase();
		try {
			return TimeUnit.valueOf(parallelizedExecutionDelayDurationUnit);
		} catch (Exception e) {
			System.err.println("The value: " + parallelizedExecutionDelayDurationUnit + " is not supported by java.util.concurrent.TimeUnit");
		}
		return defaultTimeUnit;
	}

	private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {
		private final int fParameterSetNumber;

		private final List<Object[]> fParameterList;

		TestClassRunnerForParameters(Class<?> type, List<Object[]> parameterList, int index) throws InitializationError {
			super(type);
			fParameterList = parameterList;
			fParameterSetNumber = index;
		}

		@Override
		public Object createTest() throws Exception {
			return getTestClass().getOnlyConstructor().newInstance(computeParams());
		}

		private Object[] computeParams() throws Exception {
			try {
				return fParameterList.get(fParameterSetNumber);
			} catch (ClassCastException e) {
				throw new Exception(String.format("%s.%s() must return a Collection of arrays.", getTestClass().getName(), getParametersMethod(getTestClass()).getName()));
			}
		}

		@Override
		protected String getName() {
			return String.format("[%s]", getSiteName());
		}

		private String getSiteName() {
			return Arrays.toString(fParameterList.get(fParameterSetNumber)).replaceAll("[\\[\\]]", "");
		}

		@Override
		protected String testName(final FrameworkMethod method) {
			return String.format("%s", method.getName());
		}

		@Override
		protected void runChild(FrameworkMethod child, RunNotifier notifier) {
			super.runChild(child, notifier);
		}

		@Override
		protected List<FrameworkMethod> computeTestMethods() {
			List<FrameworkMethod> frameworkMethods = super.computeTestMethods();
			return frameworkMethods;
		}

		@Override
		protected void validateConstructor(List<Throwable> errors) {
			validateOnlyOneConstructor(errors);
		}

		@Override
		protected Statement classBlock(RunNotifier notifier) {
			return childrenInvoker(notifier);
		}
	}

	private static class ThreadPoolScheduler implements RunnerScheduler {
		private ExecutorService siteExecutor;

		public ThreadPoolScheduler() {
			siteExecutor = Executors.newFixedThreadPool(Integer.getInteger("maximum.sites", 3));
		}

		@Override
		public void finished() {
			siteExecutor.shutdown();
			try {
				siteExecutor.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.MINUTES);
			} catch (InterruptedException interruptedException) {
				throw new RuntimeException(interruptedException);
			}
		}

		@Override
		public void schedule(Runnable childStatement) {
			siteExecutor.submit(childStatement);
		}
	}

	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public SiteExecution(Class<?> klass) throws Throwable {
		super(klass, Collections.<Runner>emptyList());
		setScheduler(new ThreadPoolScheduler());
		List<Object[]> parametersList = getParametersList(getTestClass());
		for (int index = 0; index < parametersList.size(); index++)
			runners.add(new TestClassRunnerForParameters(getTestClass().getJavaClass(), parametersList, index));
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	@SuppressWarnings("unchecked")
	private List<Object[]> getParametersList(TestClass klass) throws Throwable {
		return (List<Object[]>) getParametersMethod(klass).invokeExplosively(null);
	}

	private FrameworkMethod getParametersMethod(TestClass testClass) throws Exception {
		List<FrameworkMethod> methods = testClass.getAnnotatedMethods(Parameterized.Parameters.class);
		for (FrameworkMethod each : methods) {
			int modifiers = each.getMethod().getModifiers();
			if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))
				return each;
		}

		throw new Exception("No public static parameters method on class " + testClass.getName());
	}

}
