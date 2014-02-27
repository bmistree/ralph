import os

from bin.preprocessor_parse_file import preprocessor_parse

from ralph.common.compiler_exceptions import CompilerException
from ralph.preprocessor_parse.preprocessor_node import IncludeStatementNode


class DependencyException(CompilerException):
    def __init__(self,fq_requesting_filename,what_requesting):
        self.fq_requesting_filename = fq_requesting_filename
        self.what_requesting = what_requesting

    def __str__(self):
        return (
            'Error: %s requesting an include from unknown %s ' %
            (self.fq_requesting_filename,self.what_requesting))


def dependencies_list(input_filename,lib_dir_list):
    '''
    Args:
        input_filename: {String} Can be relative or absolute.

        lib_dir_list: {list} Each element is a string.  The
        directories that we should look for libraries in.  Note that
        if two directories contain the same file, we'll import the one
        that appears earlier in the list.  Paths in lib_dir_list may
        be absolute or relative.
        
    Returns:
        {list} --- Should parse files from lowest index to highest
        index.
    '''
    fq_input_filename = os.path.abspath(input_filename)

    fq_lib_dir_list = map(
        lambda path: os.path.abspath(path),
        lib_dir_list)

    deps_dict = dependencies_dict(fq_input_filename,fq_lib_dir_list)
    fname_depth_pair_list = sorted(
        deps_dict.values(), key= lambda val: val.depth,reverse=True)
    to_return = map(lambda pair: pair.fname,fname_depth_pair_list)
    return to_return

    
    
class FNameDepthPair(object):
    def __init__(self,fname,depth):
        self.fname = fname
        self.depth = depth
    
    
def dependencies_dict(fq_input_filename,lib_dir_list, depth = 0,
                      captured_deps_dict = None):
    '''
    Args:

        fq_input_filename: {String} The fully-qualified name of the
        file we're looking for include dependencies for.
    
        lib_dir_list: {list} Each element is a string representing a
        folder to search for dependency libraries in.  Note: the order
        for searching for includes is 1) relative to current folder of
        file looking for includes for, 2) ascending indices of
        lib_dir_list.

        depth: {int} How many steps from the root file we are in the
        include graph.

        captured_deps_dict: {dict or None} None from first,
        non-recursive caller.  Each index is a string correspoding to
        the fully-qualified filename of a ralph library that that
        fq_input_filename depends on.  Each value is a FNameDepthPair
        containing the fully-qualified file's name and the priority
        with which we should import the file.  Ie., we should import
        files first that have the highest priority.  Priorities are
        essentially the deepest point distance in the include graph
        the file was required.
        
    '''
    if captured_deps_dict is None:
        # note: not including myself in this dict
        captured_deps_dict = {}

    depth = depth + 1
    fq_input_dir = os.path.dirname(fq_input_filename)
    
    preprocessor_statement_list_node = preprocessor_parse(fq_input_filename)
    for node in preprocessor_statement_list_node.statement_list:
        # only care about include nodes
        if isinstance(node,IncludeStatementNode):
            # the relative filename of the ralph library we're trying
            # to import.
            next_to_try_to_include = node.where_to_include_from

            found_dependency = False
            
            # Look first relative to existing file's location, and
            # then relative to other library directories
            
            # FIXME: we're making an assumption that we won't
            # have circular dependencies.
            for folder in [fq_input_dir] + lib_dir_list:
                fq_next_to_try_to_include = os.path.join(
                    folder,next_to_try_to_include)
            
                if os.path.exists(fq_next_to_try_to_include):
                    if fq_next_to_try_to_include not in captured_deps_dict:
                        captured_deps_dict[fq_next_to_try_to_include] = (
                            FNameDepthPair(fq_next_to_try_to_include,depth))
                        dependencies_dict(
                            fq_next_to_try_to_include,lib_dir_list,depth,
                            captured_deps_dict)
                    else:
                        pair = captured_deps_dict[fq_next_to_try_to_include]
                        if pair.depth < depth:
                            captured_deps_dict[fq_next_to_try_to_include] = (
                                FNameDepthPair(fq_next_to_try_to_include,depth))

                    found_dependency = True
                    break

            # could not find requested dependency: throw error
            if not found_dependency:
                raise DependencyException(
                    fq_input_filename,next_to_try_to_include)
                
    return captured_deps_dict
