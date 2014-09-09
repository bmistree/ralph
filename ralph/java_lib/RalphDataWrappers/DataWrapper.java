package RalphDataWrappers;

import RalphExceptions.BackoutException;
import ralph.ActiveEvent;

/**
 * @param <T> --- Type of internal data holding
 */
public class DataWrapper<T>
{
    public T val;
    protected boolean has_been_written_since_last_msg = false;
    
    public DataWrapper(T _val, boolean log_changes)
    {
        val= _val;
    }
	
    /**
       @param {bool} updating_from_partner --- We do not want to mark
       an object as having been written if we are just updating its
       value from partner.
    */
    public void write (T _val, boolean updating_from_partner)
    {
    	val = _val;
    	if (! updating_from_partner)
            has_been_written_since_last_msg = true;
    }
    
    public void write(T _val)
    {
    	write(_val,false);
    }
	
    public boolean get_and_reset_has_been_written_since_last_msg()
    {
    	boolean to_return = has_been_written_since_last_msg;
    	has_been_written_since_last_msg = false;
        return to_return;
    }
}
