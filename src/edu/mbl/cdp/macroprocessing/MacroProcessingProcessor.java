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
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.macro.Interpreter;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.TaggedImage;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.ReportingUtils;

public class MacroProcessingProcessor extends DataProcessor<TaggedImage> {
    
    JSONObject json = null;
    int imgDepth;    
    TaggedMacroProcessing tfa;
    TaggedImage imageOnError;

    @Override
    public void setApp(ScriptInterface gui) {
        gui_ = gui;
        tfa = new TaggedMacroProcessing();
        tfa.setApp(gui_);
        tfa.fa.processor = this;   
        tfa.frame = tfa.fa.getControlFrame();
   }

    @Override
    protected void process() {
        try {            
            final TaggedImage taggedImage = poll();
                   
            if (taggedImage == null || TaggedImageQueue.isPoison(taggedImage)) { // EOL check
                produce(taggedImage);
                return;
            }
                
            if (!tfa.fa.isUsingMacro) {
                produce(taggedImage);
                return;
            }
            
            
            // make a copy to produce if MacroProcessing throws an error
            // preserve image acquired
            imageOnError = taggedImage; 
            
            // if in Multi-D mode and disabled on plugin skip processing
            if (tfa.fa.engineWrapper_.isAcquisitionRunning() && !tfa.fa.isEnabledForImageAcquisition) {
                produce(taggedImage);
                return;
            }                        
            
            computeProduce(taggedImage); // on to computing

            if (tfa.fa.debugLogEnabled_) {
                tfa.getCMMCore().logMessage(TaggedMacroProcessing.menuName + ": exiting processor");
            }

        } catch (Exception ex) {
            produce(imageOnError);
            ReportingUtils.logError(TaggedMacroProcessing.menuName + ": ERROR in Process: ");
            ex.printStackTrace();            
        }
    }
    

    ImagePlus imp = new ImagePlus("MacroProcessingImp");    
    ImageStack impStack = null;
    
    private void computeProduce(TaggedImage taggedImageTemp) {

        try {
            if (tfa.fa.debugLogEnabled_) {
                ReportingUtils.logMessage(TaggedMacroProcessing.menuName + ": computing...");
            }
                       

            int width = MDUtils.getWidth(taggedImageTemp.tags);
            int height = MDUtils.getHeight(taggedImageTemp.tags);
            
            impStack = new ImageStack(width, height, 1);    
            impStack.setPixels(taggedImageTemp.pix, 1);
            imp = new ImagePlus(TaggedMacroProcessing.menuName, impStack);
            
            Object result = taggedImageTemp.pix;
            
            hasFinished task = new hasFinished();
            boolean bool = runMacro(tfa.fa.macroString, imp, task);
            
            if (bool) {
                result = impStack.getPixels(1);                      
            }

            // processed channel
            // Weird way of copying a JSONObject
            JSONObject tags = new JSONObject(taggedImageTemp.tags.toString());
            tags.put(MacroProcessing.METADATAKEY, tfa.fa.macroString);
            TaggedImage averagedImage = new TaggedImage(result, tags);
            produce(averagedImage);
            
            if (tfa.fa.debugLogEnabled_) {
                ReportingUtils.logMessage(TaggedMacroProcessing.menuName + ": produced computed image");
            }
        } catch (Exception ex) {
            produce(imageOnError);
            tfa.fa.isUsingMacro = false;
            tfa.fa.controlFrame_.setEnableApplyMacro(tfa.fa.isUsingMacro);
            MacroProcessingControls.showMessage("Macro error: plugin will be disabled !");
            ex.printStackTrace();
            ReportingUtils.logError(TaggedMacroProcessing.menuName + ": Error while producing computed img.");            
        }
    }
    

    public DataProcessor<TaggedImage> getDataProcessor() {
        return (DataProcessor<TaggedImage>) this;
    }

    public static boolean isPoison(TaggedImage image) {
        return ((image.pix == null) || (image.tags == null));
    }
    
    public synchronized boolean runMacro(String macro, ImagePlus imp, final hasFinished task) {
        WindowManager.setTempCurrentImage(imp);
        final Interpreter interp = new Interpreter();

        new Thread("TimeOut Thread") {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MacroProcessingProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
                while (!interp.done()) {
                    interp.abortMacro();    
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MacroProcessingProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MacroProcessingProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
                while (!task.isFinished()) {

                    getInstance().interrupt();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MacroProcessingProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }                
            }
        }.start();
        try {
            interp.runBatchMacro(macro, imp);
            task.setFinished();
            if (interp.wasError()) {
                tfa.fa.isUsingMacro = false;
                tfa.fa.controlFrame_.setEnableApplyMacro(tfa.fa.isUsingMacro);
                MacroProcessingControls.showMessage("Macro error: plugin will be disabled !");
                return false;
            }
        } catch (Exception e) {
            tfa.fa.isUsingMacro = false;
            tfa.fa.controlFrame_.setEnableApplyMacro(tfa.fa.isUsingMacro);
            MacroProcessingControls.showMessage("Macro error: plugin will be disabled !");
            interp.abortMacro();
            String msg = e.getMessage();
            if (!(e instanceof RuntimeException && msg != null && e.getMessage().equals(Macro.MACRO_CANCELED))) {
                IJ.handleException(e);
            }
            return false;
        }
        return true;
    }
    
    @Override
   public void makeConfigurationGUI() {
        if (tfa==null) {
            tfa = new TaggedMacroProcessing();
            tfa.setApp(gui_);
            tfa.fa.processor = this;
            tfa.frame = tfa.fa.getControlFrame();
            tfa.gui.addMMBackgroundListener(tfa.frame);
        } else if (tfa.frame==null) {            
            tfa.frame = tfa.fa.getControlFrame();
            tfa.gui.addMMBackgroundListener(tfa.frame);
        }
        tfa.frame.setVisible(true);
   }
    
    @Override
   public void dispose() {
      if (tfa.frame != null) {
         tfa.frame.dispose();
         tfa.frame = null;         
      }
   }
    
    public MacroProcessingProcessor getInstance() {
        return this;
    }
}

class hasFinished {
    private boolean isFinished = false;
    
    public void setFinished() {
        isFinished = true;
    }
    
    public boolean isFinished() {
        return isFinished;
    }
}
