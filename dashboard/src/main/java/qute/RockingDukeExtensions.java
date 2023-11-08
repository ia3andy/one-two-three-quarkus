package qute;

import io.quarkus.qute.TemplateExtension;

import java.util.List;
import java.util.Random;

@TemplateExtension(namespace = "duke")
public class RockingDukeExtensions {

    public static final List<String> DUKES = List.of(
            "angus", "eddie", "geddy", "bass", "flyingV", "mohawk", "hendrix", "slash"
    );

    public static String randomName() {
        return DUKES.get(new Random().nextInt(DUKES.size()));
    }

    public static String random() {
        return get(randomName());
    }

    public static String get(String name) {
        return "/static/images/rocking_duke_" + name + ".svg";
    }
}
