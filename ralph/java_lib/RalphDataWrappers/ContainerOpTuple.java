package RalphDataWrappers;

import ralph.RalphObject;

public class ContainerOpTuple<KeyType,ValueType,DeltaValueType>
{
    public static enum OpType
    {
        DELETE,ADD,WRITE,CLEAR;
    }

    public final OpType type;
    public final KeyType key;
    // for map type, this is just what got added, not what got removed.
    public final RalphObject<ValueType,DeltaValueType> what_added_or_removed;
    
    public ContainerOpTuple(
        OpType _type, KeyType _key,
        RalphObject<ValueType,DeltaValueType> _what_added_or_removed)
    {
        type = _type;
        key = _key;
        what_added_or_removed = _what_added_or_removed;
    }		
}
