#!/usr/bin/env python
import sys
import os
from parse_file import parse

base_path = os.path.join(
    os.path.realpath(os.path.dirname(__file__)),'..')
sys.path.append(base_path)
from ralph.java_emit.emit import emit


def compile_ralph(input_filename,output_filename,package_name,program_name,
                  dependencies_list):
    '''
    @param {list} dependencies_list --- Each element is a string,
    naming a file that we should link to while generating code for
    input_filename.
    '''
    struct_types_ctx_list = []
    for dep_file in dependencies_list:
        dep_root = parse(dep_file)
        dep_struct_types_ctx = dep_root.type_check(dep_file)
        struct_types_ctx_list.append(dep_struct_types_ctx)
    
    root_node = parse(input_filename)
    struct_types_ctx = root_node.type_check(input_filename)
    for s_types_ctx in struct_types_ctx_list:
        struct_typs_ctx.merge_struct_type_context_into_me(s_types_ctx)
    
    emitted_text = emit(root_node,struct_types_ctx,package_name,program_name)
    file_fd = open(output_filename,'w')
    file_fd.write(emitted_text)
    file_fd.flush()
    file_fd.close()

def print_usage():

    print '''

./emit_file.py <input_filename> <output_filename> <pkg_name> <program_name> ...

Args:

  input_filename --- The name of a ralph file to compile

  output_filename --- The name of a java file to compile to

  pkg_name --- The package that the ralph program should compile into

  program_name --- The name of the class that wraps endpoint classes.

  ... --- A list of dependencies that this program imports from

'''

if __name__ == '__main__':

    if len(sys.argv) < 5:
        print_usage()
    else:
        input_filename = sys.argv[1]
        output_filename = sys.argv[2]
        package_name = sys.argv[3]
        program_name = sys.argv[4]
        dependencies_list = sys.argv[5:]
        compile_ralph(input_filename,output_filename,package_name,
                      program_name,dependencies_list)
