package ralph;

import RalphExceptions.BackoutException;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import RalphVersions.ObjectHistory;
import RalphVersions.IReconstructionContext;


/**
   Intended to be extended by objects that are used where RalphObjects
   should be, but aren't really.  In particular, InternalStructs.
 */
public abstract class DummyRalphObject <Type, DeltaType>
    extends RalphObject<Type,DeltaType> 
{
    @Override
    public ObjectContents serialize_contents(
        ActiveEvent active_event,Object add_contents,
        SerializationContext serialization_context)
        throws BackoutException
    {
        Util.logger_assert("Cannot call serialize_contents on dummy.");
        return null;
    }

    @Override
    public void direct_set_val(Type new_val)
    {
        Util.logger_assert("Cannot call direct_set_val on dummy.");
    }
    @Override
    public void update_event_priority(
        String uuid,String new_priority)
    {
        Util.logger_assert("Cannot call update_event_priority on dummy.");
    }
    @Override
    public Type get_val(
        ActiveEvent active_event) throws BackoutException
    {
        Util.logger_assert("Cannot call get_val on dummy.");
        return null;
    }
    @Override
    public void set_val(
        ActiveEvent active_event, Type new_val) throws BackoutException
    {
        Util.logger_assert("Cannot call set_val on dummy.");
    }
    @Override
    public boolean return_internal_val_from_container()
    {
        Util.logger_assert("Cannot call set_val on dummy.");
        return false;
    }
    @Override
    public void complete_commit(ActiveEvent active_event)
    {
        Util.logger_assert("Cannot call complete_commit on dummy.");
    }
    @Override
    public void backout(ActiveEvent active_event)
    {
        Util.logger_assert("Cannot call backout on dummy.");
    }
}