#!/usr/bin/env python
import sys
import os
import argparse

base_path = os.path.join(
    os.path.realpath(os.path.dirname(__file__)),'..')
sys.path.append(base_path)

from bin.parse_file import parse
from bin.produce_dependencies import dependencies_list    
from ralph.java_emit.emit import emit
            
def compile_ralph(input_filename,output_filename,package_name,program_name,
                  include_dir_list):
    # Each element is a string, naming a file that we should link to
    # while generating code for input_filename.
    # FIXME: actually take in lib_dir_list
    dep_list = dependencies_list(input_filename,include_dir_list)
    
    struct_types_ctx = None
    for dep_file in dep_list:
        dep_root = parse(dep_file)
        struct_types_ctx = dep_root.type_check(struct_types_ctx,dep_file)
        
    root_node = parse(input_filename)
    struct_types_ctx = root_node.type_check(struct_types_ctx,input_filename)
    
    emitted_text = emit(root_node,struct_types_ctx,package_name,program_name)
    file_fd = open(output_filename,'w')
    file_fd.write(emitted_text)
    file_fd.flush()
    file_fd.close()


def run_cli():
    # parser setup
    description = 'Transcompiler for Ralph.'
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument(
        '-i','--input',help='Input ralph file',
        required=True)
    parser.add_argument(
        '-o','--output',help='File to emit ralph to',
        required=True)
    parser.add_argument(
        '-p','--pkg-name',
        help='The Java package name for the new file',
        required=True)
    parser.add_argument(
        '-c','--class-name',
        help='All structs/endpoints are created as static Java ' +
        'classes wrapped by a surrounding class of this name.  ' +
        'Likely should use same name as output file, without ' +
        'the .java suffix.',
        required=True)
    parser.add_argument(
        '-I','--include-dirs',
        help='Add directories to list of directories that will be ' +
        'searched for Ralph files in.  Note, the earlier the listed ' +
        'directory appears in the command line argument, the higher ' +
        'priority it will have.  Ie, if two directories contain the same ' +
        'file, we include the file from the folder listed earlier.',
        nargs='*',default=[])

    # actually run parser and collect user-passed arguments
    args = parser.parse_args()
    compile_ralph(
        args.input,args.output,args.pkg_name,args.class_name,
        args.include_dirs)
    
    

if __name__ == '__main__':
    run_cli()
