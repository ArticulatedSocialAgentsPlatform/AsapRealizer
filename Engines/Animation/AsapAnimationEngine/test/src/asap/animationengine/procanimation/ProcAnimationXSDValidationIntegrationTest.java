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
package asap.animationengine.procanimation;

import hmi.testutil.xml.XSDValidationTest;
import hmi.util.Resources;

import java.io.File;
import java.util.Collection;

import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;

/**
 * Testcase to validates the procanimation resource data against the procanimation xsd
 * @author hvanwelbergen
 *
 */
public class ProcAnimationXSDValidationIntegrationTest extends
        XSDValidationTest
{
    private static final Resources PROCANIMATION_XSD_RES = new Resources("xsd");
    private static final String PROCANIMATION_XSD = "procanimation.xsd";
    private static final String PROCANIMATION_DIRS[] = {
        //System.getProperty("shared.project.root")+ "/AsapResource/AsapHumanoidControlShared/resource/Humanoids/shared/procanimation"};
        //System.getProperty("shared.project.root")+ "/AsapResource/AsapHumanoidControlArmandia/resource/Humanoids/blueguy/procanimation",
        System.getProperty("shared.project.root")+ "/AsapResource/AsapHumanoidControlArmandia/resource/Humanoids/armandia/procanimation"};
    
    @Before
    public void setup()
    {
        xsdReader = PROCANIMATION_XSD_RES.getReader(PROCANIMATION_XSD);
    }

    @Parameters
    public static Collection<Object[]> configs()
    {
        return configs(PROCANIMATION_DIRS);
    }

    public ProcAnimationXSDValidationIntegrationTest(String label, File f)
    {
        super(label, f);
    }
}
