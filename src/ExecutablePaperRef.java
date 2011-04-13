import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

public final class ExecutablePaperRef {

    private static IHDF5Reader reader;
    private static IHDF5Writer writer;

    public static void setAccessors(IHDF5Reader aReader, IHDF5Writer aWriter) {
        reader = aReader;
        writer = aWriter;
    }

    public static IHDF5Reader getReader() {
        return reader;
    }

    public static IHDF5Writer getWriter() {
        return writer;
    }

}