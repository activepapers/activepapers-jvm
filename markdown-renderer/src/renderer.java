import com.petebevin.markdown.MarkdownProcessor;
import org.lobobrowser.html.parser.*;
import org.lobobrowser.html.test.*;
import org.lobobrowser.html.gui.*;
import org.lobobrowser.html.*;
import javax.swing.*;
import java.io.StringReader;
import org.w3c.dom.*;

import active_paper_runtime.DataAccess;
import active_paper_runtime.HDF5Node;

public class renderer {
    public static void render(String text) {
        JFrame window = new JFrame();
        HtmlPanel panel = new HtmlPanel();
        UserAgentContext ucontext = new SimpleUserAgentContext();
        SimpleHtmlRendererContext rcontext = new SimpleHtmlRendererContext(panel, ucontext);
        DocumentBuilderImpl dbi = new DocumentBuilderImpl(ucontext, rcontext);
        String html = new MarkdownProcessor().markdown(text);
        Document document = null;
        try {
            document = dbi.parse(new InputSourceImpl(new StringReader(html), ""));
        }
        catch (Exception ex) {
        }
        window.getContentPane().add(panel);
        window.setSize(600, 400);
        window.setVisible(true);
        panel.setDocument(document, rcontext);
    }

    public static void main(String[] args) {
        HDF5Node text_node = DataAccess.getItem(args[0]);
        render(text_node.getReader().readString(text_node.getPath()));
    }
}