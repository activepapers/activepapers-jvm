from e_paper_runtime import HDF5Node
from ch.systemsx.cisd.hdf5 import HDF5DataClass
from java.lang import String
import jarray

type_map = {HDF5DataClass.FLOAT: float,
            HDF5DataClass.INTEGER: int,
            HDF5DataClass.STRING: str}

read_method = {(float, 0): "readDouble",
               (float, 1): "readDoubleArray",
               (int, 0): "readLong",
               (int, 1): "readLongArray",
               (str, 0): "readString",
               (str, 1): "readStringArray"}

write_method = {(float, 0): "writeDouble",
                (float, 1): "writeDoubleArray",
                (int, 0): "writeLong",
                (int, 1): "writeLongArray",
                (str, 0): "writeString",
                (str, 1): "writeStringArray"}

jarray_type = {float: 'd', int: 'l', str: String}

def _is_sequence(arg):
    return (not hasattr(arg, "strip") and
            hasattr(arg, "__getitem__") or
            hasattr(arg, "__iter__"))

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

def read(node):
    dtype, shape = typeAndShape(node)
    try:
        method = read_method[dtype, len(shape)]
    except KeyError:
        raise NotImplementedError(
            "Datasets of %s and rank %d are not yet implemented"
            % (str(dtype), len(shape)))
    return getattr(node.getReader(), method)(node.getPath())

def create(node, value):
    try:
        if _is_sequence(value):
            dtype = type(value[0])
            method = write_method[(dtype, 1)]
            value = jarray.array(value, jarray_type[dtype])
        else:
            dtype = type(value)
            method = write_method[(dtype, 0)]
    except KeyError:
        raise NotImplementedError(
            "Datasets of %s and rank %d are not yet implemented"
            % (str(dtype), len(shape)))
    writer = node.getWriter()
    if writer is None:
        raise IOError("HDF5 file not writable")
    getattr(writer, method)(node.getPath(), value)
