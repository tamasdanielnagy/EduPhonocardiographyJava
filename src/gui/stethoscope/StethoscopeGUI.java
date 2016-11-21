package gui.stethoscope;

import gui.general.AnalysisPanel;
import gui.general.Graph;
import gui.general.OptionsListener;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import soundcard.StethCapture;
import calculation.Phonocardiography;

/**
 * GUI for the sound card measurements.
 * 
 * @author Nagy Tamas
 *
 */
public class StethoscopeGUI extends JFrame implements ActionListener,
		WindowListener, KeyListener, OptionsListener {

	private static final long serialVersionUID = -2191074802078732591L;

	private static final String ICON_PATH = "etc/steth_icon.png";

	// Menu bar
	private JMenuBar mb;

	// File menu
	private File f;
	JMenu mFile = new JMenu("File");
	private JMenuItem miOpen = new JMenuItem("Open");
	private JMenuItem miSave = new JMenuItem("Save as...");
	private JFileChooser fc;

	// Measurement menu
	private JMenu mMeasurement = new JMenu("Measurement");
	// Menu items in the Graph menu
	private JMenuItem miStart = new JMenuItem("Start");
	private JMenuItem miStop = new JMenuItem("Stop");
	private JMenuItem miClearData = new JMenuItem("Clear data");
	private JMenuItem miOptions = new JMenuItem("Options");
	private JPopupMenu popup = new JPopupMenu();
	private JMenuItem miStartPopup = new JMenuItem("Start");
	private JMenuItem miStopPopup = new JMenuItem("Stop");
	private JMenuItem miClearDataPopup = new JMenuItem("Clear data");
	private JMenuItem miOptionsPopup = new JMenuItem("Options");

	// Graph menu
	private JMenu mGraph = new JMenu("Graph");
	// Menu items in the Graph menu
	private JRadioButtonMenuItem rbHeartSound = new JRadioButtonMenuItem(
			"Heart sound", true);
	private JCheckBoxMenuItem cbOriginalPeaks = new JCheckBoxMenuItem(
			"Heart beats", false);
	private JRadioButtonMenuItem rbRRintervals = new JRadioButtonMenuItem(
			"R-R intervals", false);
	private JCheckBoxMenuItem cbOriginalRRintervals = new JCheckBoxMenuItem(
			"Original R-R intervals", false);
	private JCheckBoxMenuItem cbNormalRRintervals = new JCheckBoxMenuItem(
			"Normal R-R intervals", false);
	private JRadioButtonMenuItem rbBeatsPerMinute = new JRadioButtonMenuItem(
			"Beats/minute", false);
	private ButtonGroup rbGroup = new ButtonGroup();

	// Analysis menu
	private JMenu mAnalysis = new JMenu("Analysis");
	private JCheckBoxMenuItem cbNonSpectral = new JCheckBoxMenuItem(
			"RR analysis");

	// Content pane
	private Container cp;

	// Phonocardiography
	private Phonocardiography pcg;

	// Measurement
	private StethCapture capture;

	// Options
	private StethoscopeOptions opt;

	// Graph panel
	private Graph graph;
	private Graph fetalGraph;

	// Analysis panel
	private AnalysisPanel analysisPanel;

	private GridBagLayout gbl;

	// Threads
	private Thread captureThread;
	private Thread graphThread;
	private Thread pcgThread;
	private Thread analysisPanelThread;


	/**
	 * Constructor
	 */
	public StethoscopeGUI() {
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		setTitle("Phonocardiography");
		setSize(Graph.STARTING_PANEL_WIDTH, Graph.STARTING_PANEL_HEIGHT);
		setLocationRelativeTo(null);
//		BufferedImage icon = null;
//		try {
//			icon=ImageIO.read(new File(ICON_PATH));
//			setIconImage(icon);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		cp = getContentPane();
		opt = new StethoscopeOptions();
		cp.setBackground(Color.WHITE);
		// Menu
		createMenu();
		//
		fc = new JFileChooser("./data");

		// GridBagLayout
		gbl = new GridBagLayout();
		cp.setLayout(gbl);
		//

		opt.addOptionsListener(this);
		graph = new Graph();
		fetalGraph = new Graph();
		graph.setWindowSize(Graph.NO_WINDOW);
		pcg = new Phonocardiography();
		refreshOptions();

		analysisPanel = new AnalysisPanel(pcg);
		graph.setComponentPopupMenu(popup);
		addKeyListener(this);

		//
		handlePletismographyRadioButtons();
		paintGraph();
		fillContentPane();
		//
		setVisible(true);
	}

	/**
	 * Create the menu bar.
	 */
	public void createMenu() {
		mb = new JMenuBar();

		setJMenuBar(mb);
		mb.add(mFile);
		mb.add(mMeasurement);
		mb.add(mGraph);
		mb.add(mAnalysis);
		// File menu
		mFile.add(miOpen);
		mFile.add(miSave);
		miOpen.addActionListener(this);
		miSave.addActionListener(this);
		// Measurement menu
		miClearData.addActionListener(this);
		miClearData.setEnabled(true);
		miStart.addActionListener(this);
		miStart.setEnabled(true);
		miStop.addActionListener(this);
		miStop.setEnabled(false);
		miOptions.addActionListener(this);
		mMeasurement.add(miStart);
		mMeasurement.add(miStop);
		mMeasurement.addSeparator();
		mMeasurement.add(miClearData);
		mMeasurement.addSeparator();
		mMeasurement.add(miOptions);
		// Popup menu
		miClearDataPopup.addActionListener(this);
		miClearDataPopup.setEnabled(true);
		miStartPopup.addActionListener(this);
		miStartPopup.setEnabled(true);
		miStopPopup.addActionListener(this);
		miStopPopup.setEnabled(false);
		miOptionsPopup.addActionListener(this);
		popup.add(miStartPopup);
		popup.add(miStopPopup);
		popup.addSeparator();
		popup.add(miClearDataPopup);
		popup.addSeparator();
		popup.add(miOptionsPopup);

		// Graph menu
		rbGroup.add(rbHeartSound);
		rbGroup.add(rbRRintervals);
		rbGroup.add(rbBeatsPerMinute);
		cbOriginalPeaks.addActionListener(this);
		// cbCurveOfPrimaryPeaks.addActionListener(this);
		// cbCurveOfNegativePeaks.addActionListener(this);
		// cbMaternalHeartSound.addActionListener(this);
		rbHeartSound.addActionListener(this);
		rbRRintervals.addActionListener(this);
		cbOriginalRRintervals.addActionListener(this);
		cbNormalRRintervals.addActionListener(this);
		rbBeatsPerMinute.addActionListener(this);

		createGraphMenu();

		// Analysis menu
		mAnalysis.add(cbNonSpectral);
		cbNonSpectral.addActionListener(this);
	}

	public void createGraphMenu() {
		mGraph.removeAll();
		mGraph.add(rbHeartSound);
		mGraph.add(cbOriginalPeaks);
		
		// mGraph.add(cbMaternalHeartSound);

		// mGraph.add(cbCurveOfPrimaryPeaks);
		// mGraph.add(cbCurveOfNegativePeaks);
		mGraph.addSeparator();
		mGraph.add(rbRRintervals);
		mGraph.add(cbOriginalRRintervals);
		mGraph.add(cbNormalRRintervals);
		mGraph.addSeparator();
		mGraph.add(rbBeatsPerMinute);
	}

	public void handlePletismographyRadioButtons() {
		if (!rbHeartSound.isSelected()) {
			cbOriginalPeaks.setSelected(false);
			// cbMaternalHeartSound.setSelected(false);

			// cbCurveOfPrimaryPeaks.setSelected(false);
			// cbCurveOfNegativePeaks.setSelected(false);
			cbOriginalPeaks.setEnabled(false);
			// cbMaternalHeartSound.setEnabled(false);

			// cbCurveOfPrimaryPeaks.setEnabled(false);
			// cbCurveOfNegativePeaks.setEnabled(false);
		} else {
			cbOriginalPeaks.setEnabled(true);
			cbOriginalPeaks.setSelected(false);
			// cbMaternalHeartSound.setEnabled(true);

			// cbCurveOfPrimaryPeaks.setEnabled(true);
			// cbCurveOfNegativePeaks.setEnabled(true);
			// cbMaternalHeartSound.setSelected(true);

			// cbCurveOfPrimaryPeaks.setSelected(false);
			// cbCurveOfNegativePeaks.setSelected(false);
		}

		if (!rbRRintervals.isSelected()) {
			cbOriginalRRintervals.setSelected(false);
			cbNormalRRintervals.setSelected(false);
			cbOriginalRRintervals.setEnabled(false);
			cbNormalRRintervals.setEnabled(false);
		} else {
			cbOriginalRRintervals.setEnabled(true);
			cbNormalRRintervals.setEnabled(true);
			cbOriginalRRintervals.setSelected(true);
			cbNormalRRintervals.setSelected(false);
		}
	}

	/*
	 * Create threads
	 */

	public void createCaptureThread() {
		captureThread = new Thread(capture);
		captureThread.setPriority(6);
	}

	public void createGraphThread() {
		graphThread = new Thread(graph);
		graphThread.setPriority(5);
	}

	public void createPcgThread() {
		pcgThread = new Thread(pcg);
		pcgThread.setPriority(4);
	}

	public void createAnalysisPanelThreads() {
		analysisPanelThread = new Thread(analysisPanel);
		analysisPanelThread.setPriority(3);
	}

	/**
	 * Set the curves to draw.
	 */
	public void paintGraph() {
		graph.reset();
		fetalGraph.reset();
		// Draw grid
			if (rbHeartSound.isSelected()) {
				graph.setParamCurve(pcg.getHeartSound());
			}
			if (rbBeatsPerMinute.isSelected()) {
				graph.setParamCurve(pcg.getBeatsPerMinute());
			}
			if (rbRRintervals.isSelected()) {
				graph.setParamCurve(pcg.getTimeBetweenBeats());
			}

		// Draw curves
			if (rbHeartSound.isSelected()) {
				// System.out.println("cboriginal");
				graph.addSignal(pcg.getHeartSound());
			}
			if (cbOriginalPeaks.isSelected()) {
				graph.addSignal(pcg.getBeats());
			}
			if (rbBeatsPerMinute.isSelected()) {
				graph.addSignal(pcg.getBeatsPerMinute());
			}
			if (cbOriginalRRintervals.isSelected())
				graph.addSignal(pcg.getRRintervals());
			if (cbNormalRRintervals.isSelected())
				graph.addSignal(pcg.getNormalRRintervals());

		// if(cbCurveOfPrimaryPeaks.isSelected())
		// graph.addSignal(plet.getPrimaryPeaks());
		// if(cbCurveOfNegativePeaks.isSelected())
		// graph.addSignal(plet.getNegativePeaks());
		graph.repaint();
		fetalGraph.repaint();
	}

	/*
	 * Creates a new Plethysmography, puts it into the frame and fills the Graph
	 * menu with the new items
	 */
	public void openPhonocardiography() {
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			stopMeasurement();
			f = fc.getSelectedFile();
			pcg = new Phonocardiography();
			capture.setNegateSignal(true);
			capture.setSignal(pcg.getHeartSound());
			capture.readWavIntoSignal(f);
			pcg.setLoaded(true);
			pcg.runCalculations();
				analysisPanel.setPlet(pcg);
				analysisPanel.createAnalyisisPanel();
				
			// handlePletismographyRadioButtons();
			fillContentPane();
			graph.setComponentPopupMenu(null);
			paintGraph();
			//System.out.println(pcg.getFetalppg().getPulse());
		}
	}

	/**
	 * Saves the plethysmography to a file.
	 */
	public void savePhonocardiography() {
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			stopMeasurement();
			f = fc.getSelectedFile();
			// pcg.savePlethysmography(f);
			capture.writeTempToWavFile(f);
		}
	}

	public void fillContentPane() {
		cp.removeAll();
		GridBagConstraints cons = new GridBagConstraints();
		// add graph
		cons.fill = GridBagConstraints.BOTH;
		cons.gridx = 0;
		cons.gridy = 0;
		cons.weightx = 10.0;
		cons.weighty = 1.0;
		cons.gridheight = 1;
		cons.gridwidth = 1;
		gbl.setConstraints(graph, cons);
		cp.add(graph);

		// add analysis
		cons.fill = GridBagConstraints.HORIZONTAL;
		if (cbNonSpectral.isSelected()) {
			if (!(analysisPanel.getParent() == cp)) {
				cons.gridx = 1;
				cons.gridy = 0;
				cons.weightx = 1.0;
				cons.weighty = 1.0;
				cons.gridheight = 1;
				cons.gridwidth = 1;
				gbl.setConstraints(analysisPanel, cons);
				cp.add(analysisPanel);
			}
		}
		

		// pack();
		setVisible(true);
	}

	/**
	 * Creates the type of capture is set in the mod.
	 */
	public void createCapture() {
		capture = new StethCapture(pcg.getHeartSound());
		capture.setNegateSignal(true);
	}

	/**
	 * Creates the type of capture is set in the mod.
	 * 
	 * @param sampleRate
	 *            the sound card sample rate.
	 */
	public void createCapture(int sampleRate) {
		capture = new StethCapture(pcg.getHeartSound(),
				sampleRate);
		capture.setNegateSignal(true);
	}

	/**
	 * Start the measurement.
	 */
	public void startMeasurement() {
		pcg.reset();
		refreshOptions();
		if (pcg.isLoaded()) {
			pcg.setLoaded(false);
			capture.setSignal(pcg.getHeartSound());
			capture.setNegateSignal(false);
			paintGraph();
		}
		createCaptureThread();
		createAnalysisPanelThreads();
			analysisPanel.setPlet(pcg);
		// paintGraph();
		captureThread.start();
		createGraphThread();
		createPcgThread();
		// handlePletismographyButtons();

		graphThread.start();
		pcgThread.start();
		analysisPanelThread.start();
		miStart.setEnabled(false);
		miStop.setEnabled(true);
		miStartPopup.setEnabled(false);
		miStopPopup.setEnabled(true);
	}

	/**
	 * Stop the measurement
	 */
	public void stopMeasurement() {
		capture.setCapture(false);
		graph.setRefreshing(false);
		fetalGraph.setRefreshing(false);
		pcg.setRefreshing(false);
		analysisPanel.setRefreshing(false);
		miStart.setEnabled(true);
		miStop.setEnabled(false);
		miStartPopup.setEnabled(true);
		miStopPopup.setEnabled(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plethysmography.gui.general.OptionsListener#optionsChanged()
	 */
	@Override
	public void optionsChanged() {
		refreshOptions();
	}

	/**
	 * Refresh options, when options changed.
	 */
	public void refreshOptions() {
		createCapture(opt.getSampleRate());

		graph.setRefreshRate(opt.getDispRefreshRate());
		pcg.setRefreshRate(opt.getDispRefreshRate());
		pcg.setMaxLength(opt.getMaxLength());
	}

	/*
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == miOpen)
			openPhonocardiography();

		if (e.getSource() == miSave)
			savePhonocardiography();
		if (e.getSource() == miClearData || e.getSource() == miClearDataPopup) {
			pcg.reset();
		}
		if (e.getSource() == miStart || e.getSource() == miStartPopup) {
			startMeasurement();
		}
		if (e.getSource() == miStop || e.getSource() == miStopPopup) {
			stopMeasurement();
		}

		if (e.getSource() == miOptions || e.getSource() == miOptionsPopup) {
			if (capture.isCapture())
				stopMeasurement();
			pcg.reset();
			opt.setVisible(true);
		}

		if (e.getSource() == miOpen || e.getSource() == rbHeartSound
				|| e.getSource() == rbRRintervals
				|| e.getSource() == rbBeatsPerMinute) {
			handlePletismographyRadioButtons();
		}

		if (e.getSource() == cbNonSpectral) {
			analysisPanel.createAnalyisisPanel();
			fillContentPane();
		}
		fillContentPane();
		paintGraph();
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * To start, stop, clear measurement from the keyboard.
	 */
	@Override
	public void keyTyped(KeyEvent arg0) {
		char c = arg0.getKeyChar();
		switch (c) {
		case 'c':
			pcg.reset();
			break;
		case ' ':
			if (miStart.isEnabled())
				startMeasurement();
			else
				stopMeasurement();
			break;
		}
		paintGraph();
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	/**
	 * Save the options before closes the window.
	 */
	@Override
	public void windowClosing(WindowEvent e) {
		opt.saveOptions(StethoscopeOptions.SAVED_OPTIONS_FILENAME);
		capture.deleteTempFile();
		dispose();
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	/**
	 * Main function for the sound card measurements.
	 */
	public static void main(String Args[]) {
		new StethoscopeGUI();
	}

}
