package calculation;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import signal.HighPassFilter;
import signal.MovingAverage;
import signal.SignalD;
import signal.SignalGraphType;
import signal.SignalPoint;
import signal.SignalXY;
import exception.SignalIsEmptyException;
import exception.TooFewDataToCalculateException;

/**
 * Stores the measured and calculated curves and indicators. Does all calculations used for plethysmography.
 * 
 * @author Nagy Tamas
 *
 */
public class Phonocardiography implements Runnable {

	// Colors
	public static final Color DARK_GRAY = new Color(150, 150, 150);
	public static final Color LIGHT_GRAY = new Color(200, 200, 200);
	public static Color firstDerivativeColor = Color.BLUE;
	public static Color RRintervalsColor = Color.BLACK;
	public static Color sampledRRintervalsColor = Color.BLUE;
	public static Color normalRRintervalsColor = Color.RED;
	public static Color beatsPerMinuteColor = Color.BLACK;
	public static Color statisticsColor = Color.BLACK;
	public static Color originalDataColor = Color.BLUE;
	public static Color maternalHeartSoundColor = Color.BLUE;
	public static Color fetalBeatColor = Color.BLACK;
	public static Color rangeOfBeatsColor = Color.BLACK;
	public static Color maternalBeatColor = Color.RED;
	public static Color fetalHeartSoundColor = Color.MAGENTA;
	private static Color originalBeatColor = Color.RED;

	// Used in power spectra
	public static final double MAXIMUM_OF_FREQUENCY = 1.1;
	public static final int DEFAULT_NUMBER_OF_SLICES = 10;
	public static final int DEFAULT_NUMBER_OF_SAMPLES = 256;
	public static final double DEFAULT_SAMPLING_TIME = 0.469;

	public static final int DEFAULT_REFRESH_RATE = 60;

	// Time window duration in ms
	public static final double STARTING_TIME_WINDOW_DURATION = 10000;

	private boolean loaded;
	public static final double NO_MAX_LENGTH = -1.0;
	public static final double DEFAULT_MAX_LENGTH = 15.0;
	private double maxLength = NO_MAX_LENGTH;

	// The name of the input file
	private String fileName;
	
	private PhonocardType type = PhonocardType.ORIGINAL;

	/* The data of the curves */
	// The PCG measurement
	private SignalD heartSound;

	// The heart beats
	private SignalXY peaks;
	private SignalXY rangeOfBeats;
	// The time between hearth beats in ms
	private SignalXY RRintervals;
	private SignalXY normalRRintervals;
	private SignalD sampledRRintervals;
	private SignalXY statistics;
	private int numberOfSlices = DEFAULT_NUMBER_OF_SLICES;
	private int numberOfSamples = DEFAULT_NUMBER_OF_SAMPLES;
	private double samplingTime = DEFAULT_SAMPLING_TIME;
	// Beats/minute
	private SignalXY beatsPerMinute;

	// Used in heartBeatDetection()
	private double threshold = 0.0;
	private static final double MIN_OF_LENGTH_IN_PEAK_DETECTION = 3.0;
	

	// Non-spectral analysis, in ms
	private double pulse = 0.0;
	private int meanRR = 0;
	private int sdRR = 0;
	private double pNN50 = 0.0;
	private int rMSSD = 0;

	private boolean refreshing;
	private int refreshRate = DEFAULT_REFRESH_RATE;

	// Heart beat detection
	private double minOfRiseBeforeBeatMultiplier = 0.45;
	private double maxTimeToPeak = 0.1;
	private double jumpedTimeAdaptingPeakDetection = 0.3;

	//Filtering
	private double fetalFc = 60.0;
	
	/**
	 * Constructor.
	 */
	public Phonocardiography() {
		loaded = false;
		type = PhonocardType.ORIGINAL;
		constructCurves();
	}
	
	public Phonocardiography(PhonocardType type) {
		loaded = false;
		this.type = type;
		constructCurves();
	}
	
	public Phonocardiography(PhonocardType type, double minOfRiseBeforeBeatMultiplier, double maxTimeToPeak, double jumpedTimeAdaptingPeakDetection) {
		loaded = false;
		this.type = type;
		this.minOfRiseBeforeBeatMultiplier = minOfRiseBeforeBeatMultiplier;
		this.maxTimeToPeak = maxTimeToPeak;
		this.jumpedTimeAdaptingPeakDetection = jumpedTimeAdaptingPeakDetection;
		constructCurves();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		double i = 0.0;
		int sleepingTime = 1000 / refreshRate;
		refreshing = true;
		while (refreshing) {
			i += sleepingTime;
			runCalculations();
			if (i >= 1000.0) {
				nonSpectralAnalysis();
				i = 0.0;
			}
			try {
				Thread.sleep(sleepingTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * Creates the curves.
	 */
	public void constructCurves() {
		heartSound = new SignalD(originalDataColor);
		
		heartSound.setyAxisTitle("Amplitude [A. U.]");
		heartSound.setxAxisTitle("Time [s]");
		
		switch (type) {
		case ORIGINAL:
			heartSound.setTitle("Heart sound");
			heartSound.setColor(originalDataColor);
			peaks = new SignalXY(originalBeatColor);
			break;
		case MATERNAL:
			heartSound.setColor(maternalHeartSoundColor);
			heartSound.setTitle("Maternal heart sound");
			peaks = new SignalXY(maternalBeatColor);
			break;
		case FETAL:
			heartSound.setColor(fetalHeartSoundColor);
			heartSound.setTitle("Fetal heart sound");
			peaks = new SignalXY(fetalBeatColor);
			break;
		}

		
		peaks.setGraphType(SignalGraphType.SIGNS);
		rangeOfBeats = new SignalXY(rangeOfBeatsColor);
		rangeOfBeats.setTitle("Range of heart beats");
		rangeOfBeats.setxAxisTitle("Time [s]");
		rangeOfBeats.setyAxisTitle("Amplitude [A. U.]");
		RRintervals = new SignalXY(RRintervalsColor);
		RRintervals.setTitle("R-R intervals");
		RRintervals.setxAxisTitle("Heart beats");
		RRintervals.setyAxisTitle("Time [ms]");
		normalRRintervals = new SignalXY(normalRRintervalsColor);
		sampledRRintervals = new SignalD(sampledRRintervalsColor);
		statistics = new SignalXY(statisticsColor);
		statistics = new SignalXY(statisticsColor);
		statistics.setTitle("R-R intervals statistics - " + fileName);
		statistics.setxAxisTitle("R-R intervals [ms]");
		statistics.setyAxisTitle("Incidence");
		beatsPerMinute = new SignalXY(beatsPerMinuteColor);
		beatsPerMinute.setTitle("Beats/minute");
		beatsPerMinute.setxAxisTitle("Time [s])");
		beatsPerMinute.setyAxisTitle("Beats/minute");
	}

	/**
	 * Does the plethysmographyc calculations.
	 */
	public void runCalculations() {
		//manageLength();
		
		calculateHeartBeatDetectionParams();
		heartBeatDetection(heartSound, peaks, threshold, maxTimeToPeak, jumpedTimeAdaptingPeakDetection);
		calculateRRintervals();
		calculateNormalRRintervals();
		calculateSampledRRintervals();
		calculateStatistics();
		calculateBeatsPerMinute();
		if (loaded) {
			nonSpectralAnalysis();
			//System.out.println(type + " " + pulse);
		}
	}

	/**
	 * Doesn't let the original data to be longer than the maxLength.
	 */
	public synchronized void manageLength() {
		synchronized (heartSound) {
			while (!loaded && !heartSound.isEmpty() && (heartSound.getLength()) > maxLength)
				heartSound.remove(0);
		}
	}
	
	/**
	 * 
	 */
	public void calculateMaternalHeartSound(SignalD maternalHeartSound) {
		synchronized (heartSound) {
			synchronized (maternalHeartSound) {
				MovingAverage mavg = new MovingAverage(15);
				maternalHeartSound.clear();
				maternalHeartSound.setDt(heartSound.getDt());
				maternalHeartSound.setStartTime(heartSound.getStartTime());
				for (double d : heartSound) {
					//System.out.println(d);
					maternalHeartSound.add(mavg.makeNext(d));
				}
			}
		}
	}
	

	/**
	 * 
	 */
	public void calculateFetalHeartSound(SignalD fetalHeartSound) {
		synchronized (heartSound) {
			synchronized (fetalHeartSound) {
				HighPassFilter hpf = new HighPassFilter(
						1.0 / heartSound.getDt(), fetalFc);
				fetalHeartSound.clear();
				fetalHeartSound.setDt(heartSound.getDt());
				fetalHeartSound.setStartTime(heartSound.getStartTime());
				for (double d : heartSound)
					fetalHeartSound.add(hpf.filterNext(-d));
			}
		}
	}

	/**
	 * Params for adapting peak detection
	 */
	public void calculateHeartBeatDetectionParams() {
		synchronized (heartSound) {
			try {
				if (heartSound.isEmpty())
					throw (new SignalIsEmptyException());
				if (heartSound.getLength() >= MIN_OF_LENGTH_IN_PEAK_DETECTION) {
					double max = heartSound.max();
					threshold = minOfRiseBeforeBeatMultiplier * max;
				}
			} catch (SignalIsEmptyException e) {
				// do nothing
			}
		}
	}

	
	/**
	 * 
	 * Detects the heart beats in a phonocardiographic signal.
	 */
	public void heartBeatDetection(SignalD heartSound, SignalXY beats, double threshold, double maxTimeToPeak, double jumpedTime) {
		synchronized (heartSound) {
			synchronized (beats) {
				try {
					// if the curve is too short, do nothing
					if (!heartSound.isEmpty()
							&& ((heartSound.getLength()) <= MIN_OF_LENGTH_IN_PEAK_DETECTION))
						throw (new TooFewDataToCalculateException());

					// index calculation
					int startIndex;
					double peaksLastX;
					if (beats.isEmpty()) {
						startIndex = 0;
					} else {
						peaksLastX = beats.getLast().getX();
						startIndex = (int) Math.round((peaksLastX - heartSound
								.getStartTime()) / heartSound.getDt()) + 1;
					}
					int jumpInIndex = (int) Math
							.round(jumpedTime
									/ (heartSound.getDt())) - 1;
					int timeToPeakInIndex = (int) Math.round(maxTimeToPeak / (heartSound.getDt()));
					if (jumpInIndex < 0)
						jumpInIndex = 0;
					if (!beats.isEmpty()) {
						startIndex += jumpInIndex;
					}
					int maxIndex = 0;
					int tempIndex = 0;
					int index = startIndex;
					if (heartSound.size() - startIndex <= timeToPeakInIndex)
						throw (new TooFewDataToCalculateException());

					// this cycle is responsible for the peak detection
					while (heartSound.size() > index) {
						// level crossing
						if (heartSound.get(index) >= threshold) {
							maxIndex = index;

							if (timeToPeakInIndex >= (heartSound.size() - index))
								throw (new TooFewDataToCalculateException());

							// maximum searching for heart beat
							for (tempIndex = index; tempIndex - index <= timeToPeakInIndex; tempIndex++) {
								if (heartSound.get(maxIndex) < heartSound
										.get(tempIndex)) {
									maxIndex = tempIndex;
								}
							}

							// store found heart beat
							beats.add(heartSound.getX(maxIndex),
									heartSound.get(maxIndex));
							index += (maxIndex - index) + jumpInIndex;
						}
						index++;
					}
				} catch (TooFewDataToCalculateException e) {
					// do nothing
				}
			}
		}


		// manage signal length
		while (!peaks.isEmpty() && !heartSound.isEmpty()
				&& peaks.getFirst().getX() < heartSound.getX(0)) {
			peaks.removeFirst();
		}
	}

	/**
	 * RR intervals
	 */
	public void calculateRRintervals() {
		if (peaks == null) {
			heartBeatDetection(heartSound, peaks, threshold, maxTimeToPeak, jumpedTimeAdaptingPeakDetection);
		}
		synchronized (peaks) {
			synchronized (RRintervals) {
				try {
					if (peaks.isEmpty())
						throw (new SignalIsEmptyException());
					RRintervals.removeAll();
					Iterator<SignalPoint> hearthBeatsIterator = peaks.iterator();
					SignalPoint point1, point2 = new SignalPoint(0.0f, 0.0f);
					if (hearthBeatsIterator.hasNext())
						point2 = hearthBeatsIterator.next();
					while (hearthBeatsIterator.hasNext()) {
						point1 = point2;
						point2 = hearthBeatsIterator.next();
						RRintervals
								.add(new SignalPoint(point2.getX(), (float) 1000.0 * (point2.getX() - point1.getX())));
					}
				} catch (SignalIsEmptyException e) {
					// do nothing
				}
			}
		}
	}

	/**
	 * Remove ectopic beats from RR intervals.
	 */
	public void calculateNormalRRintervals() {
		if (RRintervals == null)
			calculateRRintervals();
		synchronized (RRintervals) {
			synchronized (normalRRintervals) {
				try {
					if (RRintervals.isEmpty())
						throw (new SignalIsEmptyException());
					SignalPoint point1, point2, point3;
					normalRRintervals.removeAll();
					Iterator<SignalPoint> it = RRintervals.iterator();
					if (it.hasNext())
						point2 = it.next();
					else
						throw (new TooFewDataToCalculateException());
					if (it.hasNext())
						point3 = it.next();
					else
						throw (new TooFewDataToCalculateException());
					//it = RRintervals.iterator();
					normalRRintervals.add(point2);
					// Could be problem if the first RRinterval is ectopic
					while (it.hasNext()) {
						point1 = point2;
						point2 = point3;
						point3 = it.next();
						if (point2.getY() > 1.5 * point1.getY() || point2.getY() < 0.5 * point1.getY()) {
							normalRRintervals.add(new SignalPoint(point2.getX(), SignalPoint.linearInterpolateInX(
									point2.getX(), point1, point3)));
						} else
							normalRRintervals.add(point2);
					}
					if (point3.getY() > 1.5 * point2.getY() || point3.getY() < 0.5 * point2.getY()) {
						normalRRintervals.add(new SignalPoint(point2.getX(), point2.getY()));
					} else
						normalRRintervals.add(point3);
				} catch (SignalIsEmptyException e) {
					// do nothing
				} catch (TooFewDataToCalculateException e) {
					// do nothing
				}
			}
		}
	}

	/**
	 * Re-sample the RR intervals.
	 */
	public void calculateSampledRRintervals() {
		synchronized (normalRRintervals) {
			synchronized (sampledRRintervals) {
				normalRRintervals.sample(sampledRRintervals, (float) samplingTime, numberOfSamples);
			}
		}
	}

	/**
	 * Statistics.
	 */
	public void calculateStatistics() {
		synchronized (RRintervals) {
			synchronized (statistics) {
				try {
					if (RRintervals == null)
						calculateRRintervals();
					if (RRintervals.isEmpty())
						throw (new SignalIsEmptyException());
					statistics.removeAll();
					int i;
					double y;
					double resolution;
					double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
					Iterator<SignalPoint> tbbIt = RRintervals.iterator();
					SignalPoint point;
					while (tbbIt.hasNext()) {
						point = tbbIt.next();
						if (point.getY() > max)
							max = point.getY();
						if (point.getY() < min)
							min = point.getY();
					}
					resolution = (max - min) / numberOfSlices;
					tbbIt = RRintervals.iterator();
					y = 0.0f;
					while (tbbIt.hasNext()) {
						point = tbbIt.next();
						if (min <= point.getY() && point.getY() <= min + resolution)
							y++;
					}
					statistics.add(new SignalPoint((float) (min + resolution / 2), (float) y));
					for (i = 1; i < numberOfSlices; i++) {
						tbbIt = RRintervals.iterator();
						y = 0.0f;
						while (tbbIt.hasNext()) {
							point = tbbIt.next();
							if (min + i * resolution < point.getY() && point.getY() <= min + (i + 1) * resolution)
								y++;
						}
						statistics.add(new SignalPoint((float) (min + i * resolution + resolution / 2), (float) y));
					}
				} catch (SignalIsEmptyException e) {
					// do nothing
				}
			}
		}
	}

	/**
	 * BPM.
	 */
	public void calculateBeatsPerMinute() {
		synchronized (RRintervals) {
			synchronized (beatsPerMinute) {
				try {
					if (RRintervals == null)
						calculateRRintervals();
					if (RRintervals.isEmpty())
						throw (new SignalIsEmptyException());
					beatsPerMinute.removeAll();
					Iterator<SignalPoint> timeBetweenBeatsIterator = RRintervals.iterator();
					SignalPoint point;
					while (timeBetweenBeatsIterator.hasNext()) {
						point = timeBetweenBeatsIterator.next();
						beatsPerMinute.add(new SignalPoint(point.getX(), (float) (6E4 / point.getY())));
					}
				} catch (SignalIsEmptyException e) {
					// do nothing
				}
			}
		}
	}


	/**
	 * Cardiac function indicators.
	 */
	public synchronized void nonSpectralAnalysis() {
		int n = 0;
		// meanRR, sdRR, pNN50, rMSSD
		double meanRRdouble = 0.0;
		double sdRRdouble = 0.0;
		double rMSSDdouble = 0.0;
		int noc = 0;
		double prevPointY = 0.0;
		double pointY;
		double delta;
		synchronized (normalRRintervals) {
			for (SignalPoint point : normalRRintervals) {
				pointY = point.getY();
				meanRRdouble += pointY;
				//System.out.println(type + " nonspectral  " + pointY);
				sdRRdouble += Math.pow(pointY, 2);
				if (n > 0) {
					delta = prevPointY - pointY;
					rMSSDdouble += Math.pow(delta, 2);
					if (Math.abs(delta) > 50.0)
						noc++;
				}
				prevPointY = pointY;
				n++;
			}
		}
		meanRRdouble /= n;
		sdRRdouble /= n;
		sdRRdouble = Math.sqrt(sdRRdouble - Math.pow(meanRRdouble, 2));
		rMSSDdouble /= n - 1;
		rMSSDdouble = Math.sqrt(rMSSDdouble);
		pulse = (meanRRdouble > 0.0) ? 60000.0 / meanRRdouble : 0.0;
		meanRR = (int) Math.round(meanRRdouble);
		sdRR = (int) Math.round(sdRRdouble);
		rMSSD = (int) Math.round(rMSSDdouble);
		pNN50 = 100.0 * ((double) noc / (n - 1));
	}

	/**
	 * Clears the curves.
	 */
	public void reset() {
		if (heartSound != null)
			heartSound.clear();
		if (peaks != null)
			peaks.removeAll();
		if (rangeOfBeats != null)
			rangeOfBeats.removeAll();
		if (RRintervals != null)
			RRintervals.removeAll();
		if (normalRRintervals != null)
			normalRRintervals.removeAll();
		if (sampledRRintervals != null)
			sampledRRintervals.removeAll();
		if (statistics != null)
			statistics.removeAll();
		numberOfSlices = DEFAULT_NUMBER_OF_SLICES;
		numberOfSamples = DEFAULT_NUMBER_OF_SAMPLES;
		samplingTime = DEFAULT_SAMPLING_TIME;
		if (beatsPerMinute != null)
			beatsPerMinute.removeAll();
		threshold = 0.0;
		meanRR = 0;
	}


	/**
	 * For Statistics.
	 * @param numberOfSlices
	 */
	public void setNumberOfSlices(int numberOfSlices) {
		this.numberOfSlices = numberOfSlices;
	}

	/**
	 * @return original plethysmographic curve.
	 */
	public SignalD getHeartSound() {
		return heartSound;
	}


	/**
	 * @return the heart beats.
	 */
	public SignalXY getBeats() {
		return peaks;
	}

	/**
	 * @return range of beats.
	 */
	public SignalXY getRangeOfBeats() {
		return rangeOfBeats;
	}

	/**
	 * @return time between beats.
	 */
	public SignalXY getTimeBetweenBeats() {
		return RRintervals;
	}

	/**
	 * @return the statistics.
	 */
	public SignalXY getStatistics() {
		return statistics;
	}

	/**
	 * @return BPM.
	 */
	public SignalXY getBeatsPerMinute() {
		return beatsPerMinute;
	}

	/**
	 * @return the file name.
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName the file name.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the RR intervals.
	 */
	public SignalXY getRRintervals() {
		return RRintervals;
	}

	/**
	 * @return the non-ectopic RR intervals.
	 */
	public SignalXY getNormalRRintervals() {
		return normalRRintervals;
	}

	/**
	 * @return sampled RR intervals
	 */
	public SignalD getSampledRRintervals() {
		return sampledRRintervals;
	}

	/**
	 * @return the mean of the RR intervals.
	 */
	public int getMeanRR() {
		return meanRR;
	}

	/**
	 * @return sdRR.
	 */
	public int getsdRR() {
		return sdRR;
	}

	/**
	 * @return pNN50.
	 */
	public double getpNN50() {
		return pNN50;
	}

	/**
	 * @return rMSSD.
	 */
	public int getrMSSD() {
		return rMSSD;
	}

	/**
	 * @return is refreshing.
	 */
	public boolean isRefreshing() {
		return refreshing;
	}

	/**
	 * @param refreshing refreshing.
	 */
	public void setRefreshing(boolean refreshing) {
		this.refreshing = refreshing;
	}

	/**
	 * @return the rate of refreshing.
	 */
	public int getRefreshRate() {
		return refreshRate;
	}

	/**
	 * @param refreshRate rate of refreshing.
	 */
	public void setRefreshRate(int refreshRate) {
		this.refreshRate = refreshRate;
	}

	/**
	 * @return is loaded from file.
	 */
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * @param loaded loaded from file.
	 */
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	/**
	 * @return the max length of the curves.
	 */
	public double getMaxLength() {
		return maxLength;
	}

	/**
	 * @param maxLength the max length of the curves.
	 */
	public void setMaxLength(double maxLength) {
		this.maxLength = maxLength;
	}

	/**
	 * @return the pulse.
	 */
	public double getPulse() {
		return pulse;
	}

	/**
	 * @param pulse the pulse.
	 */
	public void setPulse(double pulse) {
		this.pulse = pulse;
	}

}
