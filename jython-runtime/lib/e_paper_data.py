from e_paper_runtime import DataAccess
import HDF5

def readData(name):
  return HDF5.read(DataAccess.getData(name))

def createData(name, value):
  ds = DataAccess.createData(name)
  HDF5.create(ds, value)
  DataAccess.finalizeData(ds)
  
