package active_papers;

import java.lang.SecurityManager;

public class ActivePaperSecurityManager extends SecurityManager {
    @SuppressWarnings("rawtypes")
	public Class[] context() {
        return getClassContext();
    }
}
