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
package asap.bml.ext.bmlt;

import static org.junit.Assert.assertEquals;
import hmi.xml.XMLTokenizer;

import java.io.IOException;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.SAXException;

import saiba.bml.core.AbstractBehaviourTest;
import saiba.bml.core.Behaviour;
import saiba.utils.TestUtil;

/**
 * Unit tests for BMLTMorphKeyframeBehaviour
 * @author herwinvw
 *
 */
public class BMLTFaceKeyframeBehaviourTest extends AbstractBehaviourTest
{
    @Override
    protected Behaviour createBehaviour(String bmlId, String extraAttributeString) throws IOException
    {
        String str = "<bmlt:facekeyframe xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" " + TestUtil.getDefNS()
                + "id=\"kf1\" name=\"file1.xml\"" + extraAttributeString + "/>";
        return new BMLTFaceKeyframeBehaviour(bmlId, new XMLTokenizer(str));
    }

    @Override
    protected Behaviour parseBehaviour(String bmlId, String bmlString) throws IOException
    {
        return new BMLTFaceKeyframeBehaviour(bmlId, new XMLTokenizer(bmlString));
    }
    
    @Test
    public void testReadXML() throws IOException
    {
        String str = "<bmlt:facekeyframe xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" " + TestUtil.getDefNS()
                + "id=\"kf1\" name=\"file1.xml\"/>";
        BMLTFaceKeyframeBehaviour beh = new BMLTFaceKeyframeBehaviour("bml1", new XMLTokenizer(str));
        assertEquals("file1.xml", beh.name);
    }

    @Test
    public void readInternalKeyframe() throws IOException
    {
        String keyframe = "<FaceInterpolator parts=\"morph1\">" + "0 0"
                + "</FaceInterpolator>";
        String str = "<bmlt:facekeyframe xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" " + TestUtil.getDefNS() + "id=\"kf1\">" + keyframe
                + "</bmlt:facekeyframe>";
        BMLTFaceKeyframeBehaviour beh = new BMLTFaceKeyframeBehaviour("bml1", new XMLTokenizer(str));
        assertEquals(keyframe, beh.content);
    }

    @Test
    public void readInternalKeyframeWithParameters() throws IOException
    {
        String keyframe = "<FaceInterpolator parts=\"morph1\" >" + "0 0"
                + "</FaceInterpolator>";
        String str = "<bmlt:facekeyframe xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" " + TestUtil.getDefNS() + "id=\"kf1\">"
                + "<bmlt:parameter name=\"before\" value=\"b1\"/>" + keyframe + "<bmlt:parameter name=\"after\" value=\"a1\"/>"
                + "</bmlt:facekeyframe>";
        BMLTFaceKeyframeBehaviour beh = new BMLTFaceKeyframeBehaviour("bml1", new XMLTokenizer(str));
        assertEquals(keyframe, beh.content);
        assertEquals("a1", beh.getStringParameterValue("after"));
        assertEquals("b1", beh.getStringParameterValue("before"));
    }

    @Test
    public void writeInternalKeyframeWithParameters() throws IOException, SAXException
    {
        String keyframe = "<FaceInterpolator parts=\"morph1\">" + "0 0.5"
                + "</FaceInterpolator>";
        String str = "<bmlt:facekeyframe xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" " + TestUtil.getDefNS() + "id=\"kf1\">"
                + "<bmlt:parameter name=\"before\" value=\"b1\"/>" + keyframe + "<bmlt:parameter name=\"after\" value=\"a1\"/>"
                + "</bmlt:facekeyframe>";
        BMLTFaceKeyframeBehaviour behIn = new BMLTFaceKeyframeBehaviour("bml1", new XMLTokenizer(str));

        StringBuilder buf = new StringBuilder();
        behIn.appendXML(buf);

        BMLTFaceKeyframeBehaviour behOut = new BMLTFaceKeyframeBehaviour("bml1", new XMLTokenizer(buf.toString()));
        XMLTestCase xmlTester = new XMLTestCase("")
        {
        };
        XMLUnit.setIgnoreWhitespace(true);
        xmlTester.assertXMLEqual(keyframe, behOut.content);
        assertEquals("a1", behOut.getStringParameterValue("after"));
        assertEquals("b1", behOut.getStringParameterValue("before"));
    }

    @Test
    public void testWriteXML() throws IOException
    {
        String str = "<bmlt:facekeyframe xmlns:bmlt=\"http://hmi.ewi.utwente.nl/bmlt\" " + TestUtil.getDefNS()
                + "id=\"kf1\" name=\"file1.xml\"/>";
        BMLTFaceKeyframeBehaviour behIn = new BMLTFaceKeyframeBehaviour("bml1", new XMLTokenizer(str));
        StringBuilder buf = new StringBuilder();
        behIn.appendXML(buf);

        BMLTFaceKeyframeBehaviour behOut = new BMLTFaceKeyframeBehaviour("bml1", new XMLTokenizer(buf.toString()));
        assertEquals("file1.xml", behOut.name);
    }
}
