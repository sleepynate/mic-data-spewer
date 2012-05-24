import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;

import netscape.javascript.JSObject;

public class MicDataSpewer extends Applet {
	private static final long serialVersionUID = 1L;
	int multiplier = 2;

	private short[] sbAsArrays;

	boolean errorState;
	boolean running;
	SampleThread sampleThread;
	String errMsg;

	public void start() {
		sampleThread = new SampleThread();
		sampleThread.start();
	}

	public class SampleThread extends Thread {
		public void run() {
			try {
				running = true;
				double fCyclePosition = 0;
				while (running) {
					final int SAMPLING_RATE = 44100; // Audio sampling rate

					double fFreq = 440; // Frequency of sine wave in hz

					// Position through the sine wave as a percentage (i.e. 0 to
					// 1 is 0 to 2*PI)
					sbAsArrays = new short[882];

					for (int i = 0; i < 882; i++) {
						sbAsArrays[i] = (short) (Short.MAX_VALUE * Math.sin(2
								* Math.PI * fCyclePosition));

						double fCycleInc = fFreq / SAMPLING_RATE;
						fCyclePosition += fCycleInc;
						if (fCyclePosition > 1)
							fCyclePosition -= 1;
					}
					executeJS(sbAsArrays);
				}
			} catch (Exception e) {
				errorState = true;
				errMsg = "Could not start sampling thread. -- "
						+ e.getMessage();
				repaint();
			}
		}
	}

	@Override
	public void stop() {
		running = false;
		super.stop();
	}

	@Override
	public void destroy() {
		running = false;
		super.destroy();
	}

	public void executeJS(short[] pcmData) {
		JSObject j = JSObject.getWindow(this);
		j.call("passAudio", new Object[] { pcmData });
	}

	int map(int x, int in_min, int in_max, int out_min, int out_max) {
		return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}

	public void paint(Graphics g) {

		if (errorState) {
			g.setColor(Color.black);
			g.drawString(errMsg, 86, 124);
			return;
		}
		if (sbAsArrays != null) {

		} else {
			g.drawString("Loading...", 20, 60);
		}
	}

}