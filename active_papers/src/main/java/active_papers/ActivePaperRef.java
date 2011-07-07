package active_papers;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.lang.SecurityManager;
import active_papers.ActivePaperSecurityManager;

public final class ActivePaperRef {


    private static Map<ClassLoader,IHDF5Reader> readers = Collections.synchronizedMap(new HashMap<ClassLoader,IHDF5Reader>());
    private static Map<ClassLoader,IHDF5Writer> writers = Collections.synchronizedMap(new HashMap<ClassLoader,IHDF5Writer>());
    private static Map<ClassLoader,String> calclets = Collections.synchronizedMap(new HashMap<ClassLoader,String>());
    private static Map<ClassLoader,ArrayList<String>> dependencies = Collections.synchronizedMap(new HashMap<ClassLoader,ArrayList<String>>());

    @SuppressWarnings("rawtypes")
	public static Class[] getStack() {
        SecurityManager sm = System.getSecurityManager();
        if (sm instanceof ActivePaperSecurityManager) {
            ActivePaperSecurityManager esm = (ActivePaperSecurityManager) sm;
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
        for (@SuppressWarnings("rawtypes") Class cl: getStack()) {
            reader = (IHDF5Reader)readers.get(cl.getClassLoader());
            if (reader != null)
                break;
        }
        return reader;
    }

    public static IHDF5Writer getWriter() {
        IHDF5Writer writer = null;
        for (@SuppressWarnings("rawtypes") Class cl: getStack()) {
            writer = (IHDF5Writer)writers.get(cl.getClassLoader());
            if (writer != null)
                break;
        }
        return writer;
    }

    public static void setCurrentCalclet(ClassLoader cl,
                                         String calclet_name) {
        if (calclet_name == null)
            calclets.remove(cl);
        else
            calclets.put(cl, calclet_name);
    }

    public static String getCurrentCalclet() {
        String calclet = null;
        for (@SuppressWarnings("rawtypes") Class cl: getStack()) {
            calclet = (String)calclets.get(cl.getClassLoader());
            if (calclet != null)
                break;
        }
        return calclet;
    }

    public static void clearDependencyList(ClassLoader cl) {
        dependencies.remove(cl);
    }

    public static void initializeDependencyList(ClassLoader cl) {
        dependencies.put(cl, new ArrayList<String>());
    }

    public static List<String> getDependencyList() {
        List<String> dependency_list = null;
        for (@SuppressWarnings("rawtypes") Class cl: getStack()) {
            dependency_list = dependencies.get(cl.getClassLoader());
            if (dependency_list != null)
                break;
        }
        return dependency_list;
    }

    public static void addDependency(String ds_name) {
        List<String> dependency_list = null;
        for (@SuppressWarnings("rawtypes") Class cl: getStack()) {
            dependency_list = dependencies.get(cl.getClassLoader());
            if (dependency_list != null)
                break;
        }
        if (dependency_list != null && !dependency_list.contains(ds_name))
            dependency_list.add(ds_name);
    }

}
