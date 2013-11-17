package ralph;

import java.util.concurrent.Callable;
import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;


/**
   Ralph has a parallel keyword:
   
   parallel(
       some_iterable,
       ParallelBlock);
   
   Each parallel block takes in an item from the iterable and executes
   call on it, returning a value to indicate success or error.
 */
public abstract class ParallelBlock <E> implements Callable<Integer>
{
    /**
       Return codes for call methods.
     */
    public final static Integer CALL_NO_ERROR = null;
    public final static Integer CALL_BACKOUT_EXCEPTION = new Integer(1);
    public final static Integer CALL_APPLICATION_EXCEPTION = new Integer(2);
    public final static Integer CALL_NETWORK_EXCEPTION = new Integer(3);
    public final static Integer CALL_STOPPED_EXCEPTION = new Integer(4);

        
    protected VariableStack vstack = null;
    protected LockedActiveEvent active_event = null;
    protected E to_run_on = null;
    
    public ParallelBlock(
        VariableStack _vstack,LockedActiveEvent _active_event)
    {
        vstack = _vstack;
        active_event = _active_event;
    }
    public void set_to_run_on(E _to_run_on)
    {
        to_run_on = _to_run_on;
    }

    /**
       Returns one of the return codes defined at top of class.
    */
    public Integer call()
    {
        try
        {
            internal_call();
        }
        catch (BackoutException _ex)
        {
            return CALL_BACKOUT_EXCEPTION;
        }
        catch (ApplicationException _ex)
        {
            return CALL_APPLICATION_EXCEPTION;
        }
        catch (NetworkException _ex)
        {
            return CALL_NETWORK_EXCEPTION;
        }
        catch (StoppedException _ex)
        {
            return CALL_STOPPED_EXCEPTION;
        }

        return CALL_NO_ERROR;
    }

    /**
       Should be overridden in place by each parallel block.
     */
    public abstract void internal_call()
        throws ApplicationException, BackoutException, NetworkException,StoppedException;

}
                                   