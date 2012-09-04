package org.junit.tests.experimental.runners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.experimental.runners.Combination;
import org.junit.experimental.runners.Combination.Attributes;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

/**
 * @author Claudio Corsi
 *
 */
public class CombinationTestTest {

	@RunWith(Combination.class)
	public static class WrongAttributesTest {
	
		@Attributes
		public static Map<Object, Object> inputs() {
			Map<Object, Object> map = new HashMap<Object, Object>();
			map.put(new Object(), new Object());
			return map;
		}
		
		@Test
		public void noTest() {
			
		}
	}
	
	@Test
	public void count() {
		Result result = executeTest(WrongAttributesTest.class);
		assertEquals(1, result.getRunCount());
		assertEquals(1, result.getFailureCount());
	}

	/**
	 * @param testClass 
	 * @return
	 */
	private Result executeTest(Class<?> testClass) {
		return JUnitCore.runClasses(testClass);
	}

	@RunWith(Combination.class)
	public static class MissingAttributeTest {
		
		@Attributes(attributes = {"nonExistentAttribute", "aValue"})
		public static List<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] { 1, 2 });
			list.add(new Object[] { 1 });
			return list;
		}

		public void setAValue(int aValue) {
		}
		
		@Test
		public void test() {
		}
	}
	
	@Test
	public void testMissingAttributeTest() {
		Result result = this.executeTest(MissingAttributeTest.class);
		assertEquals(1, result.getFailureCount());
	}
	
	
	@Test
	public void testMissingAttributeTestCount() {
		Result result = this.executeTest(MissingAttributeTest.class);
		assertEquals(1, result.getRunCount());
	}
	
	@RunWith(Combination.class)
	public static class IncompatibleAttributesTest {
		
		@Attributes(attributes = { "value" })
		public static List<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] { 1.0, 2.0, 3.0 });
			return list;
		}

		public void setValue(int value) {
		}
		
		@Test
		public void test() {}
		
	}
	
	@Test
	public void testIncompatibleAttributesTestCount() {
		Result result = this.executeTest(IncompatibleAttributesTest.class);
		assertEquals(3, result.getRunCount());
	}
	
	@Test
	public void testIncompatibleAttributesTestFailureCount() {
		Result result = this.executeTest(IncompatibleAttributesTest.class);
		assertEquals(3, result.getFailureCount());
	}
	
	@RunWith(Combination.class)
	public static class WrongAccessTest {
		
		@Attributes(attributes = { "simple" })
		public static List<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] { 1, 2 });
			return list;
		}
		
		public void setSimple(int simple) {
		}
		
		@Test public void withArg(int value) {}
		@Test void wrongAccess() {}
		@Test public int returnType() { return 1; }
		@Test int nonPublicReturnType() { return 1; }
	}
	
	@Test
	public void testWrongAccessTestCount() {
		Result result = this.executeTest(WrongAccessTest.class);
		assertEquals(5, result.getRunCount());
		assertEquals(5, result.getFailureCount());
	}
	
	@Test
	public void testWrongAccessTestFailures() {
		Result result = this.executeTest(WrongAccessTest.class);
		assertEquals(5, result.getFailureCount());
	}

	@RunWith(Combination.class)
	public static class ValidTest {
		
		public static int testValue = 0;
		
		@Attributes(attributes = { "value" })
		public static List<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] {1});
			return list;
		}
		
		private int value = 0;
		
		public void setValue(int value) {
			this.value = value;
		}
		
		@Test
		public void increment() {
			testValue = value;
		}
	}
	
	@Test
	public void testValidTestValue() {
		ValidTest.testValue = 0;
		this.executeTest(ValidTest.class);
		assertEquals(1, ValidTest.testValue);
	}
	
	@Test
	public void testValidTestCount() {
		ValidTest.testValue = 0;
		Result result = this.executeTest(ValidTest.class);
		assertEquals(1, result.getRunCount());
	}
	
	@RunWith(Combination.class)
	public static class MultipleAttributesTest {
		
		private static List<Object[]> meFirstList;
		private static List<Object[]> meSecondList;
		static List<Integer> meFirstResult  = new LinkedList<Integer>();
		static List<Integer> meSecondResult = new LinkedList<Integer>();
		static List<Integer> meAllResult    = new LinkedList<Integer>();
		static List<Integer> meFirstExpected  = new LinkedList<Integer>();
		static List<Integer> meSecondExpected = new LinkedList<Integer>();
		static List<Integer> meAllExpected    = new LinkedList<Integer>();

		static public int meFirstCnt = 0;
		static public int meSecondCnt = 0;
		static public int meAllCnt = 0;
		
		static {
			meFirstList = new LinkedList<Object[]>();
			meFirstList.add(new Object[] { 1, 2, 3, 4, 5 });
			meSecondList = new LinkedList<Object[]>();
			meSecondList.add(new Object[] { 6, 7, 8, 9, 10 });
			for(int cnt = 1 ; cnt <= 5 ; cnt++) {
				meFirstExpected.add(cnt);
			}
			for(int cnt = 6 ; cnt <= 10 ; cnt++) {
				meSecondExpected.add(cnt);
			}
			meAllExpected.addAll(meFirstExpected);
			meAllExpected.addAll(meSecondExpected);
		}

		private int meFirst;
		private int meSecond;
		
		@Attributes(tests="testMeFirst", attributes={ "meFirst" })
		public static List<Object[]> meFirstInputs() {
			return meFirstList;
		}
		
		@Attributes(tests="testMeSecond", attributes={ "meSecond" })
		public static List<Object[]> meSecondInputs() {
			return meSecondList;
		}
		
		@Attributes(tests="testMe.*", attributes={ "meFirst", "meSecond" })
		public static List<Object[]> meAllInputs() {
			List<Object[]> list = new LinkedList<Object[]>(meFirstList);
			list.addAll(meSecondList);
			return list;
		}

		/**
		 * @param meFirst the meFirst to set
		 */
		public void setMeFirst(int meFirst) {
			this.meFirst= meFirst;
		}

		/**
		 * @param meSecond the meSecond to set
		 */
		public void setMeSecond(int meSecond) {
			this.meSecond= meSecond;
		}
		
		@Test
		public void testMeFirst() {
			meFirstResult.add(meFirst);
			meFirstCnt++;
		}
		
		@Test
		public void testMeSecond() {
			meSecondResult.add(meSecond);
			meSecondCnt++;
		}
		
		@Test
		public void testMeAll() {
			meAllResult.add(meFirst);
			meAllResult.add(meSecond);
			meAllCnt++;
		}
	}
	
	@Test
	public void testMultipleAttributesTestCount() {
		Result result = this.executeTest(MultipleAttributesTest.class);
		assertEquals(50, MultipleAttributesTest.meAllResult.size());
		assertEquals(30, MultipleAttributesTest.meFirstResult.size());
		assertEquals(30, MultipleAttributesTest.meSecondResult.size());
		assertEquals(85, result.getRunCount());
		assertEquals(30, MultipleAttributesTest.meFirstCnt);
		assertEquals(30, MultipleAttributesTest.meSecondCnt);
		assertEquals(25, MultipleAttributesTest.meAllCnt);
		assertEquals(0, result.getFailureCount());
	}
	
	@RunWith(Combination.class)
	public static class NoAttributesTest {
		
		@Test
		public void test() {
			fail("This should never be called");
		}
	}
	
	@Test
	public void testNoAttributesTest() {
		Result result = this.executeTest(NoAttributesTest.class);
		assertEquals(1, result.getRunCount());
		assertEquals(1, result.getFailureCount());
	}
	
	@RunWith(Combination.class)
	public static class SingleAttributesTest {
		
		@Attributes(attributes={ "single" })
		public static List<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] { 1 });
			return list;
		}

		private int single;
		private static int singleValue;
		
		@Test
		public void test() {
			singleValue = single;
		}

		/**
		 * @param single the single to set
		 */
		public void setSingle(int single) {
			this.single= single;
		}
	}
	
	@Test
	public void testSingleAttributesTest() {
		SingleAttributesTest.singleValue = 0;
		this.executeTest(SingleAttributesTest.class);
		assertEquals(1, SingleAttributesTest.singleValue);
	}
	
	@RunWith(Combination.class)
	public static class DefaultAttributesTest {
		
		private int value = 1;
		
		private static int defaultValue;
		
		@Attributes(tests="testNonDefaultValues", attributes={ "value" })
		public static List<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] { 2 });
			return list;
		}

		public void setValue(int value) {
			this.value = value;
		}
		
		@Test
		public void testDefaultValues() {
			defaultValue = value;
		}
	}
	
	@Test
	public void testDefaultAttributesTest() {
		DefaultAttributesTest.defaultValue = 0;
		Result result = this.executeTest(DefaultAttributesTest.class);
		assertEquals(0, DefaultAttributesTest.defaultValue);
		assertEquals(0, result.getRunCount());
	}
	
	@RunWith(Combination.class)
	public static class ZeroLengthAttributesTest {
		
		private static int defaultValue;
		
		@Attributes(attributes={ "value" })
		public static List<Object[]> inputs() {
			List<Object[]> list = new LinkedList<Object[]>();
			list.add(new Object[] {});
			return list;
		}
		
		@Test
		public void testZeroLengthDefaultValues() {
			fail("This test should never be called");
		}

		public void setValue(int value) {
		}
	}
	
	@Test
	public void testZeroLengthAttributesTest() {
		DefaultAttributesTest.defaultValue = 0;
		Result result = this.executeTest(ZeroLengthAttributesTest.class);
		assertEquals(0, ZeroLengthAttributesTest.defaultValue);
		assertEquals(0, result.getRunCount());
	}

    @RunWith(Combination.class)
    public static class InnerCombinationAttributesTest {

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

            private InnerCombinationAttributesTest getOuterType() {
                return InnerCombinationAttributesTest.this;
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
        
        @Attributes(tests="test", attributes={ "name", "value" })
        public static List<Object[]> values() {
            List<Object[]> values = new LinkedList<Object[]>();
            values.add(objects[0]);
            values.add(objects[1]);
            return values;
        }
	
        @Attributes(tests="testMore", attributes={ "name", "value" })
        public static List<Object[]> moreValues() {
            List<Object[]> values = new LinkedList<Object[]>();
            values.add(moreObjects[0]);
            values.add(moreObjects[1]);
            return values;
        }

        private String name;
        
        public int value;
	
        public void setName(String name) {
            this.name = name;
        }

        public InnerCombinationAttributesTest() {}
	
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
    public void testInnerCombinationAttributesTest() {
        Result result = JUnitCore.runClasses(InnerCombinationAttributesTest.class);
        int total = InnerCombinationAttributesTest.objects[0].length * InnerCombinationAttributesTest.objects[1].length;
        assertEquals("Not all parameters where generated", total, InnerCombinationAttributesTest.values.size());
        total = InnerCombinationAttributesTest.moreObjects[0].length * InnerCombinationAttributesTest.moreObjects[1].length;
        assertEquals("Not all parameters where generated", total, InnerCombinationAttributesTest.moreValues.size());
        assertEquals(122, result.getRunCount());
        assertEquals(0, result.getFailureCount());
    }

	@RunWith(Combination.class)
	public static class ZeroLengthParametersTest {
		
		@Attributes
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


	@RunWith(Combination.class)
	public static class SingleLengthParametersTest {
		
		private int value;
		private static int staticValue;
		
		@Attributes
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

	@RunWith(Combination.class)
	public static class WrongParameterLengthTest {
		
		@Attributes
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

	@RunWith(Combination.class)
	public static class WrongParameterTypeTest {
		
		@Attributes
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

	@RunWith(Combination.class)
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
		
		@Attributes
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

    @RunWith(Combination.class)
    public static class InnerCombinationConstructorTest {

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

            private InnerCombinationConstructorTest getOuterType() {
                return InnerCombinationConstructorTest.this;
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
	
        @Attributes(tests="test")
        public static List<Object[]> values() {
            List<Object[]> values = new LinkedList<Object[]>();
            for(Object[] value : objects) {
                values.add(value);
            }
            return values;
        }
	
        @Attributes(tests="testMore")
        public static List<Object[]> moreValues() {
            List<Object[]> values = new LinkedList<Object[]>();
            for(Object[] value : moreObjects) {
                values.add(value);
            }
            return values;
        }

        private String name;
        private int value;
	
        public InnerCombinationConstructorTest(String name, int value) {
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
	public void innerCombinationContructorTest() {
        Result result = execute(InnerCombinationConstructorTest.class);
		int total = InnerCombinationConstructorTest.objects[0].length * InnerCombinationConstructorTest.objects[1].length;
		assertEquals("Not all parameters where generated", total, InnerCombinationConstructorTest.values.size());
		total = InnerCombinationConstructorTest.moreObjects[0].length * InnerCombinationConstructorTest.moreObjects[1].length;
		assertEquals("Not all parameters where generated", total, InnerCombinationConstructorTest.moreValues.size());
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
