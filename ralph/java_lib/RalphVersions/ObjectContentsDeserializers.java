package RalphVersions;

import ralph.RalphGlobals;
import ralph.RalphObject;
import ralph.Util;
import ralph.Variables;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_local_version_protobuffs.DeltaProto.Delta;
import ralph_local_version_protobuffs.DeltaProto.Delta.ValueType;

public class ObjectContentsDeserializers
{
    public static RalphObject deserialize(
        ObjectContents obj_contents, RalphGlobals ralph_globals)
    {
        if (obj_contents.getAtomic())
        {
            // deserialize atomics

            if (obj_contents.hasValType())
            {
                ValueType val_type = obj_contents.getValType();

                if (val_type.hasNum())
                {
                    return new Variables.AtomicNumberVariable(
                        false,new Double(val_type.getNum()),
                        ralph_globals);
                }
                else if (val_type.hasText())
                {
                    return new Variables.AtomicTextVariable(
                        false,val_type.getText(),
                        ralph_globals);
                }
                else if (val_type.hasTf())
                {
                    return new Variables.AtomicTrueFalseVariable(
                        false,val_type.getTf(),
                        ralph_globals);
                }
                //// DEBUG
                Util.logger_assert(
                    "Unknown how to deserialize atomic val type.");
                //// END DEBUG

            }
            // must have reference type
            Util.logger_assert(
                "FIXME: must allow deserializing reference types");
        }
        else
        {
            // deserializes non-atomics
            if (obj_contents.hasValType())
            {
                ValueType val_type = obj_contents.getValType();

                if (val_type.hasNum())
                {
                    return new Variables.NonAtomicNumberVariable(
                        false,new Double(val_type.getNum()),
                        ralph_globals);
                }
                else if (val_type.hasText())
                {
                    return new Variables.NonAtomicTextVariable(
                        false,val_type.getText(),
                        ralph_globals);
                }
                else if (val_type.hasTf())
                {
                    return new Variables.NonAtomicTrueFalseVariable(
                        false,val_type.getTf(),
                        ralph_globals);
                }
                //// DEBUG
                Util.logger_assert(
                    "Unknown how to deserialize nonatomic val type.");
                //// END DEBUG

            }
            
            // must have reference type
            Util.logger_assert(
                "FIXME: must allow deserializing reference types");
        }

        Util.logger_assert("Unknown object contents deserialization.");
        return null;
    }
}