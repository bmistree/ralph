// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: partnerCompleteCommitRequest.proto

package ralph_protobuffs;

public final class PartnerCompleteCommitRequestProto {
  private PartnerCompleteCommitRequestProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface PartnerCompleteCommitRequestOrBuilder
      extends com.google.protobuf.MessageOrBuilder {

    // required .UUID event_uuid = 1;
    /**
     * <code>required .UUID event_uuid = 1;</code>
     */
    boolean hasEventUuid();
    /**
     * <code>required .UUID event_uuid = 1;</code>
     */
    ralph_protobuffs.UtilProto.UUID getEventUuid();
    /**
     * <code>required .UUID event_uuid = 1;</code>
     */
    ralph_protobuffs.UtilProto.UUIDOrBuilder getEventUuidOrBuilder();
  }
  /**
   * Protobuf type {@code PartnerCompleteCommitRequest}
   *
   * <pre>
   *
   *Sent by root endpoint (and forwarded by subsequent endpoints) to
   *finish second phase of commit.
   * </pre>
   */
  public static final class PartnerCompleteCommitRequest extends
      com.google.protobuf.GeneratedMessage
      implements PartnerCompleteCommitRequestOrBuilder {
    // Use PartnerCompleteCommitRequest.newBuilder() to construct.
    private PartnerCompleteCommitRequest(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private PartnerCompleteCommitRequest(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final PartnerCompleteCommitRequest defaultInstance;
    public static PartnerCompleteCommitRequest getDefaultInstance() {
      return defaultInstance;
    }

    public PartnerCompleteCommitRequest getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private PartnerCompleteCommitRequest(
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
                subBuilder = eventUuid_.toBuilder();
              }
              eventUuid_ = input.readMessage(ralph_protobuffs.UtilProto.UUID.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(eventUuid_);
                eventUuid_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
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
      return ralph_protobuffs.PartnerCompleteCommitRequestProto.internal_static_PartnerCompleteCommitRequest_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return ralph_protobuffs.PartnerCompleteCommitRequestProto.internal_static_PartnerCompleteCommitRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest.class, ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest.Builder.class);
    }

    public static com.google.protobuf.Parser<PartnerCompleteCommitRequest> PARSER =
        new com.google.protobuf.AbstractParser<PartnerCompleteCommitRequest>() {
      public PartnerCompleteCommitRequest parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new PartnerCompleteCommitRequest(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<PartnerCompleteCommitRequest> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    // required .UUID event_uuid = 1;
    public static final int EVENT_UUID_FIELD_NUMBER = 1;
    private ralph_protobuffs.UtilProto.UUID eventUuid_;
    /**
     * <code>required .UUID event_uuid = 1;</code>
     */
    public boolean hasEventUuid() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required .UUID event_uuid = 1;</code>
     */
    public ralph_protobuffs.UtilProto.UUID getEventUuid() {
      return eventUuid_;
    }
    /**
     * <code>required .UUID event_uuid = 1;</code>
     */
    public ralph_protobuffs.UtilProto.UUIDOrBuilder getEventUuidOrBuilder() {
      return eventUuid_;
    }

    private void initFields() {
      eventUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;

      if (!hasEventUuid()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!getEventUuid().isInitialized()) {
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
        output.writeMessage(1, eventUuid_);
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
          .computeMessageSize(1, eventUuid_);
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

    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest prototype) {
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
     * Protobuf type {@code PartnerCompleteCommitRequest}
     *
     * <pre>
     *
     *Sent by root endpoint (and forwarded by subsequent endpoints) to
     *finish second phase of commit.
     * </pre>
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return ralph_protobuffs.PartnerCompleteCommitRequestProto.internal_static_PartnerCompleteCommitRequest_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return ralph_protobuffs.PartnerCompleteCommitRequestProto.internal_static_PartnerCompleteCommitRequest_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest.class, ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest.Builder.class);
      }

      // Construct using ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest.newBuilder()
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
          getEventUuidFieldBuilder();
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        if (eventUuidBuilder_ == null) {
          eventUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
        } else {
          eventUuidBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return ralph_protobuffs.PartnerCompleteCommitRequestProto.internal_static_PartnerCompleteCommitRequest_descriptor;
      }

      public ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest getDefaultInstanceForType() {
        return ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest.getDefaultInstance();
      }

      public ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest build() {
        ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest buildPartial() {
        ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest result = new ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        if (eventUuidBuilder_ == null) {
          result.eventUuid_ = eventUuid_;
        } else {
          result.eventUuid_ = eventUuidBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest) {
          return mergeFrom((ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest other) {
        if (other == ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest.getDefaultInstance()) return this;
        if (other.hasEventUuid()) {
          mergeEventUuid(other.getEventUuid());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (!hasEventUuid()) {
          
          return false;
        }
        if (!getEventUuid().isInitialized()) {
          
          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      // required .UUID event_uuid = 1;
      private ralph_protobuffs.UtilProto.UUID eventUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder> eventUuidBuilder_;
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      public boolean hasEventUuid() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      public ralph_protobuffs.UtilProto.UUID getEventUuid() {
        if (eventUuidBuilder_ == null) {
          return eventUuid_;
        } else {
          return eventUuidBuilder_.getMessage();
        }
      }
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      public Builder setEventUuid(ralph_protobuffs.UtilProto.UUID value) {
        if (eventUuidBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          eventUuid_ = value;
          onChanged();
        } else {
          eventUuidBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      public Builder setEventUuid(
          ralph_protobuffs.UtilProto.UUID.Builder builderForValue) {
        if (eventUuidBuilder_ == null) {
          eventUuid_ = builderForValue.build();
          onChanged();
        } else {
          eventUuidBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      public Builder mergeEventUuid(ralph_protobuffs.UtilProto.UUID value) {
        if (eventUuidBuilder_ == null) {
          if (((bitField0_ & 0x00000001) == 0x00000001) &&
              eventUuid_ != ralph_protobuffs.UtilProto.UUID.getDefaultInstance()) {
            eventUuid_ =
              ralph_protobuffs.UtilProto.UUID.newBuilder(eventUuid_).mergeFrom(value).buildPartial();
          } else {
            eventUuid_ = value;
          }
          onChanged();
        } else {
          eventUuidBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      public Builder clearEventUuid() {
        if (eventUuidBuilder_ == null) {
          eventUuid_ = ralph_protobuffs.UtilProto.UUID.getDefaultInstance();
          onChanged();
        } else {
          eventUuidBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        return this;
      }
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      public ralph_protobuffs.UtilProto.UUID.Builder getEventUuidBuilder() {
        bitField0_ |= 0x00000001;
        onChanged();
        return getEventUuidFieldBuilder().getBuilder();
      }
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      public ralph_protobuffs.UtilProto.UUIDOrBuilder getEventUuidOrBuilder() {
        if (eventUuidBuilder_ != null) {
          return eventUuidBuilder_.getMessageOrBuilder();
        } else {
          return eventUuid_;
        }
      }
      /**
       * <code>required .UUID event_uuid = 1;</code>
       */
      private com.google.protobuf.SingleFieldBuilder<
          ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder> 
          getEventUuidFieldBuilder() {
        if (eventUuidBuilder_ == null) {
          eventUuidBuilder_ = new com.google.protobuf.SingleFieldBuilder<
              ralph_protobuffs.UtilProto.UUID, ralph_protobuffs.UtilProto.UUID.Builder, ralph_protobuffs.UtilProto.UUIDOrBuilder>(
                  eventUuid_,
                  getParentForChildren(),
                  isClean());
          eventUuid_ = null;
        }
        return eventUuidBuilder_;
      }

      // @@protoc_insertion_point(builder_scope:PartnerCompleteCommitRequest)
    }

    static {
      defaultInstance = new PartnerCompleteCommitRequest(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:PartnerCompleteCommitRequest)
  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_PartnerCompleteCommitRequest_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_PartnerCompleteCommitRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\"partnerCompleteCommitRequest.proto\032\nut" +
      "il.proto\"9\n\034PartnerCompleteCommitRequest" +
      "\022\031\n\nevent_uuid\030\001 \002(\0132\005.UUIDB5\n\020ralph_pro" +
      "tobuffsB!PartnerCompleteCommitRequestPro" +
      "to"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_PartnerCompleteCommitRequest_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_PartnerCompleteCommitRequest_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_PartnerCompleteCommitRequest_descriptor,
              new java.lang.String[] { "EventUuid", });
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
