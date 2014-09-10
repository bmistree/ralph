package ralph;

import java.io.Serializable;

import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;

public abstract class NonAtomicValueVariable<T>
    extends NonAtomicObject<T,T>
{
    public NonAtomicValueVariable(RalphGlobals ralph_globals)
    {
        super(ralph_globals);
    }

    public NonAtomicValueVariable(
        T init_val,
        ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<T> version_helper,
        RalphGlobals ralph_globals)
    {
        super (ralph_globals);
        init(vtdwc,init_val, version_helper);
    }
    
    public void init_non_atomic_value_variable(
        T init_val,ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<T> version_helper)
    {
        init(vtdwc,init_val,version_helper);
    }

    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        this.set_val(active_event,(T)to_swap_with.get_val(active_event));
    }
    
    public boolean return_internal_val_from_container()
    {
        return true;
    }
}
