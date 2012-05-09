import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class MicDataSpewer extends Applet implements AdjustmentListener {
	private static final long serialVersionUID = 1L;
	TargetDataLine dataline;
	SamplingThread sampleThread;
	AudioFormat audioFormat;
	int multiplier;

	private short [] sbAsArrays;
	
	Scrollbar slider = new Scrollbar(Scrollbar.VERTICAL, 0 , 1, 0, 100);
	
	boolean errorState;
	String errMsg;
	public void start() {
		audioFormat = new AudioFormat(44100, 16, 1, true, false);
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class,
				audioFormat);
		System.out.println(dataLineInfo);
		
		add(slider);
		slider.addAdjustmentListener(this);

		try {
			dataline = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
			System.out.println(dataline);
			dataline.open(audioFormat);
		} catch (Exception e) {
			errorState = true;
			errMsg = "Could not open data line for AudioSystem -- "
					+ e.getMessage();
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
				int sampleSize = (int) (audioFormat.getFrameSize()
						* audioFormat.getChannels()
						* audioFormat.getSampleRate()) / 50; // 20ms
				System.out.println("Sample Size is: " + sampleSize + " bytes");
				System.out.println("Byte-order is big edian?: "+audioFormat.isBigEndian());
				byte sampleBuffer[] = new byte[sampleSize];
				dataline.start();

				while (running
						&& dataline.read(sampleBuffer, 0, sampleSize) > 0) {
					ByteBuffer bob = ByteBuffer.wrap(sampleBuffer);
					if (audioFormat.isBigEndian()) {
						bob.order(ByteOrder.BIG_ENDIAN);						
					} else {
						bob.order(ByteOrder.LITTLE_ENDIAN);
					}
					ShortBuffer sb = bob.asShortBuffer();
					sbAsArrays = new short[sb.capacity()];
					sb.get(sbAsArrays);

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

	int map(int x, int in_min, int in_max, int out_min, int out_max)
	{
	  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}
	
	public void paint(Graphics g) {

		if (errorState) {
			g.setColor(Color.black);
			g.drawString(errMsg, 86, 124);
			this.sampleThread.running = false;
			return;
		}
		if (sbAsArrays != null) {
//			g.drawString("Average: " + runningAvg.toString(), 20, 60);
//			g.drawString("Count: " + runningAvgCount.toString(), 20, 80);
			
			Rectangle drawingSpace = g.getClipBounds();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, drawingSpace.width, drawingSpace.height);
			for (int i = 1; i<sbAsArrays.length; i++) {
				g.setColor(Color.GREEN);
				int oldX = map(i-1, 0, sbAsArrays.length, 0, drawingSpace.width);
				int newX = map(i, 0, sbAsArrays.length, 0, drawingSpace.width);
				int oldY = map(sbAsArrays[i-1]*multiplier, Short.MAX_VALUE, Short.MIN_VALUE, 0, drawingSpace.height);
				int newY = map(sbAsArrays[i]*multiplier, Short.MAX_VALUE, Short.MIN_VALUE, 0, drawingSpace.height);

				g.drawLine(oldX, oldY, newX, newY);
			}
		} else {
			g.drawString("Loading...", 20, 60);
		}
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent arg0) {
		multiplier = slider.getValue() * 50 / 100;
		repaint();
	}

}