package active_paper;

import java.lang.SecurityManager;

public class ActivePaperSecurityManager extends SecurityManager {
    @SuppressWarnings("rawtypes")
	public Class[] context() {
        return getClassContext();
    }
}
