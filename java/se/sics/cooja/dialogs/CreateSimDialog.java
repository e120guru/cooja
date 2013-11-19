/*
 * Copyright (c) 2006, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

package se.sics.cooja.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.Random;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;

import se.sics.cooja.Cooja;
import se.sics.cooja.RadioMedium;
import se.sics.cooja.Simulation;

/**
 * A dialog for creating and configuring a simulation.
 *
 * @author Fredrik Osterlind
 */
public class CreateSimDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(CreateSimDialog.class);

  private final static int LABEL_WIDTH = 170;
  private final static int LABEL_HEIGHT = 25;

  private Simulation mySimulation = null;

  private JFormattedTextField randomSeed, delayedStartup;
  private JCheckBox randomSeedGenerated;

  private JTextField title;
  private JComboBox radioMediumBox;

  private JButton cancelButton;

  /**
   * Shows a dialog for configuring a simulation.
   *
   * @param parent Parent container for dialog
   * @param simulation Simulation to configure
   * @return True if simulation configured correctly
   */
  public static boolean showDialog(Container parent, Simulation simulation) {
    final CreateSimDialog dialog = new CreateSimDialog((Window) parent, simulation.getCooja());
    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        dialog.cancelButton.doClick();
      }
    });

    dialog.mySimulation = simulation;

    // Set title
    if (simulation.getTitle() != null) {
      // Title already preset
      dialog.title.setText(simulation.getTitle());
    } else {
      // Suggest title
      dialog.title.setText("My simulation");
    }

    // Select radio medium
    if (simulation.getRadioMedium() != null) {
      Class<? extends RadioMedium> radioMediumClass =
        simulation.getRadioMedium().getClass();

      String currentDescription = Cooja.getDescriptionOf(radioMediumClass);

      for (int i=0; i < dialog.radioMediumBox.getItemCount(); i++) {
        String menuDescription = (String) dialog.radioMediumBox.getItemAt(i);
        if (menuDescription.equals(currentDescription)) {
          dialog.radioMediumBox.setSelectedIndex(i);
          break;
        }
      }
    }

    // Set random seed
    if (simulation.getRandomSeedGenerated()) {
      dialog.randomSeedGenerated.setSelected(true);
      dialog.randomSeed.setEnabled(false);
      dialog.randomSeed.setText("[autogenerated]");
    } else {
      dialog.randomSeed.setEnabled(true);
      dialog.randomSeed.setValue(new Long(simulation.getRandomSeed()));
    }

    // Set delayed mote startup time (ms)
    dialog.delayedStartup.setValue(new Long(simulation.getDelayedMoteStartupTime()/Simulation.MILLISECOND));


    // Set position and focus of dialog
    dialog.setLocationRelativeTo(parent);
    dialog.title.requestFocus();
    dialog.title.select(0, dialog.title.getText().length());

    // Dispose on escape key
    InputMap inputMap = dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "dispose");
    AbstractAction cancelAction = new AbstractAction(){
      public void actionPerformed(ActionEvent e) {
        dialog.cancelButton.doClick();
      }
    };
    dialog.getRootPane().getActionMap().put("dispose", cancelAction);

    dialog.setVisible(true);

    if (dialog.mySimulation != null) {
      // Simulation configured correctly
      return true;
    }
    return false;
  }

  private CreateSimDialog(Window window, Cooja gui) {
    super(window, "Create new simulation", ModalityType.APPLICATION_MODAL);
    Box vertBox = Box.createVerticalBox();

    JLabel label;
    JTextField textField;
    Box horizBox;
    JButton button;
    JComboBox comboBox;
    JFormattedTextField numberField;
    NumberFormat integerFormat = NumberFormat.getIntegerInstance();


    // BOTTOM BUTTON PART
    Box buttonBox = Box.createHorizontalBox();
    buttonBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

    buttonBox.add(Box.createHorizontalGlue());

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mySimulation = null;
        dispose();
      };
    });
    buttonBox.add(cancelButton);

    button = new JButton("Create");
    button.addActionListener(createSimulationListener);
    buttonBox.add(Box.createHorizontalStrut(5));
    getRootPane().setDefaultButton(button);
    buttonBox.add(button);


    // MAIN PART

    // Title
    horizBox = Box.createHorizontalBox();
    horizBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,LABEL_HEIGHT));
    horizBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    label = new JLabel("Simulation name");
    label.setPreferredSize(new Dimension(LABEL_WIDTH,LABEL_HEIGHT));

    textField = new JTextField();
    textField.setText("[no title]");
    textField.setColumns(25);
    title = textField;

    horizBox.add(label);
    horizBox.add(Box.createHorizontalStrut(10));
    horizBox.add(textField);

    vertBox.add(horizBox);
    vertBox.add(Box.createRigidArea(new Dimension(0,5)));

    // -- Advanced settings --
    Box advancedBox = Box.createVerticalBox();
    advancedBox.setBorder(BorderFactory.createTitledBorder("Advanced settings"));

    // Radio Medium selection
    horizBox = Box.createHorizontalBox();
    horizBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,LABEL_HEIGHT));
    horizBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    label = new JLabel("Radio medium");
    label.setPreferredSize(new Dimension(LABEL_WIDTH,LABEL_HEIGHT));

    Vector<String> radioMediumDescriptions = new Vector<String>();
    for (Class<? extends RadioMedium> radioMediumClass: gui.getRegisteredRadioMediums()) {
      String description = Cooja.getDescriptionOf(radioMediumClass);
      radioMediumDescriptions.add(description);
    }

    comboBox = new JComboBox(radioMediumDescriptions);

    comboBox.setSelectedIndex(0);
    radioMediumBox = comboBox;
    label.setLabelFor(comboBox);

    horizBox.add(label);
    horizBox.add(Box.createHorizontalStrut(10));
    horizBox.add(comboBox);
    horizBox.setToolTipText("Determines the radio surroundings behaviour");

    /*vertBox.add(horizBox);
    vertBox.add(Box.createRigidArea(new Dimension(0,5)));*/
    advancedBox.add(horizBox);
    advancedBox.add(Box.createRigidArea(new Dimension(0,5)));

    // Delayed startup
    horizBox = Box.createHorizontalBox();
    horizBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,LABEL_HEIGHT));
    horizBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    label = new JLabel("Mote startup delay (ms)");
    label.setPreferredSize(new Dimension(LABEL_WIDTH,LABEL_HEIGHT));

    numberField = new JFormattedTextField(integerFormat);
    numberField.setValue(new Integer(10000));
    numberField.setColumns(4);
    delayedStartup = numberField;

    horizBox.add(label);
    horizBox.add(Box.createHorizontalStrut(150));
    horizBox.add(numberField);
    horizBox.setToolTipText("Maximum mote startup delay (random interval: [0, time])");

    advancedBox.add(horizBox);
    advancedBox.add(Box.createVerticalStrut(5));

    // Random seed
    horizBox = Box.createHorizontalBox();
    horizBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,LABEL_HEIGHT));
    horizBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    label = new JLabel("Random seed");
    label.setPreferredSize(new Dimension(LABEL_WIDTH,LABEL_HEIGHT));

    numberField = new JFormattedTextField(integerFormat);
    numberField.setValue(new Integer(123456));
    numberField.setColumns(4);
    randomSeed = numberField;

    horizBox.add(label);
    horizBox.add(Box.createHorizontalStrut(150));
    horizBox.add(numberField);
    horizBox.setToolTipText("Simulation random seed. Controls the random behavior such as mote startup delays, node positions etc.");

    advancedBox.add(horizBox);
    advancedBox.add(Box.createVerticalStrut(5));

    horizBox = Box.createHorizontalBox();
    horizBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,LABEL_HEIGHT));
    horizBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    label = new JLabel("New random seed on reload");
    label.setPreferredSize(new Dimension(LABEL_WIDTH,LABEL_HEIGHT));
    randomSeedGenerated = new JCheckBox();
    randomSeedGenerated.setToolTipText("Automatically generate random seed at simulation load");
    randomSeedGenerated.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (((JCheckBox)e.getSource()).isSelected()) {
          randomSeed.setEnabled(false);
          randomSeed.setText("[autogenerated]");
        } else {
          randomSeed.setEnabled(true);
          randomSeed.setValue(new Integer(123456));
        }
      }

    });

    horizBox.add(label);
    horizBox.add(Box.createHorizontalStrut(144));
    horizBox.add(randomSeedGenerated);

    advancedBox.add(horizBox);
    advancedBox.add(Box.createVerticalStrut(5));

    vertBox.add(advancedBox);
    vertBox.add(Box.createVerticalGlue());

    vertBox.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    Container contentPane = getContentPane();
    contentPane.add(vertBox, BorderLayout.CENTER);
    contentPane.add(buttonBox, BorderLayout.SOUTH);

    pack();
  }

  private ActionListener createSimulationListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      mySimulation.setTitle(title.getText());

      String currentRadioMediumDescription = (String) radioMediumBox.getSelectedItem();
      for (Class<? extends RadioMedium> radioMediumClass: mySimulation.getCooja().getRegisteredRadioMediums()) {
        String radioMediumDescription = Cooja.getDescriptionOf(radioMediumClass);

        if (currentRadioMediumDescription.equals(radioMediumDescription)) {
          try {
            RadioMedium radioMedium = RadioMedium.generateRadioMedium(radioMediumClass, mySimulation);
            mySimulation.setRadioMedium(radioMedium);
          } catch (Exception ex) {
            logger.fatal("Error generating radio medium: " + ex.getMessage(), ex);
            mySimulation.setRadioMedium(null);
          }
          break;
        }
      }

      if (randomSeedGenerated.isSelected()) {
        mySimulation.setRandomSeedGenerated(true);
        mySimulation.setRandomSeed(new Random().nextLong());
      } else {
        mySimulation.setRandomSeedGenerated(false);
        mySimulation.setRandomSeed(((Number) randomSeed.getValue()).longValue());
      }

      mySimulation.setDelayedMoteStartupTime((int) ((Number) delayedStartup.getValue()).intValue()*Simulation.MILLISECOND);

      dispose();
    }
  };

}
