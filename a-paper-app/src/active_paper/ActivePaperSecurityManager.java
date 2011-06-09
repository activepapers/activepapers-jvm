package active_paper;

import java.lang.SecurityManager;

public class ActivePaperSecurityManager extends SecurityManager {
    public Class[] context() {
        return getClassContext();
    }
}
