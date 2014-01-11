#!/usr/bin/env python
import sys
import os
from parse_file import parse

base_path = os.path.join(
    os.path.realpath(os.path.dirname(__file__)),'..')
sys.path.append(base_path)
from ralph.java_emit.emit import emit


def compile_ralph(input_filename,output_filename,package_name,program_name):
    root_node = parse(input_filename,False)
    struct_types_ctx = root_node.type_check()
    emitted_text = emit(root_node,struct_types_ctx,package_name,program_name)
    file_fd = open(output_filename,'w')
    file_fd.write(emitted_text)
    file_fd.flush()
    file_fd.close()

def print_usage():

    print '''

./emit_file.py <input_filename> <output_filename> <pkg_name> <program_name>

Args:

  input_filename --- The name of a ralph file to compile

  output_filename --- The name of a java file to compile to

  pkg_name --- The package that the ralph program should compile into

  program_name --- The name of the class that wraps endpoint classes.

'''

if __name__ == '__main__':

    if len(sys.argv) != 5:
        print_usage()
    else:
        input_filename = sys.argv[1]
        output_filename = sys.argv[2]
        package_name = sys.argv[3]
        program_name = sys.argv[4]
        compile_ralph(input_filename,output_filename,package_name,program_name)
