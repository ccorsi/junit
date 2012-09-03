package org.junit.tests.experimental.runners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.experimental.runners.Permutation;
import org.junit.experimental.runners.Permutation.Parameters;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

/**
 * @author Claudio Corsi
 *
 */
public class PermutationTestTest {

	@RunWith(Permutation.class)
	public static class ZeroLengthParametersTest {
		
		@Parameters
		public static Iterable<Object[]> parameters() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] {});
			return list;
		}
		
		public ZeroLengthParametersTest(int value) { /*nothing here*/}
		
		@Test
		public void doNotCall() {
			fail("This test should never be called");
		}
	}
	
	@Test
	public void testZeroLengthParametersTest() {
		Result result = execute(ZeroLengthParametersTest.class);
		assertEquals(0, result.getRunCount());
		assertEquals(0, result.getFailureCount());
	}


	@RunWith(Permutation.class)
	public static class SingleLengthParametersTest {
		
		private int value;
		private static int staticValue;
		
		@Parameters
		public static Iterable<Object[]> parameters() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] {1});
			return list;
		}
		
		public SingleLengthParametersTest(int value) {
			this.value = value;
		}
		
		@Test
		public void testSingleLengthParameter() {
			staticValue = value;
		}
	}
	
	@Test
	public void testSingleParametersTest() {
		SingleLengthParametersTest.staticValue = 0;
		Result result = execute(SingleLengthParametersTest.class);
		assertEquals(1, result.getRunCount());
		assertEquals(0, result.getFailureCount());
		assertEquals(1, SingleLengthParametersTest.staticValue);
	}

	@RunWith(Permutation.class)
	public static class WrongParameterLengthTest {
		
		@Parameters
		public static Iterable<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] { 1, 2, 3});
			list.add(new Object[] { "A", "B", "C" });
			return list;
		}
		
		public WrongParameterLengthTest(int value) {
		}
		
		@Test
		public void doNotCall() {
			fail("This method should not of been called");
		}
	}
	
	@Test
	public void testWrongParameterLengthTest() {
		Result result = execute(WrongParameterLengthTest.class);
		assertEquals(9, result.getRunCount());
		assertEquals(9, result.getFailureCount());
	}

	@RunWith(Permutation.class)
	public static class WrongParameterTypeTest {
		
		@Parameters
		public static Iterable<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] { 1, 2, 3});
			list.add(new Object[] { "A", "B", "C" });
			return list;
		}
		
		public WrongParameterTypeTest(int value, int wrong) {
		}
		
		@Test
		public void doNotCall() {
			fail("This method should not of been called");
		}
	}
	
	@Test
	public void testWrongParameterTypeTest() {
		Result result = execute(WrongParameterTypeTest.class);
		assertEquals(9, result.getRunCount());
		assertEquals(9, result.getFailureCount());
	}

	@RunWith(Permutation.class)
	public static class ParameterCountTest {
		
		static class Entry {
			
			private int value;
			private String string;

			Entry(int value, String string) {
				this.value = value;
				this.string = string;
			}

			/* (non-Javadoc)
			 * @see java.lang.Object#hashCode()
			 */
			@Override
			public int hashCode() {
				final int prime= 31;
				int result= 1;
				result= prime * result
						+ ((string == null) ? 0 : string.hashCode());
				result= prime * result + value;
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
				Entry other= (Entry) obj;
				if (string == null) {
					if (other.string != null)
						return false;
				} else if (!string.equals(other.string))
					return false;
				if (value != other.value)
					return false;
				return true;
			}
			
		}
		
		static Set<Entry> expected;
		static Set<Entry> actual = new HashSet<Entry>();
		
		static {
			expected = new HashSet<Entry>();
			expected.add(new Entry(1, "A"));
			expected.add(new Entry(1, "B"));
			expected.add(new Entry(1, "C"));
			expected.add(new Entry(2, "A"));
			expected.add(new Entry(2, "B"));
			expected.add(new Entry(2, "C"));
			expected.add(new Entry(3, "A"));
			expected.add(new Entry(3, "B"));
			expected.add(new Entry(3, "C"));
		}
		
		@Parameters
		public static Iterable<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] { 1, 2, 3});
			list.add(new Object[] { "A", "B", "C" });
			return list;
		}

		private int value;
		private String string;
		
		public ParameterCountTest(int value, String string) {
			this.value = value;
			this.string = string;
		}
		
		@Test
		public void callMe() {
			actual.add(new Entry(value, string));
		}
	}
	
	@Test
	public void testParameterCountTestCount() {
		ParameterCountTest.actual.clear();
		Result result = execute(ParameterCountTest.class);
		assertEquals(9, result.getRunCount());
		assertEquals(0, result.getFailureCount());
	}
	
	@Test
	public void testParameterCountTestEntries() {
		ParameterCountTest.actual.clear();
		execute(ParameterCountTest.class);
		assertEquals(ParameterCountTest.expected, ParameterCountTest.actual);
	}

    @RunWith(Permutation.class)
    public static class InnerPermutationTest {

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

            private InnerPermutationTest getOuterType() {
                return InnerPermutationTest.this;
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
            { "A", "B", "C", "D", "E"},
            { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
        };

        private static Object[][] moreObjects = new Object[][] {
            { "A", "B", "C", "D", "E", "F", "G"},
            { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 },
        };

        private static List<Pair> values = new LinkedList<Pair>();
        private static List<Pair> moreValues = new LinkedList<Pair>();
	
        @Parameters(tests="test")
        public static List<Object[]> values() {
            List<Object[]> values = new LinkedList<Object[]>();
            for(Object[] value : objects) {
                values.add(value);
            }
            return values;
        }
	
        @Parameters(tests="testMore")
        public static List<Object[]> moreValues() {
            List<Object[]> values = new LinkedList<Object[]>();
            for(Object[] value : moreObjects) {
                values.add(value);
            }
            return values;
        }

        private String name;
        private int value;
	
        public InnerPermutationTest(String name, int value) {
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
	public void innerPermutationTest() {
        Result result = execute(InnerPermutationTest.class);
		int total = InnerPermutationTest.objects[0].length * InnerPermutationTest.objects[1].length;
		assertEquals("Not all parameters where generated", total, InnerPermutationTest.values.size());
		total = InnerPermutationTest.moreObjects[0].length * InnerPermutationTest.moreObjects[1].length;
		assertEquals("Not all parameters where generated", total, InnerPermutationTest.moreValues.size());
        assertEquals(122, result.getRunCount());
        assertEquals(0, result.getFailureCount());
	}
	
	/**
	 * @param testClass
	 * @return
	 */
	private Result execute(Class<?> testClass) {
		return JUnitCore.runClasses(testClass);
	}

}
