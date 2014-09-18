package ralph;

import java.util.ArrayList;
import java.util.List;
import RalphExceptions.BackoutException;
import ralph_protobuffs.VariablesProto;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_local_version_protobuffs.DeltaProto.Delta;

    
/**
 * @param <V>  ---- The type of each internal value in the internal arraylist
 *
 * A list of strings:
 * 
 * LockedListVariable<String>
 * 
 * A list of lists of numbers:
 * 
 * LockedListVariable<
 *     LockedListVariable< Number > >
 * 
 */
public class AtomicList<ValueType, ValueDeltaType>
    extends AtomicReferenceVariable<
    // this wraps a locked container object.  Ie, calling get_val on
    // this will return AtomicListContainer.  when call set val, must
    // pass in a AtomicListContainer.  Note:version helper gets passed
    // in delta of this type.
    AtomicInternalList<ValueType, ValueDeltaType>>
{
    private EnsureAtomicWrapper<ValueType, ValueDeltaType> locked_wrapper = null;

    public final static String deserialization_label = "Atomic List";
    private final Class<ValueType> value_type_class;
    
    public AtomicList(
        boolean _log_changes,
        EnsureAtomicWrapper<ValueType, ValueDeltaType> locked_wrapper,
        VersionHelper<IReference> version_helper,
        Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        this(
            _log_changes,
            new AtomicInternalList<ValueType, ValueDeltaType>(
                ralph_globals,
                _log_changes,
                new ListTypeDataWrapperFactory<ValueType, ValueDeltaType>(_value_type_class),
                new ArrayList<RalphObject<ValueType, ValueDeltaType>>(),
                locked_wrapper),
            locked_wrapper,version_helper,_value_type_class,ralph_globals);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another MultiThreadedList.  This
       constructor is for this.
     */
    public AtomicList(
        boolean _log_changes,
        AtomicInternalList<ValueType, ValueDeltaType> internal_val,
        EnsureAtomicWrapper<ValueType, ValueDeltaType> locked_wrapper,
        VersionHelper<IReference> version_helper,
        Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        super(
            _log_changes,internal_val,
            new ValueTypeDataWrapperFactory<
                AtomicInternalList<ValueType,ValueDeltaType>>(),
            version_helper,ralph_globals,
            // additional serialization contents gets passed back to
            // serialize_contents as Object.
            new AdditionalAtomicListSerializationContents(
                _value_type_class.getName()));
        
        this.locked_wrapper = locked_wrapper;
        this.value_type_class = _value_type_class;
    }
    
    @Override
    protected
        // return type
        SpeculativeAtomicObject<
            AtomicInternalList<ValueType, ValueDeltaType>,
            IReference>
        // function name and arguments
        duplicate_for_speculation(AtomicInternalList<ValueType, ValueDeltaType> to_speculate_on)
    {
        SpeculativeAtomicObject<
            AtomicInternalList<ValueType, ValueDeltaType>,
            IReference>
            to_return =
            new AtomicList(
                log_changes,
                to_speculate_on,
                locked_wrapper,
                version_helper,
                value_type_class,
                ralph_globals);
        
        to_return.set_derived(this);
        return to_return;
    }

    @Override
    public ObjectContents serialize_contents(
        ActiveEvent active_event,Object additional_serialization_contents)
        throws BackoutException
    {
        String value_type_name = null;
        if (additional_serialization_contents == null)
            value_type_name = value_type_class.getName();
        else
        {
            AdditionalAtomicListSerializationContents add_ser_contents =
                (AdditionalAtomicListSerializationContents)
                additional_serialization_contents;
            value_type_name = add_ser_contents.val_class_name;
        }

        AtomicInternalList<ValueType,ValueDeltaType> internal_list = 
            get_val(active_event);

        String internal_reference = null;
        if (internal_list != null)
            internal_reference = internal_list.uuid();
        
        return AtomicList.serialize_list_reference(
            uuid(),internal_reference,value_type_name,true);
    }
    
    /**
       @param internal_uuid --- Can be null if pointing at null.
     */
    public static ObjectContents serialize_list_reference(
        String holder_uuid,String internal_uuid,
        String value_type_class_name, boolean atomic)
    {
        Delta.ReferenceType.Builder ref_type_builder =
            Delta.ReferenceType.newBuilder();
        if (internal_uuid != null)
            ref_type_builder.setReference(internal_uuid);

        ObjectContents.List.Builder list_builder =
            ObjectContents.List.newBuilder();
        list_builder.setRefType(ref_type_builder);
        list_builder.setValTypeClassName(value_type_class_name);

        ObjectContents.Builder contents_builder =
            ObjectContents.newBuilder();
        contents_builder.setUuid(holder_uuid);
        contents_builder.setAtomic(atomic);
        contents_builder.setListType(list_builder);
        return contents_builder.build();
    }
    
    @Override
    public void serialize_as_rpc_arg(
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException
    {
        AtomicInternalList<ValueType, ValueDeltaType> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
    }

    public static class AdditionalAtomicListSerializationContents
    {
        public final String val_class_name;
        
        public AdditionalAtomicListSerializationContents(
            String _val_class_name)
        {
            val_class_name = _val_class_name;
        }
    }
}