import java.nio.ByteBuffer;
import javax.sound.sampled.*;
 
 
public class Square {
	
   //This is just an example - you would want to handle LineUnavailable properly...
   public static void main(String[] args) throws InterruptedException, LineUnavailableException
   {
	  
	  double c1=0.0011, c2=0.0011, c4=0.0011, c5=0.0011, c6=0.0011, c7=0.0011, c8=0.0011; //time in s per channel
	  double c3 = 0.0015;
	  double controls[] = {c1,c2,c3,c4,c5,c6,c7,c8};
	  double stop = 0.00004;	//0,4ms stop signal
	  double all = 0.0225; 	//22,5ms (control signals + stop signals + flat signal)
	  
      final int SAMPLING_RATE = 44100;            // Audio sampling rate
      final int SAMPLE_SIZE = 2;          	      // Audio sample size in bytes
 
      SourceDataLine line;
      double fFreq = 10000;                       // Frequency of sine wave in hz
 
      //Position through the sine wave as a percentage (i.e. 0 to 1 is 0 to 2*PI)
      double fCyclePosition = 0;
 
      //Open up audio output, using 44100hz sampling rate, 16 bit samples, mono, and big
      // endian byte ordering
      AudioFormat format = new AudioFormat(SAMPLING_RATE, 16, 1, true, true);
      DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
 
      if (!AudioSystem.isLineSupported(info)){
         System.out.println("Line matching " + info + " is not supported.");
         throw new LineUnavailableException();
      }
 
      line = (SourceDataLine)AudioSystem.getLine(info);
      line.open(format);  
      line.start();
 
      // Make our buffer size match audio system's buffer
      ByteBuffer cBuf = ByteBuffer.allocate(line.getBufferSize());  
 
      int ctSamplesTotal = SAMPLING_RATE*5;       // Output for roughly 5 seconds
 
 
      //On each pass main loop fills the available free space in the audio buffer
      //Main loop creates audio samples for sine wave, runs until we tell the thread to exit
      //Each sample is spaced 1/SAMPLING_RATE apart in time
      while (ctSamplesTotal>0) {
         double fCycleInc = fFreq/SAMPLING_RATE;  // Fraction of cycle between samples
 
         cBuf.clear();                            // Discard samples from previous pass

          // Figure out how many samples we can add
         int ctSamplesThisPass = line.available()/SAMPLE_SIZE;  
         for (int i=0; i < ctSamplesThisPass; i++) {
           
        	 for (int counter=0; counter<controls.length;counter++){
        		 cBuf.putShort((short)(((fCycleInc*controls[counter])%2) - ((fCycleInc*controls[counter])%1)));
        	 }
        
        	/*cBuf.putShort((short)(Short.MAX_VALUE * Math.sin(2*Math.PI * fCyclePosition)));  <---The original version, for sine waves.
 
            fCyclePosition += fCycleInc;
            if (fCyclePosition > 1)
               fCyclePosition -= 1; */
         }
 
         //Write sine samples to the line buffer.  If the audio buffer is full, this will
         // block until there is room (we never write more samples than buffer will hold)
         line.write(cBuf.array(), 0, cBuf.position());            
         ctSamplesTotal -= ctSamplesThisPass;     // Update total number of samples written
 
         //Wait until the buffer is at least half empty  before we add more
         while (line.getBufferSize()/2 < line.available())  
            Thread.sleep(1);                                            
      }
 
 
      //Done playing the whole waveform, now wait until the queued samples finish
      //playing, then clean up and exit
      line.drain();                                        
      line.close();
   }
}