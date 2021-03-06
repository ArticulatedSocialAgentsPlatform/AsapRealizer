/*******************************************************************************
 * Copyright (C) 2009-2020 Human Media Interaction, University of Twente, the Netherlands
 *
 * This file is part of the Articulated Social Agents Platform BML realizer (ASAPRealizer).
 *
 * ASAPRealizer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ASAPRealizer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ASAPRealizer.  If not, see http://www.gnu.org/licenses/.
 ******************************************************************************/
package asap.math.splines;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for the TCBSplineN
 * @author hvanwelbergen
 * 
 */
public class TCBSplineNTest
{
    private static final double PRECISION = 0.0001;

    private List<Double> afTime = new ArrayList<>();
    private List<float[]> akPoint = new ArrayList<>();
    private List<Double> afTension = new ArrayList<>();
    private List<Double> afContinuity = new ArrayList<>();
    private List<Double> afBias = new ArrayList<>();

    @Test
    public void testLinear()
    {
        int segments = 5;
        for (int i = 0; i < segments + 1; i++)
        {
            afTension.add(0d);
            afContinuity.add(0d);
            afBias.add(0d);
            afTime.add((double) i);
            float av[] = new float[1];
            av[0] = i;
            akPoint.add(av);
        }

        TCBSplineN spline = new TCBSplineN(segments, afTime, akPoint, afTension, afContinuity, afBias);
        for (double i = 0; i < segments; i += 0.1)
        {
            assertEquals(i, spline.GetPosition(i)[0], PRECISION);
            assertEquals(1, spline.GetFirstDerivative(i)[0], PRECISION);
            assertEquals(0, spline.GetSecondDerivative(i)[0], PRECISION);
            assertEquals(0, spline.GetThirdDerivative(i)[0], PRECISION);
        }
        assertEquals(segments, spline.GetPosition(segments + 0.2)[0], PRECISION);
    }

    @Test
    public void testQuadratic()
    {
        int segments = 5;
        for (int i = 0; i < segments + 1; i++)
        {
            afTension.add(0d);
            afContinuity.add(0d);
            afBias.add(0d);
            afTime.add((double) i);
            float av[] = new float[1];
            av[0] = i * i;
            akPoint.add(av);
        }

        TCBSplineN spline = new TCBSplineN(segments, afTime, akPoint, afTension, afContinuity, afBias);
        for (double i = 0; i < segments; i++)
        {
            assertEquals(i * i, spline.GetPosition(i)[0], PRECISION);
        }

        assertEquals(segments * segments, spline.GetPosition(segments + 0.2)[0], PRECISION);

        // some random test values obtained from the C++ version
        assertEquals(14.44, spline.GetPosition(3.8)[0], PRECISION);
        assertEquals(7.6, spline.GetFirstDerivative(3.8)[0], PRECISION);
        assertEquals(2, spline.GetSecondDerivative(3.8)[0], PRECISION);
        assertEquals(0, spline.GetThirdDerivative(3.8)[0], PRECISION);

        assertEquals(16.819, spline.GetPosition(4.1)[0], PRECISION);
        assertEquals(8.37, spline.GetFirstDerivative(4.1)[0], PRECISION);
        assertEquals(3.4, spline.GetSecondDerivative(4.1)[0], PRECISION);
        assertEquals(-6, spline.GetThirdDerivative(4.1)[0], PRECISION);
    }
}
