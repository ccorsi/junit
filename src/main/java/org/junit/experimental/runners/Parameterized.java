package org.junit.experimental.runners;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * The custom runner <code>Parameterized</code> implements parameter based tests.
 * When using the parameterized test class, the instances are created for a given
 * set of parameter list values. The values are defined as two dimensional array
 * where each ith index is an array of values passed to the tests constructor. </p>    
 * 
 * For example, you want to create tests that uses a combinations of inputs.
 * 
 * <pre>
 * 
 * &#064;RunWith(Parameterized.class)
 * public class CombinationTest {
 * 
 *  &#064;Parameters(tests = "test")
 *  public static List&lt;Object[]&gt; inputs() {
 *     return Arrays.asList(new Object[][] {
 *         { { 1, "A" }, { 2, "B" },  { 3, "C" },
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
 * Each instance of the <code>Parameterized</code> test will be passed one of the list
 * parameter values returned from the call to the inputs static method.
 * </p>
 * 
 * The defined tests attribute of the Parameters is a regular expression that is
 * applied to the list of tests associated with the test.  If the regular expression is
 * satisfied then the test is executed using the set of parameter list. </p>
 * 
 * This runner is an extension of the standard Parameterized runner but this one
 * can associate a regular expression to each parameter list the will be used to determine
 * for which test those parameters will be applied to. </p>
 * 
 * The other extension that this runner has over the standard Parameterized runner is that
 * you can set multiple Parameters for a given test and be able to associate the different
 * list of Parameters will different tests. The example above will use the parameters passed
 * by the inputs static method for the test method only and will not pass those parameters
 * to any other test method.</p>
 *
 * Finally, the one remaining extension over the original implementation is that you can now
 * still use a default constructor and be able to set your parameters using public fields or
 * public methods.  The advantage of this feature is that it allows you more flexible tests
 * where you are not limited to a given set of parameters but to the number of accessible
 * fields and methods.  You can define a test class that contains multiple public fields and/or
 * methods that you can use to pass parameters to.  The Parameters attributes field allows 
 * you to define an associated set of fields or methods to pass the parameters to.  This 
 * flexibility allows you to create a single test class, Foo, that has multiple tests for 
 * different number of parameters. </p>
 * 
 * For example, let us take the prior example and make it use the attributes annotation
 * field instead. </p>
 *
 * <pre>
 * 
 * &#064;RunWith(Parameterized.class)
 * public class CombinationTest {
 * 
 *  &#064;Parameters(tests = "test", attributes = { "input", "name" })
 *  public static List&lt;Object[]&gt; inputs() {
 *     return Arrays.asList(new Object[][] {
 *         { { 1, "A" }, { 2, "B" },  { 3, "C" },
 *       });
 *  }
 *  
 *  private int input;
 *  
 *  public String name;
 *  
 *  public void setInput(int input) {
 *     this.input = input;
 *  }
 *
 *  public CombinationTest() {
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
 * Note that the constructor does not contain any parameter lists and is the only constructor available.
 * This is a requirement to be able to use the attributes field of the Parameters annotation.  You need
 * to pass the name{s} of the attributes in the same order as the list that you are returning. The value
 * will then be passed to the test using a field or method that is public to the test class.  In this case,
 * the public name field and the public setInput method is used to pass the returned values. </p>
 *
 * @author Claudio Corsi
 *
 */
public class Parameterized extends Suite {
	
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
		 * {index} - the current parameter index
		 * {0} - the first parameter value
		 * {1} - the second parameter value
		 * etc...
		 * </pre>
		 * <p>
		 * Default value is "{index}" for compatibility with previous JUnit
		 * versions.
		 * </p>
		 * 
		 * @return {@link MessageFormat} pattern string, except the index
		 *         placeholder.
		 * @see MessageFormat
		 */
		String name() default "{index}";

        /**
         * This is used to map the parameters to a given test class field or method.
         * The field has to be public and have the same name defined in this array.
         * The method should be prefixed by set and the first letter should be a 
         * capital letter.
         * The Parameterized test suite will look for a public field first and then
         * look for a public method if the field is not available.
         *
         * @return array of attributes names that will be used to set the attribute
         *         values with, defaults to empty array.
         */
        String[] attributes() default {};
	}
	
	private class TestClassRunnerForParameterized extends BlockJUnit4ClassRunner {

		private String name;
		private Object[] parameters;
		private Parameters annotation;
        private Map<String, CommandFactory> cmds;

		public TestClassRunnerForParameterized(Class<?> clazz, Object[] parameters, String name, Parameters annotation, Map<String, CommandFactory> cmds)
				throws InitializationError {
			super(clazz);
			this.name       = name;
			this.parameters = parameters;
			this.annotation = annotation;
            this.cmds       = cmds;
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
            if (cmds.isEmpty()) {
                // Pass the parameters to the Constructor...
                return getTestClass().getOnlyConstructor().newInstance(parameters);
            } else {
                if (cmds.size() != parameters.length) {
                    throw new IllegalArgumentException("Number of attribute names does not match the number of parameters");
                }
                Object instance = getTestClass().getOnlyConstructor().newInstance();
                String attributeNames[] = annotation.attributes();
                for(int idx = 0 ; idx < attributeNames.length ; idx++) {
                    // We need not worry that the attribute name was
                    // not found since this was already confirmed that
                    // it exists.
                    cmds.get(attributeNames[idx]).create(parameters[idx]).execute(instance);
                }
                return instance;
            }
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
						// This test will not be run for the given parameters...
						remove.add(method);
					}
				}
			}
			// Removes all noncompliant test methods for the given parameters....
			methods.removeAll(remove);
			// Return the resulting test methods, can be empty causing an failure...
			return methods;
		}
		
	}

	private static final List<Runner> NO_RUNNERS = Collections.emptyList();
	
	private List<Runner> runners = new LinkedList<Runner>();

	public Parameterized(Class<?> clz) throws Throwable {
		super(clz, NO_RUNNERS);
		List<FrameworkMethod> parametersMethods = getParametersMethods(getTestClass());
		for(FrameworkMethod frameworkMethod : parametersMethods) {
			Parameters annotation = frameworkMethod.getAnnotation(Parameters.class);
			String namePattern = annotation.name();
            String attributeNames[] = annotation.attributes();
            Map<String, CommandFactory> cmds = validateAttributes(getTestClass().getJavaClass(), 
                                                                  attributeNames);
			Iterable<Object[]> parametersList = getAllParameters(frameworkMethod);
			int idx = 0;
			for (Iterator<Object[]> iterator = parametersList.iterator() ; iterator.hasNext() ; idx++) {
				try {
					Object[] parameters= iterator.next();
					runners.add(new TestClassRunnerForParameterized(
							getTestClass().getJavaClass(), parameters, nameFor(
                                    namePattern, idx, parameters), annotation, cmds));
				} catch (ClassCastException e) {
					throw parametersMethodReturnedWrongType(frameworkMethod);
				}
			}
		}
	}
	
	static interface CommandFactory {
		Command create(Object value);
	}
	
	static class FieldCommandFactory implements CommandFactory {
		
		private Field field;

		FieldCommandFactory(Field field) {
			this.field = field;
		}

		/* (non-Javadoc)
		 * @see org.apache.activemq.junit.Combination.CommandFactory#create(java.lang.Object)
		 */
		public Command create(Object value) {
			return new FieldCommand(field, value);
		}
		
	}
	
	static class MethodCommandFactory implements CommandFactory {

		private Method method;

		MethodCommandFactory(Method method) {
			this.method = method;
		}
		
		/* (non-Javadoc)
		 * @see org.apache.activemq.junit.Combination.CommandFactory#create(java.lang.Object)
		 */
		public Command create(Object value) {
			return new MethodCommand(method, value);
		}
		
	}
	
	static interface Command {
		void execute(Object object) throws Exception;
	}
	
	static class FieldCommand implements Command {

		private Field field;
		private Object value;

		FieldCommand(Field field, Object value) {
			this.field = field;
			this.value = value;
		}
		
		/* (non-Javadoc)
		 * @see org.apache.activemq.junit.Combination.Command#execute(java.lang.Object)
		 */
		public void execute(Object object) throws Exception {
			field.set(object, value);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "[" + field.getName() + "," + value + "]";
		}
		
	}
	
	static class MethodCommand implements Command {
		
		private Method method;
		private Object value;

		MethodCommand(Method method, Object value) {
			this.method = method;
			this.value  = value;
		}

		/* (non-Javadoc)
		 * @see org.apache.activemq.junit.Combination.Command#execute(java.lang.Object)
		 */
		public void execute(Object object) throws Exception {
			method.invoke(object, value);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "[" + method.getName() + "," + value + "]";
		}
		
	}
	
	/**
	 * @param javaClass
	 * @param keySet
	 * @return returns a attribute name/command factory map
	 */
	private Map<String, CommandFactory> validateAttributes(Class<?> javaClass, String attributeNames[]) {
        if (attributeNames == null || attributeNames.length == 0) {
            return new HashMap<String, CommandFactory>();
        }
		// Get all reference of public attributes...
		Field fields[] = javaClass.getFields();
		Map<String, Field> fieldMap = new HashMap<String, Field>();
		for(Field field : fields) {
			fieldMap.put(field.getName(), field);
		}
		// Get all references of public methods...
		Method methods[] = javaClass.getMethods();
		Map<String, Method> methodMap = new HashMap<String, Method>();
		for(Method method : methods) {
			methodMap.put(method.getName(), method);
		}
		// Generate a attribute name / command factory map for passed attribute names....
		Map<String, CommandFactory> commands = new HashMap<String, CommandFactory>();
		for(String attributeName : attributeNames) {
			Field field = fieldMap.get(attributeName);
			if (field != null) {
				commands.put(attributeName, new FieldCommandFactory(field));
			} else {
				Method method = methodMap.get("set"
						+ attributeName.substring(0, 1).toUpperCase()
						+ attributeName.substring(1));
				if (method != null) {
					commands.put(attributeName, new MethodCommandFactory(method));
				} else {
					throw new IllegalArgumentException(
							"No public field or set method for attribute "
									+ attributeName);
				}
			}
		}
		return commands;
	}

	/**
	 * @param frameworkMethod
	 * @return
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	private Iterable<Object[]> getAllParameters(FrameworkMethod frameworkMethod)
			throws Throwable {
		Object parameters= frameworkMethod.invokeExplosively(null);
		if (parameters instanceof Iterable) {
			return (Iterable<Object[]>) parameters;
		} else {
			throw parametersMethodReturnedWrongType(frameworkMethod);
		}
	}

	private Exception parametersMethodReturnedWrongType(FrameworkMethod frameworkMethod) throws Exception {
		String className= getTestClass().getName();
		String methodName= frameworkMethod.getName();
		String message= MessageFormat.format(
				"{0}.{1}() must return an Iterable of arrays.",
				className, methodName);
		return new Exception(message);
	}

	private String nameFor(String namePattern, int index, Object[] parameters) {
		String finalPattern= namePattern.replaceAll("\\{index\\}",
				Integer.toString(index));
		String name= MessageFormat.format(finalPattern, parameters);
		return "[" + name + "]";
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
			throw new Exception("No public static parameters method(s) on class "
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
