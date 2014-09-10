package RalphDataWrappers;

import RalphExceptions.BackoutException;
import ralph.ActiveEvent;

/**
 * @param <T> --- Type of internal data holding
 */
public class DataWrapper<T>
{
    public T val;
    public DataWrapper(T _val, boolean log_changes)
    {
        val= _val;
    }
	
    public void write(T _val)
    {
        val = _val;
    }
}
