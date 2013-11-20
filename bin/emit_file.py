#!/usr/bin/env python
import sys
import os
from parse_file import parse

base_path = os.path.join(
    os.path.realpath(os.path.dirname(__file__)),'..')
sys.path.append(base_path)
from ralph.java_emit.emit import emit


def compile_ralph(input_filename,output_filename,package_name,program_name):
    root_node = parse(input_filename)

    emitted_text = emit(root_node,package_name,program_name)
    file_fd = open(output_filename,'w')
    file_fd.write(emitted_text)
    file_fd.flush()
    file_fd.close()
    

if __name__ == '__main__':
    input_filename = sys.argv[1]
    output_filename = sys.argv[2]
    package_name = sys.argv[3]
    program_name = sys.argv[4]
    compile_ralph(input_filename,output_filename,package_name,program_name)
