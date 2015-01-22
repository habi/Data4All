package io.github.data4all.smoothing;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;


public class BasicSmoothingSensorMethodTest {

	private BasicSmoothingSensorMethod basicSmoothing;
	private float[] input = new float[3];
	private float[] output = new float[3];
	/**
	 * get BasicSmoothingSensorMethod 
	 * @return basicSmoothing
	 */
	public BasicSmoothingSensorMethod getBasicSmoothingSensorMethod(){
		return basicSmoothing;
	}
	
	@Before
	public void setUp(){
		 basicSmoothing = null;
		try {
			basicSmoothing = new BasicSmoothingSensorMethod();
		} catch (Exception exception) {
			assertNotNull(basicSmoothing);
		}
	}
	
	/**
	 * we presume that output = null
	 * and we want to check, what are the filteredValues
	 */
	@Test
	public void checkFilteredValues_WhenOuput_isNull(){
		setUp();
		input[0] = 7.5f;
		input[1] = -2.5f;
		input[2] = 4.1f;
		output=null;
		assertEquals(basicSmoothing.filter(input, output),input);	
	}
	
	/**
	 * we presume that inputs and output are not null
	 * and we want to check the filteredValues
	 */
	@Test
	public void checkFilteredValues_WhenInputAndOutput_NotNull(){
		setUp();
		input[0] = 7.5f;
		input[1] = -2.5f;
		input[2] = 4.1f;
		output[0] = 3.0f;
		output[1] = 2.0f;
		output[2] = 1.0f;
		
		assertEquals(basicSmoothing.filter(input, output),output);	
	}
	
}
