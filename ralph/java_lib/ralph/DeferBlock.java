package ralph;
import java.lang.Runnable;

public abstract class DeferBlock implements Runnable
{
    protected VariableStack vstack = null;
    protected LockedActiveEvent active_event = null;
    public DeferBlock(
        VariableStack _vstack,LockedActiveEvent _active_event)
    {
        vstack = _vstack.fork_stack();
        active_event = _active_event;
    }

    public abstract void run();
}
