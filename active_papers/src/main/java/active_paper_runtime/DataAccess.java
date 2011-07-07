package active_paper_runtime;

import active_paper.ActivePaperRef;

import active_paper_runtime.HDF5Node;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

import java.util.List;
import java.util.ArrayList;

public final class DataAccess {

    public static HDF5Node getItem(String path) {
        IHDF5Reader reader = ActivePaperRef.getReader();
        IHDF5Writer writer = ActivePaperRef.getWriter();
        if (reader == null)
            throw new RuntimeException("no active paper open");
        if (reader.exists(path)) {
            ActivePaperRef.addDependency(path);
            if (writer == null)
                return new HDF5Node(reader, path).dereference();
            else
                return new HDF5Node(writer, path).dereference();
        }
        else
            throw new RuntimeException("HDF5 path ".concat(path).concat(" does not exist"));
    }

    public static HDF5Node getData(String name) {
        String path = "/data/".concat(name);
        return getItem(path);
    }

    public static HDF5Node getText(String name) {
        String path = "/text/".concat(name);
        return getItem(path);
    }

    public static HDF5Node createData(String name) {
        IHDF5Writer writer = ActivePaperRef.getWriter();
        if (writer == null)
            throw new RuntimeException("active paper not opened or not writable");
        String calclet = ActivePaperRef.getCurrentCalclet();
        if (calclet == null)
            throw new RuntimeException("no calclet active");
        String path = "/data/".concat(name);
        if (writer.exists(path)) {
            boolean may_overwrite = false;
            String attr_name = "active-paper-generating-calclet";
            if (writer.hasAttribute(path, attr_name)) {
                String creator = writer.getStringAttribute(path, attr_name);
                if (creator.equals(calclet))
                    may_overwrite = true;
                else
                    throw new RuntimeException("Attempt to overwrite dataset ".concat(name).concat(" created by calclet ".concat(calclet)));
            }
            if (!may_overwrite)
                throw new RuntimeException("Attempt to overwrite dataset ".concat(name).concat(" not created by any calclet"));
        }
        return new HDF5Node(writer, path);
    }

    public static void finalizeData(HDF5Node node) {
        IHDF5Writer writer = node.getWriter();
        String path = node.getPath();
        if (writer == null)
            throw new RuntimeException("active paper not opened or not writable");
        String calclet = ActivePaperRef.getCurrentCalclet();
        if (calclet == null)
            throw new RuntimeException("no calclet active");
        List<String> deps = new ArrayList<String>(ActivePaperRef.getDependencyList());
        deps.add(calclet);
        String[] jars = writer.getStringArrayAttribute(calclet, "jvm-jar-files");
        for (String jar: jars)
            deps.add(jar);
        writer.setStringAttribute(path, "active-paper-datatype", "data");
        writer.setStringAttribute(path, "active-paper-generating-calclet", calclet);
        writer.setStringArrayAttribute(path, "active-paper-dependencies", deps.toArray(new String[] {}));
    }

}
