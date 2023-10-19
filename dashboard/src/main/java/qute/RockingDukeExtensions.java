package qute;

import io.quarkus.qute.TemplateExtension;

import java.util.List;
import java.util.Random;

@TemplateExtension(namespace = "duke")
public class RockingDukeExtensions {

    private static final List<String> DUKES = List.of(
            "angus", "eddie", "geddy", "bass", "flyingV", "mohawk", "hendrix", "slash"
    );

    /**
     * This registers the String.capitalise extension method
     */
    public static String random() {
        StringBuilder sb = new StringBuilder("/static/images/rocking_duke_");
        final int i = new Random().nextInt(DUKES.size());
        return sb.append(DUKES.get(i)).append(".svg").toString();
    }
}
