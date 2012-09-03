package org.junit.tests.experimental.runners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
		
		@Attributes
		public static Map<String, Object[]> inputs() {
			Map<String, Object[]> map = new HashMap<String, Object[]>();
			map.put("nonExistentAttribute", new Object[] { 1, 2 });
			map.put("aValue", new Object[] { 1 });
			return map;
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
		
		@Attributes
		public static Map<String, Object[]> inputs() {
			Map<String, Object[]> map = new HashMap<String, Object[]>();
			map.put("value", new Object[] { 1.0, 2.0, 3.0 });
			return map;
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
		
		@Attributes
		public static Map<String, Object[]> inputs() {
			Map<String, Object[]> map = new HashMap<String, Object[]>();
			map.put("simple", new Object[] { 1, 2 });
			return map;
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
		
		@Attributes
		public static Map<String, Object[]> inputs() {
			Map<String, Object[]> map = new HashMap<String, Object[]>();
			map.put("value", new Object[] {1});
			return map;
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
		
		private static Map<String, Object[]> meFirstMap;
		private static Map<String, Object[]> meSecondMap;
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
			meFirstMap = new HashMap<String, Object[]>();
			meFirstMap.put("meFirst", new Object[] { 1, 2, 3, 4, 5 });
			meSecondMap = new HashMap<String, Object[]>();
			meSecondMap.put("meSecond", new Object[] { 6, 7, 8, 9, 10 });
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
		
		@Attributes(tests="testMeFirst")
		public static Map<String,Object[]> meFirstInputs() {
			return meFirstMap;
		}
		
		@Attributes(tests="testMeSecond")
		public static Map<String,Object[]> meSecondInputs() {
			return meSecondMap;
		}
		
		@Attributes(tests="testMe.*")
		public static Map<String, Object[]> meAllInputs() {
			Map<String, Object[]> map = new HashMap<String,Object[]>(meFirstMap);
			map.putAll(meSecondMap);
			return map;
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
		
		@Attributes
		public static Map<String, Object[]> inputs() {
			Map<String, Object[]> map = new HashMap<String, Object[]>();
			map.put("single", new Object[] { 1 });
			return map;
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
		
		@Attributes(tests="testNonDefaultValues")
		public static Map<String, Object[]> inputs() {
			Map<String, Object[]> map = new HashMap<String, Object[]>();
			map.put("value", new Object[] { 2 });
			return map;
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
		
		@Attributes()
		public static Map<String, Object[]> inputs() {
			Map<String, Object[]> map = new HashMap<String, Object[]>();
			map.put("value", new Object[] {});
			return map;
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
    public static class InnerCombinationTest {

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

            private InnerCombinationTest getOuterType() {
                return InnerCombinationTest.this;
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
        public static Map<String, Object[]> values() {
            Map<String, Object[]> values = new HashMap<String,Object[]>();
            values.put("name", objects[0]);
            values.put("value", objects[1]);
            return values;
        }
	
        @Attributes(tests="testMore")
        public static Map<String, Object[]> moreValues() {
            Map<String,Object[]> values = new HashMap<String,Object[]>();
            values.put("name", moreObjects[0]);
            values.put("value", moreObjects[1]);
            return values;
        }

        private String name;
        
        public int value;
	
        public void setName(String name) {
            this.name = name;
        }

        public InnerCombinationTest() {}
	
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
    public void testInnerCombinationTest() {
        Result result = JUnitCore.runClasses(InnerCombinationTest.class);
        int total = InnerCombinationTest.objects[0].length * InnerCombinationTest.objects[1].length;
        assertEquals("Not all parameters where generated", total, InnerCombinationTest.values.size());
        total = InnerCombinationTest.moreObjects[0].length * InnerCombinationTest.moreObjects[1].length;
        assertEquals("Not all parameters where generated", total, InnerCombinationTest.moreValues.size());
        assertEquals(122, result.getRunCount());
        assertEquals(0, result.getFailureCount());
    }
}
