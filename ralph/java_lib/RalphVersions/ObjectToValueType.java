package RalphVersions;

import ralph_protobuffs.DeltaProto.Delta;
import ralph_protobuffs.DeltaProto.Delta.ValueType;

public class ObjectToValueType
{
    public final static DoubleToValueType DOUBLE_SERIALIZER =
        new DoubleToValueType();
    public final static IntegerToValueType INTEGER_SERIALIZER =
        new IntegerToValueType();
    public final static StringToValueType STRING_SERIALIZER =
        new StringToValueType();
    public final static BooleanToValueType BOOLEAN_SERIALIZER =
        new BooleanToValueType();
    

    protected static class DoubleToValueType
        implements IValueTypeSerializer<Double>
    {
        @Override
        public ValueType serialize_value_type(Double to_serialize)
        {
            Delta.ValueType.Builder value =
                Delta.ValueType.newBuilder();
            value.setNum(to_serialize.doubleValue());
            return value.build();
        }
    }

    // FIXME: serializes down to double because have no real integer
    // type in the language.
    protected static class IntegerToValueType
        implements IValueTypeSerializer<Integer>
    {
        @Override
        public ValueType serialize_value_type(Integer to_serialize)
        {
            return DOUBLE_SERIALIZER.serialize_value_type(
                to_serialize.doubleValue());
        }
    }

    
    protected static class StringToValueType
        implements IValueTypeSerializer<String>
    {
        @Override
        public ValueType serialize_value_type(String to_serialize)
        {
            Delta.ValueType.Builder value =
                Delta.ValueType.newBuilder();
            value.setText(to_serialize);
            return value.build();
        }
    }

    protected static class BooleanToValueType
        implements IValueTypeSerializer<Boolean>
    {
        @Override
        public ValueType serialize_value_type(Boolean to_serialize)
        {
            Delta.ValueType.Builder value =
                Delta.ValueType.newBuilder();
            value.setTf(to_serialize.booleanValue());
            return value.build();
        }
    }
}
