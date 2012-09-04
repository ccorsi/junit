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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * The custom runner <code>Combination</code> implements attribute or
 * parameter based tests.  When using the combination test class, the
 * instances are created for different permutations of the given
 * values. The values are defined as a list.  Where each object array
 * is associated to a given attribute or test class constructor.  The
 * developer can use the <code>Attributes</code> annotation
 * <code>attributes</code> field to define the different attribute
 * name for each entry in the list. If the attributes field does not
 * contain any attribute names then this runner assumes that the test
 * class contains a constructor with the same number of parameters as
 * the size of the list and the same parameter types that are defined
 * for each object array. </p>
 *
 * If the attributes field of the Attributes annotation contain
 * attribute names then this runner will use those names to set the
 * values. It will initially determine if the field exists and is
 * public.  If it exists, it will use that field to set the value to.
 * If not, it will then determine if there is a set method a la java
 * bean.  If it exists, it will use that method to set the value else
 * it will generate an exception.</p>
 * 
 * If the attributes field of the Attributes annotation does not
 * contain any attribute names then this runner will use the test
 * class constructor to pass the values.  If no single valid
 * constructor was found or the constructor parameter type are
 * incompatible then an exception will be raised. </p>
 *
 * For example, you want to create tests that uses a combinations of inputs
 * that uses the attributes field.
 * 
 * <pre>
 * 
 * &#064;RunWith(Combination.class)
 * public class CombinationTest {
 * 
 *  &#064;Attributes(tests = "test", attributes = { "input", "name" })
 *  public static List&lt;Object[]&gt; inputs() {
 *     List&lt;Object[]&gt; inputs = new LinkedList&lt;Object[]&gt;();
 *     inputs.add(new Object[] { 1, 2, 3, 4, 5 });
 *     inputs.add(new Object[] { "A", "B", "C" });
 *     return inputs;
 *  }
 *  
 *  private int input;
 *  
 *  public String name;
 *  
 *  public CombinationTest() {}
 *  
 *  public void setInput(int input) {
 *     this.input = input;
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
 * Each instance of the <code>CombinationTest</code> test will be
 * passed one of all possible permutations of the two attribute input
 * and name returned from the call of the <code>inputs</code> static
 * method.  </p>
 * 
 * Note that the defined <code>tests</code> attribute of the
 * <code>Attributes</code> is a regular expression that is applied to
 * the list of tests associated with the test class.  If the regular
 * expression is satisfied then the test is executed using the set of
 * generated attribute list.</p>
 * 
 * Note also that you can define one or more static method annotated
 * by <code>Attributes</code> within a given test class and be able to
 * associated each methods to one or more test for the given test
 * class depending on the regular expression defined by the
 * <code>tests</code> field. </p>
 * 
 * Here is an example that uses the test class constructor to pass the
 * values from a list of object arrays. </p>
 * 
 * <pre>
 * 
 * &#064;RunWith(Combination.class)
 * public class CombinationTest {
 * 
 *  &#064;Attributes(tests = "test")
 *  public static List&lt;Object[]&gt; inputs() {
 *     List&lt;Object[]&gt; inputs = new LinkedList&lt;Object[]&gt;();
 *     inputs.add(new Object[] { 1, 2, 3, 4, 5 });
 *     inputs.add(new Object[] { "A", "B", "C" });
 *     return inputs;
 *  }
 *  
 *  private int input;
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
 * In this example we've decided not to use the
 * <code>attributes</code> field of the <code>Attributes</code>
 * annotation and instead decided to just pass the parameters using a
 * constructor instead. </p>
 *
 * You might be wondering when to use the constructor pattern or the
 * attribute pattern?  The pattern used depends on how many and want
 * types of parameters are used for each test of a given test class.
 * If each test uses the same parameter inputs then using the
 * constructor based approach should suffice.  If each test uses
 * different combination of inputs then using the attributes based
 * approach is better.  This approach provides you with the ability to
 * create a single test instance and perform these different tests
 * instead of having to create potentially multiple copies of the same
 * test class or sub-class a base class. </p>
 *
 * Note that this test suite was inspired by the 
 * <a href="http://svn.apache.org/viewvc/activemq/trunk/activemq-core/src/test/java/org/apache/activemq/CombinationTestSupport.java?view=markup">
 * CombinationTestSupport.java</a> class that is part of the ActiveMQ distribution. </p>
 *
 *
 * @author Claudio Corsi
 *
 */
public class Combination extends Suite {

	/**
	 * This is a simple Map.Entry implementation that will counteract the issue
	 * that you can not create an array of Map.Entry<K,V> elements.
	 * 
	 * @author Claudio Corsi
	 *
	 */
	private static class EntryImpl implements Map.Entry<String, Object[]> {

		private String key;
		private Object[] value;

		EntryImpl(String key, Object[] value) {
			this.key   = key;
			this.value = value;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Map.Entry#getKey()
		 */
		public String getKey() {
			return key;
		}

		/* (non-Javadoc)
		 * @see java.util.Map.Entry#getValue()
		 */
		public Object[] getValue() {
			return value;
		}

		/* (non-Javadoc)
		 * @see java.util.Map.Entry#setValue(java.lang.Object)
		 */
		public Object[] setValue(Object[] value) {
			return this.value;
		}

	}

	/**
	 * This annotation is used for a static method which will return an array of arrays
	 * with input used to create each instance of the test class.  These parameters are
	 * used to create every permutation possible and are then passed to the test
	 * constructor. </p>
	 * 
	 * Note that the returned instance has to be a Map<String,Object[]> type where the
	 * key is the name of the attribute and the value is an Object array of values that 
	 * will be set for the given attribute. </p>
	 * 
	 * It is required that the test class contain a public field with the key name or
	 * that it contains a public set method where it starts with set and the attribute
	 * name where the first letter is a capital. For example, input should contain a
	 * method call setInput(T input). Where the parameter of the method is the same
	 * type as the values that are associated within the returned map. </p>
	 * 
	 * @author Claudio Corsi
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Attributes {
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
	
	private static class TestClassRunnerForCombination extends BlockJUnit4ClassRunner {

		private Command[]  commands;
		private Attributes annotation;
		private String     name;

		public TestClassRunnerForCombination(Class<?> clazz, Command commands[], String name, Attributes annotation)
				throws InitializationError {
			super(clazz);
			this.commands   = commands;
			this.annotation = annotation;
			this.name       = name;
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
			Object test = getTestClass().getOnlyConstructor().newInstance();
			for(Command command : commands) {
				command.execute(test);
			}
			return test;
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

	private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {

		private String name;
		private Object[] parameters;
		private Attributes annotation;

		public TestClassRunnerForParameters(Class<?> clazz, Object[] parameters, String name, Attributes annotation)
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

	public Combination(Class<?> clz) throws Throwable {
		super(clz, NO_RUNNERS);
		List<FrameworkMethod> attributesMethods = getAttributesMethods(getTestClass());
		Class<?> javaClass = getTestClass().getJavaClass();
		for(FrameworkMethod frameworkMethod : attributesMethods) {
            Attributes annotation = frameworkMethod.getAnnotation(Attributes.class);
            String attributeNames[] = annotation.attributes();
            String namePattern= annotation.name();
			List<Object[]> attributesList = getAllAttributes(frameworkMethod);
			try {
				Map<String, CommandFactory> factories= validateAttributes(
                        javaClass, attributeNames);
                if (factories.isEmpty()) {
                    for (Map.Entry<Object[], int[]> parameters : new ParametersListIterable(attributesList) ) {
                        Object objects[] = parameters.getKey();
                        runners.add(new TestClassRunnerForParameters(getTestClass().getJavaClass(),
                                objects, nameFor(namePattern, objects, parameters.getValue()), annotation));
                    }
                } else {
                    for (Map.Entry<Command[], int[]> commands : new AttributesListIterable(
                            factories, attributesList, attributeNames)) {
                        Command cmds[]= commands.getKey();
                        runners.add(new TestClassRunnerForCombination(javaClass,
                            cmds, nameFor(namePattern, cmds), annotation));
                    }
                }
			} catch (ClassCastException e) {
				throw attributesMethodReturnedWrongType(frameworkMethod);
			}
		}
	}

	/**
	 * @param frameworkMethod
	 * @return
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	private List<Object[]> getAllAttributes(
			FrameworkMethod frameworkMethod) throws Throwable {
		Object attributes= frameworkMethod.invokeExplosively(null);
		if (attributes instanceof List) {
			return (List<Object[]>) attributes;
		} else {
			throw attributesMethodReturnedWrongType(frameworkMethod);
		}
	}

	private Exception attributesMethodReturnedWrongType(FrameworkMethod frameworkMethod) throws Exception {
		String className= getTestClass().getName();
		String methodName= frameworkMethod.getName();
		String message= MessageFormat.format(
				"{0}.{1}() must return a List of Object arrays.",
				className, methodName);
		return new Exception(message);
	}

	private String nameFor(String namePattern, Command[] commands) {
		List<Command> list = Arrays.asList(commands);
		String finalPattern= namePattern.replaceAll("\\{list\\}",
				list.toString());
		String name= MessageFormat.format(finalPattern, list.toArray());
		return name;
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
	
	static interface CommandFactory {
		Command create(Object value);
	}
	
	static class FieldCommandFactory implements CommandFactory {
		
		private Field field;

		FieldCommandFactory(Field field) {
			this.field = field;
		}

		/* (non-Javadoc)
		 * @see org.junit.experimental.runners.Combination.CommandFactory#create(java.lang.Object)
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
		 * @see org.junit.experimental.runners.Combination.CommandFactory#create(java.lang.Object)
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
		 * @see org.junit.experimental.runners.Combination.Command#execute(java.lang.Object)
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
		 * @see org.junit.experimental.runners.Combination.Command#execute(java.lang.Object)
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
	 * @param attributeNames
	 * @return returns a attribute name/command factory map
	 */
	private Map<String, CommandFactory> validateAttributes(Class<?> javaClass, String[] attributeNames) {
        // If no attribute names were passed then we are going to use
        // a constructor to pass the values to the test.
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

	static class AttributesListIterable implements Iterable<Map.Entry<Command[], int[]>> {

		private Map<String, CommandFactory> factories;
		private List<Object[]>              attributesList;
        private String[]                    attributeNames;

		AttributesListIterable(Map<String, CommandFactory> factories,
                List<Object[]> attributesList, String[] attributeNames) {
			this.factories      = factories;
			this.attributesList = attributesList;
            this.attributeNames = attributeNames;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		public Iterator<Entry<Command[], int[]>> iterator() {
			return new AttributesListIterator(this.factories, this.attributesList, this.attributeNames);
		}
		
		
		/**
		 * @author Claudio Corsi
		 *
		 */
		public static class AttributesListIterator implements
				Iterator<Entry<Command[], int[]>> {

			private int sizes[];
			private int startIndexes[];
			private Map.Entry<String, Object[]> entries[];
			private Object[] objects;
			private int[] indexes;
			private Map<String, CommandFactory> factories;

			/**
			 * @param factories
			 * @param attributesList
             * @param attributeNames;
			 */
			public AttributesListIterator(Map<String, CommandFactory> factories,
                    List<Object[]> attributesList, String[] attributeNames) {
				this.factories = factories;
				sizes = new int[attributesList.size()];
				startIndexes = new int[sizes.length];
				Arrays.fill(startIndexes, 0);
				entries = new EntryImpl[attributesList.size()];
                for(int index = 0 ; index < attributeNames.length ; index++) {
                    Object[] values = attributesList.get(index);
					entries[index] = new EntryImpl(attributeNames[index], values);
					sizes[index]   = values.length;
				}
				objects = new Object[sizes.length];
				indexes = new int[sizes.length];
				// Setup a current set of defaults that start with the zero indexed version....
				for(int cnt = 0 ; cnt < sizes.length ; cnt++) {
                    Object[] values = entries[cnt].getValue();
					objects[cnt] = (values.length > 0) ? values[0] : null;
					indexes[cnt] = 0;
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
			public Entry<Command[], int[]> next() {
				if (hasNext() == false) {
					throw new NoSuchElementException("No elements remaining");
				}
				Object[] curObjects = new Object[sizes.length];
				int[] curIndexes = new int[sizes.length];
				System.arraycopy(objects, 0, curObjects, 0, sizes.length);
				System.arraycopy(indexes, 0, curIndexes, 0, sizes.length);
				incrementIndexes();
				return createCommands(curObjects, factories, entries, indexes);
			}

			/**
			 * 
			 */
			private void incrementIndexes() {
				for (int curRow = sizes.length - 1 ; curRow > -1 ; curRow--) {
					indexes[curRow]++;
					if (indexes[curRow] < sizes[curRow]) {
						objects[curRow] = entries[curRow].getValue()[indexes[curRow]];
						break; // exit while loop, we are done...
					} else if (curRow != 0) {
						indexes[curRow] = 0;
						objects[curRow] = entries[curRow].getValue()[0];
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


	/**
	 * @param factories 
	 * @param curObjects
	 * @param entries
	 * @param indexes 
	 * @return
	 */
	private static Map.Entry<Command[], int[]> createCommands(Object[] objects,
			Map<String, CommandFactory> factories, Entry<String, Object[]>[] entries, int[] indexes) {
		Command commands[] = new Command[objects.length];
		for(int idx = 0 ; idx < objects.length ; idx++) {
			commands[idx] = factories.get(entries[idx].getKey()).create(objects[idx]);
		}
		int indexesCopy[] = new int[indexes.length];
		System.arraycopy(indexes, 0, indexesCopy, 0, indexes.length);
		return new Map.Entry<Combination.Command[], int[]>() {

			private Command[] commands;
			private int[] indexes;

			public Command[] getKey() {
				return commands;
			}

			public Entry<Command[], int[]> setKeyValue(Command[] commands,
					int[] indexes) {
				this.commands = commands;
				this.indexes  = indexes;
				return this;
			}

			public int[] getValue() {
				return indexes;
			}

			public int[] setValue(int[] value) {
				return null;
			}
		}.setKeyValue(commands, indexesCopy);
	}

	private List<FrameworkMethod> getAttributesMethods(TestClass testClass)
			throws Exception {
		List<FrameworkMethod> parametersMethods = new LinkedList<FrameworkMethod>();
		List<FrameworkMethod> methods = testClass
				.getAnnotatedMethods(Attributes.class);
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
