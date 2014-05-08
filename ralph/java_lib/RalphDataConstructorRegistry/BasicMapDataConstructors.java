package RalphDataConstructorRegistry;

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
import static RalphDataConstructorRegistry.DataConstructorRegistry.dummy_deserialization_active_event;
import RalphAtomicWrappers.EnsureAtomicWrapper;


public class BasicMapDataConstructors
{
    // only used to force populating internal static fields
    private final static BasicMapDataConstructors instance =
        new BasicMapDataConstructors();
    protected BasicMapDataConstructors()
    {}
    public static BasicMapDataConstructors get_instance()
    {
        return instance;
    }

    // Note: Atomic maps still have non-atomic internal elements.
    // number indices
    private final static AtomMapConstructor<Double,Double> atom_num_num_map_constructor =
        new AtomMapConstructor<Double,Double>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE);
    private final static AtomMapConstructor<Double,String> atom_num_text_map_constructor =
        new AtomMapConstructor<Double,String>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE);
    private final static AtomMapConstructor<Double,Boolean> atom_num_tf_map_constructor =
        new AtomMapConstructor<Double,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE);
    // text indices
    private final static AtomMapConstructor<String,Double> atom_text_num_map_constructor =
        new AtomMapConstructor<String,Double>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING);
    private final static AtomMapConstructor<String,String> atom_text_text_map_constructor =
        new AtomMapConstructor<String,String>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING);
    private final static AtomMapConstructor<String,Boolean> atom_text_tf_map_constructor =
        new AtomMapConstructor<String,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING);
    // tf indices
    private final static AtomMapConstructor<Boolean,Double> atom_tf_num_map_constructor =
        new AtomMapConstructor<Boolean,Double>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN);
    private final static AtomMapConstructor<Boolean,String> atom_tf_text_map_constructor =
        new AtomMapConstructor<Boolean,String>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN);
    private final static AtomMapConstructor<Boolean,Boolean> atom_tf_tf_map_constructor =
        new AtomMapConstructor<Boolean,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN);
    

    // NonAtomic map deserializers
    
    // number indices
    private final static NonAtomMapConstructor<Double,Double> non_atom_num_num_map_constructor =
        new NonAtomMapConstructor<Double,Double>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE);
    private final static NonAtomMapConstructor<Double,String> non_atom_num_text_map_constructor =
        new NonAtomMapConstructor<Double,String>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE);
    private final static NonAtomMapConstructor<Double,Boolean> non_atom_num_tf_map_constructor =
        new NonAtomMapConstructor<Double,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.DOUBLE);
    // text indices
    private final static NonAtomMapConstructor<String,Double> non_atom_text_num_map_constructor =
        new NonAtomMapConstructor<String,Double>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING);
    private final static NonAtomMapConstructor<String,String> non_atom_text_text_map_constructor =
        new NonAtomMapConstructor<String,String>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING);
    private final static NonAtomMapConstructor<String,Boolean> non_atom_text_tf_map_constructor =
        new NonAtomMapConstructor<String,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.STRING);
    // tf indices
    private final static NonAtomMapConstructor<Boolean,Double> non_atom_tf_num_map_constructor =
        new NonAtomMapConstructor<Boolean,Double>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN);
    private final static NonAtomMapConstructor<Boolean,String> non_atom_tf_text_map_constructor =
        new NonAtomMapConstructor<Boolean,String>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN);
    private final static NonAtomMapConstructor<Boolean,Boolean> non_atom_tf_tf_map_constructor =
        new NonAtomMapConstructor<Boolean,Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
            NonAtomicInternalMap.IndexType.BOOLEAN);
        
    
    public static class AtomMapConstructor<IndexType,ValueType>
        implements DataConstructor
    {
        private final EnsureAtomicWrapper wrapper;
        private final NonAtomicInternalMap.IndexType index_type;
        
        public AtomMapConstructor(
            String index_label,String value_label,EnsureAtomicWrapper _wrapper,
            NonAtomicInternalMap.IndexType _index_type)
        {
            wrapper = _wrapper;
            index_type = _index_type;
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String pre_label = deserializer.merge_labels(
                AtomicMap.deserialization_label,
                index_label);
            String label = deserializer.merge_labels(pre_label,value_label);
            deserializer.register(label,this);
        }

        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            AtomicMapVariable<IndexType,ValueType,ValueType> outer_map =
                new AtomicMapVariable<IndexType,ValueType,ValueType>(
                    false,index_type,wrapper,ralph_globals);
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

            
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
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
    public static class NonAtomMapConstructor<IndexType,ValueType>
        implements DataConstructor
    {
        private final EnsureAtomicWrapper wrapper;
        private final NonAtomicInternalMap.IndexType index_type;
        
        public NonAtomMapConstructor(
            String index_label,String value_label,EnsureAtomicWrapper _wrapper,
            NonAtomicInternalMap.IndexType _index_type)
        {
            wrapper = _wrapper;
            index_type = _index_type;
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String pre_label = deserializer.merge_labels(
                NonAtomicMap.deserialization_label,
                index_label);
            String label = deserializer.merge_labels(pre_label,value_label);
            deserializer.register(label,this);
        }

        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            NonAtomicMapVariable<IndexType,ValueType,ValueType> outer_map =
                new NonAtomicMapVariable<IndexType,ValueType,ValueType>(
                    false,index_type,wrapper,ralph_globals);
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
            
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
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