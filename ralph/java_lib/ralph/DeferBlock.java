package ralph;
import java.lang.Runnable;

public abstract class DeferBlock implements Runnable
{
    protected VariableStack vstack = null;
    public DeferBlock(VariableStack _vstack)
    {
        vstack = _vstack.fork_stack();
    }

    public abstract void run();
}
