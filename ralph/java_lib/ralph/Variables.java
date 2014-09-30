package ralph;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

import ralph_protobuffs.VariablesProto;
import ralph_protobuffs.UtilProto.UUID;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;

import RalphDataWrappers.NumberTypeDataWrapperFactory;
import RalphDataWrappers.TextTypeDataWrapperFactory;
import RalphDataWrappers.TrueFalseTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ServiceFactoryTypeDataWrapperFactory;
import RalphDataWrappers.ServiceReferenceTypeDataWrapperFactory;

import static ralph.BaseTypeVersionHelpers.DOUBLE_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.STRING_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.BOOLEAN_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.REFERENCE_VERSION_HELPER;


import static ralph.BaseTypeVersionHelpers.INTERFACE_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.ENUM_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.SERVICE_FACTORY_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.SERVICE_REFERENCE_VERSION_HELPER;

import RalphVersions.ObjectHistory;
import RalphVersions.IReconstructionContext;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_local_version_protobuffs.DeltaProto.Delta;

public class Variables
{
    public final static NumberTypeDataWrapperFactory
        number_value_type_data_wrapper_factory =
        new NumberTypeDataWrapperFactory();
    final static Double default_number = new Double(0.0);

    final static TextTypeDataWrapperFactory
        text_value_type_data_wrapper_factory =
        new TextTypeDataWrapperFactory();
    
    final static String default_text = new String();
	
    final static TrueFalseTypeDataWrapperFactory
        true_false_value_type_data_wrapper_factory = 
        new TrueFalseTypeDataWrapperFactory();
    
    final static Boolean default_tf = false;
	
    final static ServiceFactoryTypeDataWrapperFactory
        service_factory_value_type_data_wrapper_factory = 
        new ServiceFactoryTypeDataWrapperFactory();

    final static InternalServiceFactory default_service_factory = null;

    final static ServiceReferenceTypeDataWrapperFactory
        service_reference_value_type_data_wrapper_factory = 
        new ServiceReferenceTypeDataWrapperFactory();

    final static InternalServiceReference default_service_reference = null;

    /**
       @param held_reference --- Can be null if pointing at null.
     */
    public static ObjectContents serialize_reference (
        IReference held_reference,boolean atomic,String holder_uuid)
    {
        Delta.ReferenceType.Builder reference_type_builder =
            Delta.ReferenceType.newBuilder();
        if (held_reference != null)
            reference_type_builder.setReference(held_reference.uuid());
        
        ObjectContents.Builder contents_builder =
            ObjectContents.newBuilder();
        contents_builder.setRefType(reference_type_builder);
        contents_builder.setUuid(holder_uuid);
        contents_builder.setAtomic(atomic);
        return contents_builder.build();
    }

    /**
       @param internal_uuid --- Can be null if pointing at null.
     */
    public static ObjectContents serialize_struct_reference(
        String holder_uuid,String internal_uuid, String struct_class_name,
        boolean atomic)
    {
        Delta.ReferenceType.Builder ref_type_builder =
            Delta.ReferenceType.newBuilder();
        if (internal_uuid != null)
            ref_type_builder.setReference(internal_uuid);

        ObjectContents.Struct.Builder struct_builder =
            ObjectContents.Struct.newBuilder();
        struct_builder.setRefType(ref_type_builder);
        struct_builder.setStructTypeClassName(struct_class_name);

        ObjectContents.Builder contents_builder =
            ObjectContents.newBuilder();
        contents_builder.setUuid(holder_uuid);
        contents_builder.setAtomic(atomic);
        contents_builder.setStructType(struct_builder);
        return contents_builder.build();
    }
    
    
    /** Atomics */
    
    public static class AtomicNumberVariable
        extends AtomicValueVariable<Double>
    {
        public AtomicNumberVariable(
            boolean _log_changes,Double init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,init_val,
                number_value_type_data_wrapper_factory,
                DOUBLE_VERSION_HELPER,ralph_globals);
        }

        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_number(this,obj_history,to_play_until);
        }
        
        /**
           @param {ActiveEvent} active_event --- Can be null, in which
           case will return internal value without taking any locks.
         */
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            return AtomicNumberVariable.serialize_num_contents(
                active_event,this,true);
        }

        public static ObjectContents serialize_num_contents(
            ActiveEvent active_event,RalphObject<Double,Double> ralph_object,
            boolean atomic) throws BackoutException
        {
            Double contents = ralph_object.get_val(active_event);
            Delta.ValueType.Builder value_type_builder =
                Delta.ValueType.newBuilder();

            if (contents == null)
                value_type_builder.setNullNum(true);
            else
                value_type_builder.setNum(contents.doubleValue());
            
            ObjectContents.Builder contents_builder =
                ObjectContents.newBuilder();
            contents_builder.setValType(value_type_builder);
            contents_builder.setUuid(ralph_object.uuid());
            contents_builder.setAtomic(atomic);
            return contents_builder.build();
        }
        
        @Override
        protected SpeculativeAtomicObject<Double,Double>
            duplicate_for_speculation(Double to_speculate_on)
        {
            SpeculativeAtomicObject<Double,Double> to_return =
                new AtomicNumberVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
        
        public AtomicNumberVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_number,
                number_value_type_data_wrapper_factory,DOUBLE_VERSION_HELPER,
                ralph_globals);
        }
        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder) throws BackoutException
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setIsTvar(true);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    public static class AtomicTextVariable
        extends AtomicValueVariable<String>
    {
        public AtomicTextVariable(
            boolean _log_changes,String init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,init_val,
                text_value_type_data_wrapper_factory,STRING_VERSION_HELPER,
                ralph_globals);
        }
        public AtomicTextVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_text,
                text_value_type_data_wrapper_factory,STRING_VERSION_HELPER,
                ralph_globals);
        }

        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_text(this,obj_history,to_play_until);
        }
        
        /**
           @param {ActiveEvent} active_event --- Can be null, in which
           case will return internal value without taking any locks.
         */
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            return AtomicTextVariable.serialize_text_contents(
                active_event,this,true);
        }

        public static ObjectContents serialize_text_contents(
            ActiveEvent active_event,RalphObject<String,String> ralph_object,
            boolean atomic) throws BackoutException
        {
            String contents = ralph_object.get_val(active_event);
            Delta.ValueType.Builder value_type_builder =
                Delta.ValueType.newBuilder();
            if (contents == null)
                value_type_builder.setNullText(true);
            else
                value_type_builder.setText(contents);
            
            ObjectContents.Builder contents_builder =
                ObjectContents.newBuilder();
            contents_builder.setValType(value_type_builder);
            contents_builder.setUuid(ralph_object.uuid());
            contents_builder.setAtomic(atomic);
            return contents_builder.build();
        }

        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setIsTvar(true);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
        
        @Override
        protected SpeculativeAtomicObject<String,String>
            duplicate_for_speculation(String to_speculate_on)
        {
            SpeculativeAtomicObject<String,String> to_return =
                new AtomicTextVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
    }
	
    public static class AtomicTrueFalseVariable
        extends AtomicValueVariable<Boolean>
    {
        public AtomicTrueFalseVariable(
            boolean _log_changes,Boolean init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,init_val,
                true_false_value_type_data_wrapper_factory,BOOLEAN_VERSION_HELPER,
                ralph_globals);
        }
        public AtomicTrueFalseVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_tf,
                true_false_value_type_data_wrapper_factory,BOOLEAN_VERSION_HELPER,
                ralph_globals);
        }

        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_tf(this,obj_history,to_play_until);
        }

        /**
           @param {ActiveEvent} active_event --- Can be null, in which
           case will return internal value without taking any locks.
         */
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            return
                AtomicTrueFalseVariable.serialize_true_false_contents(
                    active_event,this,true);
        }
        
        public static ObjectContents serialize_true_false_contents(
            ActiveEvent active_event,RalphObject<Boolean,Boolean> ralph_object,
            boolean atomic) throws BackoutException
        {
            Boolean contents = ralph_object.get_val(active_event);
            Delta.ValueType.Builder value_type_builder =
                Delta.ValueType.newBuilder();
            
            if (contents == null)
                value_type_builder.setNullTf(true);
            else
                value_type_builder.setTf(contents.booleanValue());
            
            ObjectContents.Builder contents_builder =
                ObjectContents.newBuilder();
            contents_builder.setValType(value_type_builder);
            contents_builder.setUuid(ralph_object.uuid());
            contents_builder.setAtomic(atomic);
            return contents_builder.build();
        }


        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setIsTvar(true);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
        
        @Override
        protected SpeculativeAtomicObject<Boolean,Boolean>
            duplicate_for_speculation(Boolean to_speculate_on)
        {
            SpeculativeAtomicObject<Boolean,Boolean> to_return =
                new AtomicTrueFalseVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
    }

    public static class AtomicEnumVariable<T extends Enum>
        extends AtomicValueVariable<T>
    {
        protected final EnumConstructorObj<T> enum_constructor_obj;
        
        public AtomicEnumVariable(
            boolean _log_changes,T init_val,
            EnumConstructorObj<T> _enum_constructor_obj,
            VersionHelper<T> version_helper,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,init_val,
                new ValueTypeDataWrapperFactory<T>(),version_helper,
                ralph_globals,_enum_constructor_obj);
            enum_constructor_obj = _enum_constructor_obj;
        }
        public AtomicEnumVariable(
            boolean _log_changes,
            EnumConstructorObj<T> _enum_constructor_obj,
            VersionHelper<T> version_helper,
            RalphGlobals ralph_globals)
        {
            this(
                _log_changes,null,_enum_constructor_obj,version_helper,
                ralph_globals);
        }

        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.<T>replay_enum(
                this,obj_history,to_play_until,
                reconstruction_context.get_local_version_replayer());
        }

        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            T internal_val = get_val(active_event);
            int ordinal = -1;
            if (internal_val != null)
                ordinal = internal_val.ordinal();
            
            EnumConstructorObj<T> constructor_obj = enum_constructor_obj;
            if (constructor_obj == null)
                constructor_obj = (EnumConstructorObj<T>)additional_contents;
            
            return AtomicEnumVariable.<T>serialize_enum_contents(
                ordinal,constructor_obj,uuid(),true);
        }

        /**
           internal_ordinal is -1, if null.
         */
        public static <EnumType extends Enum>
            ObjectContents serialize_enum_contents(
                int internal_ordinal,
                EnumConstructorObj<EnumType> enum_constructor_obj,
                String holder_uuid,boolean atomic)
        {
            ObjectContents.Enum.Builder enum_builder =
                ObjectContents.Enum.newBuilder();
            enum_builder.setEnumConstructorObjClassName(
                enum_constructor_obj.getClass().getName());
            enum_builder.setEnumOrdinal(internal_ordinal);


            ObjectContents.Builder object_contents_builder =
                ObjectContents.newBuilder();
            object_contents_builder.setUuid(holder_uuid);
            object_contents_builder.setAtomic(atomic);
            object_contents_builder.setEnumType(enum_builder);
            return object_contents_builder.build();
        }
        
        @Override
        protected SpeculativeAtomicObject<T,T>
            duplicate_for_speculation(T to_speculate_on)
        {
            SpeculativeAtomicObject<T,T> to_return =
                new AtomicEnumVariable(
                    log_changes,to_speculate_on,enum_constructor_obj,
                    version_helper,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            // FIXME: Should allow serializing enum types across
            // network.
            Util.logger_assert(
                "FIXME: Should allow serializing enums across network.");
        }
    }
    
    public static class AtomicInterfaceVariable<T extends IReference>
        extends AtomicValueVariable<T>
    {
        public AtomicInterfaceVariable(
            boolean _log_changes,T init_val, RalphGlobals ralph_globals)
        {
            super(
                _log_changes,init_val,
                new ValueTypeDataWrapperFactory<T>(),INTERFACE_VERSION_HELPER,
                ralph_globals);
        }
        public AtomicInterfaceVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,null,
                new ValueTypeDataWrapperFactory<T>(),INTERFACE_VERSION_HELPER,
                ralph_globals);
        }
        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            return AtomicInterfaceVariable.serialize_interface_contents(
                get_val(active_event),uuid(),true);
        }

        public static ObjectContents serialize_interface_contents(
            IReference internal_endpoint,String holder_uuid,boolean atomic)
        {
            Delta.ReferenceType.Builder ref_type_builder =
                Delta.ReferenceType.newBuilder();
            
            if (internal_endpoint != null)
                ref_type_builder.setReference(internal_endpoint.uuid());

            ObjectContents.Interface.Builder interface_builder =
                ObjectContents.Interface.newBuilder();
            interface_builder.setRefType(ref_type_builder);
            
            ObjectContents.Builder contents_builder =
                ObjectContents.newBuilder();
            contents_builder.setInterface(interface_builder);
            contents_builder.setUuid(holder_uuid);
            contents_builder.setAtomic(atomic);
            return contents_builder.build();
        }
        
        @Override
        protected SpeculativeAtomicObject<T,T>
            duplicate_for_speculation(T to_speculate_on)
        {
            SpeculativeAtomicObject<T,T> to_return =
                new AtomicInterfaceVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            Util.logger_assert(
                "Cannot pass reference to interface across network.");
        }
    }
    
    public static class AtomicServiceFactoryVariable
        extends AtomicValueVariable<InternalServiceFactory>
    {
        public AtomicServiceFactoryVariable(
            boolean _log_changes,Object init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,(InternalServiceFactory)init_val,
                service_factory_value_type_data_wrapper_factory,
                SERVICE_FACTORY_VERSION_HELPER,ralph_globals);
        }
        public AtomicServiceFactoryVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_service_factory,
                service_factory_value_type_data_wrapper_factory,
                SERVICE_FACTORY_VERSION_HELPER,ralph_globals);
        }

        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            // FIXME: add code for serializing service factories variables.
            Util.logger_assert(
                "FIXME: fill in serialization for service factories");
            return null;
        }
        
        @Override
        protected
            // return type
            SpeculativeAtomicObject<
                InternalServiceFactory,InternalServiceFactory>
            // method name and arguments
            duplicate_for_speculation(InternalServiceFactory to_speculate_on)
        {
            SpeculativeAtomicObject<InternalServiceFactory,InternalServiceFactory>
                to_return =
                    new AtomicServiceFactoryVariable(
                        log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            InternalServiceFactory internal_val = get_val(active_event);
            serialize_service_factory(internal_val,any_builder);
            any_builder.setIsTvar(true);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }

        /**
           Can be used by both atomic and non-atomic service factories.
         */
        public static void serialize_service_factory (
            InternalServiceFactory internal_val,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            any_builder.setVarName("");
            try {
                any_builder.setServiceFactory(
                    com.google.protobuf.ByteString.copyFrom(
                        internal_val.convert_constructor_to_byte_array()));
            } catch (IOException ex) {
                ex.printStackTrace();
                Util.logger_assert(
                    "\nUnhandled IOException when serializing " +
                    "service factory.\n");
            }
        }
    }
    
    public static class AtomicServiceReferenceVariable
        extends AtomicValueVariable<InternalServiceReference>
    {
        public AtomicServiceReferenceVariable(
            boolean _log_changes,Object init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,(InternalServiceReference)init_val,
                service_reference_value_type_data_wrapper_factory,
                SERVICE_REFERENCE_VERSION_HELPER,ralph_globals);
        }
        public AtomicServiceReferenceVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_service_reference,
                service_reference_value_type_data_wrapper_factory,
                SERVICE_REFERENCE_VERSION_HELPER,ralph_globals);
        }

        @Override
        protected SpeculativeAtomicObject<InternalServiceReference,InternalServiceReference>
            duplicate_for_speculation(InternalServiceReference to_speculate_on)
        {
            SpeculativeAtomicObject<InternalServiceReference,InternalServiceReference> to_return =
                new AtomicServiceReferenceVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            InternalServiceReference internal_val = get_val(active_event);
            serialize_service_reference(internal_val,any_builder);
            any_builder.setIsTvar(true);
        }

        
        /**
           Can be used by both atomic and non-atomic service factories.
         */
        public static void serialize_service_reference (
            InternalServiceReference internal_val,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            any_builder.setVarName("");

            UUID.Builder service_uuid = UUID.newBuilder();
            service_uuid.setData(internal_val.service_uuid);
            
            VariablesProto.Variables.ServiceReference.Builder service_reference =
                VariablesProto.Variables.ServiceReference.newBuilder();
            
            service_reference.setIpAddr(internal_val.ip_addr);
            service_reference.setTcpPort(internal_val.tcp_port);
            service_reference.setServiceUuid(service_uuid);

            any_builder.setServiceReference(service_reference);
        }
        
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }

        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            // FIXME: add code for serializing service reference variables.
            Util.logger_assert(
                "FIXME: fill in serialization for service references");
            return null;
        }
    }
    
    public static class NonAtomicNumberVariable
        extends NonAtomicValueVariable<Double>
    {
        public NonAtomicNumberVariable(
            boolean _dummy_log_changes, Double init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                number_value_type_data_wrapper_factory,DOUBLE_VERSION_HELPER,
                ralph_globals);
        }
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            return AtomicNumberVariable.serialize_num_contents(
                active_event,this,false);
        }

        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_number(this,obj_history,to_play_until);
        }
        
        public NonAtomicNumberVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_number,
                number_value_type_data_wrapper_factory,DOUBLE_VERSION_HELPER,
                ralph_globals);
        }
        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setIsTvar(false);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    public static class NonAtomicTextVariable
        extends NonAtomicValueVariable<String>
    {
        public NonAtomicTextVariable(
            boolean _dummy_log_changes,String init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                text_value_type_data_wrapper_factory,STRING_VERSION_HELPER,
                ralph_globals);
        }
		
        public NonAtomicTextVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_text,
                text_value_type_data_wrapper_factory,STRING_VERSION_HELPER,
                ralph_globals);
        }

        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_text(this,obj_history,to_play_until);
        }
        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            return AtomicTextVariable.serialize_text_contents(
                active_event,this,false);
        }
        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setIsTvar(false);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    public static class NonAtomicTrueFalseVariable
        extends NonAtomicValueVariable<Boolean>
    {
        public NonAtomicTrueFalseVariable(
            boolean _dummy_log_changes, Boolean init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                true_false_value_type_data_wrapper_factory,BOOLEAN_VERSION_HELPER,
                ralph_globals);
        }

        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            return AtomicTrueFalseVariable.serialize_true_false_contents(
                active_event,this,false);
        }

        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_tf(this,obj_history,to_play_until);
        }
        
        public NonAtomicTrueFalseVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_tf,
                true_false_value_type_data_wrapper_factory,BOOLEAN_VERSION_HELPER,
                ralph_globals);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setIsTvar(false);
        }

        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    public static class NonAtomicEnumVariable<T extends Enum>
        extends NonAtomicValueVariable<T>
    {
        protected final EnumConstructorObj<T> enum_constructor_obj;
        
        public NonAtomicEnumVariable(
            boolean _dummy_log_changes, T init_val,
            EnumConstructorObj<T> _enum_constructor_obj,
            VersionHelper<T> version_helper,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                new ValueTypeDataWrapperFactory<T>(),version_helper,
                ralph_globals);
            enum_constructor_obj = _enum_constructor_obj;
        }

        public NonAtomicEnumVariable(
            boolean _dummy_log_changes,
            EnumConstructorObj<T> _enum_constructor_obj,
            VersionHelper<T> version_helper,
            RalphGlobals ralph_globals)
        {
            this(
                false,null,_enum_constructor_obj, version_helper,
                ralph_globals);
        }

        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            T internal_val = get_val(active_event);
            int ordinal = -1;
            if (internal_val != null)
                ordinal = internal_val.ordinal();

            return AtomicEnumVariable.<T>serialize_enum_contents(
                ordinal,enum_constructor_obj,uuid(),false);
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            // FIXME: Should allow serializing enums across network.
            any_builder.setIsTvar(false);
            Util.logger_assert(
                "FIXME: Should allow serializing enum variables across network");
        }
    }
    
    public static class NonAtomicInterfaceVariable<T extends IReference>
        extends NonAtomicValueVariable<T>
    {
        public NonAtomicInterfaceVariable(
            boolean _dummy_log_changes, T init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                new ValueTypeDataWrapperFactory<T>(),INTERFACE_VERSION_HELPER,
                ralph_globals);
        }

        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            return AtomicInterfaceVariable.serialize_interface_contents(
                get_val(active_event),uuid(),false);
        }
        
        public NonAtomicInterfaceVariable(
            boolean _dummy_log_changes, RalphGlobals ralph_globals)
        {
            super(
                null,new ValueTypeDataWrapperFactory<T>(),
                INTERFACE_VERSION_HELPER, ralph_globals);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            any_builder.setIsTvar(false);
            Util.logger_assert(
                "Cannot pass interface reference over network.");
        }
    }
    
    public static class NonAtomicServiceFactoryVariable
        extends NonAtomicValueVariable<InternalServiceFactory>
    {
        public NonAtomicServiceFactoryVariable(
            boolean _log_changes,InternalServiceFactory init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                service_factory_value_type_data_wrapper_factory,
                SERVICE_FACTORY_VERSION_HELPER,ralph_globals);
        }
        public NonAtomicServiceFactoryVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_service_factory,
                service_factory_value_type_data_wrapper_factory,
                SERVICE_FACTORY_VERSION_HELPER,ralph_globals);
        }

        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            // FIXME: add code for serializing service factories variables.
            Util.logger_assert(
                "FIXME: fill in serialization for service factories");
            return null;
        }
        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            InternalServiceFactory internal_val = get_val(active_event);
            AtomicServiceFactoryVariable.serialize_service_factory(
                internal_val,any_builder);
            any_builder.setIsTvar(false);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }


    public static class NonAtomicServiceReferenceVariable
        extends NonAtomicValueVariable<InternalServiceReference>
    {
        public NonAtomicServiceReferenceVariable(
            boolean _log_changes,InternalServiceReference init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                service_reference_value_type_data_wrapper_factory,
                SERVICE_REFERENCE_VERSION_HELPER,ralph_globals);
        }
        public NonAtomicServiceReferenceVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_service_reference,
                service_reference_value_type_data_wrapper_factory,
                SERVICE_REFERENCE_VERSION_HELPER,ralph_globals);
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            InternalServiceReference internal_val = get_val(active_event);
            AtomicServiceReferenceVariable.serialize_service_reference(
                internal_val,any_builder);
            any_builder.setIsTvar(false);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents)
            throws BackoutException
        {
            // FIXME: add code for serializing service reference variables.
            Util.logger_assert(
                "FIXME: fill in serialization for service references");
            return null;
        }
        
    }

    
    /************ Handling maps ********/
    //non-atomic
    public static class NonAtomicMapVariable <K,V,ValueDeltaType>
        extends NonAtomicMap<K,V,ValueDeltaType>
    {
        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
            InternalContainerTypeVersionHelper<K> internal_version_helper,
            Class<K> key_type_class, Class<V> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                _index_type,locked_wrapper,REFERENCE_VERSION_HELPER,
                internal_version_helper,key_type_class, value_type_class,
                ralph_globals);
        }

        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalMap<K,V,ValueDeltaType> internal_val,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
            InternalContainerTypeVersionHelper<K> internal_version_helper,
            Class<K> key_type_class, Class<V> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                internal_val,_index_type,locked_wrapper,REFERENCE_VERSION_HELPER,
                key_type_class, value_type_class,ralph_globals);
        }
    }

    // atomic
    public static class AtomicMapVariable <K,V,ValueDeltaType>
        extends AtomicMap<K,V,ValueDeltaType>
    {
        public AtomicMapVariable(
            boolean _log_changes,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
            InternalContainerTypeVersionHelper<K> internal_version_helper,
            Class<K> key_type_class, Class<V> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,_index_type,locked_wrapper,
                REFERENCE_VERSION_HELPER,internal_version_helper,
                key_type_class, value_type_class,ralph_globals);
        }

        public AtomicMapVariable(
            boolean _log_changes,
            AtomicInternalMap<K,V,ValueDeltaType> internal_val,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
            InternalContainerTypeVersionHelper<K> internal_version_helper,
            Class<K> key_type_class, Class<V> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,internal_val,_index_type,locked_wrapper,
                REFERENCE_VERSION_HELPER,key_type_class,value_type_class,
                ralph_globals);
        }
    }
    
    /************ Handling Lists ********/
    public static class AtomicListVariable <ValueType,ValueDeltaType>
        extends AtomicList<ValueType,ValueDeltaType>
    {
        public AtomicListVariable(
            boolean _log_changes,
            EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper,
            Class<ValueType> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,locked_wrapper,REFERENCE_VERSION_HELPER,
                value_type_class,ralph_globals);
        }


        public AtomicListVariable(
            boolean _log_changes,
            AtomicInternalList<ValueType,ValueDeltaType> internal_val,
            EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper,
            Class<ValueType> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,internal_val,locked_wrapper,REFERENCE_VERSION_HELPER,
                value_type_class, ralph_globals);
        }
    }

    public static class NonAtomicListVariable <ValueType,ValueDeltaType>
        extends NonAtomicList<ValueType,ValueDeltaType>
    {
        public NonAtomicListVariable(
            boolean _dummy_log_changes,
            EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper,
            Class<ValueType> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                locked_wrapper,REFERENCE_VERSION_HELPER,
                value_type_class,ralph_globals);
        }

        public NonAtomicListVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalList<ValueType,ValueDeltaType> internal_val,
            EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper,
            Class<ValueType> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                internal_val,locked_wrapper,REFERENCE_VERSION_HELPER,
                value_type_class,ralph_globals);
        }
    }
}
