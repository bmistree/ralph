package RalphVersions;

import ralph_local_version_protobuffs.DeltaProto.Delta;
import ralph_local_version_protobuffs.DeltaProto.Delta.ValueType;

public class ObjectToValueType
{
    public final static DoubleToValueType DOUBLE_SERIALIZER =
        new DoubleToValueType();
    public final static StringToValueType STRING_SERIALIZER =
        new StringToValueType();
    public final static BooleanToValueType BOOLEAN_SERIALIZER =
        new BooleanToValueType();
    

    protected static class DoubleToValueType
        implements ILocalValueTypeSerializer<Double>
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

    protected static class StringToValueType
        implements ILocalValueTypeSerializer<String>
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
        implements ILocalValueTypeSerializer<Boolean>
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
