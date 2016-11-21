package gui.stethoscope;

import gui.general.OptionsEventGenerator;
import gui.general.OptionsListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.omg.CORBA.FREE_MEM;

import calculation.Phonocardiography;

/**
 * Options dialog for the sound card measurements.
 * 
 * @author Nagy Tamas
 *
 */
public class StethoscopeOptions extends JDialog implements OptionsEventGenerator, ActionListener, WindowListener {

	//
	private static final long serialVersionUID = -2606334582190458578L;

	public static final String SAVED_OPTIONS_FILENAME = "./etc/steth_saved_options.dat";
	private static final String DEFAULT_OPTIONS_FILENAME = "./etc/steth_default_options.dat";
	
	// Use this instead of default options file!
	public static final String DEFAULT_OPTIONS = "Default options, DO NOT MODIFY!" + "\n"
												+ "Sample rate:	11025" + "\n"
												+ "Moving avg N:	1" + "\n"
												+ "Display refresh rate:	60" + "\n"
												+ "Graph max length:	60.0";
	
	public static final int DEFAULT_REFRESH_RATE = 60;
	public static final int tfColumns = 6;
	// OptionListener
	private OptionsListener optionsListener;

	/* Graphics */
	private Container cp;
	private JPanel optPanel;

	// Meas
	private JPanel measPanel;
	private JPanel sampleRatePanel;
	private JTextField tfSampleRate;
	private JPanel movAvgPanel;
	private JTextField tfMovAvg;

	// Display
	private JPanel dispPanel;
	private JPanel maxLengthPanel;
	private JCheckBox cbLimitedLength;
	private JTextField tfMaxLength;
	private JPanel refreshRatePanel;
	private JTextField tfRefreshRate;

	// Buttons
	private JPanel buttonPanel;
	private JButton btOk = new JButton("OK");
	private JButton btCancel = new JButton("Cancel");
	private JButton btReset = new JButton("Reset");

	/* Options */
	// Mesurement
	private int sampleRate = 0;
	private int movAvg = 10;

	// Display
	private double maxLength = Phonocardiography.DEFAULT_MAX_LENGTH;
	private int dispRefreshRate = DEFAULT_REFRESH_RATE;

	/**
	 * Constructor.
	 */
	public StethoscopeOptions() {
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setTitle("Options");
		setModal(true);
		addWindowListener(this);
		// setSize(Graph.STARTING_PANEL_WIDTH, Graph.STARTING_PANEL_HEIGHT);
		cp = getContentPane();
		// Mesurement
		createOptPanel();
		cp.add(optPanel, BorderLayout.CENTER);

		// Buttons
		createButtonPanel();
		cp.add(buttonPanel, BorderLayout.SOUTH);

		//
		pack();
		loadOptions(SAVED_OPTIONS_FILENAME);
		setSize(new Dimension(445, 382));
		setLocationRelativeTo(null);
		// System.out.println(getSize().width+", "+ getSize().height);
	}

	/**
	 * Save options to a file.
	 * @param fileName
	 */
	public void saveOptions(String fileName) {
		File file = new File(fileName);
		BufferedWriter out;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write("Saved options\n");
			out.write("Sample rate:\t" + sampleRate + "\n");
			out.write("Moving avg N:\t" + movAvg + "\n");

			out.write("Display refresh rate:\t" + dispRefreshRate + "\n");
			out.write("Graph max length:\t" + maxLength + "\n");
			out.write(" ");
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,
					new JLabel("Failed to save options.\n" + e.getMessage(), JLabel.CENTER), "Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Load option from a file.
	 * @param fileName
	 * @throws IOException
	 */
	public void loadOptions(String fileName) {
		BufferedReader in;
		try {
			
			File file = new File(fileName);
			in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			loadOptions(in);
			in.close();

			storeNewOptions();

		} catch (FileNotFoundException e) {
				loadOptions();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, new JLabel("Failed to open saved options. Incorrect number format.\n"
					+ e.getMessage(), JLabel.CENTER), "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			loadOptions();
		}
	}
	
	public void loadOptions() {
		BufferedReader in;
		try {
			
			in = new BufferedReader(new StringReader(DEFAULT_OPTIONS));
			loadOptions(in);
			in.close();
			
			storeNewOptions();
			
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, new JLabel("Failed to open saved options. Incorrect number format.\n"
					+ e.getMessage(), JLabel.CENTER), "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadOptions(BufferedReader in) throws IOException {
		try {
			String line;
			String data;
			
			line = in.readLine();

			line = in.readLine();
			data = line.substring(line.indexOf('\t') + 1, line.length());
			tfSampleRate.setText(data);

			line = in.readLine();
			data = line.substring(line.indexOf('\t') + 1, line.length());
			tfMovAvg.setText(data);

			line = in.readLine();
			data = line.substring(line.indexOf('\t') + 1, line.length());
			tfRefreshRate.setText(data);

			line = in.readLine();
			data = line.substring(line.indexOf('\t') + 1, line.length());
			if (Double.parseDouble(data) == Phonocardiography.NO_MAX_LENGTH) {
				cbLimitedLength.setSelected(false);
				tfMaxLength.setText(Double.toString(Phonocardiography.DEFAULT_MAX_LENGTH));
				tfMaxLength.setEnabled(false);
			} else {
				cbLimitedLength.setSelected(true);
				tfMaxLength.setText(data);
				tfMaxLength.setEnabled(true);
			}

		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, new JLabel("Failed to open saved options. Incorrect number format.\n"
					+ e.getMessage(), JLabel.CENTER), "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btOk) {
			storeNewOptions();
			notifyListeners();
		}
		if (e.getSource() == btCancel)
			cancel();
		if (e.getSource() == btReset) {
				loadOptions(DEFAULT_OPTIONS_FILENAME);
			setVisible(true);
		}

		if (e.getSource() == cbLimitedLength) {
			if (!cbLimitedLength.isSelected()) {
				tfMaxLength.setEnabled(false);
			} else {
				tfMaxLength.setEnabled(true);
			}
		}
	}

	/**
	 * If OK is pressed.
	 */
	public void storeNewOptions() {
		sampleRate = Integer.parseInt(((String) tfSampleRate.getText()));
		movAvg = Integer.parseInt(((String) tfMovAvg.getText()));
		dispRefreshRate = Integer.parseInt(((String) tfRefreshRate.getText()));
		if (cbLimitedLength.isSelected())
			maxLength = Double.parseDouble(tfMaxLength.getText());
		else
			maxLength = Phonocardiography.NO_MAX_LENGTH;
		setVisible(false);
	}

	/**
	 * If Cancel is pressed.
	 */
	public void cancel() {
		setVisible(false);

		tfSampleRate.setText(Integer.toString(sampleRate));
		tfMovAvg.setText(Integer.toString(movAvg));
		tfRefreshRate.setText(Integer.toString(dispRefreshRate));
		if (maxLength == Phonocardiography.NO_MAX_LENGTH) {
			cbLimitedLength.setSelected(true);
			tfMaxLength.setEnabled(false);
		} else {
			cbLimitedLength.setSelected(true);
			tfMaxLength.setEnabled(true);
		}

	}

	/*
	 * Create the GUI.
	 */
	
	public void createOptPanel() {
		optPanel = new JPanel();
		optPanel.setLayout(new BoxLayout(optPanel, BoxLayout.X_AXIS));
		createMeasPanel();
		createDispPanel();

		Box A = Box.createVerticalBox();
		Box B = Box.createVerticalBox();

		A.add(Box.createRigidArea(new Dimension(5, 5)));
		A.add(measPanel);
		A.add(Box.createRigidArea(new Dimension(5, 20)));
		A.add(Box.createHorizontalStrut(5));

		B.add(Box.createRigidArea(new Dimension(5, 5)));
		B.add(dispPanel);
		B.add(Box.createRigidArea(new Dimension(5, 5)));
		B.add(Box.createHorizontalStrut(5));

		/*
		 * A.add(Box.createRigidArea(new Dimension(5,0))); A.add(tfScaleA);
		 * B.add(new JLabel("B")); B.add(Box.createRigidArea(new
		 * Dimension(5,0))); B.add(tfScaleB);
		 */

		optPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		optPanel.add(A);
		optPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		optPanel.add(B);
		optPanel.add(Box.createRigidArea(new Dimension(10, 0)));

	}

	/* Meas */
	public void createMeasPanel() {
		measPanel = new JPanel();
		// sensorPanel.setMinimumSize(new Dimension(150, 200));
		measPanel.setLayout(new BoxLayout(measPanel, BoxLayout.X_AXIS));
		measPanel
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Measurement"));
		createSampleRatePanel();
		createMovAvgPanel();

		Box A = Box.createVerticalBox();

		A.add(Box.createRigidArea(new Dimension(0, 5)));
		A.add(sampleRatePanel);
		A.add(Box.createRigidArea(new Dimension(0, 5)));
		A.add(movAvgPanel);
		A.add(Box.createRigidArea(new Dimension(0, 5)));

		/*
		 * A.add(Box.createRigidArea(new Dimension(5,0))); A.add(tfScaleA);
		 * B.add(new JLabel("B")); B.add(Box.createRigidArea(new
		 * Dimension(5,0))); B.add(tfScaleB);
		 */

		measPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		measPanel.add(A);
		measPanel.add(Box.createRigidArea(new Dimension(5, 5)));

	}

	

	public void createSampleRatePanel() {
		sampleRatePanel = new JPanel();
		tfSampleRate = new JTextField(Integer.toString(sampleRate), tfColumns);
		tfSampleRate.setFont(new Font(tfSampleRate.getFont().getName(), Font.BOLD, tfSampleRate.getFont().getSize()));
		tfSampleRate.setHorizontalAlignment(JTextField.RIGHT);
		sampleRatePanel.setLayout(new FlowLayout());
		sampleRatePanel.add(tfSampleRate);
		sampleRatePanel.add(new JLabel("Hz"));
		sampleRatePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
				"Sound card sample rate"));
	}

	public void createMovAvgPanel() {
		movAvgPanel = new JPanel();
		tfMovAvg = new JTextField(Integer.toString(movAvg), tfColumns);
		tfMovAvg.setFont(new Font(tfMovAvg.getFont().getName(), Font.BOLD, tfMovAvg.getFont().getSize()));
		tfMovAvg.setHorizontalAlignment(JTextField.RIGHT);
		movAvgPanel.setLayout(new FlowLayout());
		movAvgPanel.add(tfMovAvg);
		movAvgPanel.add(new JLabel("samples"));
		movAvgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
				"Moving average N"));
	}

	/* Display */
	public void createDispPanel() {
		dispPanel = new JPanel();
		// sensorPanel.setMinimumSize(new Dimension(150, 200));
		dispPanel.setLayout(new BoxLayout(dispPanel, BoxLayout.X_AXIS));
		dispPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Display"));
		createMaxLengthPanel();
		createRefreshRatePanel();

		Box A = Box.createVerticalBox();

		A.add(Box.createRigidArea(new Dimension(0, 5)));
		A.add(refreshRatePanel);
		A.add(Box.createRigidArea(new Dimension(0, 5)));
		A.add(maxLengthPanel);
		A.add(Box.createRigidArea(new Dimension(0, 5)));
		// A.add(Box.createRigidArea(new Dimension(200,135)));

		/*
		 * A.add(Box.createRigidArea(new Dimension(5,0))); A.add(tfScaleA);
		 * B.add(new JLabel("B")); B.add(Box.createRigidArea(new
		 * Dimension(5,0))); B.add(tfScaleB);
		 */

		dispPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		dispPanel.add(A);
		dispPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		cbLimitedLength.addActionListener(this);

	}

	public void createMaxLengthPanel() {
		maxLengthPanel = new JPanel();
		cbLimitedLength = new JCheckBox("Limited graph length");
		tfMaxLength = new JTextField(Double.toString(maxLength), tfColumns);
		if (maxLength == Phonocardiography.NO_MAX_LENGTH) {
			tfMaxLength.setText(Double.toString(Phonocardiography.DEFAULT_MAX_LENGTH));
			tfMaxLength.setEnabled(false);
			cbLimitedLength.setSelected(false);
		} else {
			tfMaxLength.setText(Double.toString(maxLength));
			tfMaxLength.setEnabled(true);
			cbLimitedLength.setSelected(true);
		}

		maxLengthPanel.setLayout(new BoxLayout(maxLengthPanel, BoxLayout.Y_AXIS));
		Box cb = Box.createHorizontalBox();
		cb.removeAll();
		Box tf = Box.createHorizontalBox();
		// maxLengthPanel.add(Box.createHorizontalGlue());
		tfMaxLength.setFont(new Font(tfMaxLength.getFont().getName(), Font.BOLD, tfMaxLength.getFont().getSize()));
		tfMaxLength.setMaximumSize(new Dimension(80, 30));
		tfMaxLength.setHorizontalAlignment(JTextField.RIGHT);
		cb.add(cbLimitedLength);
		tf.add(Box.createVerticalStrut(5));
		tf.add(Box.createRigidArea(new Dimension(10, 0)));
		tf.add(new JLabel("Max length"));
		tf.add(Box.createRigidArea(new Dimension(10, 0)));
		tf.add(tfMaxLength);
		tf.add(Box.createRigidArea(new Dimension(5, 0)));
		tf.add(new JLabel("s"));
		tf.add(Box.createRigidArea(new Dimension(10, 0)));
		tf.add(Box.createVerticalStrut(5));

		maxLengthPanel.add(cb);
		maxLengthPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		maxLengthPanel.add(tf);
		maxLengthPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		maxLengthPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
				"Graph length"));
	}

	public void createButtonPanel() {
		btOk.addActionListener(this);
		btCancel.addActionListener(this);
		btReset.addActionListener(this);
		btOk.setPreferredSize(new Dimension(100, 26));
		btCancel.setPreferredSize(new Dimension(100, 26));
		btReset.setPreferredSize(new Dimension(100, 26));
		buttonPanel = new JPanel();
		buttonPanel.setPreferredSize(new Dimension(400, 47));
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
		buttonPanel.add(btReset);
		buttonPanel.add(btCancel);
		buttonPanel.add(btOk);
	}

	public void createRefreshRatePanel() {
		refreshRatePanel = new JPanel();
		refreshRatePanel.setMaximumSize(new Dimension(700, 80));
		tfRefreshRate = new JTextField(Integer.toString(dispRefreshRate), tfColumns);
		tfRefreshRate
				.setFont(new Font(tfRefreshRate.getFont().getName(), Font.BOLD, tfRefreshRate.getFont().getSize()));
		tfRefreshRate.setHorizontalAlignment(JTextField.RIGHT);
		refreshRatePanel.setLayout(new FlowLayout());
		refreshRatePanel.add(tfRefreshRate);
		refreshRatePanel.add(new JLabel("Hz"));
		refreshRatePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
				"Refresh rate"));
	}

	/**
	 * @return the sample rate.
	 */
	public int getSampleRate() {
		return sampleRate;
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		cancel();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see plethysmography.gui.general.OptionsEventGenerator#addOptionsListener(plethysmography.gui.general.OptionsListener)
	 */
	public void addOptionsListener(OptionsListener optionsListener) {
		this.optionsListener = optionsListener;
	}

	/* (non-Javadoc)
	 * @see plethysmography.gui.general.OptionsEventGenerator#notifyListeners()
	 */
	public void notifyListeners() {
		optionsListener.optionsChanged();
	}

	/**
	 * @return the display refresh rate.
	 */
	public int getDispRefreshRate() {
		return dispRefreshRate;
	}

	/**
	 * @return max length of the signal.
	 */
	public double getMaxLength() {
		return maxLength;
	}

	/**
	 * @param maxLength max length of the signal.
	 */
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}


	/**
	 * @return number of elements of the moving average.
	 */
	public int getMovAvg() {
		return movAvg;
	}

	/**
	 * @param movAvg number of elements of the moving average.
	 */
	public void setMovAvg(int movAvg) {
		this.movAvg = movAvg;
	}

	/**
	 * @param sampleRate sample rate of the sound card.
	 */
	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	/**
	 * @param dispRefreshRate display refresh rate.
	 */
	public void setDispRefreshRate(int dispRefreshRate) {
		this.dispRefreshRate = dispRefreshRate;
	}

}
