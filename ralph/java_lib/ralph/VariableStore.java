package ralph;

import java.util.HashMap;
import java.util.Map.Entry;

import RalphExceptions.BackoutException;
import ralph_protobuffs.VariablesProto.Variables;

public class VariableStore 
{
    private String host_uuid;
    private HashMap<String,LockedObject> name_to_var_map = 
        new HashMap<String, LockedObject>();

    public VariableStore(String _host_uuid)
    {
        host_uuid = _host_uuid;
    }

    /**
     * @param {String} unique_name ---

     @param {_WaldoVariable} waldo_variable 
    */
    public void add_var(String unique_name,LockedObject waldo_variable)
    {
        // DEBUG
        if (get_var_if_exists(unique_name) != null)
        {
            Util.logger_assert(
                "Already had an entry for variable trying to " +
                "insert into store.");
        }
        //#### END DEBUG
          
        name_to_var_map.put(unique_name, waldo_variable); 
    }

    /**
     *
     @param {String} unique_name --- 
     @returns {_WaldoVariable or None} --- None if variable does
     not exist, _WaldoVariable otherwise.
    */
    public LockedObject get_var_if_exists(String unique_name) 
    {
        return name_to_var_map.get(unique_name);
    }

}


