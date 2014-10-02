package RalphVersions;

import java.util.List;
import java.util.ArrayList;

import ralph.RalphGlobals;
import ralph.RalphObject;
import ralph.Util;
import ralph.Variables;
import ralph.ContainerFactorySingleton;
import ralph.IAtomicMapVariableFactory;
import ralph.IAtomicListVariableFactory;
import ralph.IAtomicStructWrapperBaseClassFactory;
import ralph.StructWrapperBaseClass;
import ralph.InternalStructBaseClass;
import ralph.EnumConstructorObj;
import ralph.IReference;

import ralph_version_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_version_protobuffs.DeltaProto.Delta;
import ralph_version_protobuffs.DeltaProto.Delta.ReferenceType;
import ralph_version_protobuffs.DeltaProto.Delta.ValueType;

public class ObjectContentsDeserializers
{
    public static RalphObject deserialize(
        ObjectContents obj_contents, RalphGlobals ralph_globals,
        IReconstructionContext reconstruction_context)
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

                if (val_type.hasNum() || val_type.hasNullNum())
                {
                    Double to_set_to =
                        val_type.hasNum() ? val_type.getNum() : null;
                    return new Variables.AtomicNumberVariable(
                        false,to_set_to,ralph_globals);
                }
                else if (val_type.hasText() || val_type.hasNullText())
                {
                    String to_set_to =
                        val_type.hasText() ? val_type.getText() : null;
                    return new Variables.AtomicTextVariable(
                        false,to_set_to,ralph_globals);
                }
                else if (val_type.hasTf() || val_type.hasNullTf())
                {
                    Boolean to_set_to =
                        val_type.hasTf() ? val_type.getTf() : null;
                    return new Variables.AtomicTrueFalseVariable(
                        false,to_set_to,ralph_globals);
                }
                //// DEBUG
                Util.logger_assert(
                    "Unknown how to deserialize atomic val type.");
                //// END DEBUG
            }
            else if (obj_contents.hasEnumType())
            {
                ObjectContents.Enum enum_msg = obj_contents.getEnumType();
                String enum_constructor_obj_class_name =
                    enum_msg.getEnumConstructorObjClassName();
                int ordinal = enum_msg.getEnumOrdinal();

                ILocalVersionReplayer local_version_replayer =
                    reconstruction_context.get_local_version_replayer();
                EnumConstructorObj enum_constructor =
                    local_version_replayer.get_enum_constructor_obj(
                        enum_constructor_obj_class_name);
                return enum_constructor.construct(ordinal,ralph_globals);
            }
            else if (obj_contents.hasInterface())
            {
                ObjectContents.Interface interface_msg =
                    obj_contents.getInterface();
                ReferenceType ref_type = interface_msg.getRefType();
                
                String initial_endpt_uuid_reference = null;
                if (ref_type.hasReference())
                    initial_endpt_uuid_reference = ref_type.getReference();

                Variables.AtomicInterfaceVariable to_return =
                    new Variables.AtomicInterfaceVariable(false,ralph_globals);

                to_return.set_initial_reference(
                    initial_endpt_uuid_reference);
                return to_return;
            }
            else if (obj_contents.hasMapType())
            {                
                ObjectContents.Map map = obj_contents.getMapType();
                // FIXME: check for null
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
                // FIXME: check for null
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
                // FIXME: check for null
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
                ObjectContents.InternalStruct internal_struct_contents =
                    obj_contents.getInternalStructType();

                // Creating an internal struct in a very hackish way:
                // creating an atomic struct and then reaching into it
                // to get internal struct.
                IAtomicStructWrapperBaseClassFactory factory =
                    ContainerFactorySingleton.instance.get_atomic_struct_wrapper_base_class_factory(
                        internal_struct_contents.getStructTypeClassName());
                if (factory == null)
                {
                    Util.logger_assert(
                        "No factory to contents deserialize internalstruct.");
                }

                StructWrapperBaseClass to_return_wrapper =
                    factory.construct(ralph_globals);

                InternalStructBaseClass internal_struct =
                    (InternalStructBaseClass) to_return_wrapper.val.val;

                List<String> internal_references_to_replay_on =
                    new ArrayList<String> ();
                for (ObjectContents.InternalStructField struct_field :
                         internal_struct_contents.getFieldsList())
                {
                    internal_references_to_replay_on.add(
                        struct_field.getFieldContentsReference().getReference());
                }
                internal_struct.set_internal_references_to_replay_on(
                    internal_references_to_replay_on);
                return internal_struct;
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

                if (val_type.hasNum() || val_type.hasNullNum())
                {
                    Double to_set_to =
                        val_type.hasNum() ? val_type.getNum() : null;
                    return new Variables.NonAtomicNumberVariable(
                        false,to_set_to, ralph_globals);
                }
                else if (val_type.hasText() || val_type.hasNullText())
                {
                    String to_set_to =
                        val_type.hasText() ? val_type.getText() : null;
                    return new Variables.NonAtomicTextVariable(
                        false,to_set_to,ralph_globals);
                }
                else if (val_type.hasTf() || val_type.hasNullTf())
                {
                    Boolean to_set_to =
                        val_type.hasTf() ? val_type.getTf() : null;
                    return new Variables.NonAtomicTrueFalseVariable(
                        false,to_set_to,ralph_globals);
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
            else if (obj_contents.hasInterface())
            {
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic " +
                    "interface.");
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