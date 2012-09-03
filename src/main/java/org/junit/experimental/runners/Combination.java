/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * The custom runner <code>Combination</code> implements attribute based tests.
 * When using the combination test class, the instances are created for different
 * permutations of the given values. The values are defined as a map.  Where the
 * key is the attribute field name and the values is an array of Object instances.
 * This runner will then use the field names to set the values. It will initially
 * determine if the field exists and is public.  If it exists, it will use that
 * field to set the value to.  If not, it will then determine if there is a set 
 * method a la java bean.  If it exists, it will use that method to set the value
 * else it will generate an exception.</p>    
 * 
 * For example, you want to create tests that uses a combinations of inputs.
 * 
 * <pre>
 * 
 * &#064;RunWith(Combination.class)
 * public class CombinationTest {
 * 
 *  &#064;Attributes(tests = "test")
 *  public static Map&lt;String,Object[]&gt; inputs() {
 *     Map&lt;String,Object[]&gt; inputs = new HashMap&lt;String,Object[]&gt;();
 *     map.put("input", new Object[] { 1, 2, 3, 4, 5 });
 *     map.put("name",  new Object[] { "A", "B", "C" });
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
 * Each instance of the <code>CombinationTest</code> test will be passed one of all possible
 * permutations of the two attribute input returned from the call to the inputs static method.
 * </p>
 * 
 * The defined tests attribute of the Attributes is a regular expression that is
 * applied to the list of tests associated with the test class.  If the regular expression is
 * satisfied then the test is executed using the set of generated attribute list.</p>
 * 
 * Note also that you can define one or more static annotated Attributes methods within a 
 * given test class and be able to associated each methods to one or more test for the given
 * test class depending on the regular expression defined by the tests field. </p>
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
	}
	
	private static class TestClassRunnerForPermutations extends BlockJUnit4ClassRunner {

		private Command[]  commands;
		private Attributes annotation;
		private String     name;

		public TestClassRunnerForPermutations(Class<?> clazz, Command commands[], String name, Attributes annotation)
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

	private static final List<Runner> NO_RUNNERS = Collections.emptyList();
	
	private List<Runner> runners = new LinkedList<Runner>();

	public Combination(Class<?> clz) throws Throwable {
		super(clz, NO_RUNNERS);
		List<FrameworkMethod> attributesMethods = getAttributesMethods(getTestClass());
		Class<?> javaClass = getTestClass().getJavaClass();
		for(FrameworkMethod frameworkMethod : attributesMethods) {
			Map<String,Object[]> attributesMap = getAllAttributes(frameworkMethod);
			try {
				Map<String, CommandFactory> factories= validateAttributes(
						javaClass, attributesMap.keySet());
				for (Map.Entry<Command[], int[]> commands : new AttributesListIterable(
						factories, attributesMap)) {
					Command cmds[]= commands.getKey();
					Attributes attributes= frameworkMethod
							.getAnnotation(Attributes.class);
					String namePattern= attributes.name();
					runners.add(new TestClassRunnerForPermutations(javaClass,
							cmds, nameFor(namePattern, cmds), attributes));
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
	private Map<String, Object[]> getAllAttributes(
			FrameworkMethod frameworkMethod) throws Throwable {
		Object attributes= frameworkMethod.invokeExplosively(null);
		if (attributes instanceof Map) {
			return (Map<String, Object[]>) attributes;
		} else {
			throw attributesMethodReturnedWrongType(frameworkMethod);
		}
	}

	private Exception attributesMethodReturnedWrongType(FrameworkMethod frameworkMethod) throws Exception {
		String className= getTestClass().getName();
		String methodName= frameworkMethod.getName();
		String message= MessageFormat.format(
				"{0}.{1}() must return a Map of String/arrays.",
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
	private Map<String, CommandFactory> validateAttributes(Class<?> javaClass, Set<String> attributeNames) {
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
		private Map<String, Object[]> attributesMap;

		AttributesListIterable(Map<String, CommandFactory> factories,
				Map<String, Object[]> attributesMap) {
			this.factories    = factories;
			this.attributesMap = attributesMap;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		public Iterator<Entry<Command[], int[]>> iterator() {
			return new AttributesListIterator(this.factories, this.attributesMap);
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
			 * @param attributesMap
			 */
			public AttributesListIterator(Map<String, CommandFactory> factories,
					Map<String, Object[]> attributesMap) {
				this.factories = factories;
				sizes = new int[attributesMap.size()];
				startIndexes = new int[sizes.length];
				Arrays.fill(startIndexes, 0);
				entries = new EntryImpl[attributesMap.size()];
				int index = 0;
				for(Map.Entry<String, Object[]> entry : attributesMap.entrySet()) {
					entries[index++] = new EntryImpl(entry.getKey(), entry.getValue());
				}
				index = 0;
				for(Map.Entry<String, Object[]> entry : entries) {
					sizes[index++] = entry.getValue().length;
				}
				objects = new Object[sizes.length];
				indexes = new int[sizes.length];
				// Setup a current set of defaults that start with the zero indexed version....
				int cnt = 0;
				for(Map.Entry<String, Object[]> entry : entries) {
					Object[] values = entry.getValue();
					objects[cnt] = (values.length > 0) ? values[0] : null;
					indexes[cnt] = 0;
					cnt++;
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
