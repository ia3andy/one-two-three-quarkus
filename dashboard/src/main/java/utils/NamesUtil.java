package utils;

import io.quarkus.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class NamesUtil {

    private static final int MAX_NAME_LENGTH = 4;
    public static final List<String> NAMES;

    static  {
        try(final InputStream nameInputStream = NamesUtil.class.getClassLoader().getResourceAsStream("names.txt")) {
            if (nameInputStream == null) {
                throw new IOException("names list not found");
            }
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(nameInputStream))) {
                final List<String> names = new ArrayList<>();
                while(reader.ready()) {
                    names.add(reader.readLine());
                }
                NAMES = names.stream().filter(s -> s.length() <= MAX_NAME_LENGTH).toList();
                Log.infof("List of names initialized with %d items", NAMES.size());
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error while loading name list", e);
        }
    }

    public static String getNameById(int id) {
        if (id >= NAMES.size()) {
            throw new IllegalArgumentException("This name id is too big: " + id + "/" + NAMES.size());
        }
        return NAMES.get(id);
    }

}
