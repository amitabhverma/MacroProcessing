package edu.mbl.cdp.macroprocessing;

/*
 * Copyright © 2009 – 2014, Marine Biological Laboratory
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of 
 * the authors and should not be interpreted as representing official policies, 
 * either expressed or implied, of any organization.
 * 
 * Macro Processing plug-in for Micro-Manager
 * @author Amitabh Verma (averma@mbl.edu)
 * Marine Biological Laboratory, Woods Hole, Mass.
 * 
 */

import javax.swing.JFrame;
import mmcorej.CMMCore;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.acquisition.AcquisitionWrapperEngine;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class TaggedMacroProcessing implements MMPlugin, org.micromanager.api.MMProcessorPlugin {

    public static final String menuName = "Macro Processing";
    public static final String tooltipDescription = "Apply ImageJ Macro Processing";
    //
    public JFrame frame;
    public MacroProcessing fa;
    public ScriptInterface gui;    
        
    public static Class<?> getProcessorClass() {
      return MacroProcessingProcessor.class;
    }
        
    @Override
    public void setApp(ScriptInterface si) {
        gui = si;
        if (fa==null) {
            fa = new MacroProcessing(getAcquisitionWrapperEngine(), gui.getMMCore(), this);
        }
    }    
    
    public AcquisitionWrapperEngine getAcquisitionWrapperEngine() {
        AcquisitionWrapperEngine engineWrapper = (AcquisitionWrapperEngine) MMStudioMainFrame.getInstance().getAcquisitionEngine();
        return engineWrapper;
    }
    
    public CMMCore getCMMCore() {       
        return gui.getMMCore();
    }
    
    public ScriptInterface getScriptInterface() {       
        return gui;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void show() {
        if (frame == null) {
            frame = fa.getControlFrame();
            gui.addMMBackgroundListener(frame);
            frame.setLocation(fa.controlFrame_.FrameXpos, fa.controlFrame_.FrameYpos);
        }
        frame.setVisible(true);
    }

//    @Override
    public void configurationChanged() {
    }

    @Override
    public String getDescription() {
        return tooltipDescription;
    }

    @Override
    public String getInfo() {
        return "MacroProcessing";
    }

    @Override
    public String getVersion() {
        return "1.01b";
    }

    @Override
    public String getCopyright() {
        return "MBL, 2014";
    }
}
