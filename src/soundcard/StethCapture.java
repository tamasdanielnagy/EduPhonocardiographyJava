package soundcard;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import signal.ByteArray;
import signal.SignalD;


public class StethCapture implements Runnable {

	public static final float DEFAULT_SAMLING_RATE = 44100.0f;
	
	public static final int DEFAULT_MEAS_WINDOW = 88;
	public static final int AVERAGE_N = 44;
	public static final double THRESHOLD_MULTIPLIER = 0.45;
	public static final String tempFileName = "temp.dat";
	
	
	private TargetDataLine targetLine;
	private AudioFormat format;
	private SignalD signal;
	private boolean capture;
	private int maxValue;
	private double fs;
	private boolean negateSignal = false;
	BufferedOutputStream tempOut;
		
	public StethCapture() {
		format = new AudioFormat(DEFAULT_SAMLING_RATE, 16, 1, true, true);	
		signal = new SignalD(Color.BLUE);
	}
	
	public StethCapture(SignalD signal) {
		format = new AudioFormat(DEFAULT_SAMLING_RATE, 16, 1, true, true);	
		this.signal = signal;
	}
	
	public StethCapture(SignalD signal, float sampleRate) {
		format = new AudioFormat(sampleRate, 16, 1, true, true);	
		this.signal = signal;
	}
	
	public void createLine() {
		try {
			tempOut = new BufferedOutputStream(new FileOutputStream(tempFileName, false));
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			targetLine = (TargetDataLine) AudioSystem.getLine(info);
			targetLine.open();
			
			//Thread.sleep(500);

		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		createLine();
		targetLine.flush();
		targetLine.start();
		capture = true;

		record();
		
		targetLine.stop();
		targetLine.flush();
		targetLine.close();
		
		try {
			tempOut.flush();
			tempOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
/*	public void whiteNoise() {
		signal.setDt(1.0);
		while (capture) {
			synchronized (signal) {
				signal.add(MyRand.randomDouble(0.0, 1.0));
			}
		}
	}*/
	
	public void record() {
		fs = targetLine.getFormat().getSampleRate();
		int bufferSize = targetLine.getFormat().getFrameSize() * AVERAGE_N;
		// System.out.println(bufferSize);
		signal.setDt((1.0 / fs) * AVERAGE_N);
		byte buffer[] = new byte[bufferSize];
		int count = 0;
		while (capture) {
			synchronized (buffer) {
				count = targetLine.read(buffer, 0, buffer.length);
				//System.out.println(count);
				if (count > 0) {
					writeBufferAverageToSignal(buffer);
					writeBufferToTemp(buffer, true);
				}
			}
		}
	}
	
	public void readWavIntoSignal(File wavFile) {
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(wavFile);
			format = stream.getFormat();
			int buffSize = format.getFrameSize() * AVERAGE_N;
			long i = 0;
			int frameSize = format.getFrameSize();
			long byteLength = stream.getFrameLength() * frameSize;
			byte[] buffer = new byte[buffSize];
			int count = 0;
			signal.setDt((1.0 / format.getFrameRate()) * AVERAGE_N);
			tempOut = new BufferedOutputStream(new FileOutputStream(tempFileName, false));
			while (i < byteLength) {
				
				count = stream.read(buffer, 0, buffer.length);
				if (count > 0) {
					i += count;
					writeBufferAverageToSignal(buffer, false);
					writeBufferToTemp(buffer, true);
				}
				//System.out.println(byteLength + ", " + i);
			}
			tempOut.close();
		} catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTempToWavFile(File wavFile) {
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(tempFileName));
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			BufferedOutputStream out = new BufferedOutputStream(b);
			byte[] buff = new byte[in.available()];
			while (in.read(buff, 0, buff.length) > 0)
				out.write(buff, 0, buff.length);
			out.flush();
			byte[] barr = b.toByteArray();
			b.close();
			in.close();
			out.close();
			
			AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(barr), format, barr.length / format.getFrameSize());
			AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, wavFile);
			inputStream.close();
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deleteTempFile() {
		File tmp = new File(tempFileName);
		tmp.delete();
	}
	
	public void writeBufferToTemp(byte[] buffer, boolean bigEndian) {
		synchronized (buffer) {
			try {
				if (bigEndian) {
					tempOut.write(buffer);
				} else {
					byte[] b2 = new byte[buffer.length];
					int frameSize = format.getFrameSize();
					if (buffer.length % frameSize != 0)
						throw new InvalidAlgorithmParameterException("Buffer contains incomplete frame.");
					for (int i = 0; i < buffer.length / frameSize; i++) {
						b2[2 * i] = buffer[(2 * i) + 1];
						b2[(2 * i) + 1] = buffer[2 * i];						
					}
					tempOut.write(b2);	
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void writeBufferToSignal(byte[] buffer) {
		synchronized (signal) {
			synchronized (buffer) {
				signal.concatByteArrayToSignal(1 / format.getFrameRate(),
						buffer, 2);
			}
		}
	}
	
	public void writeBufferAverageToSignal(byte[] buffer, boolean bigEndian) {
		synchronized (signal) {
			synchronized (buffer) {
				if (negateSignal)
					signal.add(-ByteArray.averageDouble(buffer,format.getFrameSize(), false));
				else
					signal.add(ByteArray.averageDouble(buffer, format.getFrameSize(), false));
			}
		}
	}
	
	public void writeBufferAverageToSignal(byte[] buffer) {
		synchronized (signal) {
			synchronized (buffer) {
				if (negateSignal)
					signal.add(-ByteArray.averageDouble(buffer, format.getFrameSize()));
				else
					signal.add(ByteArray.averageDouble(buffer,format.getFrameSize()));
			}
		}
	}
	
	

	public SignalD getSignal() {
		return signal;
	}

	public void setSignal(SignalD signal) {
		this.signal = signal;
	}

	public void clearSignal() {
		synchronized (signal) {
			signal.clear();
		}
	}

	public boolean isCapture() {
		return capture;
	}

	public void setCapture(boolean capture) {
		this.capture = capture;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}
	

	public boolean isNegateSignal() {
		return negateSignal;
	}

	public void setNegateSignal(boolean negateSignal) {
		this.negateSignal = negateSignal;
	}


	

}
