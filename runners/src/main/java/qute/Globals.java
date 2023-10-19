package qute;

import io.quarkus.qute.TemplateGlobal;
import io.quarkus.runtime.LaunchMode;

@TemplateGlobal
public class Globals {
    public static final String LAUNCH_MODE = LaunchMode.current().toString();
}
