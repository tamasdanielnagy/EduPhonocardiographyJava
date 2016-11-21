package gui.general;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import calculation.Phonocardiography;

/**
 * Visualizes the calculated cardiac function indicators.
 * 
 * @author Nagy Tamas
 *
 */
public class AnalysisPanel extends JPanel implements Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4924150135135560079L;

	private Phonocardiography plet;
	private boolean refreshing;

	private Locale loc = Locale.US;
	private int fontSize = 16;

	public AnalysisPanel(Phonocardiography plet) {
		this.plet = plet;
		setBackground(Color.WHITE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		refreshing = true;
		while (refreshing) {
			createAnalyisisPanel();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

			}
		}
	}

	/**
	 * Creatze the panel.
	 */
	public void createAnalyisisPanel() {
		removeAll();
		GridBagLayout agbl = new GridBagLayout();
		GridBagConstraints acons = new GridBagConstraints();
		acons.fill = GridBagConstraints.BOTH;
		acons.insets = new Insets(0, 0, 0, 5);
		acons.weightx = 1.0;
		acons.weighty = 1.0;
		acons.gridheight = 1;
		acons.gridy = GridBagConstraints.RELATIVE;
		setLayout(agbl);
		JLabel[] nonSpectralLabels = new JLabel[10];
		int i;
		JLabel lbNonSpectral = new JLabel("RR analysis");
		lbNonSpectral.setFont(new Font(lbNonSpectral.getFont().getName(),
				Font.BOLD, fontSize + 2));
		acons.gridwidth = 2;
		acons.gridx = 0;
		agbl.setConstraints(lbNonSpectral, acons);
		add(lbNonSpectral);

		nonSpectralLabels[0] = new JLabel("pulse: ");
		nonSpectralLabels[1] = new JLabel(String.format(Locale.ENGLISH, "%.1f",
				plet.getPulse()) + " BPM");
		nonSpectralLabels[2] = new JLabel("meanRR: ");
		nonSpectralLabels[3] = new JLabel(plet.getMeanRR() + " ms");
		nonSpectralLabels[4] = new JLabel("sdRR: ");
		nonSpectralLabels[5] = new JLabel(plet.getsdRR() + " ms");
		nonSpectralLabels[6] = new JLabel("rMSSD: ");
		nonSpectralLabels[7] = new JLabel(plet.getrMSSD() + " ms");
		nonSpectralLabels[8] = new JLabel("pNN50: ");
		nonSpectralLabels[9] = new JLabel(String.format(loc, "%.1f",
				plet.getpNN50())
				+ " %");
		acons.gridwidth = 1;

		for (i = 0; i < nonSpectralLabels.length; i++) {
			if (i % 2 == 0)
				acons.gridx = 0;
			else
				acons.gridx = 1;
			agbl.setConstraints(nonSpectralLabels[i], acons);
			nonSpectralLabels[i].setFont(new Font(nonSpectralLabels[i]
					.getFont().getName(), Font.BOLD, fontSize));
			add(nonSpectralLabels[i]);
		}
		SwingUtilities.updateComponentTreeUI(this);
		// revalidate();
	}

	/**
	 * @return is refreshing.
	 */
	public boolean isRefreshing() {
		return refreshing;
	}

	/**
	 * @param refreshing
	 *            refreshing.
	 */
	public void setRefreshing(boolean refreshing) {
		this.refreshing = refreshing;
	}


	/**
	 * @param plet
	 *            set the Plethysmography object, to get the indicators.
	 */
	public void setPlet(Phonocardiography plet) {
		this.plet = plet;
	}

	/**
	 * @return font size.
	 */
	public int getFontSize() {
		return fontSize;
	}

	/**
	 * @param fontSize
	 *            font size.
	 */
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

}
