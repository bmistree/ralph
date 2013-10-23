
PROTOCOL_FILES := $(wildcard *.proto)
# GENERATED_FILES := $(PROTOCOL_FILES:.proto=_pb2.py)
# COMPILE_FOLDER := ../proto_compiled/
JAVA_GENERATED_FILES := $(PROTOCOL_FILES:.proto=_pb2.java)
JAVA_COMPILE_FOLDER := ../java_lib/

.PHONY : java_proto
java_proto : $(JAVA_GENERATED_FILES)
$(JAVA_GENERATED_FILES) : %_pb2.java : %.proto
	protoc -I=. --java_out=$(JAVA_COMPILE_FOLDER) $<

.PHONY : clean
clean :
	rm $(JAVA_COMPILE_FOLDER)/*_pb2.java


# to add python compile, uncomment lines at top, and use the following
# for clean, etc.

# .PHONY : all_proto
# all_proto : $(GENERATED_FILES)

# $(GENERATED_FILES) : %_pb2.py : %.proto
# 	protoc -I=. --python_out=$(COMPILE_FOLDER) $<

# .PHONY : clean
# clean :
# 	rm $(COMPILE_FOLDER)/*_pb2.py

