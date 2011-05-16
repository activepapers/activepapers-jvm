from e_paper_runtime import DataAccess
import HDF5

def readData(name):
  return HDF5.read(DataAccess.getData(name))

def createData(name, value):
  ds = DataAccess.createData(name)
  HDF5.create(ds, value)
  DataAccess.finalizeData(ds)

# Add the Lib directories of all jars on the classpath to sys.path
# This should probably be done somewhere else as part of Jython
# initialization.
import sys
from java.lang import Thread
cl = Thread.currentThread().getContextClassLoader()
for url in cl.getURLs():
  path = str(url.getPath())
  # Should check if /Lib actually exists in each jar.
  # Should check for Jython jar to avoid adding it a second time.
  sys.path.append(path+'/Lib')
del path
del url
del cl
del Thread
del sys
