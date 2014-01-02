package ralph;
import RalphExceptions.BackoutException;

public interface ImmediateCommitSupplier
{
    public void check_immediate_commit(ActiveEvent active_event)
        throws BackoutException;
}