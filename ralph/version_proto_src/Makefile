
PROTOCOL_FILES := $(wildcard *.proto)
JAVA_GENERATED_FILES := $(PROTOCOL_FILES:.proto=_pb2.java)
JAVA_COMPILE_FOLDER := ../java_lib/

.PHONY : java_proto
java_proto : $(JAVA_GENERATED_FILES)
$(JAVA_GENERATED_FILES) : %_pb2.java : %.proto
	protoc -I=. --java_out=$(JAVA_COMPILE_FOLDER) $<

.PHONY : clean
clean :
	rm $(JAVA_COMPILE_FOLDER)/*_pb2.java
