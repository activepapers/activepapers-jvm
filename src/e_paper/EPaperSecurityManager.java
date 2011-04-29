package e_paper;

import java.lang.SecurityManager;

public class EPaperSecurityManager extends SecurityManager {
    public Class[] context() {
        return getClassContext();
    }
}
