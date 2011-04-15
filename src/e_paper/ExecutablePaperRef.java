package e_paper;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import java.util.List;
import java.util.ArrayList;

public final class ExecutablePaperRef {

    private static IHDF5Reader reader = null;
    private static IHDF5Writer writer = null;
    private static String current_program = null;
    private static ArrayList dependencies = null;

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

    public static void setCurrentProgram(String program_name) {
        current_program = program_name;
    }

    public static String getCurrentProgram() {
        return current_program;
    }

    public static void clearDependencyList() {
        dependencies = null;
    }

    public static void initializeDependencyList() {
        dependencies = new ArrayList();
    }

    public static List getDependencyList() {
        return dependencies;
    }

    public static void addDependency(String ds_name) {
        dependencies.add(ds_name);
    }

}