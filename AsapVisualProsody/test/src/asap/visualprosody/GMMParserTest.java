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
package asap.visualprosody;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class GMMParserTest
{
    private static final double PRECISION = 0.000001;
    @Test  
    public void test()
    {
        String xml="<gmm k=\"2\">\r\n" + 
                "  <lambdas>\r\n" + 
                "    <lambda val=\"0.4\"/>\r\n" + 
                "    <lambda val=\"0.6\"/>\r\n" + 
                "  </lambdas>\r\n" + 
                "  <mus>\r\n" + 
                "    <mu val=\"1 2 3\"/>\r\n" + 
                "    <mu val=\"4 5 6\"/>\r\n" + 
                "  </mus>\r\n" + 
                "  <sigmas>\r\n" + 
                "    <sigma val=\"1 0 0 0 1 0 0 0 1\"/>\r\n" + 
                "    <sigma val=\"1 0 0 0 1 0 0 0 1\"/>\r\n" + 
                "  </sigmas>\r\n" + 
                "</gmm>";
        GMMParser parser = new GMMParser();
        parser.readXML(xml);
        assertEquals(2,parser.getK());
        assertEquals(0.4, parser.getLambdas().get(0), PRECISION);
        assertEquals(0.6, parser.getLambdas().get(1), PRECISION);  
        assertArrayEquals(new double[]{1,2,3}, parser.getMus().get(0),PRECISION);
        assertArrayEquals(new double[]{4,5,6}, parser.getMus().get(1),PRECISION);
        assertArrayEquals(new double[]{1,0,0}, parser.getSigmas().get(0)[0],PRECISION);
        assertArrayEquals(new double[]{0,1,0}, parser.getSigmas().get(0)[1],PRECISION);
        assertArrayEquals(new double[]{0,0,1}, parser.getSigmas().get(0)[2],PRECISION);
        assertArrayEquals(new double[]{1,0,0}, parser.getSigmas().get(1)[0],PRECISION);
        assertArrayEquals(new double[]{0,1,0}, parser.getSigmas().get(1)[1],PRECISION);
        assertArrayEquals(new double[]{0,0,1}, parser.getSigmas().get(1)[2],PRECISION);
        assertNotNull(parser.constructMixtureModel());
    }
}
