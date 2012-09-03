package org.junit.experimental.runners;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * The custom runner <code>Permutation</code> implements parameter based tests.
 * When using the permutation test class, the instances are created for different
 * permutations of the given values. The values are defined as two dimensional array
 * where each ith index is an array of values for the ith parameter of the tests
 * constructor. </p>    
 * 
 * For example, you want to create tests that uses a combinations of inputs.
 * 
 * <pre>
 * 
 * &#064;RunWith(Permutation.class)
 * public class CombinationTest {
 * 
 *  &#064;Parameters(tests = "test")
 *  public static List&lt;Object[]&gt; inputs() {
 *     return Arrays.asList(new Object[][] {
 *         { 1, 2, 3, 4, 5 },
 *         { "A", "B", "C" },
 *       });
 *  }
 *  
 *  private int input;
 *  
 *  private String name;
 *  
 *  public CombinationTest(int input, String name) {
 *     this.input = input;
 *     this.name  = name;
 *  }
 *  
 *  &#064;Test
 *  public void test() {
 *     // use input and name to run a test...
 *    ....
 *  }
 * }
 * </pre>
 * 
 * Each instance of the <code>Permutation</code> test will be passed one of all possible
 * permutations of the two argument input returned from the call to the inputs static method.
 * </p>
 * 
 * The defined tests attribute of the Parameters is a regular expression that is
 * applied to the list of tests associated with the test.  If the regular expression is
 * satisfied then the test is executed using the set of generated parameter list.
 * 
 * @author Claudio Corsi
 *
 */
public class Permutation extends Suite {
	
	/**
	 * This annotation is used for a static method which will return an array of arrays
	 * with input used to create each instance of the test class.  These parameters are
	 * used to create every permutation possible and are then passed to the test
	 * constructor. 
	 * 
	 * @author Claudio Corsi
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Parameters {
		/**
		 * This is used to define the regular expression used to determine which tests will
		 * use the give permutations of the given parameters.
		 * 
		 * @return regular expression used to contain tests
		 */
		String tests() default ".*";

		/**
		 * <p>
		 * Optional pattern to derive the test's name from the parameters. Use
		 * numbers in braces to refer to the parameters or the additional data
		 * as follows:
		 * </p>
		 * 
		 * <pre>
		 * {list} - the parameter list
		 * {0} - the first parameter value
		 * {1} - the second parameter value
		 * etc...
		 * </pre>
		 * 
		 * @return {@link MessageFormat} pattern string, except the index
		 *         placeholder.
		 * @see MessageFormat
		 */
		String name() default "{list}";
	}
	
	private class TestClassRunnerForPermutations extends BlockJUnit4ClassRunner {

		private String name;
		private Object[] parameters;
		private Parameters annotation;

		public TestClassRunnerForPermutations(Class<?> clazz, Object[] parameters, String name, Parameters annotation)
				throws InitializationError {
			super(clazz);
			this.name       = name;
			this.parameters = parameters;
			this.annotation = annotation;
		}

		/* (non-Javadoc)
		 * @see org.junit.runners.BlockJUnit4ClassRunner#validateConstructor(java.util.List)
		 */
		@Override
		protected void validateConstructor(List<Throwable> errors) {
			validateOnlyOneConstructor(errors);
		}

		/* (non-Javadoc)
		 * @see org.junit.runners.BlockJUnit4ClassRunner#createTest()
		 */
		@Override
		protected Object createTest() throws Exception {
			return getTestClass().getOnlyConstructor().newInstance(parameters);
		}

		/* (non-Javadoc)
		 * @see org.junit.runners.BlockJUnit4ClassRunner#testName(org.junit.runners.model.FrameworkMethod)
		 */
		@Override
		protected String testName(FrameworkMethod method) {
			return method.getName() + getName();
		}

		/* (non-Javadoc)
		 * @see org.junit.runners.ParentRunner#classBlock(org.junit.runner.notification.RunNotifier)
		 */
		@Override
		protected Statement classBlock(RunNotifier notifier) {
			return childrenInvoker(notifier);
		}

		/* (non-Javadoc)
		 * @see org.junit.runners.ParentRunner#getName()
		 */
		@Override
		protected String getName() {
			return name;
		}

		/* (non-Javadoc)
		 * @see org.junit.runners.ParentRunner#getRunnerAnnotations()
		 */
		@Override
		protected Annotation[] getRunnerAnnotations() {
			return new Annotation[0];
		}

		/* (non-Javadoc)
		 * @see org.junit.runners.BlockJUnit4ClassRunner#computeTestMethods()
		 */
		@Override
		protected List<FrameworkMethod> computeTestMethods() {
			List<FrameworkMethod> methods = super.computeTestMethods();
			List<FrameworkMethod> remove = new LinkedList<FrameworkMethod>();
			if (this.annotation != null) {
				String regex = this.annotation.tests();
				for (FrameworkMethod method : methods) {
					String methodName = method.getMethod().getName();
					if (! methodName.matches(regex)) {
						remove.add(method);
					}
				}
			}
			// Removes all not compliant methods with the parameters.
			methods.removeAll(remove);
			// Return the resulting test methods, can be empty causing an failure...
			return methods;
		}
		
	}

	private static final List<Runner> NO_RUNNERS = Collections.emptyList();
	
	private List<Runner> runners = new LinkedList<Runner>();

	public Permutation(Class<?> clz) throws Throwable {
		super(clz, NO_RUNNERS);
		List<FrameworkMethod> parametersMethods = getParametersMethods(getTestClass());
		for(FrameworkMethod frameworkMethod : parametersMethods) {
			Parameters annotation = frameworkMethod.getAnnotation(Parameters.class);
			String namePattern = annotation.name();
			Iterable<Object[]> parametersList = getAllParameters(frameworkMethod);
			for (Map.Entry<Object[], int[]> parameters : new ParametersListIterable(parametersList) ) {
				Object objects[] = parameters.getKey();
				runners.add(new TestClassRunnerForPermutations(getTestClass().getJavaClass(),
						objects, nameFor(namePattern, objects, parameters.getValue()), annotation));
			}
		}
	}

	/**
	 * @param frameworkMethod
	 * @return
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	private List<Object[]> getAllParameters(FrameworkMethod frameworkMethod)
			throws Throwable {
		return (List<Object[]>) frameworkMethod.invokeExplosively(
				null);
	}

	private String nameFor(String namePattern, Object[] objects, int[] indexes) {
		List<Object> list = new LinkedList<Object>();
		for(int index : indexes) {
			list.add(index);
		}
		String finalPattern= namePattern.replaceAll("\\{list\\}",
				list.toString());
		String name= MessageFormat.format(finalPattern, Arrays.asList(objects).toArray());

		return name;
	}
	
	static class ParametersListIterable implements Iterable<Map.Entry<Object[], int[]>> {

		private Iterable<Object[]> parametersList;

		ParametersListIterable(Iterable<Object[]> parametersList) {
			this.parametersList = parametersList;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		public Iterator<Entry<Object[], int[]>> iterator() {
			return new ParametersListIterator(this.parametersList);
		}
		
	}
	
	static class ParametersListIterator implements Iterator<Map.Entry<Object[], int[]>> {

		private Object[][] parametersArrs;
		private int        sizes[];
		private int        startIndexes[];
		private Object[]   objects;
		private int[]      indexes;

		ParametersListIterator(Iterable<Object[]> parametersList) {
			List<Object[]> list = new LinkedList<Object[]>();
			for(Object[] objects : parametersList) {
				list.add(objects);
			}
			parametersArrs = list.toArray(new Object[0][]);
			sizes = new int[parametersArrs.length];
			startIndexes = new int[sizes.length];
			Arrays.fill(startIndexes, 0);
			int index = 0;
			for(Object[] parameters : parametersList) {
				sizes[index++] = parameters.length;
			}
			objects = new Object[sizes.length];
			indexes = new int[sizes.length];
			// Setup a current set of defaults that start with the zero indexed version....
			for( int curRow = 0 ; curRow < sizes.length ; curRow++ ) {
				// Defense check against someone passed an empty object array
				objects[curRow] = (parametersArrs[curRow].length > 0) ? parametersArrs[curRow][0] : null;
				indexes[curRow] = 0;
			}
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return indexes[0] < sizes[0];
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Entry<Object[], int[]> next() {
			if (hasNext() == false) {
				throw new NoSuchElementException("No elements remaining");
			}
			Object[] curObjects = new Object[sizes.length];
			int[] curIndexes = new int[sizes.length];
			System.arraycopy(objects, 0, curObjects, 0, sizes.length);
			System.arraycopy(indexes, 0, curIndexes, 0, sizes.length);
			incrementIndexes();
			return createEntry(curObjects, curIndexes);
		}

		/**
		 * 
		 */
		private void incrementIndexes() {
			// Find the next starting entry....
			for (int curRow = sizes.length - 1; curRow > -1; curRow--) {
				indexes[curRow]++;
				if (indexes[curRow] < sizes[curRow]) {
					objects[curRow] = parametersArrs[curRow][indexes[curRow]];
					break; // exit while loop we are done...
				} else if (curRow != 0) {
					indexes[curRow] = 0;
					objects[curRow] = parametersArrs[curRow][0];
				}
			}
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException("remove method is not supported");
		}
		
	}
	
	/**
	 * @param objects
	 * @param indexes
	 * @return
	 */
	private static Entry<Object[], int[]> createEntry(Object[] objects, int[] indexes) {
		return new Map.Entry<Object[], int[]>() {
			
			private Object[] key;
			private int[] value;

			public Object[] getKey() {
				return key;
			}

			public Entry<Object[], int[]> init(Object[] curObjects,
					int[] curIndexes) {
				this.key   = curObjects;
				this.value = curIndexes;
				return this;
			}

			public int[] getValue() {
				return value;
			}

			public int[] setValue(int[] arg0) {
				return value;
			}
		}.init(objects, indexes);
	}

	private List<FrameworkMethod> getParametersMethods(TestClass testClass)
			throws Exception {
		List<FrameworkMethod> parametersMethods = new LinkedList<FrameworkMethod>();
		List<FrameworkMethod> methods = testClass
				.getAnnotatedMethods(Parameters.class);
		for (FrameworkMethod each : methods) {
			int modifiers= each.getMethod().getModifiers();
			if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))
				parametersMethods.add(each);
		}

		if (parametersMethods.isEmpty()) {
			throw new Exception("No public static parameters methods on class "
					+ testClass.getName());
		}
		
		return parametersMethods;
	}

	/* (non-Javadoc)
	 * @see org.junit.runners.Suite#getChildren()
	 */
	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

}
