package e_paper_runtime;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.IHDF5Factory;
import java.io.File;

public class HDF5Node {

    final static private File e_paper_library = new File(System.getenv("EPAPER_LIBRARY"));
    final static private IHDF5Factory hdf5_factory = HDF5FactoryProvider.get();

    protected IHDF5Reader reader;
    protected IHDF5Writer writer;
    protected String path;

    public HDF5Node(IHDF5Writer writer, String path) {
        this.writer = writer;
        this.reader = (IHDF5Reader)writer;
        this.path = path;
    }

    public HDF5Node(IHDF5Writer writer, String path, boolean writable) {
        if (writable)
            this.writer = writer;
        else
            this.writer = null;
        this.reader = (IHDF5Reader)writer;
        this.path = path;
    }

    public HDF5Node(IHDF5Reader reader, String path) {
        this.writer = null;
        this.reader = reader;
        this.path = path;
    }

    public boolean isWritable() {
        return this.writer != null;
    }

    public String getPath() {
        return this.path;
    }

    public IHDF5Reader getReader() {
        return this.reader;
    }

    public IHDF5Writer getWriter() {
        return this.writer;
    }

    public boolean isReference() {
        String attr_name = "e-paper-datatype";
        return (reader.hasAttribute(path, attr_name)
                && reader.getStringAttribute(path, attr_name).equals("reference"));
    }

    public HDF5Node dereference() {
        if (isReference()) {
            String[] data = reader.readStringArray(path);
            String library_name = data[0];
            String path = data[1];
            File library_file = new File(e_paper_library, library_name);
            return new HDF5Node(hdf5_factory.openForReading(library_file), path).dereference();
        }
        else
            return this;
    }

}
