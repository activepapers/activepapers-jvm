package e_paper;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.lang.SecurityManager;
import e_paper.EPaperSecurityManager;

public final class ExecutablePaperRef {


    private static Map readers = Collections.synchronizedMap(new HashMap());
    private static Map writers = Collections.synchronizedMap(new HashMap());
    private static Map programs = Collections.synchronizedMap(new HashMap());
    private static Map dependencies = Collections.synchronizedMap(new HashMap());

    public static Class[] getStack() {
        SecurityManager sm = System.getSecurityManager();
        if (sm instanceof EPaperSecurityManager) {
            EPaperSecurityManager esm = (EPaperSecurityManager) sm;
            return esm.context();
        }
        else
            return new Class[] {};
    }

    public static void setAccessors(ClassLoader cl,
                                    IHDF5Reader aReader,
                                    IHDF5Writer aWriter) {
        if (aReader == null)
            readers.remove(cl);
        else
            readers.put(cl, aReader);
        if (aWriter == null)
            writers.remove(cl);
        else
            writers.put(cl, aWriter);
    }

    public static IHDF5Reader getReader() {
        IHDF5Reader reader = null;
        for (Class cl: getStack()) {
            reader = (IHDF5Reader)readers.get(cl.getClassLoader());
            if (reader != null)
                break;
        }
        return reader;
    }

    public static IHDF5Writer getWriter() {
        IHDF5Writer writer = null;
        for (Class cl: getStack()) {
            writer = (IHDF5Writer)writers.get(cl.getClassLoader());
            if (writer != null)
                break;
        }
        return writer;
    }

    public static void setCurrentProgram(ClassLoader cl,
                                         String program_name) {
        if (program_name == null)
            programs.remove(cl);
        else
            programs.put(cl, program_name);
    }

    public static String getCurrentProgram() {
        String program = null;
        for (Class cl: getStack()) {
            program = (String)programs.get(cl.getClassLoader());
            if (program != null)
                break;
        }
        return program;
    }

    public static void clearDependencyList(ClassLoader cl) {
        dependencies.remove(cl);
    }

    public static void initializeDependencyList(ClassLoader cl) {
        dependencies.put(cl, new ArrayList());
    }

    public static List getDependencyList() {
        List dependency_list = null;
        for (Class cl: getStack()) {
            dependency_list = (List)dependencies.get(cl.getClassLoader());
            if (dependency_list != null)
                break;
        }
        return dependency_list;
    }

    public static void addDependency(String ds_name) {
        List dependency_list = null;
        for (Class cl: getStack()) {
            dependency_list = (List)dependencies.get(cl.getClassLoader());
            if (dependency_list != null)
                break;
        }
        if (dependency_list != null)
            dependency_list.add(ds_name);
    }

}
