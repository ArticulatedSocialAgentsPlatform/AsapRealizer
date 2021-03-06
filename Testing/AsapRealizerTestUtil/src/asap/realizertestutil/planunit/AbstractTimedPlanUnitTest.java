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
package asap.realizertestutil.planunit;

import static asap.testutil.bml.feedback.FeedbackAsserts.assertEqualSyncPointProgress;
import static asap.testutil.bml.feedback.FeedbackAsserts.assertOneFeedback;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.mockito.Matchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import saiba.bml.BMLGestureSync;
import saiba.bml.feedback.BMLSyncPointProgressFeedback;
import asap.realizer.feedback.FeedbackManager;
import asap.realizer.feedback.FeedbackManagerImpl;
import asap.realizer.pegboard.BMLBlockPeg;
import asap.realizer.pegboard.TimePeg;
import asap.realizer.planunit.TimedPlanUnit;
import asap.realizer.planunit.TimedPlanUnitPlayException;
import asap.realizer.planunit.TimedPlanUnitSetupException;
import asap.realizer.planunit.TimedPlanUnitState;
import asap.realizer.scheduler.BMLBlockManager;
import asap.realizerport.util.ListBMLFeedbackListener;
import asap.realizertestutil.util.TimePegUtil;

/**
 * Generic testcases for TimedPlanUnits.
 * The TimedPlanUnit is to be constructed in the setupPlanUnit function and must have non-zero duration.
 * @author welberge
 * 
 */
public abstract class AbstractTimedPlanUnitTest
{
    private static final double TIME_PRECISION = 0.0001;

    protected abstract TimedPlanUnit setupPlanUnit(FeedbackManager bfm, BMLBlockPeg bbPeg, String id, String bmlId, double startTime)
            throws TimedPlanUnitSetupException;

    protected static final String CHARACTER_ID = "character1";
    
    protected List<BMLSyncPointProgressFeedback> fbList = new ArrayList<BMLSyncPointProgressFeedback>();
    protected BMLBlockManager mockBlockManager = mock(BMLBlockManager.class);
    protected FeedbackManager fbManager = new FeedbackManagerImpl(mockBlockManager, CHARACTER_ID);

    protected TimedPlanUnit setupPlanUnitWithListener(BMLBlockPeg bbPeg, String id, String bmlId, double startTime)
            throws TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnit(fbManager, bbPeg, id, bmlId, startTime);
        fbManager.addFeedbackListener(new ListBMLFeedbackListener.Builder().feedBackList(fbList).build());
        return tpu;
    }

    protected void assertSubsiding(TimedPlanUnit tpu)
    {
        assertEquals(TimedPlanUnitState.SUBSIDING, tpu.getState());
    }

    @Before
    public void genericSetup() 
    {
    	//Many of the tests below use the fbManager, which retrieves a characterId from the BlockManager to construct feedback
    	//However, the characterID may not be null, so we always return a sensible default instead
    	when(mockBlockManager.getCharacterId(anyString())).thenReturn(CHARACTER_ID);
    }
    
    @Test
    public void testFeedback() throws TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        BMLSyncPointProgressFeedback expected = new BMLSyncPointProgressFeedback("bml1", "beh1", "stroke", 0, 0);
        tpu.feedback(expected);
        assertOneFeedback(expected, fbList);
    }

    @Test
    public void testSetup() throws TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        assertEquals(0, tpu.getStartTime(), TIME_PRECISION);
        assertEquals(TimedPlanUnitState.IN_PREP, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testPlayInPrep() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.play(0);
        assertEquals(TimedPlanUnitState.IN_PREP, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testStopInPrep() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.stop(0);
        assertEquals(TimedPlanUnitState.DONE, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testStartInPrep() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.start(0);
        assertEquals(TimedPlanUnitState.IN_PREP, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testPlayInPending() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.PENDING);
        tpu.play(0);
        assertEquals(TimedPlanUnitState.PENDING, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testStartInPending() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.PENDING);
        tpu.start(0);
        assertEquals(TimedPlanUnitState.PENDING, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testStopInPending() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.PENDING);
        tpu.stop(0);
        assertEquals(TimedPlanUnitState.DONE, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testPlayInLurking() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.LURKING);
        tpu.play(0);
        assertEquals(TimedPlanUnitState.LURKING, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testStopInLurking() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.LURKING);
        tpu.stop(0);
        assertEquals(TimedPlanUnitState.DONE, tpu.getState());
        assertThat(fbList, hasSize(0));
    }

    @Test
    public void testStartInLurking() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.LURKING);
        tpu.start(0);
        assertEquals(TimedPlanUnitState.IN_EXEC, tpu.getState());
    }

    @Test
    public void testStartThenPlayInLurking() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.LURKING);
        tpu.start(0);
        tpu.play(0);
        assertThat(tpu.getState(),
                anyOf(equalTo(TimedPlanUnitState.SUBSIDING), equalTo(TimedPlanUnitState.IN_EXEC), equalTo(TimedPlanUnitState.DONE)));
        BMLSyncPointProgressFeedback expected = new BMLSyncPointProgressFeedback("bml1", "id1", "start", 0, 0);
        assertEqualSyncPointProgress(expected, fbList.get(0));
    }

    @Test
    public void testPlayInExec() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.LURKING);
        tpu.start(0);
        tpu.play(0);
        tpu.play(0);
        assertThat(tpu.getState(),
                anyOf(equalTo(TimedPlanUnitState.SUBSIDING), equalTo(TimedPlanUnitState.IN_EXEC), equalTo(TimedPlanUnitState.DONE)));
    }

    @Test
    public void testStartInExec() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        tpu.setState(TimedPlanUnitState.IN_EXEC);
        tpu.start(0);
        assertEquals(TimedPlanUnitState.IN_EXEC, tpu.getState());
        assertThat(fbList, hasSize(0));// do not resend start feedback
    }

    @Test
    public void testSetStrokePeg() throws TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        TimePeg strokePeg = new TimePeg(BMLBlockPeg.GLOBALPEG);
        strokePeg.setGlobalValue(2);
        tpu.setTimePeg(BMLGestureSync.STROKE.getId(), strokePeg);
        assertEquals(2f, tpu.getTime(BMLGestureSync.STROKE.getId()), TIME_PRECISION);
    }

    @Test
    public void testSubsiding() throws TimedPlanUnitPlayException, TimedPlanUnitSetupException
    {
        TimedPlanUnit tpu = setupPlanUnitWithListener(BMLBlockPeg.GLOBALPEG, "id1", "bml1", 0);
        TimePeg relaxPeg = new TimePeg(BMLBlockPeg.GLOBALPEG);
        relaxPeg.setGlobalValue(2);
        relaxPeg.setAbsoluteTime(true);
        tpu.setTimePeg(BMLGestureSync.RELAX.toString(), relaxPeg);
        TimePeg end = TimePegUtil.createTimePeg(2.2);
        end.setAbsoluteTime(true);
        tpu.setTimePeg("end", end);
        tpu.setState(TimedPlanUnitState.LURKING);
        tpu.start(0);
        tpu.play(2.1);
        assertSubsiding(tpu);
    }
}
