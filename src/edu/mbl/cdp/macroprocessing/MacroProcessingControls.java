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

import com.swtdesigner.SwingResourceManager;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.io.SaveDialog;
import ij.macro.Interpreter;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import org.json.JSONObject;
import org.micromanager.imageDisplay.AcquisitionVirtualStack;
import org.micromanager.imageDisplay.VirtualAcquisitionDisplay;
import org.micromanager.internalinterfaces.AcqSettingsListener;
import org.micromanager.utils.GUIUtils;
import org.micromanager.utils.ImageFocusListener;

public class MacroProcessingControls extends javax.swing.JFrame implements AcqSettingsListener, ImageFocusListener {

    /* TODO
     * 
     * 
     */
    static MacroProcessingControls frame;
    private MacroProcessing fa_;
    private static JFrame About;
    
    private FunctionFinderOPS functionFinder;
    static String Pref_IsUsingMacro = "PREF_IsUsingMacro";
    
    public static String Pref_FileName = "PREF_FileName";
    String FileName = "";
    public static final String MMgrDir = ij.IJ.getDirectory("imagej");
    public static final String SEP_FILE = System.getProperty("file.separator");
    public static String Macros_Location = new File(MMgrDir).getAbsolutePath() + SEP_FILE + "macros";
    public static String Pref_FileDir = "PREF_FileDir";
    String FileDir;

    
    public static final Preferences MacroControlPrefs = Preferences.userNodeForPackage(MacroProcessingControls.class);
    public final String FrameXposKey = "PREF_FrameX";
    public final String FrameYposKey = "PREF_FrameY";
    public final String MacroStringKey = "PREF_MacroStringKey";
    public int FrameXpos = 300;
    public int FrameYpos = 300;
    
    static boolean showingMsg = false;
    boolean isValidMacro = false;
       
    
    Image iconImage;
    ij.plugin.frame.Editor editor;
            
    /**
     * Creates new form MacroProcessingControls
     */
    public MacroProcessingControls(MacroProcessing fa) {
        this.fa_ = fa;
        initComponents();
        iconImage = SwingResourceManager.getImage(MacroProcessingControls.class, "frameIcon.png");
        setIconImage(iconImage);
        getPreferences();
        
        jButton5.setBackground(Color.RED);
        
        addUndoManager(jTextArea1);
        addTabSpaceDefine(jTextArea1, 3);
        addLineNumbersUtil(jTextArea1, jScrollPane1);
        
        setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        this.setLocation(FrameXpos, FrameYpos);
        this.setTitle(TaggedMacroProcessing.menuName);
        
        setMacroText(fa.macroString);
        
        GUIUtils.registerImageFocusListener(this); // Image Window listener

        this.pack();
        frame = this;
    }       
    
    @Override
    public void settingsChanged() {        

    }        
    
    private void getPreferences() {
        FrameXpos = MacroControlPrefs.getInt(FrameXposKey, FrameXpos);
        FrameYpos = MacroControlPrefs.getInt(FrameYposKey, FrameYpos);
        fa_.macroString = MacroControlPrefs.get(MacroStringKey, fa_.macroString);
        fa_.macroStringBackup = fa_.macroString;
    }
    
    private void setPreferences() {
        
        FrameXpos = this.getX();
        FrameYpos = this.getY();
        
        MacroControlPrefs.putInt(FrameXposKey, FrameXpos);
        MacroControlPrefs.putInt(FrameYposKey, FrameYpos);
        MacroControlPrefs.put(MacroStringKey, fa_.macroString);
    }
    
    public void setEnableApplyMacro(boolean bool) {
        jCheckBox2.setSelected(bool);
        setPreferences();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton4 = new javax.swing.JButton();
        enabledCheckBox_ = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jPanelSouth = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jCheckBox2 = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JEditorPane();
        jButton5 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        jButton4.setText("jButton4");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(new java.awt.Rectangle(300, 300, 150, 150));
        setMinimumSize(new java.awt.Dimension(150, 150));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                formFocusLost(evt);
            }
        });

        enabledCheckBox_.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        enabledCheckBox_.setText("Enable for Multi-D Image Acquisition");
        enabledCheckBox_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enabledCheckBox_ActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel3.setText("Macro Processing");

        jPanelSouth.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        jPanelSouth.setLayout(new java.awt.BorderLayout());

        jPanel1.setBackground(new java.awt.Color(204, 204, 204));

        jCheckBox2.setBackground(new java.awt.Color(204, 204, 204));
        jCheckBox2.setText("Apply Macro Processing");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jButton1.setIcon(new javax.swing.ImageIcon("C:\\Users\\Amitabh\\Documents\\GitHub\\Micro-Manager-Addons\\MacroProcessing\\src\\edu\\mbl\\cdp\\macroprocessing\\help-icon.png")); // NOI18N
        jButton1.setToolTipText("Help with OpenPolScope functions");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setIcon(new javax.swing.ImageIcon("C:\\Users\\Amitabh\\Documents\\GitHub\\Micro-Manager-Addons\\MacroProcessing\\src\\edu\\mbl\\cdp\\macroprocessing\\save.png")); // NOI18N
        jButton2.setToolTipText("Save Current Text to a Macro File");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setIcon(new javax.swing.ImageIcon("C:\\Users\\Amitabh\\Documents\\GitHub\\Micro-Manager-Addons\\MacroProcessing\\src\\edu\\mbl\\cdp\\macroprocessing\\file.png")); // NOI18N
        jButton3.setToolTipText("Open a Macro file into Text Area");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jTextArea1.setText("// Macro Processing Example\n// Refer http://rsb.info.nih.gov/ij/developer/macro/macros.html for more info\n\nsetFont(\"SansSerif\", 50, \"antialiased\");\nsetColor(\"white\");\ndrawString(\"MacroProcessing\", 50, 100);\nrun(\"Gaussian Blur...\", \"sigma=2 slice\");\nrun(\"Measure\");");
        jTextArea1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTextArea1FocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextArea1FocusLost(evt);
            }
        });
        jScrollPane1.setViewportView(jTextArea1);

        jButton5.setBackground(new java.awt.Color(255, 0, 0));
        jButton5.setText("Test");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("0 ms.");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jCheckBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 174, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton5)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jButton3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jCheckBox2)
                        .add(jButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jButton5))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelSouth.add(jPanel1, java.awt.BorderLayout.CENTER);

        jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField1.setText("1000");
        jTextField1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField1FocusLost(evt);
            }
        });

        jLabel2.setText("TimeOut (ms.)");

        jMenu1.setText("Help");

        jMenuItem1.setText("About");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(enabledCheckBox_)
                        .addContainerGap(206, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(jLabel3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 44, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18))))
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanelSouth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 271, Short.MAX_VALUE)
                .add(enabledCheckBox_)
                .addContainerGap())
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                    .add(36, 36, 36)
                    .add(jPanelSouth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(36, 36, 36)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
      
  }//GEN-LAST:event_formWindowClosed

  private void enabledCheckBox_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enabledCheckBox_ActionPerformed
      if (!showingMsg) {
        if (fa_.core_.isSequenceRunning()) {
            enabledCheckBox_.setSelected(!enabledCheckBox_.isSelected());
            MacroProcessingControls.showMessage("Live mode is running ! Please stop before enabling/disabling.");          
            return;
        } else if (fa_.tfa_.gui.isAcquisitionRunning()) {
            enabledCheckBox_.setSelected(!enabledCheckBox_.isSelected());
            MacroProcessingControls.showMessage("Acquisition is running ! Please stop before enabling/disabling.");          
            return;
        }
        fa_.isEnabledForImageAcquisition = enabledCheckBox_.isSelected();
      } else {
          enabledCheckBox_.setSelected(!enabledCheckBox_.isSelected());
      }
  }//GEN-LAST:event_enabledCheckBox_ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        if (About == null) {
            About = new About(this);
        }
        About.setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void formFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        fa_.getDebugOptions();
    }//GEN-LAST:event_formFocusGained

    private void formFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusLost
        fa_.getDebugOptions();
    }//GEN-LAST:event_formFocusLost

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (fa_.core_.isSequenceRunning()) {
            if (enabledCheckBox_.isSelected()) {
                setVisible( true );
                MacroProcessingControls.showMessage("Live mode is running with FrameAverager ! Please stop Live before closing FrameAverager.");    
            }
        } else if (fa_.tfa_.gui.isAcquisitionRunning()) {
            if (enabledCheckBox_.isSelected()) {
                setVisible( true );
                MacroProcessingControls.showMessage("Acquisition is running with FrameAverager ! Please stop Acquisition before closing FrameAverager.");    
            }
        } else {
            if (enabledCheckBox_.isSelected()) {
                enabledCheckBox_.setSelected(false);
            }
            setVisible( true );
            setPreferences();
            showNonBlockingYesNo(iconImage);
        }
    }//GEN-LAST:event_formWindowClosing

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        if (isValidMacro) {
            fa_.isUsingMacro = jCheckBox2.isSelected();
            setPreferences();
        } else {
            fa_.isUsingMacro = false;
            jCheckBox2.setSelected(false);
            MacroProcessingControls.showMessage("Please 'Test' macro for any errors !");
        }
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (jTextArea1 != null) {
            if (functionFinder != null && functionFinder.isVisible()) {
                functionFinder.toFront(); return;
            } else if (functionFinder != null) {
                functionFinder.close();
            }
            
            java.awt.Point posi = frame.getLocationOnScreen();
            int initialX = (int) posi.getX() + 38;
            int initialY = (int) posi.getY() + 84;
            functionFinder = new FunctionFinderOPS(jTextArea1, initialX, initialY);
        } else {
            showMessage("Macro Editor is not open !");
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        SaveDialog sd = new SaveDialog("Save As...", Macros_Location, FileName, null);
        String fName = sd.getFileName();
        String dir = sd.getDirectory();
        if (fName != null) {
            FileName = fName;
            FileDir = sd.getDirectory();
            writeFile(fa_.macroString, (dir + SEP_FILE + FileName));
            setPreferences();
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        String str = IJ.openAsString(null);
        if (str != null && !str.isEmpty() && !str.startsWith("Error:")) {
            
            fa_.macroString = str;
            
            setMacroText(fa_.macroString);
            setPreferences();
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jTextArea1FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextArea1FocusLost
        final String str = getMacroText();
        
        if (!str.equals(fa_.macroStringBackup)) {
            isValidMacro = false;
            jButton5.setBackground(Color.RED);
            fa_.isUsingMacro = false;
            jCheckBox2.setSelected(false);
            jLabel1.setText("0 ms.");
        }
        
        if (!showingMsg && !str.equals(fa_.macroStringBackup)) {
            if (fa_.core_.isSequenceRunning()) {
                setMacroText(fa_.macroStringBackup);
                MacroProcessingControls.showMessage("Live mode is running ! Please stop before editing.");
                return;
            } else if (fa_.tfa_.gui.isAcquisitionRunning()) {
                setMacroText(fa_.macroStringBackup);
                MacroProcessingControls.showMessage("Acquisition is running ! Please stop before editing.");                
                return;
            }
        }
        
        final Runnable runn = new Runnable() {
            public void run() {
                if (!showingMsg) {
                    if (isValidMacro) {
                        fa_.macroString = str;
                        setPreferences();
                    } else {
                        MacroProcessingControls.showMessage("Please 'Test' macro for any errors !");
                    }
                } else {
                    setMacroText(fa_.macroStringBackup);
                }
            }
        };   
        
        if (!TestMacroDialog.isAlive()) {
            TestMacroDialog = new Thread() {
                public void run() {
                    try {
                        while (!isValidMacro) {
                            Thread.sleep(10);
                        }
                        while (isTestingMacro) {
                            Thread.sleep(10);
                        }
                        Thread.sleep(1000);
                        runn.run();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MacroProcessingControls.class.getName()).log(Level.SEVERE, null, ex);                    
                    }
                }
            };
            TestMacroDialog.start();
        }
    }//GEN-LAST:event_jTextArea1FocusLost

    private void jTextArea1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextArea1FocusGained
        fa_.macroStringBackup = getMacroText();        
    }//GEN-LAST:event_jTextArea1FocusGained

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
       String str = getMacroText();
       long startTime = System.currentTimeMillis();
       boolean bool = testMacro(str);
       long estimatedTime = System.currentTimeMillis() - startTime;
       jLabel1.setText(estimatedTime + " ms.");
       
       jTextField1.setText(String.valueOf(estimatedTime*5));
              
       if (bool) {
           jButton5.setBackground(Color.GREEN);
       } else {
           jButton5.setBackground(Color.RED);
           jCheckBox2.setSelected(false);
           fa_.isUsingMacro = false;
       }   
       
       isValidMacro = bool;
       isTestingMacro = false;
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jTextField1FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextField1FocusLost
        String str = jTextField1.getText();
        try {
            fa_.processor.timeOut = (long) Integer.parseInt(str);
        } catch (NumberFormatException e) {
            System.out.println("CAUGHT: " + e.toString());
            System.out.println("Defaulting value to 1000");
            fa_.processor.timeOut = 1000;
            jTextField1.setText(String.valueOf(1000));
        }
    }//GEN-LAST:event_jTextField1FocusLost

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox enabledCheckBox_;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelSouth;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JEditorPane jTextArea1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables

    
    public static void showMessage(String msg) {
        showNonBlockingMessage(JOptionPane.WARNING_MESSAGE, TaggedMacroProcessing.menuName, msg, getInstance());
    }
    
    public static void showMessage(String title, String msg) {
        showNonBlockingMessage(JOptionPane.WARNING_MESSAGE, title, msg, getInstance());
    }
    
    
    public static void showNonBlockingMessage(int msgType, String title, String message, Frame owningFrame_) {
      if (null != owningFrame_) {
         Object[] options = { "OK" };
         final JOptionPane optionPane = new JOptionPane(message, msgType, JOptionPane.DEFAULT_OPTION, null, options);
         /* the false parameter is for not modal */
         final JDialog dialog = new JDialog(owningFrame_, title, false);
         optionPane.addPropertyChangeListener(
                 new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent e) {
                       String prop = e.getPropertyName();
                       if (dialog.isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                          dialog.setVisible(false);
                          showingMsg = false;
                       }
                    }
                 });         
         
         dialog.setContentPane(optionPane);
         /* adapting the frame size to its content */
         dialog.pack();
         dialog.setLocationRelativeTo(owningFrame_);
         dialog.setVisible(true);
         showingMsg = true;
      }
   }
    
    public static void showNonBlockingYesNo(Image iconImage) {
        showNonBlockingYesNo(JOptionPane.QUESTION_MESSAGE, TaggedMacroProcessing.menuName, "Remove from Micro-Manager's Image Pipeline ?", getInstance(), iconImage);
    }
    
    public static void showNonBlockingYesNo(int msgType, String title, String message, final MacroProcessingControls owningFrame_, Image iconImage) {
      if (null != owningFrame_) {
         Object[] options = { "Yes", "No", "Cancel" };
         final JOptionPane optionPane = new JOptionPane(message, msgType, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
         /* the false parameter is for not modal */
         final JDialog dialog = new JDialog((JDialog)null, title, false);
         dialog.setIconImage(iconImage);

         optionPane.addPropertyChangeListener(
                 new PropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent e) {
                       String prop = e.getPropertyName();
                       
                       if (dialog.isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                          String selectValue = (String) e.getNewValue();
                          
                           if ((selectValue.equals("Cancel"))) {
                               
                           } else {                                
                                if ((selectValue.equals("Yes"))) {
                                    owningFrame_.fa_.stopAndClearProcessor();
                                }
                                owningFrame_.setVisible(false);
                           }  

                           dialog.setVisible(false);
                           dialog.dispose();
                           showingMsg = false;
                       }
                    }
                 });         
         
         dialog.setContentPane(optionPane);
         /* adapting the frame size to its content */
         dialog.pack();
         dialog.setLocationRelativeTo(owningFrame_);
         dialog.setVisible(true);
         showingMsg = true;
      }
   }
    
    public VirtualAcquisitionDisplay display_;
    @Override
    public void focusReceived(ImageWindow focusedWindow) {
        // discard if closed
        if (focusedWindow == null) {
            return;
        }
        // discard Snap/Live Window
        if (focusedWindow != null) {
            String str = focusedWindow.getTitle();
            if (focusedWindow.getTitle().startsWith("Snap/Live Window") ||  str.equals(" (100%)")) {
                ImageStack ImpStack = focusedWindow.getImagePlus().getImageStack();
                if (ImpStack instanceof AcquisitionVirtualStack) {
                    display_ = ((AcquisitionVirtualStack) ImpStack).getVirtualAcquisitionDisplay();
                } else {
                    display_ = null;
                }
                return;
            }     
        }
        
        if (!focusedWindow.isClosed()) {
            ImageStack ImpStack = focusedWindow.getImagePlus().getImageStack();
            VirtualAcquisitionDisplay display;
            if (ImpStack instanceof AcquisitionVirtualStack) {
                display = ((AcquisitionVirtualStack) ImpStack).getVirtualAcquisitionDisplay();
                if (display.acquisitionIsRunning()) {
                    display_ = display;
                }
            } else {
                if (display_!=null && !display_.acquisitionIsRunning()) {
                    display_ = null;
                }
            }
        }
    }
    
    public String getMacroText() {
        return jTextArea1.getText();
    }
    
    public void setMacroText(String str) {
        jTextArea1.setText(str);
    }
        
    Thread TestMacroDialog = new Thread();
    boolean isTestingMacro = false;
    
    
    public void fillMetadataArray() {
        if (display_ != null) {
            int chN = display_.getNumChannels();
            MyVariables.ChannelMetadata_ = new JSONObject[chN];
            MyVariables.SummaryMetadata = display_.getSummaryMetadata();
            for (int i=0; i < chN; i++) {
                MyVariables.ChannelMetadata_[i] = display_.getImageCache().getImageTags(i, 0, 0, 0);
            }
        }
    }
    
    public boolean testMacro(String macro) {
        isTestingMacro = true;
        
        ImagePlus imp = WindowManager.getCurrentImage();
        
        if (imp == null) {
            MacroProcessingControls.showMessage("Macro error: No Image window was found ! \n"
                    + "Use 'Snap' to create a test image window.");
            return false;
        }
        
        fillMetadataArray();
        
        String extStr = "var x=installOpsExtensions();"
                    + "function installOpsExtensions() {"
                    + "      run(\"MacroExtensionsStub\");"
                    + "      return 0;"
                    + "  }";
        
        macro = extStr + macro;        
                                
        WindowManager.setTempCurrentImage(imp);
        final Interpreter interp = new Interpreter();
        try {
            interp.run(macro);
            
            if (interp.wasError()) {
                macroError(interp, null);
                return false;
            }
            } catch (Exception e) {
                macroError(interp, e);
                return false;
            }
        
        return true;
    }
        
    public void macroError(Interpreter interp, Exception e) {
        MacroProcessingControls.showMessage("Macro error: Macro will not be saved until errors are fixed !");
        interp.abortMacro();
        if (e != null){
            String msg = e.getMessage();
            if (!(e instanceof RuntimeException && msg != null && e.getMessage().equals(Macro.MACRO_CANCELED))) {
                IJ.handleException(e);
            }
        }
    }    
    
    public void writeFile(String text, String path) {
        
		char[] chars = new char[text.length()];
		text.getChars(0, text.length(), chars, 0);
		try {
			BufferedReader br = new BufferedReader(new CharArrayReader(chars));
			BufferedWriter bw = new BufferedWriter(new FileWriter(path));
			while (true) {
				String s = br.readLine();
				if (s==null) break;
				bw.write(s, 0, s.length());
				bw.newLine();
			}
			bw.close();
			IJ.showStatus(text.length()+" chars saved to " + path);
		} catch
			(IOException e) {}
    }
        
    public static void addUndoManager(JTextComponent textArea) {
        final UndoManager undoManager = new UndoManager();
        Document doc = textArea.getDocument();
        doc.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
            }
        });

        InputMap im = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = textArea.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Redo");

        am.put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } catch (CannotUndoException exp) {
                    exp.printStackTrace();
                }
            }
        });
        am.put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                } catch (CannotUndoException exp) {
                    exp.printStackTrace();
                }
            }
        });
    }
    
    public static void addTabSpaceDefine(JTextComponent jta, final int tabSpaceCount) {

        jta.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                String selectionString = "";
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    JTextComponent textArea = (JTextComponent) e.getSource();
                    selectionString = textArea.getSelectedText();

                    String s = "\t";
                    if (tabSpaceCount > 0) {
                        s = "";
                        for (int i=0; i < tabSpaceCount;i++) {
                            s = s + " ";
                        }
                    }
                    int n = textArea.getCaretPosition();
                    String str = s;
                    if (selectionString == null) {
                        textArea.select(n, n);
                        textArea.replaceSelection(s);
                        textArea.setCaretPosition(n + str.length());
                    } else {
                        int n2 = textArea.getSelectionStart();
                        if (n > n2) {
                            n = n2;
                        }
                        if (!selectionString.contains("\n")) {
                            textArea.select(n, n + selectionString.length());
                            str = s + selectionString;
                        } else {
                            textArea.select(n, n + selectionString.length());
                            str = s + selectionString.replace("\n", "\n" + s);
                            if (str.endsWith(s)) {
                                str = str.substring(0, str.length() - s.length());
                            }
                        }
                        textArea.replaceSelection(str);
                        textArea.select(n + s.length(), n + str.length());
                        textArea.moveCaretPosition(n + s.length());
                    }

                    e.consume();
                }
                if (e.getKeyCode() == KeyEvent.VK_TAB
                        && e.isShiftDown()
                        && selectionString != null) {
                    e.consume();
                }
            }
        });
    }

    public static void addLineNumbersUtil(JTextComponent textPane, JScrollPane scrollPane) {
        TextLineNumber tln = new TextLineNumber(textPane, 4);
        scrollPane.setRowHeaderView(tln);
    }
    
  public static MacroProcessingControls getInstance() {
      return frame;
  }
}
