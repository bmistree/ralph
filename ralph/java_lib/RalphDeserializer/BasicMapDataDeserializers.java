package RalphDeserializer;

import java.util.List;

import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.AtomicMap;
import ralph.NonAtomicMap;
import ralph_protobuffs.VariablesProto;
import static ralph.Variables.AtomicMapVariable;
import static ralph.Variables.NonAtomicMapVariable;
import static ralph.Variables.NonAtomicNumberVariable;
import static ralph.Variables.NonAtomicTextVariable;
import static ralph.Variables.NonAtomicTrueFalseVariable;
import ralph.NonAtomicInternalMap;
import RalphAtomicWrappers.BaseAtomicWrappers;
import ralph.Util;
import ralph.ActiveEvent;
import static RalphDeserializer.Deserializer.dummy_deserialization_active_event;
import RalphAtomicWrappers.EnsureAtomicWrapper;

import ralph.InternalMapTypeVersionHelper;

import static ralph.BaseTypeVersionHelpers.DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER;


public class BasicMapDataDeserializers
{
    // only used to force populating internal static fields
    private final static BasicMapDataDeserializers instance =
        new BasicMapDataDeserializers();
    protected BasicMapDataDeserializers()
    {}
    public static BasicMapDataDeserializers get_instance()
    {
        return instance;
    }

    // Note: Atomic maps still have non-atomic internal elements.
    // number indices
    private final static AtomMapDeserializer<Double,Double> atom_num_num_map_constructor =
        new AtomMapDeserializer<Double,Double>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Double.class,Double.class);
    private final static AtomMapDeserializer<Double,String> atom_num_text_map_constructor =
        new AtomMapDeserializer<Double,String>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Double.class,String.class);
    private final static AtomMapDeserializer<Double,Boolean> atom_num_tf_map_constructor =
        new AtomMapDeserializer<Double,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Double.class,Boolean.class);
    // text indices
    private final static AtomMapDeserializer<String,Double> atom_text_num_map_constructor =
        new AtomMapDeserializer<String,Double>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            String.class,Double.class);
    private final static AtomMapDeserializer<String,String> atom_text_text_map_constructor =
        new AtomMapDeserializer<String,String>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            String.class,String.class);
    private final static AtomMapDeserializer<String,Boolean> atom_text_tf_map_constructor =
        new AtomMapDeserializer<String,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            String.class,Boolean.class);
    // tf indices
    private final static AtomMapDeserializer<Boolean,Double> atom_tf_num_map_constructor =
        new AtomMapDeserializer<Boolean,Double>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Boolean.class,Double.class);
    private final static AtomMapDeserializer<Boolean,String> atom_tf_text_map_constructor =
        new AtomMapDeserializer<Boolean,String>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Boolean.class,String.class);
    private final static AtomMapDeserializer<Boolean,Boolean> atom_tf_tf_map_constructor =
        new AtomMapDeserializer<Boolean,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Boolean.class,Boolean.class);
    

    // NonAtomic map deserializers
    
    // number indices
    private final static NonAtomMapDeserializer<Double,Double> non_atom_num_num_map_constructor =
        new NonAtomMapDeserializer<Double,Double>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Double.class,Double.class);
    private final static NonAtomMapDeserializer<Double,String> non_atom_num_text_map_constructor =
        new NonAtomMapDeserializer<Double,String>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Double.class,String.class);
    private final static NonAtomMapDeserializer<Double,Boolean> non_atom_num_tf_map_constructor =
        new NonAtomMapDeserializer<Double,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Double.class,Boolean.class);
    // text indices
    private final static NonAtomMapDeserializer<String,Double> non_atom_text_num_map_constructor =
        new NonAtomMapDeserializer<String,Double>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            String.class,Double.class);
    private final static NonAtomMapDeserializer<String,String> non_atom_text_text_map_constructor =
        new NonAtomMapDeserializer<String,String>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            String.class,String.class);
    private final static NonAtomMapDeserializer<String,Boolean> non_atom_text_tf_map_constructor =
        new NonAtomMapDeserializer<String,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            String.class,Boolean.class);
    // tf indices
    private final static NonAtomMapDeserializer<Boolean,Double> non_atom_tf_num_map_constructor =
        new NonAtomMapDeserializer<Boolean,Double>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Boolean.class,Double.class);
    private final static NonAtomMapDeserializer<Boolean,String> non_atom_tf_text_map_constructor =
        new NonAtomMapDeserializer<Boolean,String>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Boolean.class,String.class);
    private final static NonAtomMapDeserializer<Boolean,Boolean> non_atom_tf_tf_map_constructor =
        new NonAtomMapDeserializer<Boolean,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER,
            Boolean.class,Boolean.class);
        
    
    public static class AtomMapDeserializer<IndexType,ValueType>
        implements DataDeserializer
    {
        private final EnsureAtomicWrapper wrapper;
        private final NonAtomicInternalMap.IndexType index_type;

        private final Class<IndexType> index_type_class;
        private final Class<ValueType> value_type_class;
        private final InternalMapTypeVersionHelper<IndexType>
            internal_version_helper;
        
        public AtomMapDeserializer(
            String index_label,String value_label,EnsureAtomicWrapper _wrapper,
            NonAtomicInternalMap.IndexType _index_type,
            InternalMapTypeVersionHelper<IndexType> internal_version_helper,
            Class<IndexType> index_type_class,
            Class<ValueType> value_type_class)
        {
            this.internal_version_helper = internal_version_helper;
            this.index_type_class = index_type_class;
            this.value_type_class = value_type_class;
            
            wrapper = _wrapper;
            index_type = _index_type;
            Deserializer deserializer =
                Deserializer.get_instance();
            
            String pre_label = deserializer.merge_labels(
                AtomicMap.deserialization_label,
                index_label);
            String label = deserializer.merge_labels(pre_label,value_label);
            deserializer.register(label,this);
        }

        @Override
        public RalphObject deserialize(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            AtomicMapVariable<IndexType,ValueType,ValueType> outer_map =
                new AtomicMapVariable<IndexType,ValueType,ValueType>(
                    false,index_type,wrapper,internal_version_helper,
                    index_type_class,value_type_class,ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();
            
            //// DEBUG
            if ((! any.hasMap()) || (! any.getIsTvar()))
                Util.logger_assert("Cannot deserialize map without map field");
            //// END DEBUG

            VariablesProto.Variables.Map map_message = any.getMap();
            List<VariablesProto.Variables.Any> any_map_indices =
                map_message.getMapIndicesList();
            List<VariablesProto.Variables.Any> any_map_values =
                map_message.getMapValuesList();

            
            Deserializer deserializer =
                Deserializer.get_instance();
            for (int i = 0; i < any_map_indices.size(); ++i)
            {
                VariablesProto.Variables.Any map_index_message =
                    any_map_indices.get(i);
                VariablesProto.Variables.Any map_value_message =
                    any_map_values.get(i);

                RalphObject index_object = 
                    deserializer.deserialize(map_index_message,ralph_globals);
                RalphObject value_object = 
                    deserializer.deserialize(map_value_message,ralph_globals);
                try
                {
                    outer_map.get_val(evt).set_val_on_key(
                        evt,
                        (IndexType)index_object.get_val(evt),
                        (ValueType)value_object.get_val(evt));
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing map");
                }
            }

            try
            {
                to_return = outer_map.get_val(null);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing map");
            }
            

            // return internal map
            return to_return;
        }
    }



    // Non atomic deserializer
    public static class NonAtomMapDeserializer<IndexType,ValueType>
        implements DataDeserializer
    {
        private final EnsureAtomicWrapper wrapper;
        private final NonAtomicInternalMap.IndexType index_type;
        
        private final Class<IndexType> index_type_class;
        private final Class<ValueType> value_type_class;

        private final InternalMapTypeVersionHelper<IndexType>
            internal_version_helper;
        
        public NonAtomMapDeserializer(
            String index_label,String value_label,EnsureAtomicWrapper _wrapper,
            NonAtomicInternalMap.IndexType _index_type,
            InternalMapTypeVersionHelper<IndexType> internal_version_helper,
            Class<IndexType> _index_type_class,
            Class<ValueType> _value_type_class)
        {
            this.internal_version_helper = internal_version_helper;
            wrapper = _wrapper;
            index_type_class = _index_type_class;
            value_type_class = _value_type_class;
            
            index_type = _index_type;
            Deserializer deserializer =
                Deserializer.get_instance();
            
            String pre_label = deserializer.merge_labels(
                NonAtomicMap.deserialization_label,
                index_label);
            String label = deserializer.merge_labels(pre_label,value_label);
            deserializer.register(label,this);
        }

        @Override
        public RalphObject deserialize(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            NonAtomicMapVariable<IndexType,ValueType,ValueType> outer_map =
                new NonAtomicMapVariable<IndexType,ValueType,ValueType>(
                    false,index_type,wrapper,internal_version_helper,
                    index_type_class,value_type_class,ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();
            
            //// DEBUG
            if ((! any.hasMap()) || any.getIsTvar())
                Util.logger_assert("Cannot deserialize map without map field");
            //// END DEBUG

            VariablesProto.Variables.Map map_message = any.getMap();
            List<VariablesProto.Variables.Any> any_map_indices =
                map_message.getMapIndicesList();
            List<VariablesProto.Variables.Any> any_map_values =
                map_message.getMapValuesList();
            
            Deserializer deserializer =
                Deserializer.get_instance();
            for (int i = 0; i < any_map_indices.size(); ++i)
            {
                VariablesProto.Variables.Any map_index_message =
                    any_map_indices.get(i);
                VariablesProto.Variables.Any map_value_message =
                    any_map_values.get(i);

                RalphObject index_object = 
                    deserializer.deserialize(map_index_message,ralph_globals);
                RalphObject value_object = 
                    deserializer.deserialize(map_value_message,ralph_globals);
                try
                {
                    outer_map.get_val(evt).set_val_on_key(
                        evt,
                        (IndexType)index_object.get_val(evt),
                        (ValueType)value_object.get_val(evt));
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing map");
                }
            }

            try
            {
                to_return = outer_map.get_val(null);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing map");
            }
            

            // return internal map
            return to_return;
        }
    }    
}