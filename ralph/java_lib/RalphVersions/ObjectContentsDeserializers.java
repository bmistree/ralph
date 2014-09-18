package RalphVersions;

import ralph.RalphGlobals;
import ralph.RalphObject;
import ralph.Util;
import ralph.Variables;
import ralph.ContainerFactorySingleton;
import ralph.IAtomicMapVariableFactory;
import ralph.IAtomicListVariableFactory;
import ralph.IAtomicStructWrapperBaseClassFactory;
import ralph.StructWrapperBaseClass;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_local_version_protobuffs.DeltaProto.Delta;
import ralph_local_version_protobuffs.DeltaProto.Delta.ValueType;

public class ObjectContentsDeserializers
{
    public static RalphObject deserialize(
        ObjectContents obj_contents, RalphGlobals ralph_globals)
    {
        // See note on top of force_initialization in
        // BaseAtomicMapVariableFactory.
        ralph.BaseAtomicMapVariableFactory.instance.force_initialization();
        ralph.BaseAtomicListVariableFactory.instance.force_initialization();
        
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
            else if (obj_contents.hasMapType())
            {                
                ObjectContents.Map map = obj_contents.getMapType();
                String initial_reference = map.getRefType().getReference();
                IAtomicMapVariableFactory factory =
                    ContainerFactorySingleton.instance.get_atomic_map_variable_factory(
                        map.getKeyTypeClassName(),map.getValTypeClassName());
                if (factory == null)
                {
                    Util.logger_assert(
                        "No factory to contents deserialize map.");
                }
                Variables.AtomicMapVariable to_return =
                    factory.construct(ralph_globals);
                to_return.set_initial_reference(initial_reference);
                return to_return;
            }
            else if (obj_contents.hasInternalMapType())
            {
                ObjectContents.InternalMap internal_map =
                    obj_contents.getInternalMapType();

                // Creating an internal map in a very hackish way:
                // creating an atomic map and then reaching into it to
                // get internal map.
                IAtomicMapVariableFactory factory =
                    ContainerFactorySingleton.instance.get_atomic_map_variable_factory(
                        internal_map.getKeyTypeClassName(),
                        internal_map.getValTypeClassName());
                if (factory == null)
                {
                    Util.logger_assert(
                        "No factory to contents deserialize internalmap.");
                }

                Variables.AtomicMapVariable to_return_wrapper =
                    factory.construct(ralph_globals);
                return (RalphObject) to_return_wrapper.val.val;
            }
            else if (obj_contents.hasInternalListType())
            {
                ObjectContents.InternalList internal_list =
                    obj_contents.getInternalListType();

                // Creating an internal list in a very hackish way:
                // creating an atomic list and then reaching into it to
                // get internal list.
                IAtomicListVariableFactory factory =
                    ContainerFactorySingleton.instance.get_atomic_list_variable_factory(
                        internal_list.getValTypeClassName());
                if (factory == null)
                {
                    Util.logger_assert(
                        "No factory to contents deserialize internallist.");
                }

                Variables.AtomicListVariable to_return_wrapper =
                    factory.construct(ralph_globals);
                return (RalphObject) to_return_wrapper.val.val;
            }
            else if (obj_contents.hasListType())
            {                
                ObjectContents.List list = obj_contents.getListType();
                String initial_reference = list.getRefType().getReference();
                IAtomicListVariableFactory factory =
                    ContainerFactorySingleton.instance.get_atomic_list_variable_factory(
                        list.getValTypeClassName());
                if (factory == null)
                {
                    Util.logger_assert(
                        "No factory to contents deserialize list.");
                }
                Variables.AtomicListVariable to_return =
                    factory.construct(ralph_globals);
                to_return.set_initial_reference(initial_reference);
                return to_return;
            }
            else if (obj_contents.hasStructType())
            {
                ObjectContents.Struct struct = obj_contents.getStructType();
                String initial_reference = struct.getRefType().getReference();
                IAtomicStructWrapperBaseClassFactory factory =
                    ContainerFactorySingleton.instance.get_atomic_struct_wrapper_base_class_factory(
                        struct.getStructTypeClassName());
                if (factory == null)
                {
                    Util.logger_assert(
                        "No factory to contents deserialize struct.");
                }
                StructWrapperBaseClass to_return = factory.construct(ralph_globals);
                to_return.set_initial_reference(initial_reference);
                return to_return;
            }
            else if (obj_contents.hasInternalStructType())
            {
                ObjectContents.InternalStruct internal_struct =
                    obj_contents.getInternalStructType();

                // Creating an internal struct in a very hackish way:
                // creating an atomic struct and then reaching into it
                // to get internal struct.
                IAtomicStructWrapperBaseClassFactory factory =
                    ContainerFactorySingleton.instance.get_atomic_struct_wrapper_base_class_factory(
                        internal_struct.getStructTypeClassName());
                if (factory == null)
                {
                    Util.logger_assert(
                        "No factory to contents deserialize internalstruct.");
                }

                // Warning: when deserializing an internal struct,
                // actually returning the wrapper that it holds.
                // Caller will need to reach in to grab out internal
                // struct.  Doing this because this method must return
                // a RalphObject and internal structs do not inherit
                // from RalphObject.
                StructWrapperBaseClass to_return_wrapper =
                    factory.construct(ralph_globals);
                return to_return_wrapper;
            }

            
            // must have reference type
            Util.logger_assert(
                "FIXME: must allow deserializing unknown atomic type.");
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
            else if (obj_contents.hasRefType())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "reference types");
            }
            else if (obj_contents.hasMapType())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "map types");
            }
            else if (obj_contents.hasListType())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "list types");
            }
            else if (obj_contents.hasInternalMapType())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "internal map types");
            }
            else if (obj_contents.hasInternalListType())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "internal list types");
            }
            else if (obj_contents.hasNullType())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "internal null types");
            }
            else if (obj_contents.hasInternalStructType())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "internal struct types");
            }
            else if (obj_contents.hasStructType())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "struct types");
            }
                        
            // must have reference type
            Util.logger_assert(
                "FIXME: unknown non-atomic object to deserialize");
        }

        Util.logger_assert("Unknown object contents deserialization.");
        return null;
    }
}