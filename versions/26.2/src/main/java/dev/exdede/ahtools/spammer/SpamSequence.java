package dev.exdede.ahtools.spammer;

import java.util.ArrayList;
import java.util.List;

/**
 * A repeating cursor over the configured command list: 1, 2, 3, 1, 2, 3 and so
 * on. No random selection, by design: the point of the spammer is a
 * predictable rotation the user can reason about.
 *
 * Blank entries are dropped at construction so an empty row left in the GUI
 * cannot turn into an empty message at the send gate.
 */
public final class SpamSequence {
    private final List<String> commands;
    private int index;

    public SpamSequence(List<String> commands) {
        List<String> usable = new ArrayList<>();
        for (String command : commands) {
            if (command != null && !command.isBlank()) usable.add(command.trim());
        }
        this.commands = List.copyOf(usable);
    }

    public List<String> commands() { return commands; }
    public int index() { return index; }

    /** The next command in the rotation, or null when there are none. */
    public String next() {
        if (commands.isEmpty()) return null;
        String command = commands.get(index);
        index = (index + 1) % commands.size();
        return command;
    }

    public void reset() {
        index = 0;
    }
}
