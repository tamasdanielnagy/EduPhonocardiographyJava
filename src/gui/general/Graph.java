package gui.general;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.JPanel;

import signal.Signal;
import signal.SignalPoint;
import signal.SignalXY;
import signal.YSignal;

/**
 * Visualizes all inherited object of Signal.
 * 
 * @author Nagy Tamas
 * 
 */
public class Graph extends JPanel implements Runnable {

	private static final long serialVersionUID = -3126925040151712739L;

	private int fontSize = 14;
	/* Constants */
	// Colors
	public static final Color DARK_GRAY = new Color(80, 80, 80);
	public static final Color LIGHT_GRAY = new Color(200, 200, 200);
	// Parameters of the graph in pixels
	public static final int GRAPH_BORDER = 70;
	public static final int MARK_LENGTH = 8;
	public static final int STARTING_PANEL_WIDTH = 1200;
	public static final int STARTING_PANEL_HEIGHT = 600;
	public static final int FONT_WIDTH = 9;

	public static final int POINT_SIZE = 4;

	public static final double NO_WINDOW = -1.0;

	/* Default graph parameters */
	// x
	public static final double DEFAULT_X_MIN = 0.0;
	public static final double DEFAULT_X_MAX = 10.0;
	public static final double DEFAULT_X_AXIS_SCALE = 1.0;
	// y
	public static final double DEFAULT_Y_MIN = 0.0;
	public static final double DEFAULT_Y_MAX = 10.0;
	public static final double DEFAULT_Y_AXIS_SCALE = 1.0;
	// In Hz
	public static final int DEFAULT_REFRESH_RATE = 60;

	/* Graph parameters */
	// x
	private double xMin;
	private double xMax;
	private int scaledXMax;
	private final int scaledXMin = 0;
	private double xAxisScale;
	private double windowSize = NO_WINDOW;
	private boolean windowed;
	// x=A*x + B
	private double xScaleA;
	private double xScaleB;
	// y
	private double yMax;
	private double yMin;
	private int scaledYMax;
	private final int scaledYMin = 0;
	private double yAxisScale;
	// y=A*y + B
	private double yScaleA;
	private double yScaleB;

	private Signal<?> paramCurve = null;
	private ArrayList<Signal<?>> signals;

	private int refreshRate;
	private boolean refreshing;

	private Locale loc = Locale.US;

	private boolean isDefault = false;

	/**
	 * Default constructor.
	 */
	public Graph() {
		paramCurve = null;
		signals = new ArrayList<>();
		refreshRate = DEFAULT_REFRESH_RATE;
		refreshing = false;
		setPreferredSize(new Dimension(STARTING_PANEL_WIDTH, STARTING_PANEL_HEIGHT));
		setBackground(Color.WHITE);
		repaint();
	}

	/**
	 * Constructor with window size.
	 * @param windowSize
	 */
	public Graph(double windowSize) {
		paramCurve = null;
		signals = new ArrayList<>();
		refreshRate = DEFAULT_REFRESH_RATE;
		refreshing = false;
		setPreferredSize(new Dimension(STARTING_PANEL_WIDTH, STARTING_PANEL_HEIGHT));
		setBackground(Color.WHITE);
		this.windowSize = windowSize;
		repaint();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		int sleepingTime = 1000 / refreshRate;
		refreshing = true;
		while (refreshing) {
			repaint();
			try {
				Thread.sleep(sleepingTime);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Does the painting.
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics gr) {

		super.paintComponent(gr);
		Graphics2D g2d = (Graphics2D) gr;
		// setPreferredSize(new Dimension(STARTING_PANEL_WIDTH,
		// STARTING_PANEL_HEIGHT));
		g2d.translate(GRAPH_BORDER, getSize().height - GRAPH_BORDER);
		calculateGraphParameters(paramCurve);
		// Draw grid
		drawGrid(g2d);
		// Draw curves
		synchronized (signals) {

			if (!signals.isEmpty())
				for (Signal<?> signal : signals)
					if (!signal.isEmpty())
						drawSignal(g2d, signal);
		}
		// Draw axis
		drawAxis(g2d);
	}


	/**
	 * Calculates mins, maxes and scales.
	 * @param curve
	 */
	public void calculateGraphParameters(Signal<?> curve) {
		synchronized (curve) {
			if (curve == null)
				setGraphParametersToDefault();
			else if (curve.isEmpty())
				setGraphParametersToDefault();
			else {
				isDefault = false;
				yMax = curve.max().doubleValue();
				yMin = curve.min().doubleValue();

				yAxisScale = 0.0001f;
				while ((yMax - yMin) / yAxisScale > 20)
					yAxisScale *= 10.0f;

				yMax = yAxisScale * (Math.ceil(yMax / yAxisScale));
				yMin = yAxisScale * (Math.floor(yMin / yAxisScale));
				yScaleA = (getSize().height - 2.0f * GRAPH_BORDER) / (yMin - yMax);
				yScaleB = -1.0f * yScaleA * yMin;
				scaledYMax = scaleAndRoundY(yMax);

				if (curve instanceof SignalXY) {
					xMin = ((SignalXY) curve).getFirst().getX();
					xMax = ((SignalXY) curve).getLast().getX();
				} else {
					xMin = ((YSignal<?>) curve).getStartTime();
					xMax = xMin + ((YSignal<?>) curve).getLength();
				}
				if (windowSize != NO_WINDOW && xMax - xMin > windowSize) {
					xMin = xMax - windowSize;
					windowed = true;
				} else {
					windowed = false;
				}

				xAxisScale = 0.0001f;
				while ((xMax - xMin) / xAxisScale > 30)
					xAxisScale *= 10.0f;
				if (!refreshing) {
					xMax = xAxisScale * (Math.ceil(xMax / xAxisScale));
					xMin = xAxisScale * (Math.floor(xMin / xAxisScale));
				}
				xScaleA = (getSize().width - 2.0f * GRAPH_BORDER) / (xMax - xMin);
				xScaleB = -1.0f * xScaleA * xMin;
				scaledXMax = scaleAndRoundX(xMax);
			}
		}
	}


	/**
	 * Default params.
	 */
	public void setGraphParametersToDefault() {
		if (isDefault == false) {
			xMin = DEFAULT_X_MIN;
			xMax = DEFAULT_X_MAX;
			xAxisScale = DEFAULT_X_AXIS_SCALE;
			yMin = DEFAULT_Y_MIN;
			yMax = DEFAULT_Y_MAX;
			yAxisScale = DEFAULT_Y_AXIS_SCALE;
			isDefault = true;
		}
		yScaleA = (getSize().height - 2.0f * GRAPH_BORDER) / (yMin - yMax);
		yScaleB = -1.0f * yScaleA * yMin;
		xScaleA = (getSize().width - 2.0f * GRAPH_BORDER) / (xMax - xMin);
		xScaleB = -1.0f * xScaleA * xMin;
		scaledYMax = scaleAndRoundY(yMax);
		scaledXMax = scaleAndRoundX(xMax);
		windowed = false;
	}

	/**
	 * Draws the axes.
	 * @param g2d
	 */
	public void drawAxis(Graphics2D g2d) {
		synchronized (paramCurve) {
			g2d.setColor(Color.BLACK);
			g2d.setFont(new Font(g2d.getFont().getName(), Font.BOLD, fontSize));
			double xCount1, xCount2;
			double yCount;
			int xDigitsBeforeDecPoint;
			int yDigitsBeforeDecPoint = 0;

			int xDigitsAfterDecPoint = (int) Math.round(Math.log10(Math.abs(xAxisScale)));
			if (xDigitsAfterDecPoint >= 0)
				xDigitsAfterDecPoint = 0;
			else
				xDigitsAfterDecPoint *= -1;

			int yDigitsAfterDecPoint = (int) Math.round(Math.log10(Math.abs(yAxisScale)));
			if (yDigitsAfterDecPoint >= 0)
				yDigitsAfterDecPoint = 0;
			else
				yDigitsAfterDecPoint *= -1;
			int x;
			int y;
			String number;
			// Draw the horizontal lines + numbers
			g2d.setColor(DARK_GRAY);
			// the * 1.01 is needed because of the rounding errors
			for (yCount = yMin; yCount <= yMax; yCount += yAxisScale) {
				y = scaleAndRoundY(yCount);
				g2d.drawLine(scaledXMin - MARK_LENGTH / 2, y, scaledXMin + MARK_LENGTH / 2, y);
				switch (yDigitsAfterDecPoint) {
				case 4:
					number = String.format(loc, "%.4f", yCount);
					break;
				case 3:
					number = String.format(loc, "%.3f", yCount);
					break;
				case 2:
					number = String.format(loc, "%.2f", yCount);
					break;
				case 1:
					number = String.format(loc, "%.1f", yCount);
					break;
				case 0:
					number = String.format(loc, "%.0f", yCount);
					break;
				default:
					number = Double.toString(yCount);
					break;
				}
				yDigitsBeforeDecPoint = (int) Math.floor(Math.log10(Math.abs(Math.rint(yCount))));
				if (yDigitsBeforeDecPoint < 1)
					yDigitsBeforeDecPoint = 1;
				if (yCount >= 0)
					g2d.drawString(number, -4 - FONT_WIDTH * (yDigitsBeforeDecPoint + yDigitsAfterDecPoint)
							- MARK_LENGTH, y + 3);
				else
					g2d.drawString(number, -6 - FONT_WIDTH * (yDigitsBeforeDecPoint + yDigitsAfterDecPoint)
							- MARK_LENGTH, y + 3);
			}
			g2d.drawLine(scaledXMin - MARK_LENGTH / 2, scaledYMax, scaledXMax + MARK_LENGTH / 2, scaledYMax);
			g2d.drawLine(scaledXMin - MARK_LENGTH / 2, scaledYMin, scaledXMax + MARK_LENGTH / 2, scaledYMin);
			g2d.drawLine(scaledXMin, scaledYMin + MARK_LENGTH / 2, scaledXMin, scaledYMax - MARK_LENGTH / 2);
			// Draw the vertical line + numbers
			g2d.setColor(Color.BLACK);
			boolean writeNumber;
			for (xCount1 = Math.rint(xMin / xAxisScale) * xAxisScale; xCount1 <= xMax; xCount1 += xAxisScale) {
				if (!windowed) {
					if (xCount1 > xMin + xAxisScale * 0.1) {
						xCount2 = xCount1;
						x = scaleAndRoundX(xCount2);
						writeNumber = true;
					} else if (xCount1 < xMin - xAxisScale * 0.1) {
						xCount2 = xMin;
						x = scaledXMin;
						writeNumber = false;
					} else {
						xCount2 = xCount1;
						x = scaledXMin;
						writeNumber = true;
					}
				} else {
					if (xCount1 > xMin + xAxisScale * 0.001) {
						xCount2 = xCount1;
						x = scaleAndRoundX(xCount2);
						writeNumber = true;
					} else if (xCount1 < xMin - xAxisScale * 0.001) {
						xCount2 = xMin;
						x = scaledXMin;
						writeNumber = false;
					} else {
						xCount2 = xCount1;
						x = scaledXMin;
						writeNumber = true;
					}
				}
				g2d.drawLine(x, scaledYMin + MARK_LENGTH / 2, x, scaledYMin - MARK_LENGTH / 2);
				switch (xDigitsAfterDecPoint) {
				case 4:
					number = String.format(loc, "%.4f", xCount2);
					break;
				case 3:
					number = String.format(loc, "%.3f", xCount2);
					break;
				case 2:
					number = String.format(loc, "%.2f", xCount2);
					break;
				case 1:
					number = String.format(loc, "%.1f", xCount2);
					break;
				case 0:
					number = String.format(loc, "%.0f", xCount2);
					break;
				default:
					number = String.format(loc, "%.4f", xCount2);
					break;
				}
				xDigitsBeforeDecPoint = (int) Math.floor(Math.log10(Math.abs(Math.rint(xCount2))));
				if (xDigitsBeforeDecPoint < 1)
					xDigitsBeforeDecPoint = 1;
				if (writeNumber)
					if (xCount2 >= 0)
						g2d.drawString(number, x - (FONT_WIDTH / 2) * (xDigitsBeforeDecPoint + xDigitsAfterDecPoint),
								20);
					else
						g2d.drawString(number, x - 2 - (FONT_WIDTH / 2)
								* (xDigitsBeforeDecPoint + xDigitsAfterDecPoint), 20);
			}
			g2d.drawLine(scaledXMax, scaledYMin + MARK_LENGTH / 2, scaledXMax, scaledYMax - MARK_LENGTH / 2);
			// Draw titles
			if (paramCurve != null) {
				g2d.setColor(Color.BLACK);
				g2d.setFont(new Font(g2d.getFont().getName(), Font.BOLD, fontSize + 2));
				if (paramCurve.getTitle() != null) {
					g2d.drawString(paramCurve.getTitle(),
							(scaledXMax - FONT_WIDTH * paramCurve.getTitle().length()) / 2, scaledYMax - GRAPH_BORDER
									/ 2);
				}
				if (paramCurve.getxAxisTitle() != null) {
					g2d.drawString(paramCurve.getxAxisTitle(), (scaledXMax - FONT_WIDTH
							* paramCurve.getxAxisTitle().length()) / 2, GRAPH_BORDER / 2 + FONT_WIDTH);
				}
				if (paramCurve.getyAxisTitle() != null) {
					AffineTransform Tx = g2d.getTransform();
					g2d.rotate(-Math.PI / 2);
					g2d.drawString(paramCurve.getyAxisTitle(), (-scaledYMax - FONT_WIDTH
							* paramCurve.getyAxisTitle().length()) / 2, -GRAPH_BORDER / 2
							- (FONT_WIDTH + yDigitsBeforeDecPoint * 2));
					g2d.setTransform(Tx);
				}
			}
		}
	}

	
	/**
	 * Draws the grid.
	 * @param g2d
	 */
	public void drawGrid(Graphics2D g2d) {
		g2d.setColor(LIGHT_GRAY);
		double yCount;
		int y;
		// Draw the horizontal lines
		for (yCount = yMin; yCount < yMax; yCount += yAxisScale) {
			y = scaleAndRoundY(yCount);
			g2d.drawLine(scaledXMin - MARK_LENGTH / 2, y, scaledXMax + MARK_LENGTH / 2, y);
		}
	}

	/**
	 * Draws a signal. Uses the private functions to draw different kinds of signals.
	 * @param g2d
	 * @param signal
	 */
	public void drawSignal(Graphics2D g2d, Signal<?> signal) {
		switch (signal.getGraphType()) {
		case LINE:
			if (signal instanceof SignalXY)
				drawCurve(g2d, (SignalXY) signal);
			else if (signal instanceof YSignal<?>)
				drawCurve(g2d, (YSignal<?>) signal);
			break;
		case POINTS:
			if (signal instanceof SignalXY)
				drawPoints(g2d, (SignalXY) signal);
			else if (signal instanceof YSignal<?>)
				drawPoints(g2d, (YSignal<?>) signal);
			break;
		case LINE_AND_POINTS:
			if (signal instanceof SignalXY) {
				drawCurve(g2d, (SignalXY) signal);
				drawPoints(g2d, (SignalXY) signal);
			} else if (signal instanceof YSignal<?>) {
				drawCurve(g2d, (YSignal<?>) signal);
				drawPoints(g2d, (YSignal<?>) signal);
			}
			break;
		case SIGNS:
			if (signal instanceof SignalXY)
				drawSigns(g2d, (SignalXY) signal);
			else if (signal instanceof YSignal<?>)
				drawSigns(g2d, (YSignal<?>) signal);
			break;
		}

	}
	
	
	/*
	 * Private functions to draw signals.
	 */

	private void drawCurve(Graphics2D g2d, SignalXY curve) {
		synchronized (curve) {
			g2d.setColor(curve.getColor());
			Iterator<SignalPoint> curveIterator = curve.iterator();
			SignalPoint point = curveIterator.next();
			int x1, y1, x2, y2;
			double x = point.getX();
			while (curveIterator.hasNext() && x < xMin) {
				point = curveIterator.next();
				x = point.getX();
			}
			
			x2 = scaleAndRoundX(point.getX());
			y2 = scaleAndRoundY(point.getY());
			while (curveIterator.hasNext()) {
				x1 = x2;
				y1 = y2;
				point = curveIterator.next();
				x2 = scaleAndRoundX(point.getX());
				y2 = scaleAndRoundY(point.getY());
				if (scaledPointIsOnGraph(x1, y1) && scaledPointIsOnGraph(x2, y2)) {
						g2d.drawLine(x1, y1, x2, y2);
				}
			}
		}
	}

	private void drawCurve(Graphics2D g2d, YSignal<?> curve) {
		synchronized (curve) {
			g2d.setColor(curve.getColor());
			int x1, y1, x2, y2;
			int startIndex = (int) Math.floor((xMin - curve.getStartTime()) / curve.getDt());
			if (startIndex < 0)
				startIndex = 0;
			x2 = scaleAndRoundX(curve.getX(startIndex));
			y2 = scaleAndRoundY(curve.get(startIndex).doubleValue());
			for (int i = startIndex + 1; i < curve.size(); i++) {
				x1 = x2;
				y1 = y2;
				x2 = scaleAndRoundX(curve.getX(i));
				y2 = scaleAndRoundY(curve.get(i).doubleValue());
				if (scaledPointIsOnGraph(x1, y1) && scaledPointIsOnGraph(x2, y2)) 
					g2d.drawLine(x1, y1, x2, y2);
				
			}
		}
	}

	private void drawPoints(Graphics2D g2d, SignalXY curve) {
		synchronized (curve) {
			Iterator<SignalPoint> curveIterator = curve.iterator();
			SignalPoint point1 = curveIterator.next();
			double x = point1.getX();
			while (curveIterator.hasNext() && x < xMin) {
				point1 = curveIterator.next();
				x = point1.getX();
			}
			int x1 = scaleAndRoundX(point1.getX());
			int y1 = scaleAndRoundY(point1.getY());
			g2d.setColor(curve.getColor());
			if (scaledPointIsOnGraph(x1, y1))
				g2d.fillRect(x1 - POINT_SIZE / 2, y1 - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
			while (curveIterator.hasNext()) {
				point1 = curveIterator.next();
				x1 = scaleAndRoundX(point1.getX());
				y1 = scaleAndRoundY(point1.getY());
				if (scaledPointIsOnGraph(x1, y1))
					g2d.fillRect(x1 - POINT_SIZE / 2, y1 - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
			}
		}
	}

	private void drawPoints(Graphics2D g2d, YSignal<?> curve) {
		synchronized (curve) {
			int startIndex = (int) Math.floor((xMin - curve.getStartTime()) / curve.getDt());
			if (startIndex < 0)
				startIndex = 0;
			int x1, y1;
			g2d.setColor(curve.getColor());
			for (int i = startIndex; i < curve.size(); i++) {
				x1 = scaleAndRoundX(curve.getX(i));
				y1 = scaleAndRoundY(curve.get(i).doubleValue());
				if (scaledPointIsOnGraph(x1, y1))
					g2d.fillRect(x1 - POINT_SIZE / 2, y1 - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
			}
		}
	}

	/*
	 * 
	 */
	private void drawSigns(Graphics2D g2d, SignalXY points) {
		synchronized (points) {
			Iterator<SignalPoint> curveIterator = points.iterator();
			SignalPoint point1 = curveIterator.next();
			double x = point1.getX();
			while (curveIterator.hasNext() && x < xMin) {
				point1 = curveIterator.next();
				x = point1.getX();
			}
			int x1 = scaleAndRoundX(point1.getX());
			int y1 = scaleAndRoundY(point1.getY());
			g2d.setColor(points.getColor());
			if (scaledPointIsOnGraph(x1, y1))
				g2d.drawLine(x1, scaledYMin, x1, scaledYMax);
			while (curveIterator.hasNext()) {
				point1 = curveIterator.next();
				x1 = scaleAndRoundX(point1.getX());
				y1 = scaleAndRoundY(point1.getY());
				if (scaledPointIsOnGraph(x1, y1))
					g2d.drawLine(x1, scaledYMin, x1, scaledYMax);
			}
		}
	}

	private void drawSigns(Graphics2D g2d, YSignal<?> points) {
		synchronized (points) {
			int startIndex = (int) Math.floor((xMin - points.getStartTime()) / points.getDt());
			if (startIndex < 0)
				startIndex = 0;
			int x1;
			g2d.setColor(points.getColor());
			for (int i = startIndex; i < points.size(); i++) {
				x1 = scaleAndRoundX(points.getX(i));
				if (scaledXValueIsOnGraph(x1))
					g2d.drawLine(x1, scaledYMin, x1, scaledYMax);
			}
		}
	}

	
	@SuppressWarnings("unused")
	private boolean pointIsOnGraph(double x, double y) {
		return (x >= xMin && x <= xMax && y >= yMin && y <= yMax);
	}


	private boolean scaledPointIsOnGraph(int x, int y) {
		return (x >= scaledXMin && x <= scaledXMax && y <= scaledYMin && y >= scaledYMax);
	}

	@SuppressWarnings("unused")
	private boolean yValueIsOnGraph(double y) {
		return y >= yMin && y <= yMax;
	}

	@SuppressWarnings("unused")
	private boolean scaledYValueIsOnGraph(int y) {

		return y <= scaledYMin && y >= scaledYMax;
	}

	@SuppressWarnings("unused")
	private boolean xValueIsOnGraph(double x) {

		return x >= xMin && x <= xMax;
	}

	private boolean scaledXValueIsOnGraph(int x) {

		return x >= scaledXMin && x <= scaledXMax;
	}

	/*
	 * 
	 */
	private int scaleAndRoundY(double y) {
		return (int) Math.round(yScaleA * y + yScaleB);
	}

	/*
	 * 
	 */
	private int scaleAndRoundX(double x) {
		return (int) Math.round(xScaleA * x + xScaleB);
	}

	/**
	 * Resets the graph.
	 */
	public void reset() {
		paramCurve = null;
		signals.clear();
	}

	/**
	 * @param paramCurve the curve used to calculate the graph scaling.
	 */
	public void setParamCurve(Signal<?> paramCurve) {
		this.paramCurve = paramCurve;
	}

	/**
	 * @param signal signal to draw.
	 */
	public void addSignal(Signal<?> signal) {
		signals.add(signal);
	}

	/**
	 * @return the refresh rate.
	 */
	public int getRefreshRate() {
		return refreshRate;
	}

	/**
	 * @param refreshRate the nwe refresh rate.
	 */
	public void setRefreshRate(int refreshRate) {
		this.refreshRate = refreshRate;
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
	 * @return the max width of the graph.
	 */
	public double getWindowSize() {
		return windowSize;
	}

	/**
	 * @param windowSize the new max width of the graph.
	 */
	public void setWindowSize(double windowSize) {
		this.windowSize = windowSize;
	}

	/**
	 * @return font size.
	 */
	public int getFontSize() {
		return fontSize;
	}

	/**
	 * @param fontSize the new font size.
	 */
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

}
