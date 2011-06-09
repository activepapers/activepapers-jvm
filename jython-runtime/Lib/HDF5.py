
from active_paper_runtime import HDF5Node
from ch.systemsx.cisd.hdf5 import HDF5DataClass
from ch.systemsx.cisd.base.mdarray import MDDoubleArray, MDLongArray
from java.lang import String
import jarray

# Map from HDF5 types to Python types
type_map = {HDF5DataClass.FLOAT: float,
            HDF5DataClass.INTEGER: int,
            HDF5DataClass.STRING: str}

# Tables for looking up the right Java method to call for a given
# data type and rank.
read_method = {(float, 0): "readDouble",
               (float, 1): "readDoubleArray",
               (float, None): "readDoubleMDArrayBlockWithOffset",
               (int, 0): "readLong",
               (int, 1): "readLongArray",
               (int, None): "readLongMDArrayBlockWithOffset",
               (str, 0): "readString",
               (str, 1): "readStringArray",
               (String, None): "readStringMDArrayBlockWithOffset"}

write_method = {(float, 0): "writeDouble",
                (float, 1): "writeDoubleArray",
                (int, 0): "writeLong",
                (int, 1): "writeLongArray",
                (str, 0): "writeString",
                (str, 1): "writeStringArray"}

create_md_method = {float: "createDoubleMDArray",
                    int:   "createLongMDArray",
                    str:   "createStringMDArray"}

write_md_method = {float: "writeDoubleMDArrayBlockWithOffset",
                   int:   "writeLongMDArrayBlockWithOffset",
                   str:   "writeStringMDArrayBlockWithOffset"}

mdarray_constructor = {float: MDDoubleArray,
                       int:   MDLongArray}

# Map from Python type to jarray element typecode
jarray_type = {float: 'd', int: 'l', str: String}

# An array_spec is used as a value when calling createData
# to create an empty dataset.
class array_spec(object):

    def __init__(self, dtype, chunk_size):
        assert dtype in [float, int, str]
        assert isinstance(chunk_size, tuple)
        assert len(chunk_size) > 0
        assert all(isinstance(n, int) for n in chunk_size)
        self.dtype = dtype
        self.chunk_size = chunk_size

# Check if a value is a sequence
def _is_sequence(arg):
    return (not hasattr(arg, "strip") and
            hasattr(arg, "__getitem__") or
            hasattr(arg, "__iter__"))

# Return the element type and shape of the data in a dataset.
def typeAndShape(node):
    reader = node.getReader()
    path = node.getPath()
    info = reader.getDataSetInformation(path)
    try:
        dtype = type_map[info.getTypeInformation().getDataClass()]
    except KeyError:
        raise NotImplementedError(
            "Data type %s not yet implemented"
            % str(info.getTypeInformation().getDataClass()))
    if info.isScalar():
        return dtype, ()
    else:
        return dtype, tuple(int(n) for n in info.getDimensions())

# Read a complete dataset into memory. The return type is
# a Python scalar, a 1D Java array, or an MDArray if rank > 1.
def read(node, offset=None, block_size=None):
    dtype, shape = typeAndShape(node)
    rank = len(shape)
    if offset is None:
        offset = rank*(0,)
    else:
        assert isinstance(offset, tuple)
        assert len(offset) == rank
        assert all(isinstance(n, int) for n in offset)
    if block_size is None:
        block_size = tuple(s-o for s, o in zip(shape, offset))
    else:
        assert isinstance(block_size, tuple)
        assert len(block_size) == rank
        assert all(isinstance(n, int) for n in block_size)
    if rank == 0 or (offset == rank*(0,) and block_size == shape):
        try:
            method = read_method[dtype, rank]
        except KeyError:
            raise NotImplementedError(
                "Datasets of %s and rank %d are not yet implemented"
                % (str(dtype), rank))
        value = getattr(node.getReader(), method)(node.getPath())
        if shape == ():
            return dtype(value)
        else:
            return value
    try:
        method = read_method[dtype, None]
    except KeyError:
        raise NotImplementedError(
            "Datasets of type %s are not yet implemented"
            % str(dtype))
    value = getattr(node.getReader(), method)(node.getPath(),
                                              jarray.array(block_size, 'i'),
                                              jarray.array(offset, 'l'))
    if rank == 1:
        return value.getAsFlatArray()
    else:
        return value

# Create a new dataset. If value is an ArraySpec, create an empty
# array dataset. Otherwise, create a dataset according to the type of
# value and write it immediately.
def create(node, value):
    try:
        if isinstance(value, array_spec):
            method = create_md_method[value.dtype]
            value = jarray.array(value.chunk_size, 'i')
        elif _is_sequence(value):
            dtype = type(value[0])
            method = write_method[(dtype, 1)]
            value = jarray.array(value, jarray_type[dtype])
        else:
            dtype = type(value)
            method = write_method[(dtype, 0)]
    except KeyError:
        raise NotImplementedError(
            "Datasets of type %s are not yet implemented"
            % str(dtype))
    writer = node.getWriter()
    if writer is None:
        raise IOError("HDF5 file not writable")
    getattr(writer, method)(node.getPath(), value)

# Convert data into an MDArray
def makeMDArray(value):
    if _is_sequence(value):
        dtype = type(value[0])
        try:
            constructor = mdarray_constructor[dtype]
        except KeyError:
            raise NotImplementedError(
                "Datasets of type %s are not yet implemented"
                % str(dtype))
        return constructor(jarray.array(value, jarray_type[dtype]),
                           jarray.array([len(value)], 'l'))
    else:
        raise ValueError("can't store scalar in an array")
        
# Write data to a previously created array dataset.
def write(node, value, offset=None):
    dtype, shape = typeAndShape(node)
    if offset is None:
        offset = len(shape)*(0,)
    else:
        assert isinstance(offset, tuple)
        assert all(isinstance(n, int) for n in offset)
        assert len(shape) == len(offset)
    try:
        method = write_md_method[dtype]
    except KeyError:
        raise NotImplementedError(
            "Datasets of type %s are not yet implemented"
            % str(dtype))
    writer = node.getWriter()
    if writer is None:
        raise IOError("HDF5 file not writable")
    getattr(writer, method)(node.getPath(), makeMDArray(value),
                            jarray.array(offset, 'l'))
