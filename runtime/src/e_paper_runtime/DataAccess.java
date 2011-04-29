package e_paper_runtime;

import e_paper.ExecutablePaperRef;

import e_paper_runtime.HDF5Node;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

import java.util.List;
import java.util.ArrayList;

public final class DataAccess {

    public static HDF5Node getData(String name) {
        IHDF5Reader reader = ExecutablePaperRef.getReader();
        IHDF5Writer writer = ExecutablePaperRef.getWriter();
        String path = "/data/".concat(name);
        if (reader == null)
            throw new RuntimeException("no e-paper open");
        if (reader.exists(path)) {
            ExecutablePaperRef.addDependency(path);
            if (writer == null)
                return new HDF5Node(reader, path).dereference();
            else
                return new HDF5Node(writer, path).dereference();
        }
        else
            throw new RuntimeException("HDF5 path ".concat(path).concat(" does not exist"));
    }

    public static HDF5Node createData(String name) {
        IHDF5Writer writer = ExecutablePaperRef.getWriter();
        if (writer == null)
            throw new RuntimeException("e-paper not opened or not writable");
        String calclet = ExecutablePaperRef.getCurrentCalclet();
        if (calclet == null)
            throw new RuntimeException("no calclet active");
        String path = "/data/".concat(name);
        if (writer.exists(path)) {
            boolean may_overwrite = false;
            String attr_name = "e-paper-generating-calclet";
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
            throw new RuntimeException("e-paper not opened or not writable");
        String calclet = ExecutablePaperRef.getCurrentCalclet();
        if (calclet == null)
            throw new RuntimeException("no calclet active");
        List<String> deps = new ArrayList<String>(ExecutablePaperRef.getDependencyList());
        deps.add(calclet);
        String[] jars = writer.getStringArrayAttribute(calclet, "jvm-jar-files");
        for (String jar: jars)
            deps.add(jar);
        writer.setStringAttribute(path, "e-paper-datatype", "data");
        writer.setStringAttribute(path, "e-paper-generating-calclet", calclet);
        writer.setStringArrayAttribute(path, "e-paper-dependencies", deps.toArray(new String[] {}));
    }

}
