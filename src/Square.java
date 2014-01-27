import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

class Square {

    public static final double FRAME_TIME_MS = 22.5;
    public static final int SAMPLING_RATE = 44100;
    public static final int SAMPLE_SIZE = 2;
    public static final double SAMPLE_TIME_MS = 1000.0 / SAMPLING_RATE;

    // private double channels[] = {0.0011, 0.0011, 0.0011, 0.0011, 0.0011, 0.0011, 0.0011, 0.0011}; //Or 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1 ?
    private double channels[] = { 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1 };
    private boolean stop = false;
    private Thread playThread;

    public static void main(String args[]) throws LineUnavailableException {
        new Square().start();
    }

    public List<Double> buildFrame(double channels[]) {
        double dataLength = 0;
        List<Double> frame = new ArrayList<Double>(2 * (channels.length + 1));

        for (double chan : channels) {
            frame.add(0.4);
            frame.add(chan);
            dataLength += 0.4 + chan;
        }

        frame.add(0.4);
        dataLength += 0.4;
        frame.add(0, FRAME_TIME_MS - dataLength); // Add the padding at the beginning.
        return frame;
    }

    public void playForever(SourceDataLine line) throws InterruptedException {
        // Assume the line passed in argument is already open and started

        ByteBuffer cBuf = ByteBuffer.allocate(line.getBufferSize());

        List<Double> frame = buildFrame(this.channels);
        Iterator<Double> itr = frame.iterator();
        double nextChange = itr.next();
        byte y = 0; // Start with "flat" padding
        double time = 0;
        while (!stop) {
            cBuf.clear();
            int samplesThisPass = line.available() / SAMPLE_SIZE;
            for (int i = 0; i < samplesThisPass; ++i) {
                cBuf.putShort((short) (Short.MAX_VALUE * y));
                time += SAMPLE_TIME_MS;
                if (time >= nextChange) {

                    if (itr.hasNext()) {
                        nextChange += itr.next();
                        if (y == 0) {
                            // Padding ends, now goes LOW.
                            y = -1;
                        } else {
                            y = (byte) -y;
                        }
                    } else {
                        // Next frame
                        frame = buildFrame(this.channels); // In case the channels have been updated;
                        itr = frame.iterator();
                        time -= nextChange;
                        // nextChange = 0;
                        // nextChange += itr.next();
                        nextChange = itr.next();
                        y = 0;
                    }
                }
            }
            // Write samples to the line buffer.
            line.write(cBuf.array(), 0, cBuf.position());

            // Wait until the buffer is at least half empty before we add more
            while (line.getBufferSize() / 2 < line.available()) {
                Thread.sleep(1);
            }
        }

        line.drain();
        line.close();
    }

    public void start() throws LineUnavailableException {
        final SourceDataLine line = setupLine();
        stop = false;
        Thread playThrad = new Thread() {
            @Override
            public void run() {
                try {
                    playForever(line);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        playThrad.start();
    }

    public void stop() {
        stop = true;
    }

    public void stopAndWait() throws InterruptedException {
        stop = true;
        if (playThread != null) {
            playThread.join(); // Wait for it to finish.
        }
    }

    public void setChannel(int chan, double time) {
        // TODO: Check if the chan and time are not too big and not too small
        channels[chan] = time;
    }

    public SourceDataLine setupLine() throws LineUnavailableException {
        SourceDataLine line;
        AudioFormat format = new AudioFormat(SAMPLING_RATE, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line matching " + info + " is not supported.");
            throw new LineUnavailableException();
        }

        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        return line;
    }
}
