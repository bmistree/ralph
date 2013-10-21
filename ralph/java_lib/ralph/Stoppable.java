package ralph;
import java.util.concurrent.atomic.AtomicBoolean;

public class Stoppable {
    private AtomicBoolean _is_stopped = new AtomicBoolean(false);
	
    public void stop()
    {
        _is_stopped.set(true);
    }
    public boolean is_stopped ()
    {
        return _is_stopped.get();
    }
}
