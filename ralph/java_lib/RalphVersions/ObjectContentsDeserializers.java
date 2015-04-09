package RalphVersions;

import java.util.List;
import java.util.ArrayList;

import com.google.protobuf.ByteString;

import ralph.RalphGlobals;
import ralph.RalphObject;
import ralph.Util;
import ralph.Variables;
import ralph.ContainerFactorySingleton;
import ralph.IMapVariableFactory;
import ralph.IListVariableFactory;
import ralph.IAtomicStructWrapperBaseClassFactory;
import ralph.StructWrapperBaseClass;
import ralph.InternalStructBaseClass;
import ralph.InternalServiceFactory;
import ralph.InternalServiceReference;
import ralph.EnumConstructorObj;
import ralph.IReference;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.DeltaProto.Delta;
import ralph_protobuffs.DeltaProto.Delta.ReferenceType;
import ralph_protobuffs.DeltaProto.Delta.ValueType;

public class ObjectContentsDeserializers
{
    public static RalphObject deserialize(
        ObjectContents obj_contents, RalphGlobals ralph_globals,
        IReconstructionContext reconstruction_context)
    {
        // See note on top of force_initialization in
        // BaseMapVariableFactory.
        ralph.BaseMapVariableFactory.instance.force_initialization();
        ralph.BaseListVariableFactory.instance.force_initialization();


        boolean is_atomic = obj_contents.getAtomic();
        if (obj_contents.hasValType())
        {
            ValueType val_type = obj_contents.getValType();

            if (val_type.hasNum() || val_type.hasNullNum())
            {
                Double to_set_to =
                    val_type.hasNum() ? val_type.getNum() : null;
                if (is_atomic)
                {
                    return new Variables.AtomicNumberVariable(
                        false,to_set_to,ralph_globals);
                }
                return new Variables.NonAtomicNumberVariable(
                    false,to_set_to,ralph_globals);
            }
            else if (val_type.hasText() || val_type.hasNullText())
            {
                String to_set_to =
                    val_type.hasText() ? val_type.getText() : null;
                if (is_atomic)
                {
                    return new Variables.AtomicTextVariable(
                        false,to_set_to,ralph_globals);
                }
                return new Variables.NonAtomicTextVariable(
                    false,to_set_to,ralph_globals);
            }
            else if (val_type.hasTf() || val_type.hasNullTf())
            {
                Boolean to_set_to =
                    val_type.hasTf() ? val_type.getTf() : null;

                if (is_atomic)
                {
                    return new Variables.AtomicTrueFalseVariable(
                        false,to_set_to,ralph_globals);
                }
                return new Variables.NonAtomicTrueFalseVariable(
                    false,to_set_to,ralph_globals);
            }
            //// DEBUG
            Util.logger_assert(
                "Unknown how to deserialize atomic val type.");
            //// END DEBUG
        }
        else if (obj_contents.hasEnumType())
        {
            if (! is_atomic)
            {
                // FIXME: still must allow deserializing non-atomic
                // enums
                Util.logger_assert(
                    "FIXME: must allow deserializing non-atomic enums");
            }
            
            ObjectContents.Enum enum_msg = obj_contents.getEnumType();
            String enum_constructor_obj_class_name =
                enum_msg.getEnumConstructorObjClassName();
            int ordinal = enum_msg.getEnumOrdinal();

            IVersionReplayer version_replayer =
                reconstruction_context.get_version_replayer();
            EnumConstructorObj enum_constructor =
                version_replayer.get_enum_constructor_obj(
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

            if (is_atomic)
            {
                Variables.AtomicInterfaceVariable to_return =
                    new Variables.AtomicInterfaceVariable(false,ralph_globals);

                to_return.set_initial_reference(
                    initial_endpt_uuid_reference);
                return to_return;
            }
            Variables.NonAtomicInterfaceVariable to_return =
                    new Variables.NonAtomicInterfaceVariable(false,ralph_globals);
            
            to_return.set_initial_reference(
                initial_endpt_uuid_reference);
            return to_return;
        }
        else if (obj_contents.hasMapType())
        {
            ObjectContents.Map map = obj_contents.getMapType();
            // FIXME: check for null
            String initial_reference = map.getRefType().getReference();
            IMapVariableFactory factory =
                ContainerFactorySingleton.instance.get_atomic_map_variable_factory(
                    map.getKeyTypeClassName(),map.getValTypeClassName());
            if (factory == null)
            {
                Util.logger_assert(
                    "No factory to contents deserialize map.");
            }
            if (is_atomic)
            {                
                Variables.AtomicMapVariable to_return =
                    factory.construct_atomic(ralph_globals);
                to_return.set_initial_reference(initial_reference);
                return to_return;
            }
            Variables.NonAtomicMapVariable to_return =
                    factory.construct_non_atomic(ralph_globals);
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
            IMapVariableFactory factory =
                ContainerFactorySingleton.instance.get_atomic_map_variable_factory(
                    internal_map.getKeyTypeClassName(),
                    internal_map.getValTypeClassName());
            if (factory == null)
            {
                Util.logger_assert(
                    "No factory to contents deserialize internalmap.");
            }

            if (is_atomic)
            {
                Variables.AtomicMapVariable to_return_wrapper =
                    factory.construct_atomic(ralph_globals);
                return (RalphObject) to_return_wrapper.val.val;
            }
            Variables.NonAtomicMapVariable to_return_wrapper =
                factory.construct_non_atomic(ralph_globals);
            return (RalphObject) to_return_wrapper.val.val;
        }
        else if (obj_contents.hasInternalListType())
        {
            ObjectContents.InternalList internal_list =
                obj_contents.getInternalListType();

            // Creating an internal list in a very hackish way:
            // creating an atomic list and then reaching into it to
            // get internal list.                
            IListVariableFactory factory =
                ContainerFactorySingleton.instance.get_list_variable_factory(
                    internal_list.getValTypeClassName());
            if (factory == null)
            {
                Util.logger_assert(
                    "No factory to contents deserialize internallist.");
            }

            if (is_atomic)
            {
                Variables.AtomicListVariable to_return_wrapper =
                    factory.construct_atomic(ralph_globals);
                return (RalphObject) to_return_wrapper.val.val;
            }
            Variables.NonAtomicListVariable to_return_wrapper =
                factory.construct_non_atomic(ralph_globals);
            return (RalphObject) to_return_wrapper.val.val;
        }
        else if (obj_contents.hasListType())
        {
            ObjectContents.List list = obj_contents.getListType();
            // FIXME: check for null
            String initial_reference = list.getRefType().getReference();
            IListVariableFactory factory =
                ContainerFactorySingleton.instance.get_list_variable_factory(
                    list.getValTypeClassName());
            if (factory == null)
            {
                Util.logger_assert(
                    "No factory to contents deserialize list.");
            }
            if (is_atomic)
            {
                Variables.AtomicListVariable to_return =
                    factory.construct_atomic(ralph_globals);
                to_return.set_initial_reference(initial_reference);
                return to_return;
            }
            Variables.NonAtomicListVariable to_return =
                factory.construct_non_atomic(ralph_globals);
            to_return.set_initial_reference(initial_reference);
            return to_return;
        }
        else if (obj_contents.hasStructType())
        {
            if (! is_atomic)
            {
                Util.logger_assert(
                    "FIXME: disallowing deserializing non-atomic lists");
            }
            ObjectContents.Struct struct = obj_contents.getStructType();

            Delta.ReferenceType ref_type = struct.getRefType();
            String initial_reference = null;
            if (ref_type.hasReference())
                initial_reference = struct.getRefType().getReference();

            
            IAtomicStructWrapperBaseClassFactory factory =
                ContainerFactorySingleton.instance.get_atomic_struct_wrapper_base_class_factory(
                    struct.getStructTypeClassName());
            if (factory == null)
            {
                Util.logger_assert(
                    "No factory to contents deserialize struct.");
            }
            StructWrapperBaseClass to_return = null;
            if (initial_reference == null)
            {
                to_return =
                    factory.construct_null_internal(ralph_globals);
            }
            else
            {
                // using null here because when replaying shouldn't
                // durably re-log.
                to_return = factory.construct(ralph_globals,null,null);
            }
            
            to_return.set_initial_reference(initial_reference);
            return to_return;
        }
        else if (obj_contents.hasInternalStructType())
        {
            if (! is_atomic)
            {
                Util.logger_assert(
                    "FIXME: disallowing deserializing non-atomic " +
                    "internal structs");
            }
            
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

            // using null here because when replaying shouldn't
            // durably re-log.
            StructWrapperBaseClass to_return_wrapper =
                factory.construct(ralph_globals,null,null);

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
        else if (obj_contents.hasServiceFactoryType())
        {
            InternalServiceFactory internal_service_factory = null;
            ByteString byte_string =
                obj_contents.getServiceFactoryType().getSerializedFactory();
            if (! byte_string.isEmpty())
            {
                internal_service_factory =
                    InternalServiceFactory.deserialize (
                        byte_string, ralph_globals);
            }
            
            if (obj_contents.getAtomic())
            {
                Variables.AtomicServiceFactoryVariable to_return =
                    new Variables.AtomicServiceFactoryVariable(
                        false,internal_service_factory,ralph_globals);
                return to_return;
            }
            Variables.NonAtomicServiceFactoryVariable to_return =
                new Variables.NonAtomicServiceFactoryVariable(
                        false,internal_service_factory,ralph_globals);
            return to_return;
        }
        else if (obj_contents.hasServiceReferenceType())
        {
            InternalServiceReference internal_service_reference = null;

            Delta.ServiceReferenceDelta service_reference_delta =
                obj_contents.getServiceReferenceType();

            // internal service reference is null if missing fields.
            if (service_reference_delta.hasRemoteHostUuid())
            {
                String remote_host_uuid = service_reference_delta.getRemoteHostUuid();
                String service_uuid = service_reference_delta.getServiceUuid();
                internal_service_reference =
                    new InternalServiceReference(remote_host_uuid, service_uuid);
            }

            if (obj_contents.getAtomic())
            {
                return new Variables.AtomicServiceReferenceVariable(
                    false,internal_service_reference,ralph_globals);
            }
            
            return new Variables.NonAtomicServiceReferenceVariable(
                false,internal_service_reference,ralph_globals);
        }
        

        Util.logger_assert("Unknown object contents deserialization.");
        return null;        
    }
}