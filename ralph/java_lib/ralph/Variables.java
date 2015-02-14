package ralph;

import com.google.protobuf.ByteString;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

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
import static ralph.BaseTypeVersionHelpers.SERVICE_FACTORY_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.SERVICE_REFERENCE_VERSION_HELPER;

import RalphVersions.ObjectHistory;
import RalphVersions.IReconstructionContext;
import RalphVersions.VersionUtil;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.DeltaProto.Delta;

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

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_number(
                this,obj_history,to_play_until,deserialization_event);
        }

        
        /**
           @param {ActiveEvent} active_event --- Can be null, in which
           case will return internal value without taking any locks.
         */
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
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

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_text(
                this,obj_history,to_play_until,
                deserialization_event);
        }

        
        /**
           @param {ActiveEvent} active_event --- Can be null, in which
           case will return internal value without taking any locks.
         */
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
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

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_tf(
                this,obj_history,to_play_until,
                deserialization_event);
        }

        
        /**
           @param {ActiveEvent} active_event --- Can be null, in which
           case will return internal value without taking any locks.
         */
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
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
                reconstruction_context.get_version_replayer());
        }

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.<T>deserialize_enum(
                this,obj_history,to_play_until,
                reconstruction_context.get_version_replayer(),
                deserialization_event);
        }
        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
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
    }
    
    public static class AtomicInterfaceVariable<T extends IReference>
        extends AtomicValueVariable<T> 
        implements IInternalReferenceHolder
    {
        /**
           When we are replaying reference variables, we first must
           construct them.  Then we replay what they were pointing to.
           This field should hold the name of the reference that this
           object was pointing to when it was constructed.  
         */
        private String initial_endpt_uuid_reference = null;
        private boolean initial_endpt_uuid_reference_set = false;
        
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

        /*** IInitialReferenceHolder methods */
        @Override
        public String get_initial_reference()
        {
            return initial_endpt_uuid_reference;
        }

        @Override
        public void set_initial_reference(String new_initial_reference)
        {
            initial_endpt_uuid_reference = new_initial_reference;
            initial_endpt_uuid_reference_set = true;
        }
        @Override
        public boolean get_initial_reference_set()
        {
            return initial_endpt_uuid_reference_set;
        }
        
        public static
            <InterfaceVariableType extends RalphObject<IReference, IReference> & IInternalReferenceHolder>
            void interface_replay(
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history, Long to_play_until,
            InterfaceVariableType interface_variable,
            RalphGlobals ralph_globals)
        {
            String endpt_uuid_reference_to_use =
                ObjectHistory.find_reference(obj_history,to_play_until);
            
            if (endpt_uuid_reference_to_use == null)
            {
                // means that we never changed from using the initial
                // reference that was provided.
                //// DEBUG
                if (! interface_variable.get_initial_reference_set())
                {
                    Util.logger_assert(
                        "Error: no idea where to replay reference from.");
                }
                //// END DEBUG
                endpt_uuid_reference_to_use =
                    interface_variable.get_initial_reference();
            }

            IReference internal_val = null;
            if (endpt_uuid_reference_to_use != null)
            {
                // can == null, if internal endpt was initialized to
                // null.
                internal_val = VersionUtil.rebuild_endpoint(
                    endpt_uuid_reference_to_use,ralph_globals,
                    reconstruction_context,to_play_until);
            }
            interface_variable.direct_set_val(internal_val);
        }
        
        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            AtomicInterfaceVariable.<AtomicInterfaceVariable>interface_replay(
                reconstruction_context,obj_history,to_play_until,this,
                ralph_globals);
        }

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            Util.logger_assert(
                "Disallowing serializing endpoints across network.");
        }

        
        /** AtomicValueVariable overrides */
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
            throws BackoutException
        {
            return AtomicInterfaceVariable.serialize_interface_contents(
                get_val(active_event),uuid(),true,serialization_context);
        }

        public static ObjectContents serialize_interface_contents(
            IReference internal_endpoint,String holder_uuid,boolean atomic,
            SerializationContext serialization_context)
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
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
            throws BackoutException
        {
            return AtomicServiceFactoryVariable.service_factory_serialize_contents(
                active_event, serialization_context,this,true);
        }

        public static ObjectContents service_factory_serialize_contents(
            ActiveEvent active_event,SerializationContext serialization_context,
            RalphObject<InternalServiceFactory,InternalServiceFactory> service_factory_holder,
            boolean is_atomic) throws BackoutException
        {
            InternalServiceFactory internal_val =
                service_factory_holder.get_val(active_event);

            ByteString internal_byte_string = null;
            if (internal_val != null)
            {
                try
                {
                    internal_byte_string =
                        internal_val.convert_constructor_to_byte_string();
                }
                catch(IOException ex)
                {
                    // The only time this happens is if we get an
                    // IOException when copying internal service
                    // factory.
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Not handling IOException thrown from copying " +
                        "object contents when serializing internal " +
                        "service factory.");
                }
            }
            else
            {
                // null byte string is an empty byte array.
                internal_byte_string.copyFrom(new byte[0]);
            }

            Delta.ServiceFactoryDelta.Builder service_factory_delta_builder =
                Delta.ServiceFactoryDelta.newBuilder();
            service_factory_delta_builder.setSerializedFactory(
                internal_byte_string);

            ObjectContents.Builder obj_contents_builder =
                ObjectContents.newBuilder();

            obj_contents_builder.setServiceFactoryType(
                service_factory_delta_builder);
            obj_contents_builder.setAtomic(is_atomic);
            obj_contents_builder.setUuid(service_factory_holder.uuid());
            return obj_contents_builder.build();
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
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_service_factory(
                this,obj_history,to_play_until,ralph_globals);
        }
        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_service_factory(
                this,obj_history,to_play_until,ralph_globals,
                deserialization_event);
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
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_service_reference(
                this,obj_history,to_play_until, ralph_globals);
        }

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_service_reference(
                this,obj_history,to_play_until, ralph_globals,
                deserialization_event);
        }

        public static ObjectContents serialize_service_reference_contents(
            ActiveEvent active_event,
            RalphObject<InternalServiceReference,InternalServiceReference> service_reference_holder,
            boolean is_atomic)
            throws BackoutException
        {
            // FIXME: likely expensive.  Should serialize references
            // instead of full contents.
            InternalServiceReference internal_service_reference =
                service_reference_holder.get_val(active_event);

            
            Delta.ServiceReferenceDelta.Builder service_reference_builder =
                Delta.ServiceReferenceDelta.newBuilder();

            if (internal_service_reference != null)
            {
                service_reference_builder.setIpAddr(
                    internal_service_reference.ip_addr);
                service_reference_builder.setTcpPort(
                    internal_service_reference.tcp_port);
                service_reference_builder.setServiceUuid(
                    internal_service_reference.service_uuid);
            }

            ObjectContents.Builder object_contents_builder =
                ObjectContents.newBuilder();
            object_contents_builder.setServiceReferenceType(
                service_reference_builder);
            object_contents_builder.setUuid(service_reference_holder.uuid());
            object_contents_builder.setAtomic(is_atomic);

            return object_contents_builder.build();
        }

        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
            throws BackoutException
        {
            return AtomicServiceReferenceVariable.serialize_service_reference_contents(
                active_event,this,true);
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
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
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

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_number(
                this,obj_history,to_play_until,deserialization_event);
        }

        
        public NonAtomicNumberVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_number,
                number_value_type_data_wrapper_factory,DOUBLE_VERSION_HELPER,
                ralph_globals);
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
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_text(
                this,obj_history,to_play_until,deserialization_event);
        }

        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
            throws BackoutException
        {
            return AtomicTextVariable.serialize_text_contents(
                active_event,this,false);
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
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
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

        @Override
        public void deserialize(
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_tf(
                this,obj_history,to_play_until,deserialization_event);
        }

        
        public NonAtomicTrueFalseVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_tf,
                true_false_value_type_data_wrapper_factory,BOOLEAN_VERSION_HELPER,
                ralph_globals);
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
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            Util.logger_assert(
                "FIXME: still must allow replay of NonAtomicEnum-s");
        }

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            Util.logger_assert(
                "FIXME: still must allow deserialize of NonAtomicEnum-s");
        }

        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
            throws BackoutException
        {
            T internal_val = get_val(active_event);
            int ordinal = -1;
            if (internal_val != null)
                ordinal = internal_val.ordinal();

            return AtomicEnumVariable.<T>serialize_enum_contents(
                ordinal,enum_constructor_obj,uuid(),false);
        }
    }
    
    public static class NonAtomicInterfaceVariable<T extends IReference>
        extends NonAtomicValueVariable<T>
        implements IInternalReferenceHolder
    {
        /**
           When we are replaying reference variables, we first must
           construct them.  Then we replay what they were pointing to.
           This field should hold the name of the reference that this
           object was pointing to when it was constructed.  
         */
        private String initial_endpt_uuid_reference = null;
        private boolean initial_endpt_uuid_reference_set = false;

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
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
            throws BackoutException
        {
            return AtomicInterfaceVariable.serialize_interface_contents(
                get_val(active_event),uuid(),false,serialization_context);
        }
        
        public NonAtomicInterfaceVariable(
            boolean _dummy_log_changes, RalphGlobals ralph_globals)
        {
            super(
                null,new ValueTypeDataWrapperFactory<T>(),
                INTERFACE_VERSION_HELPER, ralph_globals);
        }

        @Override
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            AtomicInterfaceVariable.<NonAtomicInterfaceVariable>interface_replay(
                reconstruction_context,obj_history,to_play_until,this,
                ralph_globals);
        }

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            Util.logger_assert(
                "Should not allow serializing and deserializing endpoints");
        }

        
        /**
           IInternalReferenceHolders
         */
        @Override
        public String get_initial_reference()
        {
            return initial_endpt_uuid_reference;
        }

        @Override
        public void set_initial_reference(String new_initial_reference)
        {
            initial_endpt_uuid_reference = new_initial_reference;
            initial_endpt_uuid_reference_set = true;
        }
        @Override
        public boolean get_initial_reference_set()
        {
            return initial_endpt_uuid_reference_set;
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
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_service_factory(
                this,obj_history,to_play_until,ralph_globals);
        }

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_service_factory(
                this,obj_history,to_play_until,ralph_globals,
                deserialization_event);
        }

        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
            throws BackoutException
        {
            return AtomicServiceFactoryVariable.service_factory_serialize_contents(
                active_event, serialization_context,this,false);
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
        public void replay (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until)
        {
            ObjectHistory.replay_service_reference(
                this,obj_history,to_play_until, ralph_globals);
        }

        @Override
        public void deserialize (
            IReconstructionContext reconstruction_context,
            ObjectHistory obj_history,Long to_play_until,
            ActiveEvent deserialization_event) throws BackoutException
        {
            ObjectHistory.deserialize_service_reference(
                this,obj_history,to_play_until, ralph_globals,
                deserialization_event);
        }

        
        @Override
        public ObjectContents serialize_contents(
            ActiveEvent active_event, Object additional_contents,
            SerializationContext serialization_context)
            throws BackoutException
        {
            return AtomicServiceReferenceVariable.serialize_service_reference_contents(
                active_event,this,false);
        }        
    }

    
    /************ Handling maps ********/
    //non-atomic
    public static class NonAtomicMapVariable <K,V,ValueDeltaType>
        extends NonAtomicMap<K,V,ValueDeltaType>
    {
        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
            InternalContainerTypeVersionHelper<K> internal_version_helper,
            Class<K> key_type_class, Class<V> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                locked_wrapper,REFERENCE_VERSION_HELPER,
                internal_version_helper,key_type_class, value_type_class,
                ralph_globals);
        }

        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalMap<K,V,ValueDeltaType> internal_val,
            EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
            InternalContainerTypeVersionHelper<K> internal_version_helper,
            Class<K> key_type_class, Class<V> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                internal_val,locked_wrapper,REFERENCE_VERSION_HELPER,
                key_type_class, value_type_class,ralph_globals);
        }
    }

    // atomic
    public static class AtomicMapVariable <K,V,ValueDeltaType>
        extends AtomicMap<K,V,ValueDeltaType>
    {
        public AtomicMapVariable(
            boolean _log_changes,
            EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
            InternalContainerTypeVersionHelper<K> internal_version_helper,
            Class<K> key_type_class, Class<V> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,locked_wrapper,REFERENCE_VERSION_HELPER,
                internal_version_helper,key_type_class, value_type_class,
                ralph_globals);
        }

        public AtomicMapVariable(
            boolean _log_changes,
            AtomicInternalMap<K,V,ValueDeltaType> internal_val,
            EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
            InternalContainerTypeVersionHelper<K> internal_version_helper,
            Class<K> key_type_class, Class<V> value_type_class,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,internal_val,locked_wrapper,
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
