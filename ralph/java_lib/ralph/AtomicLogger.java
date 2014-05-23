package ralph;
import java.util.Random;

public class AtomicLogger
{
    private final static Random rand = new Random();
    private final static float LOG_PROBABILITY = .10f;
    private final StringBuffer buffer = new StringBuffer();
    private final boolean should_log = rand.nextFloat() < LOG_PROBABILITY;
    
    public void log(String event_note)
    {
        if (!should_log)
            return;
        
        long current_time = System.nanoTime();
        buffer.append("\n");
        buffer.append(": ");
        buffer.append(current_time);
        buffer.append("| ");
        buffer.append(event_note);
    }

    public void dump_log()
    {
        if (!should_log)
            return;
        System.out.println(buffer.toString());
    }
}
