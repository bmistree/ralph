// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: object_contents.proto

package ralph_local_version_protobuffs;

public final class ObjectContentsProto {
  private ObjectContentsProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface ObjectContentsOrBuilder
      extends com.google.protobuf.MessageOrBuilder {

    // required string uuid = 1;
    /**
     * <code>required string uuid = 1;</code>
     */
    boolean hasUuid();
    /**
     * <code>required string uuid = 1;</code>
     */
    java.lang.String getUuid();
    /**
     * <code>required string uuid = 1;</code>
     */
    com.google.protobuf.ByteString
        getUuidBytes();

    // required bool atomic = 2;
    /**
     * <code>required bool atomic = 2;</code>
     */
    boolean hasAtomic();
    /**
     * <code>required bool atomic = 2;</code>
     */
    boolean getAtomic();

    // optional .Delta.ValueType val_type = 3;
    /**
     * <code>optional .Delta.ValueType val_type = 3;</code>
     */
    boolean hasValType();
    /**
     * <code>optional .Delta.ValueType val_type = 3;</code>
     */
    ralph_local_version_protobuffs.DeltaProto.Delta.ValueType getValType();
    /**
     * <code>optional .Delta.ValueType val_type = 3;</code>
     */
    ralph_local_version_protobuffs.DeltaProto.Delta.ValueTypeOrBuilder getValTypeOrBuilder();

    // optional .Delta.ReferenceType ref_type = 4;
    /**
     * <code>optional .Delta.ReferenceType ref_type = 4;</code>
     */
    boolean hasRefType();
    /**
     * <code>optional .Delta.ReferenceType ref_type = 4;</code>
     */
    ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType getRefType();
    /**
     * <code>optional .Delta.ReferenceType ref_type = 4;</code>
     */
    ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceTypeOrBuilder getRefTypeOrBuilder();
  }
  /**
   * Protobuf type {@code ObjectContents}
   */
  public static final class ObjectContents extends
      com.google.protobuf.GeneratedMessage
      implements ObjectContentsOrBuilder {
    // Use ObjectContents.newBuilder() to construct.
    private ObjectContents(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private ObjectContents(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final ObjectContents defaultInstance;
    public static ObjectContents getDefaultInstance() {
      return defaultInstance;
    }

    public ObjectContents getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private ObjectContents(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      initFields();
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              bitField0_ |= 0x00000001;
              uuid_ = input.readBytes();
              break;
            }
            case 16: {
              bitField0_ |= 0x00000002;
              atomic_ = input.readBool();
              break;
            }
            case 26: {
              ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.Builder subBuilder = null;
              if (((bitField0_ & 0x00000004) == 0x00000004)) {
                subBuilder = valType_.toBuilder();
              }
              valType_ = input.readMessage(ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(valType_);
                valType_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000004;
              break;
            }
            case 34: {
              ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.Builder subBuilder = null;
              if (((bitField0_ & 0x00000008) == 0x00000008)) {
                subBuilder = refType_.toBuilder();
              }
              refType_ = input.readMessage(ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(refType_);
                refType_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000008;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e.getMessage()).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return ralph_local_version_protobuffs.ObjectContentsProto.internal_static_ObjectContents_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return ralph_local_version_protobuffs.ObjectContentsProto.internal_static_ObjectContents_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents.class, ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents.Builder.class);
    }

    public static com.google.protobuf.Parser<ObjectContents> PARSER =
        new com.google.protobuf.AbstractParser<ObjectContents>() {
      public ObjectContents parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new ObjectContents(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<ObjectContents> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    // required string uuid = 1;
    public static final int UUID_FIELD_NUMBER = 1;
    private java.lang.Object uuid_;
    /**
     * <code>required string uuid = 1;</code>
     */
    public boolean hasUuid() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required string uuid = 1;</code>
     */
    public java.lang.String getUuid() {
      java.lang.Object ref = uuid_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          uuid_ = s;
        }
        return s;
      }
    }
    /**
     * <code>required string uuid = 1;</code>
     */
    public com.google.protobuf.ByteString
        getUuidBytes() {
      java.lang.Object ref = uuid_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        uuid_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    // required bool atomic = 2;
    public static final int ATOMIC_FIELD_NUMBER = 2;
    private boolean atomic_;
    /**
     * <code>required bool atomic = 2;</code>
     */
    public boolean hasAtomic() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required bool atomic = 2;</code>
     */
    public boolean getAtomic() {
      return atomic_;
    }

    // optional .Delta.ValueType val_type = 3;
    public static final int VAL_TYPE_FIELD_NUMBER = 3;
    private ralph_local_version_protobuffs.DeltaProto.Delta.ValueType valType_;
    /**
     * <code>optional .Delta.ValueType val_type = 3;</code>
     */
    public boolean hasValType() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    /**
     * <code>optional .Delta.ValueType val_type = 3;</code>
     */
    public ralph_local_version_protobuffs.DeltaProto.Delta.ValueType getValType() {
      return valType_;
    }
    /**
     * <code>optional .Delta.ValueType val_type = 3;</code>
     */
    public ralph_local_version_protobuffs.DeltaProto.Delta.ValueTypeOrBuilder getValTypeOrBuilder() {
      return valType_;
    }

    // optional .Delta.ReferenceType ref_type = 4;
    public static final int REF_TYPE_FIELD_NUMBER = 4;
    private ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType refType_;
    /**
     * <code>optional .Delta.ReferenceType ref_type = 4;</code>
     */
    public boolean hasRefType() {
      return ((bitField0_ & 0x00000008) == 0x00000008);
    }
    /**
     * <code>optional .Delta.ReferenceType ref_type = 4;</code>
     */
    public ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType getRefType() {
      return refType_;
    }
    /**
     * <code>optional .Delta.ReferenceType ref_type = 4;</code>
     */
    public ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceTypeOrBuilder getRefTypeOrBuilder() {
      return refType_;
    }

    private void initFields() {
      uuid_ = "";
      atomic_ = false;
      valType_ = ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.getDefaultInstance();
      refType_ = ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.getDefaultInstance();
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;

      if (!hasUuid()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasAtomic()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeBytes(1, getUuidBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeBool(2, atomic_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeMessage(3, valType_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        output.writeMessage(4, refType_);
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, getUuidBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBoolSize(2, atomic_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(3, valType_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(4, refType_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }

    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code ObjectContents}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements ralph_local_version_protobuffs.ObjectContentsProto.ObjectContentsOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return ralph_local_version_protobuffs.ObjectContentsProto.internal_static_ObjectContents_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return ralph_local_version_protobuffs.ObjectContentsProto.internal_static_ObjectContents_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents.class, ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents.Builder.class);
      }

      // Construct using ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
          getValTypeFieldBuilder();
          getRefTypeFieldBuilder();
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        uuid_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        atomic_ = false;
        bitField0_ = (bitField0_ & ~0x00000002);
        if (valTypeBuilder_ == null) {
          valType_ = ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.getDefaultInstance();
        } else {
          valTypeBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        if (refTypeBuilder_ == null) {
          refType_ = ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.getDefaultInstance();
        } else {
          refTypeBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000008);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return ralph_local_version_protobuffs.ObjectContentsProto.internal_static_ObjectContents_descriptor;
      }

      public ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents getDefaultInstanceForType() {
        return ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents.getDefaultInstance();
      }

      public ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents build() {
        ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents buildPartial() {
        ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents result = new ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.uuid_ = uuid_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.atomic_ = atomic_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        if (valTypeBuilder_ == null) {
          result.valType_ = valType_;
        } else {
          result.valType_ = valTypeBuilder_.build();
        }
        if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
          to_bitField0_ |= 0x00000008;
        }
        if (refTypeBuilder_ == null) {
          result.refType_ = refType_;
        } else {
          result.refType_ = refTypeBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents) {
          return mergeFrom((ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents other) {
        if (other == ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents.getDefaultInstance()) return this;
        if (other.hasUuid()) {
          bitField0_ |= 0x00000001;
          uuid_ = other.uuid_;
          onChanged();
        }
        if (other.hasAtomic()) {
          setAtomic(other.getAtomic());
        }
        if (other.hasValType()) {
          mergeValType(other.getValType());
        }
        if (other.hasRefType()) {
          mergeRefType(other.getRefType());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (!hasUuid()) {
          
          return false;
        }
        if (!hasAtomic()) {
          
          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      // required string uuid = 1;
      private java.lang.Object uuid_ = "";
      /**
       * <code>required string uuid = 1;</code>
       */
      public boolean hasUuid() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required string uuid = 1;</code>
       */
      public java.lang.String getUuid() {
        java.lang.Object ref = uuid_;
        if (!(ref instanceof java.lang.String)) {
          java.lang.String s = ((com.google.protobuf.ByteString) ref)
              .toStringUtf8();
          uuid_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>required string uuid = 1;</code>
       */
      public com.google.protobuf.ByteString
          getUuidBytes() {
        java.lang.Object ref = uuid_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          uuid_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>required string uuid = 1;</code>
       */
      public Builder setUuid(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        uuid_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required string uuid = 1;</code>
       */
      public Builder clearUuid() {
        bitField0_ = (bitField0_ & ~0x00000001);
        uuid_ = getDefaultInstance().getUuid();
        onChanged();
        return this;
      }
      /**
       * <code>required string uuid = 1;</code>
       */
      public Builder setUuidBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        uuid_ = value;
        onChanged();
        return this;
      }

      // required bool atomic = 2;
      private boolean atomic_ ;
      /**
       * <code>required bool atomic = 2;</code>
       */
      public boolean hasAtomic() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>required bool atomic = 2;</code>
       */
      public boolean getAtomic() {
        return atomic_;
      }
      /**
       * <code>required bool atomic = 2;</code>
       */
      public Builder setAtomic(boolean value) {
        bitField0_ |= 0x00000002;
        atomic_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required bool atomic = 2;</code>
       */
      public Builder clearAtomic() {
        bitField0_ = (bitField0_ & ~0x00000002);
        atomic_ = false;
        onChanged();
        return this;
      }

      // optional .Delta.ValueType val_type = 3;
      private ralph_local_version_protobuffs.DeltaProto.Delta.ValueType valType_ = ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.getDefaultInstance();
      private com.google.protobuf.SingleFieldBuilder<
          ralph_local_version_protobuffs.DeltaProto.Delta.ValueType, ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.Builder, ralph_local_version_protobuffs.DeltaProto.Delta.ValueTypeOrBuilder> valTypeBuilder_;
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      public boolean hasValType() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      public ralph_local_version_protobuffs.DeltaProto.Delta.ValueType getValType() {
        if (valTypeBuilder_ == null) {
          return valType_;
        } else {
          return valTypeBuilder_.getMessage();
        }
      }
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      public Builder setValType(ralph_local_version_protobuffs.DeltaProto.Delta.ValueType value) {
        if (valTypeBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          valType_ = value;
          onChanged();
        } else {
          valTypeBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      public Builder setValType(
          ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.Builder builderForValue) {
        if (valTypeBuilder_ == null) {
          valType_ = builderForValue.build();
          onChanged();
        } else {
          valTypeBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000004;
        return this;
      }
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      public Builder mergeValType(ralph_local_version_protobuffs.DeltaProto.Delta.ValueType value) {
        if (valTypeBuilder_ == null) {
          if (((bitField0_ & 0x00000004) == 0x00000004) &&
              valType_ != ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.getDefaultInstance()) {
            valType_ =
              ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.newBuilder(valType_).mergeFrom(value).buildPartial();
          } else {
            valType_ = value;
          }
          onChanged();
        } else {
          valTypeBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      public Builder clearValType() {
        if (valTypeBuilder_ == null) {
          valType_ = ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.getDefaultInstance();
          onChanged();
        } else {
          valTypeBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      public ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.Builder getValTypeBuilder() {
        bitField0_ |= 0x00000004;
        onChanged();
        return getValTypeFieldBuilder().getBuilder();
      }
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      public ralph_local_version_protobuffs.DeltaProto.Delta.ValueTypeOrBuilder getValTypeOrBuilder() {
        if (valTypeBuilder_ != null) {
          return valTypeBuilder_.getMessageOrBuilder();
        } else {
          return valType_;
        }
      }
      /**
       * <code>optional .Delta.ValueType val_type = 3;</code>
       */
      private com.google.protobuf.SingleFieldBuilder<
          ralph_local_version_protobuffs.DeltaProto.Delta.ValueType, ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.Builder, ralph_local_version_protobuffs.DeltaProto.Delta.ValueTypeOrBuilder> 
          getValTypeFieldBuilder() {
        if (valTypeBuilder_ == null) {
          valTypeBuilder_ = new com.google.protobuf.SingleFieldBuilder<
              ralph_local_version_protobuffs.DeltaProto.Delta.ValueType, ralph_local_version_protobuffs.DeltaProto.Delta.ValueType.Builder, ralph_local_version_protobuffs.DeltaProto.Delta.ValueTypeOrBuilder>(
                  valType_,
                  getParentForChildren(),
                  isClean());
          valType_ = null;
        }
        return valTypeBuilder_;
      }

      // optional .Delta.ReferenceType ref_type = 4;
      private ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType refType_ = ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.getDefaultInstance();
      private com.google.protobuf.SingleFieldBuilder<
          ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType, ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.Builder, ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceTypeOrBuilder> refTypeBuilder_;
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      public boolean hasRefType() {
        return ((bitField0_ & 0x00000008) == 0x00000008);
      }
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      public ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType getRefType() {
        if (refTypeBuilder_ == null) {
          return refType_;
        } else {
          return refTypeBuilder_.getMessage();
        }
      }
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      public Builder setRefType(ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType value) {
        if (refTypeBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          refType_ = value;
          onChanged();
        } else {
          refTypeBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000008;
        return this;
      }
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      public Builder setRefType(
          ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.Builder builderForValue) {
        if (refTypeBuilder_ == null) {
          refType_ = builderForValue.build();
          onChanged();
        } else {
          refTypeBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000008;
        return this;
      }
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      public Builder mergeRefType(ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType value) {
        if (refTypeBuilder_ == null) {
          if (((bitField0_ & 0x00000008) == 0x00000008) &&
              refType_ != ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.getDefaultInstance()) {
            refType_ =
              ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.newBuilder(refType_).mergeFrom(value).buildPartial();
          } else {
            refType_ = value;
          }
          onChanged();
        } else {
          refTypeBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000008;
        return this;
      }
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      public Builder clearRefType() {
        if (refTypeBuilder_ == null) {
          refType_ = ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.getDefaultInstance();
          onChanged();
        } else {
          refTypeBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000008);
        return this;
      }
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      public ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.Builder getRefTypeBuilder() {
        bitField0_ |= 0x00000008;
        onChanged();
        return getRefTypeFieldBuilder().getBuilder();
      }
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      public ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceTypeOrBuilder getRefTypeOrBuilder() {
        if (refTypeBuilder_ != null) {
          return refTypeBuilder_.getMessageOrBuilder();
        } else {
          return refType_;
        }
      }
      /**
       * <code>optional .Delta.ReferenceType ref_type = 4;</code>
       */
      private com.google.protobuf.SingleFieldBuilder<
          ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType, ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.Builder, ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceTypeOrBuilder> 
          getRefTypeFieldBuilder() {
        if (refTypeBuilder_ == null) {
          refTypeBuilder_ = new com.google.protobuf.SingleFieldBuilder<
              ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType, ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceType.Builder, ralph_local_version_protobuffs.DeltaProto.Delta.ReferenceTypeOrBuilder>(
                  refType_,
                  getParentForChildren(),
                  isClean());
          refType_ = null;
        }
        return refTypeBuilder_;
      }

      // @@protoc_insertion_point(builder_scope:ObjectContents)
    }

    static {
      defaultInstance = new ObjectContents(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:ObjectContents)
  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_ObjectContents_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_ObjectContents_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\025object_contents.proto\032\013delta.proto\"z\n\016" +
      "ObjectContents\022\014\n\004uuid\030\001 \002(\t\022\016\n\006atomic\030\002" +
      " \002(\010\022\"\n\010val_type\030\003 \001(\0132\020.Delta.ValueType" +
      "\022&\n\010ref_type\030\004 \001(\0132\024.Delta.ReferenceType" +
      "B5\n\036ralph_local_version_protobuffsB\023Obje" +
      "ctContentsProto"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_ObjectContents_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_ObjectContents_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_ObjectContents_descriptor,
              new java.lang.String[] { "Uuid", "Atomic", "ValType", "RefType", });
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          ralph_local_version_protobuffs.DeltaProto.getDescriptor(),
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
