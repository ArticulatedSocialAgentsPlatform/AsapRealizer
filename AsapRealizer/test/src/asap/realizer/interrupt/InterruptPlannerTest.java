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
package asap.realizer.interrupt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import hmi.xml.XMLTokenizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import saiba.bml.parser.Constraint;
import asap.bml.ext.bmla.BMLAInfo;
import asap.bml.ext.bmla.BMLAInterruptBehaviour;
import asap.realizer.BehaviourPlanningException;
import asap.realizer.SyncAndTimePeg;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.pegboard.TimePeg;
import asap.realizer.planunit.PlanManager;
import asap.realizer.scheduler.TimePegAndConstraint;
import asap.realizertestutil.PlannerTests;
import asap.realizertestutil.util.TimePegUtil;

/**
 * Unit tests for the InterruptPlanner
 * 
 * @author welberge
 */
public class InterruptPlannerTest
{
    private InterruptPlanner interruptPlanner;
    private FeedbackManager mockBmlFeedbackManager = mock(FeedbackManager.class);
    private static final String BMLID = "bml1";
    private PlannerTests<TimedInterruptUnit> plannerTests;
    
    @Before
    public void setup()
    {
        interruptPlanner = new InterruptPlanner(mockBmlFeedbackManager, new PlanManager<TimedInterruptUnit>());
        plannerTests = new PlannerTests<TimedInterruptUnit>(interruptPlanner,new BMLBlockPeg(BMLID,0.3));
    }

    private BMLAInterruptBehaviour createInterruptBehaviour(String bml) throws IOException
    {
        return new BMLAInterruptBehaviour(BMLID, new XMLTokenizer(bml));
    }

    private BMLAInterruptBehaviour createInterruptBehaviour() throws IOException
    {
        return createInterruptBehaviour("<interrupt xmlns=\""+BMLAInfo.BMLA_NAMESPACE+"\" target=\"bml3\" id=\"i1\"/>");
    }
    
    @Test
    public void testResolveUnsetStart() throws BehaviourPlanningException, IOException
    {
        plannerTests.testResolveUnsetStart(createInterruptBehaviour());
    }
    
    @Test
    public void testResolveStartOffset() throws BehaviourPlanningException, IOException
    {
        plannerTests.testResolveStartOffset(createInterruptBehaviour());
    }
    
    @Test(expected=BehaviourPlanningException.class)
    public void testResolveNonExistingSync() throws IOException, BehaviourPlanningException
    {
        plannerTests.testResolveNonExistingSync(createInterruptBehaviour());
    }
    
    @Test
    public void testResolve() throws BehaviourPlanningException, IOException
    {
        BMLAInterruptBehaviour ipb = createInterruptBehaviour(
                "<interrupt xmlns=\""+BMLAInfo.BMLA_NAMESPACE+"\" target=\"bml3\" id=\"i1\"/>");
        ArrayList<TimePegAndConstraint> sac = new ArrayList<TimePegAndConstraint>();
        TimePeg tp = new TimePeg(BMLBlockPeg.GLOBALPEG);
        sac.add(new TimePegAndConstraint("start", tp, new Constraint(), 0));
        interruptPlanner.resolveSynchs(BMLBlockPeg.GLOBALPEG, ipb, sac);

        assertEquals(0, tp.getGlobalValue(), 0.0001);
    }

    @Test(expected = BehaviourPlanningException.class)
    public void testResolveWithUnknownSyncAndStart() throws IOException, BehaviourPlanningException
    {
        BMLAInterruptBehaviour ipb = createInterruptBehaviour(
                "<interrupt xmlns=\""+BMLAInfo.BMLA_NAMESPACE+"\" target=\"bml3\" id=\"i1\"/>");
        ArrayList<TimePegAndConstraint> sac = new ArrayList<TimePegAndConstraint>();
        TimePeg tp = new TimePeg(BMLBlockPeg.GLOBALPEG);
        sac.add(new TimePegAndConstraint("unknown", tp, new Constraint(), 0));
        sac.add(new TimePegAndConstraint("start", TimePegUtil.createTimePeg(0), new Constraint(), 0));
        interruptPlanner.resolveSynchs(BMLBlockPeg.GLOBALPEG, ipb, sac);
    }

    @Test
    public void testAdd() throws BehaviourPlanningException, IOException
    {
        BMLAInterruptBehaviour ipb = createInterruptBehaviour(
                "<interrupt xmlns=\""+BMLAInfo.BMLA_NAMESPACE+"\" target=\"bml3\" id=\"i1\"/>");
        ArrayList<TimePegAndConstraint> sac = new ArrayList<TimePegAndConstraint>();
        TimePeg tp = new TimePeg(BMLBlockPeg.GLOBALPEG);

        sac.add(new TimePegAndConstraint("start", tp, new Constraint(), 0));
        TimedInterruptUnit p = interruptPlanner.resolveSynchs(BMLBlockPeg.GLOBALPEG, ipb, sac);
        List<SyncAndTimePeg> satp = interruptPlanner.addBehaviour(BMLBlockPeg.GLOBALPEG, ipb, sac, p);
        assertEquals(1, satp.size());
        assertEquals(tp, satp.get(0).peg);
    }

    /*
     * @Test public void testSpec() throws BehaviourPlanningException { InterruptPlanner ip = new InterruptPlanner(new InterruptPlayer(),
     * mockScheduler); BMLTInterruptBehaviour ipb = new BMLTInterruptBehaviour();
     * ipb.readXML("<interrupt xmlns=\"http://hmi.ewi.utwente.nl/bmlt\" target=\"bml2\" id=\"i1\">" +
     * "<interruptspec behavior=\"speech1\" interruptSync=\"sync1\" onStart=\"bml3\"/>"+
     * "<interruptspec behavior=\"gesture1\" interruptSync=\"stroke\" onStart=\"bml4,bml5\"/>"+ "</interrupt>"); ipb.bmlId="bml1";
     * 
     * ArrayList<TimePegAndConstraint> sac = new ArrayList<TimePegAndConstraint>(); TimePeg tp = new TimePeg(BMLBlockPeg.GLOBALPEG);
     * 
     * sac.add(new TimePegAndConstraint("start", tp, new Constraint(), 0)); InterruptUnit p = ip.resolveSynchs(BMLBlockPeg.GLOBALPEG, ipb, sac); }
     */
}
