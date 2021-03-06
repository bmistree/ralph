// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: durability.proto

package ralph_protobuffs;

public final class DurabilityProto {
  private DurabilityProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface DurabilityOrBuilder
      extends com.google.protobuf.MessageOrBuilder {

    // optional .DurabilityPrepare prepare = 1;
    /**
     * <code>optional .DurabilityPrepare prepare = 1;</code>
     */
    boolean hasPrepare();
    /**
     * <code>optional .DurabilityPrepare prepare = 1;</code>
     */
    ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare getPrepare();
    /**
     * <code>optional .DurabilityPrepare prepare = 1;</code>
     */
    ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepareOrBuilder getPrepareOrBuilder();

    // optional .DurabilityComplete complete = 2;
    /**
     * <code>optional .DurabilityComplete complete = 2;</code>
     */
    boolean hasComplete();
    /**
     * <code>optional .DurabilityComplete complete = 2;</code>
     */
    ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete getComplete();
    /**
     * <code>optional .DurabilityComplete complete = 2;</code>
     */
    ralph_protobuffs.DurabilityCompleteProto.DurabilityCompleteOrBuilder getCompleteOrBuilder();

    // optional .Delta.ServiceFactoryDelta service_factory = 3;
    /**
     * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
     */
    boolean hasServiceFactory();
    /**
     * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
     */
    ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta getServiceFactory();
    /**
     * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
     */
    ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDeltaOrBuilder getServiceFactoryOrBuilder();
  }
  /**
   * Protobuf type {@code Durability}
   */
  public static final class Durability extends
      com.google.protobuf.GeneratedMessage
      implements DurabilityOrBuilder {
    // Use Durability.newBuilder() to construct.
    private Durability(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private Durability(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final Durability defaultInstance;
    public static Durability getDefaultInstance() {
      return defaultInstance;
    }

    public Durability getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private Durability(
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
              ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.Builder subBuilder = null;
              if (((bitField0_ & 0x00000001) == 0x00000001)) {
                subBuilder = prepare_.toBuilder();
              }
              prepare_ = input.readMessage(ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(prepare_);
                prepare_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
              break;
            }
            case 18: {
              ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.Builder subBuilder = null;
              if (((bitField0_ & 0x00000002) == 0x00000002)) {
                subBuilder = complete_.toBuilder();
              }
              complete_ = input.readMessage(ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(complete_);
                complete_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000002;
              break;
            }
            case 26: {
              ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.Builder subBuilder = null;
              if (((bitField0_ & 0x00000004) == 0x00000004)) {
                subBuilder = serviceFactory_.toBuilder();
              }
              serviceFactory_ = input.readMessage(ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(serviceFactory_);
                serviceFactory_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000004;
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
      return ralph_protobuffs.DurabilityProto.internal_static_Durability_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return ralph_protobuffs.DurabilityProto.internal_static_Durability_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              ralph_protobuffs.DurabilityProto.Durability.class, ralph_protobuffs.DurabilityProto.Durability.Builder.class);
    }

    public static com.google.protobuf.Parser<Durability> PARSER =
        new com.google.protobuf.AbstractParser<Durability>() {
      public Durability parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new Durability(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<Durability> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    // optional .DurabilityPrepare prepare = 1;
    public static final int PREPARE_FIELD_NUMBER = 1;
    private ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare prepare_;
    /**
     * <code>optional .DurabilityPrepare prepare = 1;</code>
     */
    public boolean hasPrepare() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>optional .DurabilityPrepare prepare = 1;</code>
     */
    public ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare getPrepare() {
      return prepare_;
    }
    /**
     * <code>optional .DurabilityPrepare prepare = 1;</code>
     */
    public ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepareOrBuilder getPrepareOrBuilder() {
      return prepare_;
    }

    // optional .DurabilityComplete complete = 2;
    public static final int COMPLETE_FIELD_NUMBER = 2;
    private ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete complete_;
    /**
     * <code>optional .DurabilityComplete complete = 2;</code>
     */
    public boolean hasComplete() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>optional .DurabilityComplete complete = 2;</code>
     */
    public ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete getComplete() {
      return complete_;
    }
    /**
     * <code>optional .DurabilityComplete complete = 2;</code>
     */
    public ralph_protobuffs.DurabilityCompleteProto.DurabilityCompleteOrBuilder getCompleteOrBuilder() {
      return complete_;
    }

    // optional .Delta.ServiceFactoryDelta service_factory = 3;
    public static final int SERVICE_FACTORY_FIELD_NUMBER = 3;
    private ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta serviceFactory_;
    /**
     * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
     */
    public boolean hasServiceFactory() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    /**
     * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
     */
    public ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta getServiceFactory() {
      return serviceFactory_;
    }
    /**
     * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
     */
    public ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDeltaOrBuilder getServiceFactoryOrBuilder() {
      return serviceFactory_;
    }

    private void initFields() {
      prepare_ = ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.getDefaultInstance();
      complete_ = ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.getDefaultInstance();
      serviceFactory_ = ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.getDefaultInstance();
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;

      if (hasPrepare()) {
        if (!getPrepare().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      if (hasComplete()) {
        if (!getComplete().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      if (hasServiceFactory()) {
        if (!getServiceFactory().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeMessage(1, prepare_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeMessage(2, complete_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeMessage(3, serviceFactory_);
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
          .computeMessageSize(1, prepare_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(2, complete_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(3, serviceFactory_);
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

    public static ralph_protobuffs.DurabilityProto.Durability parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static ralph_protobuffs.DurabilityProto.Durability parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(ralph_protobuffs.DurabilityProto.Durability prototype) {
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
     * Protobuf type {@code Durability}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements ralph_protobuffs.DurabilityProto.DurabilityOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return ralph_protobuffs.DurabilityProto.internal_static_Durability_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return ralph_protobuffs.DurabilityProto.internal_static_Durability_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                ralph_protobuffs.DurabilityProto.Durability.class, ralph_protobuffs.DurabilityProto.Durability.Builder.class);
      }

      // Construct using ralph_protobuffs.DurabilityProto.Durability.newBuilder()
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
          getPrepareFieldBuilder();
          getCompleteFieldBuilder();
          getServiceFactoryFieldBuilder();
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        if (prepareBuilder_ == null) {
          prepare_ = ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.getDefaultInstance();
        } else {
          prepareBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        if (completeBuilder_ == null) {
          complete_ = ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.getDefaultInstance();
        } else {
          completeBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000002);
        if (serviceFactoryBuilder_ == null) {
          serviceFactory_ = ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.getDefaultInstance();
        } else {
          serviceFactoryBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return ralph_protobuffs.DurabilityProto.internal_static_Durability_descriptor;
      }

      public ralph_protobuffs.DurabilityProto.Durability getDefaultInstanceForType() {
        return ralph_protobuffs.DurabilityProto.Durability.getDefaultInstance();
      }

      public ralph_protobuffs.DurabilityProto.Durability build() {
        ralph_protobuffs.DurabilityProto.Durability result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public ralph_protobuffs.DurabilityProto.Durability buildPartial() {
        ralph_protobuffs.DurabilityProto.Durability result = new ralph_protobuffs.DurabilityProto.Durability(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        if (prepareBuilder_ == null) {
          result.prepare_ = prepare_;
        } else {
          result.prepare_ = prepareBuilder_.build();
        }
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        if (completeBuilder_ == null) {
          result.complete_ = complete_;
        } else {
          result.complete_ = completeBuilder_.build();
        }
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        if (serviceFactoryBuilder_ == null) {
          result.serviceFactory_ = serviceFactory_;
        } else {
          result.serviceFactory_ = serviceFactoryBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ralph_protobuffs.DurabilityProto.Durability) {
          return mergeFrom((ralph_protobuffs.DurabilityProto.Durability)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ralph_protobuffs.DurabilityProto.Durability other) {
        if (other == ralph_protobuffs.DurabilityProto.Durability.getDefaultInstance()) return this;
        if (other.hasPrepare()) {
          mergePrepare(other.getPrepare());
        }
        if (other.hasComplete()) {
          mergeComplete(other.getComplete());
        }
        if (other.hasServiceFactory()) {
          mergeServiceFactory(other.getServiceFactory());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (hasPrepare()) {
          if (!getPrepare().isInitialized()) {
            
            return false;
          }
        }
        if (hasComplete()) {
          if (!getComplete().isInitialized()) {
            
            return false;
          }
        }
        if (hasServiceFactory()) {
          if (!getServiceFactory().isInitialized()) {
            
            return false;
          }
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        ralph_protobuffs.DurabilityProto.Durability parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (ralph_protobuffs.DurabilityProto.Durability) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      // optional .DurabilityPrepare prepare = 1;
      private ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare prepare_ = ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.getDefaultInstance();
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare, ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.Builder, ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepareOrBuilder> prepareBuilder_;
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      public boolean hasPrepare() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      public ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare getPrepare() {
        if (prepareBuilder_ == null) {
          return prepare_;
        } else {
          return prepareBuilder_.getMessage();
        }
      }
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      public Builder setPrepare(ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare value) {
        if (prepareBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          prepare_ = value;
          onChanged();
        } else {
          prepareBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      public Builder setPrepare(
          ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.Builder builderForValue) {
        if (prepareBuilder_ == null) {
          prepare_ = builderForValue.build();
          onChanged();
        } else {
          prepareBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      public Builder mergePrepare(ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare value) {
        if (prepareBuilder_ == null) {
          if (((bitField0_ & 0x00000001) == 0x00000001) &&
              prepare_ != ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.getDefaultInstance()) {
            prepare_ =
              ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.newBuilder(prepare_).mergeFrom(value).buildPartial();
          } else {
            prepare_ = value;
          }
          onChanged();
        } else {
          prepareBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      public Builder clearPrepare() {
        if (prepareBuilder_ == null) {
          prepare_ = ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.getDefaultInstance();
          onChanged();
        } else {
          prepareBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        return this;
      }
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      public ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.Builder getPrepareBuilder() {
        bitField0_ |= 0x00000001;
        onChanged();
        return getPrepareFieldBuilder().getBuilder();
      }
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      public ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepareOrBuilder getPrepareOrBuilder() {
        if (prepareBuilder_ != null) {
          return prepareBuilder_.getMessageOrBuilder();
        } else {
          return prepare_;
        }
      }
      /**
       * <code>optional .DurabilityPrepare prepare = 1;</code>
       */
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare, ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.Builder, ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepareOrBuilder> 
          getPrepareFieldBuilder() {
        if (prepareBuilder_ == null) {
          prepareBuilder_ = new com.google.protobuf.SingleFieldBuilder<
              ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare, ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.Builder, ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepareOrBuilder>(
                  prepare_,
                  getParentForChildren(),
                  isClean());
          prepare_ = null;
        }
        return prepareBuilder_;
      }

      // optional .DurabilityComplete complete = 2;
      private ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete complete_ = ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.getDefaultInstance();
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete, ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.Builder, ralph_protobuffs.DurabilityCompleteProto.DurabilityCompleteOrBuilder> completeBuilder_;
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      public boolean hasComplete() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      public ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete getComplete() {
        if (completeBuilder_ == null) {
          return complete_;
        } else {
          return completeBuilder_.getMessage();
        }
      }
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      public Builder setComplete(ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete value) {
        if (completeBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          complete_ = value;
          onChanged();
        } else {
          completeBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000002;
        return this;
      }
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      public Builder setComplete(
          ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.Builder builderForValue) {
        if (completeBuilder_ == null) {
          complete_ = builderForValue.build();
          onChanged();
        } else {
          completeBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000002;
        return this;
      }
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      public Builder mergeComplete(ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete value) {
        if (completeBuilder_ == null) {
          if (((bitField0_ & 0x00000002) == 0x00000002) &&
              complete_ != ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.getDefaultInstance()) {
            complete_ =
              ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.newBuilder(complete_).mergeFrom(value).buildPartial();
          } else {
            complete_ = value;
          }
          onChanged();
        } else {
          completeBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000002;
        return this;
      }
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      public Builder clearComplete() {
        if (completeBuilder_ == null) {
          complete_ = ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.getDefaultInstance();
          onChanged();
        } else {
          completeBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      public ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.Builder getCompleteBuilder() {
        bitField0_ |= 0x00000002;
        onChanged();
        return getCompleteFieldBuilder().getBuilder();
      }
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      public ralph_protobuffs.DurabilityCompleteProto.DurabilityCompleteOrBuilder getCompleteOrBuilder() {
        if (completeBuilder_ != null) {
          return completeBuilder_.getMessageOrBuilder();
        } else {
          return complete_;
        }
      }
      /**
       * <code>optional .DurabilityComplete complete = 2;</code>
       */
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete, ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.Builder, ralph_protobuffs.DurabilityCompleteProto.DurabilityCompleteOrBuilder> 
          getCompleteFieldBuilder() {
        if (completeBuilder_ == null) {
          completeBuilder_ = new com.google.protobuf.SingleFieldBuilder<
              ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete, ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete.Builder, ralph_protobuffs.DurabilityCompleteProto.DurabilityCompleteOrBuilder>(
                  complete_,
                  getParentForChildren(),
                  isClean());
          complete_ = null;
        }
        return completeBuilder_;
      }

      // optional .Delta.ServiceFactoryDelta service_factory = 3;
      private ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta serviceFactory_ = ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.getDefaultInstance();
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta, ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.Builder, ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDeltaOrBuilder> serviceFactoryBuilder_;
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      public boolean hasServiceFactory() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      public ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta getServiceFactory() {
        if (serviceFactoryBuilder_ == null) {
          return serviceFactory_;
        } else {
          return serviceFactoryBuilder_.getMessage();
        }
      }
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      public Builder setServiceFactory(ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta value) {
        if (serviceFactoryBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          serviceFactory_ = value;
          onChanged();
        } else {
          serviceFactoryBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      public Builder setServiceFactory(
          ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.Builder builderForValue) {
        if (serviceFactoryBuilder_ == null) {
          serviceFactory_ = builderForValue.build();
          onChanged();
        } else {
          serviceFactoryBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000004;
        return this;
      }
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      public Builder mergeServiceFactory(ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta value) {
        if (serviceFactoryBuilder_ == null) {
          if (((bitField0_ & 0x00000004) == 0x00000004) &&
              serviceFactory_ != ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.getDefaultInstance()) {
            serviceFactory_ =
              ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.newBuilder(serviceFactory_).mergeFrom(value).buildPartial();
          } else {
            serviceFactory_ = value;
          }
          onChanged();
        } else {
          serviceFactoryBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      public Builder clearServiceFactory() {
        if (serviceFactoryBuilder_ == null) {
          serviceFactory_ = ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.getDefaultInstance();
          onChanged();
        } else {
          serviceFactoryBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      public ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.Builder getServiceFactoryBuilder() {
        bitField0_ |= 0x00000004;
        onChanged();
        return getServiceFactoryFieldBuilder().getBuilder();
      }
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      public ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDeltaOrBuilder getServiceFactoryOrBuilder() {
        if (serviceFactoryBuilder_ != null) {
          return serviceFactoryBuilder_.getMessageOrBuilder();
        } else {
          return serviceFactory_;
        }
      }
      /**
       * <code>optional .Delta.ServiceFactoryDelta service_factory = 3;</code>
       */
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta, ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.Builder, ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDeltaOrBuilder> 
          getServiceFactoryFieldBuilder() {
        if (serviceFactoryBuilder_ == null) {
          serviceFactoryBuilder_ = new com.google.protobuf.SingleFieldBuilder<
              ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta, ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta.Builder, ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDeltaOrBuilder>(
                  serviceFactory_,
                  getParentForChildren(),
                  isClean());
          serviceFactory_ = null;
        }
        return serviceFactoryBuilder_;
      }

      // @@protoc_insertion_point(builder_scope:Durability)
    }

    static {
      defaultInstance = new Durability(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:Durability)
  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_Durability_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_Durability_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020durability.proto\032\027durabilityPrepare.pr" +
      "oto\032\030durabilityComplete.proto\032\013delta.pro" +
      "to\"\215\001\n\nDurability\022#\n\007prepare\030\001 \001(\0132\022.Dur" +
      "abilityPrepare\022%\n\010complete\030\002 \001(\0132\023.Durab" +
      "ilityComplete\0223\n\017service_factory\030\003 \001(\0132\032" +
      ".Delta.ServiceFactoryDeltaB#\n\020ralph_prot" +
      "obuffsB\017DurabilityProto"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_Durability_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_Durability_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_Durability_descriptor,
              new java.lang.String[] { "Prepare", "Complete", "ServiceFactory", });
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          ralph_protobuffs.DurabilityPrepareProto.getDescriptor(),
          ralph_protobuffs.DurabilityCompleteProto.getDescriptor(),
          ralph_protobuffs.DeltaProto.getDescriptor(),
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
