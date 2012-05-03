import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class MicDataSpewer extends Applet {
	private static final long serialVersionUID = 1L;
	TargetDataLine dataline;
	SamplingThread sampleThread;
	AudioFormat audioFormat;

	boolean errorState;
	String errMsg;
	Float avgAmp = 0f;
	
	Float runningAvg = 0f;
	Integer runningAvgCount = 0;

	public void start() {
		audioFormat = new AudioFormat(44100, 16, 1, true, false);
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class,
				audioFormat);
		System.out.println(dataLineInfo);
		
		try {
			dataline = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
			System.out.println(dataline);
			dataline.open(audioFormat);
		} catch (Exception e){
			errorState = true;
			errMsg = "Could not open data line for AudioSystem -- " + e.getMessage();
			repaint();
			return;
		}
		
		try {
			sampleThread = new SamplingThread();
			sampleThread.start();
		} catch (Exception e) {
			errorState = true;
			errMsg = "Could not start sampling thread. -- " + e.getMessage();
			repaint();
		}
	}

	public class SamplingThread extends Thread {
		boolean running = false;

		public void run() {
			running = true;
			try {
				int sampleSize = (int) (audioFormat.getSampleRate() * audioFormat.getFrameSize());
				byte sampleBuffer[] = new byte[sampleSize];
				dataline.start();

				while (running
						&& dataline.read(sampleBuffer, 0, sampleSize) > 0) {
					avgAmp = 0f;
					ByteBuffer bob = ByteBuffer.wrap(sampleBuffer);
					ShortBuffer sb = bob.asShortBuffer();
					System.out.println(bob.order().toString() + ' ' + sb.order().toString());
					
					while (sb.hasRemaining()) {
						avgAmp += sb.get();
					}
					avgAmp /= sb.capacity();

					//runningAvg = (avgAmp + runningAvgCount*runningAvg)/(runningAvgCount+1);
					runningAvg = avgAmp;
					runningAvgCount++;
					
					repaint();

				}
				
				dataline.flush();
				dataline.close();

			} catch (Exception e) {
				errorState = true;
				errMsg = "Problem occurred in thread loop -- " + e.getMessage();
				repaint();
			}
		}

	}

	public void paint(Graphics g) {

		if (errorState) {
			g.setColor(Color.black);
			g.drawString(errMsg, 86, 124);
			this.sampleThread.running = false;
			return;
		}
		if (avgAmp != 0) {
			g.drawString("Average: " + runningAvg.toString(), 20, 60);
			g.drawString("Count: " + runningAvgCount.toString(), 20, 80);
		} else {
			g.drawString("Loading...", 20, 60);
		}
	}

}