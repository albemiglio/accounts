package it.albemiglio.accounts.core.objects;

import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
public class Task {

    private Pair<UUID, UUID> migration;
    private String username;
    private int currFailures;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task task = (Task) o;

        if (!Objects.equals(migration, task.migration)) return false;
        return Objects.equals(username, task.username);
    }

    @Override
    public String toString() {
        return migration.getLeft() + ";" + migration.getRight() + ";" + username + ";" + currFailures;
    }

    public static Task fromString(String task) {
        // A username can legitimately contain ';' (e.g. a Bedrock/Floodgate-prefixed name), so we can't
        // blind-split: the two UUIDs never contain ';' and currFailures is the trailing int, so anchor on
        // the first two separators and the last one, and let everything in between be the username.
        int firstSep = task.indexOf(';');
        int secondSep = firstSep < 0 ? -1 : task.indexOf(';', firstSep + 1);
        int lastSep = task.lastIndexOf(';');
        if (secondSep < 0 || lastSep <= secondSep) {
            throw new IllegalArgumentException("Malformed migration task: " + task);
        }
        Task t = new Task();
        t.setMigration(new Pair<>(
                UUID.fromString(task.substring(0, firstSep)),
                UUID.fromString(task.substring(firstSep + 1, secondSep))));
        t.setUsername(task.substring(secondSep + 1, lastSep));
        t.setCurrFailures(Integer.parseInt(task.substring(lastSep + 1)));
        return t;
    }
}
