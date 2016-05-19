package com.example.grass.metering2;

import com.example.grass.metering2.calibration.DalCalibrActivity;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }
    /*
    @Test
    public void checkHeight(){

        double value = MeteringActivity.calculateHeight(30,10,1.5);
        assertEquals("wrong value 1",6.3,value,0.2);

        value = MeteringActivity.calculateHeight(45,45,2);
        assertEquals("wrong value 2",4,value,0.2);

        value = MeteringActivity.calculateHeight(60,20,2);
        assertEquals("wrong value 3",11.4,value,0.2);
    }*/
    @Test
    public void checkHeightMeter(){
        double value = DalCalibrActivity.calculateHeight(30,8.3,"calibr1");
        assertEquals("wrong value 1",4.8,value,0.3);

        value = DalCalibrActivity.calculateHeight(45,2,"calibr1");
        assertEquals("wrong value 1",2,value,0.3);

        value = DalCalibrActivity.calculateHeight(60,5.6,"calibr1");
        assertEquals("wrong value 1",9.5,value,0.3);
    }
    @Test
    public void checkDalMeter(){
        double value = DalCalibrActivity.calculateHeight(10,1.5,"calibr2");
        assertEquals("wrong value 1",8.8,value,0.3);

        value = DalCalibrActivity.calculateHeight(45,2,"calibr2");
        assertEquals("wrong value 1",2,value,0.3);

        value = DalCalibrActivity.calculateHeight(20,2,"calibr2");
        assertEquals("wrong value 1",5.6,value,0.3);
    }

}