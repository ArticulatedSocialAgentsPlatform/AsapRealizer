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
package asap.speechengine.loader;

import hmi.audioenvironment.AudioEnvironment;
import hmi.environmentbase.EmbodimentLoader;
import hmi.environmentbase.Environment;
import hmi.environmentbase.Loader;
import hmi.jcomponentenvironment.JComponentEmbodiment;
import hmi.util.ArrayUtils;
import hmi.xml.XMLStructureAdapter;
import hmi.xml.XMLTokenizer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import lombok.extern.slf4j.Slf4j;
import asap.realizer.DefaultEngine;
import asap.realizer.DefaultPlayer;
import asap.realizer.Engine;
import asap.realizer.Player;
import asap.realizer.lipsync.LipSynchProvider;
import asap.realizer.planunit.MultiThreadedPlanPlayer;
import asap.realizer.planunit.PlanManager;
import asap.realizer.planunit.PlanPlayer;
import asap.realizer.visualprosody.VisualProsodyProvider;
import asap.realizerembodiments.AsapRealizerEmbodiment;
import asap.realizerembodiments.EngineLoader;
import asap.realizerembodiments.LipSynchProviderLoader;
import asap.realizerembodiments.VisualProsodyProviderLoader;
import asap.speechengine.DirectTTSUnitFactory;
import asap.speechengine.TTSPlanner;
import asap.speechengine.TimedTTSUnit;
import asap.speechengine.TimedTTSUnitFactory;
import asap.speechengine.WavTTSUnitFactory;
import asap.speechengine.ttsbinding.TTSBinding;
import asap.speechengine.ttsbinding.TTSBindingLoader;

/**
 * Loads the a SpeechEngine
 * @author reidsma
 */
@Slf4j
public class SpeechEngineLoader implements EngineLoader
{
    private enum Factory
    {
        DIRECT_TTS, WAV_TTS
    };

    public SpeechEngineLoader()
    {
    }

    private JComponentEmbodiment jce = null;
    private AudioEnvironment aue = null;

    private JComboBox<String> voiceList = null;
    private JPanel speechUIPanel = null;
    private boolean initUI = false;

    private Engine engine = null;
    private PlanManager<TimedTTSUnit> speechPlanManager = null;
    private Player speechPlayer = null;
    private PlanPlayer speechPlanPlayer = null;
    private TTSPlanner speechPlanner = null;
    private String voicename = "";
    private Factory factory = null;
    private TTSBinding ttsBinding;
    private XMLStructureAdapter adapter = new XMLStructureAdapter();

    private String id = "";
    private String characterId = "";

    private AsapRealizerEmbodiment are = null;

    private List<LipSynchProvider> lipSyncProviders = new ArrayList<LipSynchProvider>();
    private List<VisualProsodyProvider> visualProsodyProviders = new ArrayList<VisualProsodyProvider>();

    @Override
    public void readXML(XMLTokenizer tokenizer, String loaderId, String vhId, String vhName, Environment[] environments,
            Loader... requiredLoaders) throws IOException
    {
        id = loaderId;
        characterId = vhId;

        TTSBindingLoader ttsBL = ArrayUtils.getFirstClassOfType(requiredLoaders, TTSBindingLoader.class);        
        if (ttsBL == null)
        {
            throw tokenizer.getXMLScanException("Speechengineloader requires a ttsbinding.");
        }
        ttsBinding = ttsBL.getTTSBinding();
        
        for (EmbodimentLoader el : ArrayUtils.getClassesOfType(requiredLoaders, EmbodimentLoader.class))
        {
            if (el.getEmbodiment() instanceof JComponentEmbodiment)
            {
                jce = (JComponentEmbodiment) el.getEmbodiment();
            }
            if (el.getEmbodiment() instanceof AsapRealizerEmbodiment)
            {
                are = (AsapRealizerEmbodiment) el.getEmbodiment();
            }
        }
        
        for (LipSynchProviderLoader el : ArrayUtils.getClassesOfType(requiredLoaders, LipSynchProviderLoader.class))
        {
            lipSyncProviders.add(el.getLipSyncProvider());
        }
        
        for (VisualProsodyProviderLoader el : ArrayUtils.getClassesOfType(requiredLoaders, VisualProsodyProviderLoader.class))
        {
            visualProsodyProviders.add(el.getVisualProsodyProvider());
        }
        
        for (Environment e : environments)
        {
            if (e instanceof AudioEnvironment) aue = (AudioEnvironment) e;
        }
        if (aue == null)
        {
            throw tokenizer.getXMLScanException("Speechengineloader requires audioenvironment.");
        }
        if (are == null)
        {
            throw new RuntimeException("SpeechEngineLoader requires an EmbodimentLoader containing a AsapRealizerEmbodiment");
        }
        log.debug("Reading SpeechEngine");
        while (!tokenizer.atETag("Loader"))
        {
            log.debug("Reading Section");
            readSection(tokenizer);
        }
        constructEngine(tokenizer);

    }

    @Override
    public void unload()
    {
        // engine.shutdown();already done in scheduler...
    }

    protected void readSection(XMLTokenizer tokenizer) throws IOException
    {
        HashMap<String, String> attrMap = null;
        if (tokenizer.atSTag("Voice"))
        {
            log.debug("Reading Voice");
            attrMap = tokenizer.getAttributes();
            voicename = adapter.getOptionalAttribute("voicename", attrMap, "");
            String factoryString = adapter.getOptionalAttribute("factory", attrMap, "WAV_TTS");
            if (factoryString.equals("DIRECT_TTS"))
            {
                factory = Factory.DIRECT_TTS;
            }
            else if (factoryString.equals("WAV_TTS"))
            {
                factory = Factory.WAV_TTS;
            }            
            tokenizer.takeSTag("Voice");
            tokenizer.takeETag("Voice");
        }
        else if (tokenizer.atSTag("SpeechUI"))
        {
            if (jce == null) throw tokenizer.getXMLScanException("Cannot add SpeechUI when no JComponentEmbodiment is set");
            initUI = true;
            tokenizer.takeSTag("SpeechUI");
            tokenizer.takeETag("SpeechUI");
        }
        else
        {
            throw tokenizer.getXMLScanException("Unknown tag in Loader content");
        }
    }

    /** tokenizer used for throwing scanexceptions */
    private void constructEngine(XMLTokenizer tokenizer)
    {

        speechPlanManager = new PlanManager<TimedTTSUnit>();
        speechPlanPlayer = new MultiThreadedPlanPlayer<TimedTTSUnit>(are.getFeedbackManager(), speechPlanManager);
        speechPlayer = new DefaultPlayer(speechPlanPlayer);
        if(voicename!=null && !voicename.isEmpty())
        {
            ttsBinding.setVoice(voicename);
        }
        speechPlanner = getTTSPlanner(ttsBinding);

        engine = new DefaultEngine<TimedTTSUnit>(speechPlanner, speechPlayer, speechPlanManager);
        engine.setId(id);
        engine.setCharacterId(characterId);

        // add engine to realizer;
        are.addEngine(engine);

        // init ui?
        if (initUI)
        {
            final TTSPlanner ttsp = (TTSPlanner) speechPlanner;
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    voiceList = new JComboBox<String>(ttsp.getVoices());
                    voiceList.setEditable(false);
                    voiceList.setSelectedItem(voicename);
                    voiceList.addActionListener(new VoiceSelectionListener());
                    speechUIPanel = new JPanel();
                    speechUIPanel.add(new JLabel("Voice:"));
                    speechUIPanel.add(Box.createRigidArea(new Dimension(5, 0)));
                    speechUIPanel.add(voiceList);
                    jce.addJComponent(speechUIPanel);
                }
            });
        }

    }

    private TTSPlanner getTTSPlanner(TTSBinding ttsBin)
    {
        TimedTTSUnitFactory ttsFactory = null;
        switch (factory)
        {
        case DIRECT_TTS:
            ttsFactory = new DirectTTSUnitFactory(are.getFeedbackManager());
            break;
        case WAV_TTS:
            ttsFactory = new WavTTSUnitFactory(are.getFeedbackManager(), aue.getSoundManager());
            break;
        default:
            System.err.println("cannot initialize this factory, wrong type.");
            return null;
        }
        TTSPlanner ttsPlanner = new TTSPlanner(are.getFeedbackManager(), ttsFactory, ttsBin, speechPlanManager);
        ttsPlanner.addAllLipSynchers(lipSyncProviders);
        ttsPlanner.addAllVisualProsodyProviders(visualProsodyProviders);
        ttsPlanner.setCharacterId(characterId);
        
        return ttsPlanner;
    }

    class VoiceSelectionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            speechPlanner.setSpeaker((String) voiceList.getSelectedItem());

        }
    }

    /** Return the Engine that was constructed from the XML specification */
    public Engine getEngine()
    {
        return engine;
    }

    public Player getPlayer()
    {
        return speechPlayer;
    }

    public TTSPlanner getSpeechPlanner()
    {
        return speechPlanner;
    }

    public PlanManager<TimedTTSUnit> getPlanManager()
    {
        return speechPlanManager;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String newId)
    {
        id = newId;
    }
}
