package org.junit.tests.experimental.runners;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.experimental.results.PrintableResult.testResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.experimental.runners.Parameterized;
import org.junit.experimental.runners.Parameterized.Parameters;
import org.junit.runners.model.InitializationError;

public class ParameterizedTestTest {
	@RunWith(Parameterized.class)
	static public class FibonacciTest {
		@Parameters(name= "{index}: fib({0})={1}")
		public static Iterable<Object[]> data() {
			return Arrays.asList(new Object[][] { { 0, 0 }, { 1, 1 }, { 2, 1 },
					{ 3, 2 }, { 4, 3 }, { 5, 5 }, { 6, 8 } });
		}

		private final int fInput;

		private final int fExpected;

		public FibonacciTest(int input, int expected) {
			fInput= input;
			fExpected= expected;
		}

		@Test
		public void test() {
			assertEquals(fExpected, fib(fInput));
		}

		private int fib(int x) {
			return 0;
		}
	}

	@Test
	public void count() {
		Result result= JUnitCore.runClasses(FibonacciTest.class);
		assertEquals(7, result.getRunCount());
		assertEquals(6, result.getFailureCount());
	}

	@Test
	public void failuresNamedCorrectly() {
		Result result= JUnitCore.runClasses(FibonacciTest.class);
		assertEquals(
				"test[1: fib(1)=1](" + FibonacciTest.class.getName() + ")",
				result.getFailures().get(0).getTestHeader());
	}

	@Test
	public void countBeforeRun() throws Exception {
		Runner runner= Request.aClass(FibonacciTest.class).getRunner();
		assertEquals(7, runner.testCount());
	}

	@Test
	public void plansNamedCorrectly() throws Exception {
		Runner runner= Request.aClass(FibonacciTest.class).getRunner();
		Description description= runner.getDescription();
		assertEquals("[0: fib(0)=0]", description.getChildren().get(0)
				.getDisplayName());
	}

	@RunWith(Parameterized.class)
	public static class ParameterizedWithoutSpecialTestname {
		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] { { 3 }, { 3 } });
		}

		public ParameterizedWithoutSpecialTestname(Object something) {
		}

		@Test
		public void testSomething() {
		}
	}

	@Test
	public void usesIndexAsTestName() {
		Runner runner= Request
				.aClass(ParameterizedWithoutSpecialTestname.class).getRunner();
		Description description= runner.getDescription();
		assertEquals("[1]", description.getChildren().get(1).getDisplayName());
	}

	private static String fLog;

	@RunWith(Parameterized.class)
	static public class BeforeAndAfter {
		@BeforeClass
		public static void before() {
			fLog+= "before ";
		}

		@AfterClass
		public static void after() {
			fLog+= "after ";
		}

		public BeforeAndAfter(int x) {

		}

		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] { { 3 } });
		}

		@Test
		public void aTest() {
		}
	}

	@Test
	public void beforeAndAfterClassAreRun() {
		fLog= "";
		JUnitCore.runClasses(BeforeAndAfter.class);
		assertEquals("before after ", fLog);
	}

	@RunWith(Parameterized.class)
	static public class EmptyTest {
		@BeforeClass
		public static void before() {
			fLog+= "before ";
		}

		@AfterClass
		public static void after() {
			fLog+= "after ";
		}
	}

	@Test
	public void validateClassCatchesNoParameters() {
		Result result= JUnitCore.runClasses(EmptyTest.class);
		assertEquals(1, result.getFailureCount());
	}

	@RunWith(Parameterized.class)
	static public class IncorrectTest {
		@Test
		public int test() {
			return 0;
		}

		@Parameters
		public static Collection<Object[]> data() {
			return Collections.singletonList(new Object[] { 1 });
		}
	}

	@Test
	public void failuresAddedForBadTestMethod() throws Exception {
		Result result= JUnitCore.runClasses(IncorrectTest.class);
		assertEquals(1, result.getFailureCount());
	}

	@RunWith(Parameterized.class)
	static public class ProtectedParametersTest {
		@Parameters
		protected static Collection<Object[]> data() {
			return Collections.emptyList();
		}

		@Test
		public void aTest() {
		}
	}

	@Test
	public void meaningfulFailureWhenParametersNotPublic() throws Exception {
		Result result= JUnitCore.runClasses(ProtectedParametersTest.class);
		String expected= String.format(
				"No public static parameters method(s) on class %s",
				ProtectedParametersTest.class.getName());
		assertEquals(expected, result.getFailures().get(0).getMessage());
	}

	@RunWith(Parameterized.class)
	static public class WrongElementType {
		@Parameters
		public static Iterable<String> data() {
			return Arrays.asList("a", "b", "c");
		}

		@Test
		public void aTest() {
		}
	}

	@Test
	public void meaningfulFailureWhenParametersAreNotArrays() {
		assertThat(
				testResult(WrongElementType.class).toString(),
				containsString("WrongElementType.data() must return an Iterable of arrays."));
	}

	@RunWith(Parameterized.class)
	static public class ParametersNotIterable {
		@Parameters
		public static String data() {
			return "foo";
		}

		@Test
		public void aTest() {
		}
	}

	@Test
	public void meaningfulFailureWhenParametersAreNotAnIterable() {
		assertThat(
				testResult(ParametersNotIterable.class).toString(),
				containsString("ParametersNotIterable.data() must return an Iterable of arrays."));
	}

	@RunWith(Parameterized.class)
	static public class PrivateConstructor {
		private PrivateConstructor(int x) {

		}

		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] { { 3 } });
		}

		@Test
		public void aTest() {
		}
	}

	@Test(expected= InitializationError.class)
	public void exceptionWhenPrivateConstructor() throws Throwable {
		new Parameterized(PrivateConstructor.class);
	}

    @RunWith(Parameterized.class)
    static public class AttributesTest {

        private int y;
        public  int x;

        public void setY(int y) {
            this.y = y;
        }

        @Parameters(attributes = { "x", "y" })
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] { { 1, 2 } });
        }

        @Test
        public void aTest() {
            assertEquals(x, 1);
            assertEquals(y, 2);
        }
    }

    @Test
    public void attributesTest() {
        Result result = JUnitCore.runClasses(AttributesTest.class);
        assertEquals(1, result.getRunCount());
        assertEquals(0, result.getFailureCount());
    }

    @RunWith(Parameterized.class)
    static public class UnequalAttributeListTest {

        private int y;
        public  int x;

        public void setY(int y) {
            this.y = y;
        }

        @Parameters(attributes = { "x" })
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] { { 1, 2 } });
        }

        @Test
        public void aTest() {
        }
    }

    @Test
    public void unequalAttributeListTest() {
        Result result = JUnitCore.runClasses(UnequalAttributeListTest.class);
        assertEquals(1, result.getRunCount());
        assertEquals(1, result.getFailureCount());
    }

    @RunWith(Parameterized.class)
    static public class InvalidAttributeTypeTest {

        private int y;
        public  String x;

        public void setY(int y) {
            this.y = y;
        }

        @Parameters(attributes = { "x", "y" })
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] { { 1, 2 } });
        }

        @Test
        public void aTest() {
        }
    }

    @Test
    public void invalidAttributeTypeTest() {
        Result result = JUnitCore.runClasses(InvalidAttributeTypeTest.class);
        assertEquals(1, result.getRunCount());
        assertEquals(1, result.getFailureCount());
    }

    @RunWith(Parameterized.class)
    public static class InnerParameterizedTest {

        private class Pair {
		
            private String name;
            private int value;
		
            Pair(String name, int value) {
                this.name  = name;
                this.value = value;
            }

            /* (non-Javadoc)
             * @see java.lang.Object#hashCode()
             */
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + getOuterType().hashCode();
                result = prime * result + ((name == null) ? 0 : name.hashCode());
                result = prime * result + value;
                return result;
            }

            /* (non-Javadoc)
             * @see java.lang.Object#equals(java.lang.Object)
             */
            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                Pair other = (Pair) obj;
                if (!getOuterType().equals(other.getOuterType()))
                    return false;
                if (name == null) {
                    if (other.name != null)
                        return false;
                } else if (!name.equals(other.name))
                    return false;
                if (value != other.value)
                    return false;
                return true;
            }

            private InnerParameterizedTest getOuterType() {
                return InnerParameterizedTest.this;
            }

            /* (non-Javadoc)
             * @see java.lang.Object#toString()
             */
            @Override
            public String toString() {
                return "Pair [name=" + name + ", value=" + value + "]";
            }

        }

        private static Object[][] objects = new Object[][] {
            { "A", 1 }, { "B", 2 }, { "C", 3 }, { "D", 4 }, { "E", 5 },
        };

        private static Object[][] moreObjects = new Object[][] {
            { "A", 1 }, { "B", 2 }, { "C", 3 }, { "D", 4 }, { "E", 5 }, { "F", 6 }, { "G", 7 },
        };

        private static List<Pair> values = new LinkedList<Pair>();
        private static List<Pair> moreValues = new LinkedList<Pair>();
	
        @Parameters(tests="test", name="{0},{1}")
        public static List<Object[]> values() {
            List<Object[]> values = new LinkedList<Object[]>();
            for(Object[] value : objects) {
                values.add(value);
            }
            return values;
        }
	
        @Parameters(tests="testMore", name="{0},{1}")
        public static List<Object[]> moreValues() {
            List<Object[]> values = new LinkedList<Object[]>();
            for(Object[] value : moreObjects) {
                values.add(value);
            }
            return values;
        }

        private String name;
        private int value;
	
        public InnerParameterizedTest(String name, int value) {
            this.name  = name;
            this.value = value;
        }
	
        @Test
        public void test() {
            Pair pair = new Pair(name, value);
            values.add(pair);
        }
	
        @Test
        public void testMore() {
            Pair pair = new Pair(name, value);
            moreValues.add(pair);
        }
	
        @Test
        public void testNoTest() {
            fail("This test should never be called");
        }

    }

    @Test
    public void innerParameterizedTest() {
        Result result = JUnitCore.runClasses(InnerParameterizedTest.class);
        int total = InnerParameterizedTest.objects.length;
        assertEquals("Not all parameters where generated", total, InnerParameterizedTest.values.size());
        total = InnerParameterizedTest.moreObjects.length;
        assertEquals("Not all parameters where generated", total, InnerParameterizedTest.moreValues.size());
        assertEquals(12, result.getRunCount());
        assertEquals(0, result.getFailureCount());
    }

}