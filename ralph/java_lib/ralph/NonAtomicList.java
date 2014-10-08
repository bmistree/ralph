package ralph;

import java.util.List;
import java.util.ArrayList;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import ralph.AtomicList.AdditionalAtomicListSerializationContents;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;

/**
 * @param <V> ---- The type of each internal value in the internal
 * list
 * 
 * A list of strings:
 * 
 * LockedListVariable<String>
 * 
 * A list of lists of numbers:
 * 
 * LockedListVariable<
 *     LockedListVariable< Number, List<Number > > >
 * 
 */
public abstract class NonAtomicList<ValueType,DeltaType>
    extends NonAtomicReferenceVariable<
    // this wraps a locked container object.  Ie, calling get_val on
    // this will return AtomicListContainer.  when call set val, must
    // pass in a AtomicListContainer.  Note:version helper gets passed
    // in delta of this type.
    NonAtomicInternalList<ValueType, DeltaType>>
{
    public final Class<ValueType> value_type_class;
    
    public NonAtomicList(
        EnsureAtomicWrapper<ValueType,DeltaType> locked_wrapper,
        VersionHelper<IReference> version_helper,
        Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        this(
            new NonAtomicInternalList<ValueType,DeltaType>(
                ralph_globals,
                new ListTypeDataWrapperFactory<ValueType,DeltaType>(_value_type_class),
                new ArrayList<RalphObject<ValueType,DeltaType>>(),locked_wrapper),
            locked_wrapper,version_helper,_value_type_class,ralph_globals);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another NonAtomicLockList.
       This constructor is for this.
     */
    public NonAtomicList(
        NonAtomicInternalList<ValueType,DeltaType> internal_val,
        EnsureAtomicWrapper<ValueType,DeltaType> locked_wrapper,
        VersionHelper<IReference> version_helper,
        Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        super(
            internal_val,
            new ValueTypeDataWrapperFactory<
                NonAtomicInternalList<ValueType,DeltaType>>(),
            version_helper,ralph_globals,

            // additional serialization contents gets passed back to
            // serialize_contents as Object.
            new AtomicList.AdditionalAtomicListSerializationContents(
                _value_type_class.getName()));
        
        value_type_class = _value_type_class;
    }

    @Override
    public ObjectContents serialize_contents(
        ActiveEvent active_event, Object additional_serialization_contents,
        SerializationContext serialization_context)
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

        NonAtomicInternalList<ValueType,DeltaType> internal_list = 
            get_val(active_event);

        String internal_reference = null;
        if (internal_list != null)
        {
            internal_reference = internal_list.uuid();
            if (serialization_context != null)
                serialization_context.add_to_serialize(internal_list);
        }
        
        return AtomicList.serialize_list_reference(
            uuid(),internal_reference,value_type_name,false);
    }
}