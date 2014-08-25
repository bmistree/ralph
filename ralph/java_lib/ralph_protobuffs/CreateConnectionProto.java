// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: createConnectionMessage.proto

package ralph_protobuffs;

public final class CreateConnectionProto {
  private CreateConnectionProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface CreateConnectionOrBuilder
      extends com.google.protobuf.MessageOrBuilder {

    // required .UUID target_endpoint_uuid = 1;
    /**
     * <code>required .UUID target_endpoint_uuid = 1;</code>
     */
    boolean hasTargetEndpointUuid();
    /**
     * <code>required .UUID target_endpoint_uuid = 1;</code>
     */
    ralph_protobuffs.UtilProto.UUID getTargetEndpointUuid();
    /**
     * <code>required .UUID target_endpoint_uuid = 1;</code>
     */
    ralph_protobuffs.UtilProto.UUIDOrBuilder getTargetEndpointUuidOrBuilder();

    // required .UUID host_uuid = 2;
    /**
     * <code>required .UUID host_uuid = 2;</code>
     */
    boolean hasHostUuid();
    /**
     * <code>required .UUID host_uuid = 2;</code>
     */
    ralph_protobuffs.UtilProto.UUID getHostUuid();
    /**
     * <code>required .UUID host_uuid = 2;</code>
     */
    ralph_protobuffs.UtilProto.UUIDOrBuilder getHostUuidOrBuilder();
  }
  /**
   * Protobuf type {@code CreateConnection}
   */
  public static final class CreateConnection extends
      com.google.protobuf.GeneratedMessage
      implements CreateConnectionOrBuilder {
    // Use CreateConnection.newBuilder() to construct.
    private CreateConnection(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private CreateConnection(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final CreateConnection defaultInstance;
    public static CreateConnection getDefaultInstance() {
      return defaultInstance;
    }

    public CreateConnection getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private CreateConnection(
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
              ralph_protobuffs.UtilProto.UUID.Builder subBuilder = null;
              if (((bitField0_ & 0x00000001) == 0x00000001)) {
                subBuilder = targetEndpointUuid_.toBuilder();
              }
              targetEndpointUuid_ = input.readMessage(ralph_protobuffs.UtilProto.UUID.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(targetEndpointUuid_);
                targetEndpointUuid_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
              break;
            }
            case 18: {
              ralph_protobuffs.UtilProto.UUID.Builder subBuilder = null;
              if (((bitField0_ & 0x00000002) == 0x00000002)) {
                subBuilder = hostUuid_.toBuilder();
              }
              hostUuid_ = input.readMessage(ralph_protobuffs.UtilProto.UUID.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(hostUuid_);
                hostUuid_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000002;
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
      return ralph_protobuffs.CreateConnectionProto.internal_static_CreateConnection_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return ralph_protobuffs.CreateConnectionProto.internal_static_CreateConnection_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              ralph_protobuffs.CreateConnectionProto.CreateConnection.class, ralph_protobuffs.CreateConnectionProto.CreateConnection.Builder.class);
    }

    public static com.google.protobuf.Parser<CreateConnection> PARSER =
        new com.google.protobuf.AbstractParser<CreateConnection>() {
      public CreateConnection parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new CreateConnection(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<CreateConnection> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    // required .UUID target_endpoint_uuid = 1;
    public static final int TARGET_ENDPOINT_UUID_FIELD_NUMBER = 1;
    private ralph_protobuffs.UtilProto.UUID targetEndpointUuid_;
    /**
     * <code>required .UUID target_endpoint_uuid = 1;</code>
     */
    public boolean hasTargetEndpointUuid() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required .UUID target_endpoint_uuid = 1;</code>
     */
    public ralph_protobuffs.UtilProto.UUID getTargetEndpointUuid() {
      return targetEndpointUuid_;
    }
    /**
     * <code>required .UUID target_endpoint_uuid = 1;</code>
     */
    public ralph_protobuffs.UtilProto.UUIDOrBuilder getTargetEndpointUuidOrBuilder() {
      return targetEndpointUuid_;
    }

    // required .UUID host_uuid = 2;
    public static final int HOST_UUID_FIELD_NUMBER = 2;
    private ralph_protobuffs.UtilProto.UUID hostUuid_;
    /**
     * <code>required .UUID host_uuid = 2;</code>
     */
    public boolean hasHostUuid() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required .UUID host_uuid = 2;</code>
     */
    public ralph_protobuffs.UtilProto.UUID getHostUuid() {
      return hostUuid_;
    }
    /**
     * <code>required .UUID host_uuid = 2;</code>
     */
    public ralph_protobuffs.UtilProto.UUIDOrBuilder getHostUuidOrBuilder() {
      return hostUuid_;
    }

    private void initFields() {
      targetEndpointUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
      hostUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;

      if (!hasTargetEndpointUuid()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasHostUuid()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!getTargetEndpointUuid().isInitialized()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!getHostUuid().isInitialized()) {
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
        output.writeMessage(1, targetEndpointUuid_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeMessage(2, hostUuid_);
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
          .computeMessageSize(1, targetEndpointUuid_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(2, hostUuid_);
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

    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static ralph_protobuffs.CreateConnectionProto.CreateConnection parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(ralph_protobuffs.CreateConnectionProto.CreateConnection prototype) {
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
     * Protobuf type {@code CreateConnection}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements ralph_protobuffs.CreateConnectionProto.CreateConnectionOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return ralph_protobuffs.CreateConnectionProto.internal_static_CreateConnection_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return ralph_protobuffs.CreateConnectionProto.internal_static_CreateConnection_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                ralph_protobuffs.CreateConnectionProto.CreateConnection.class, ralph_protobuffs.CreateConnectionProto.CreateConnection.Builder.class);
      }

      // Construct using ralph_protobuffs.CreateConnectionProto.CreateConnection.newBuilder()
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
          getTargetEndpointUuidFieldBuilder();
          getHostUuidFieldBuilder();
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        if (targetEndpointUuidBuilder_ == null) {
          targetEndpointUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
        } else {
          targetEndpointUuidBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        if (hostUuidBuilder_ == null) {
          hostUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
        } else {
          hostUuidBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return ralph_protobuffs.CreateConnectionProto.internal_static_CreateConnection_descriptor;
      }

      public ralph_protobuffs.CreateConnectionProto.CreateConnection getDefaultInstanceForType() {
        return ralph_protobuffs.CreateConnectionProto.CreateConnection.getDefaultInstance();
      }

      public ralph_protobuffs.CreateConnectionProto.CreateConnection build() {
        ralph_protobuffs.CreateConnectionProto.CreateConnection result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public ralph_protobuffs.CreateConnectionProto.CreateConnection buildPartial() {
        ralph_protobuffs.CreateConnectionProto.CreateConnection result = new ralph_protobuffs.CreateConnectionProto.CreateConnection(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        if (targetEndpointUuidBuilder_ == null) {
          result.targetEndpointUuid_ = targetEndpointUuid_;
        } else {
          result.targetEndpointUuid_ = targetEndpointUuidBuilder_.build();
        }
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        if (hostUuidBuilder_ == null) {
          result.hostUuid_ = hostUuid_;
        } else {
          result.hostUuid_ = hostUuidBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ralph_protobuffs.CreateConnectionProto.CreateConnection) {
          return mergeFrom((ralph_protobuffs.CreateConnectionProto.CreateConnection)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ralph_protobuffs.CreateConnectionProto.CreateConnection other) {
        if (other == ralph_protobuffs.CreateConnectionProto.CreateConnection.getDefaultInstance()) return this;
        if (other.hasTargetEndpointUuid()) {
          mergeTargetEndpointUuid(other.getTargetEndpointUuid());
        }
        if (other.hasHostUuid()) {
          mergeHostUuid(other.getHostUuid());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (!hasTargetEndpointUuid()) {
          
          return false;
        }
        if (!hasHostUuid()) {
          
          return false;
        }
        if (!getTargetEndpointUuid().isInitialized()) {
          
          return false;
        }
        if (!getHostUuid().isInitialized()) {
          
          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        ralph_protobuffs.CreateConnectionProto.CreateConnection parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (ralph_protobuffs.CreateConnectionProto.CreateConnection) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      // required .UUID target_endpoint_uuid = 1;
      private ralph_protobuffs.UtilProto.UUID targetEndpointUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder> targetEndpointUuidBuilder_;
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      public boolean hasTargetEndpointUuid() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      public ralph_protobuffs.UtilProto.UUID getTargetEndpointUuid() {
        if (targetEndpointUuidBuilder_ == null) {
          return targetEndpointUuid_;
        } else {
          return targetEndpointUuidBuilder_.getMessage();
        }
      }
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      public Builder setTargetEndpointUuid(ralph_protobuffs.UtilProto.UUID value) {
        if (targetEndpointUuidBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          targetEndpointUuid_ = value;
          onChanged();
        } else {
          targetEndpointUuidBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      public Builder setTargetEndpointUuid(
          ralph_protobuffs.UtilProto.UUID.Builder builderForValue) {
        if (targetEndpointUuidBuilder_ == null) {
          targetEndpointUuid_ = builderForValue.build();
          onChanged();
        } else {
          targetEndpointUuidBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      public Builder mergeTargetEndpointUuid(ralph_protobuffs.UtilProto.UUID value) {
        if (targetEndpointUuidBuilder_ == null) {
          if (((bitField0_ & 0x00000001) == 0x00000001) &&
              targetEndpointUuid_ != ralph_protobuffs.UtilProto.UUID.getDefaultInstance()) {
            targetEndpointUuid_ =
              ralph_protobuffs.UtilProto.UUID.newBuilder(targetEndpointUuid_).mergeFrom(value).buildPartial();
          } else {
            targetEndpointUuid_ = value;
          }
          onChanged();
        } else {
          targetEndpointUuidBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      public Builder clearTargetEndpointUuid() {
        if (targetEndpointUuidBuilder_ == null) {
          targetEndpointUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
          onChanged();
        } else {
          targetEndpointUuidBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        return this;
      }
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      public ralph_protobuffs.UtilProto.UUID.Builder getTargetEndpointUuidBuilder() {
        bitField0_ |= 0x00000001;
        onChanged();
        return getTargetEndpointUuidFieldBuilder().getBuilder();
      }
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      public ralph_protobuffs.UtilProto.UUIDOrBuilder getTargetEndpointUuidOrBuilder() {
        if (targetEndpointUuidBuilder_ != null) {
          return targetEndpointUuidBuilder_.getMessageOrBuilder();
        } else {
          return targetEndpointUuid_;
        }
      }
      /**
       * <code>required .UUID target_endpoint_uuid = 1;</code>
       */
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder> 
          getTargetEndpointUuidFieldBuilder() {
        if (targetEndpointUuidBuilder_ == null) {
          targetEndpointUuidBuilder_ = new com.google.protobuf.SingleFieldBuilder<
              ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder>(
                  targetEndpointUuid_,
                  getParentForChildren(),
                  isClean());
          targetEndpointUuid_ = null;
        }
        return targetEndpointUuidBuilder_;
      }

      // required .UUID host_uuid = 2;
      private ralph_protobuffs.UtilProto.UUID hostUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder> hostUuidBuilder_;
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      public boolean hasHostUuid() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      public ralph_protobuffs.UtilProto.UUID getHostUuid() {
        if (hostUuidBuilder_ == null) {
          return hostUuid_;
        } else {
          return hostUuidBuilder_.getMessage();
        }
      }
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      public Builder setHostUuid(ralph_protobuffs.UtilProto.UUID value) {
        if (hostUuidBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          hostUuid_ = value;
          onChanged();
        } else {
          hostUuidBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000002;
        return this;
      }
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      public Builder setHostUuid(
          ralph_protobuffs.UtilProto.UUID.Builder builderForValue) {
        if (hostUuidBuilder_ == null) {
          hostUuid_ = builderForValue.build();
          onChanged();
        } else {
          hostUuidBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000002;
        return this;
      }
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      public Builder mergeHostUuid(ralph_protobuffs.UtilProto.UUID value) {
        if (hostUuidBuilder_ == null) {
          if (((bitField0_ & 0x00000002) == 0x00000002) &&
              hostUuid_ != ralph_protobuffs.UtilProto.UUID.getDefaultInstance()) {
            hostUuid_ =
              ralph_protobuffs.UtilProto.UUID.newBuilder(hostUuid_).mergeFrom(value).buildPartial();
          } else {
            hostUuid_ = value;
          }
          onChanged();
        } else {
          hostUuidBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000002;
        return this;
      }
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      public Builder clearHostUuid() {
        if (hostUuidBuilder_ == null) {
          hostUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
          onChanged();
        } else {
          hostUuidBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      public ralph_protobuffs.UtilProto.UUID.Builder getHostUuidBuilder() {
        bitField0_ |= 0x00000002;
        onChanged();
        return getHostUuidFieldBuilder().getBuilder();
      }
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      public ralph_protobuffs.UtilProto.UUIDOrBuilder getHostUuidOrBuilder() {
        if (hostUuidBuilder_ != null) {
          return hostUuidBuilder_.getMessageOrBuilder();
        } else {
          return hostUuid_;
        }
      }
      /**
       * <code>required .UUID host_uuid = 2;</code>
       */
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder> 
          getHostUuidFieldBuilder() {
        if (hostUuidBuilder_ == null) {
          hostUuidBuilder_ = new com.google.protobuf.SingleFieldBuilder<
              ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder>(
                  hostUuid_,
                  getParentForChildren(),
                  isClean());
          hostUuid_ = null;
        }
        return hostUuidBuilder_;
      }

      // @@protoc_insertion_point(builder_scope:CreateConnection)
    }

    static {
      defaultInstance = new CreateConnection(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:CreateConnection)
  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_CreateConnection_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_CreateConnection_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\035createConnectionMessage.proto\032\nutil.pr" +
      "oto\"Q\n\020CreateConnection\022#\n\024target_endpoi" +
      "nt_uuid\030\001 \002(\0132\005.UUID\022\030\n\thost_uuid\030\002 \002(\0132" +
      "\005.UUIDB)\n\020ralph_protobuffsB\025CreateConnec" +
      "tionProto"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_CreateConnection_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_CreateConnection_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_CreateConnection_descriptor,
              new java.lang.String[] { "TargetEndpointUuid", "HostUuid", });
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          ralph_protobuffs.UtilProto.getDescriptor(),
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
