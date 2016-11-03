package com.nike.cerberus.operation;

import com.nike.cerberus.command.Command;

/**
 * Interface implemented by operations executable by the CLI.
 */
public interface Operation<C extends Command> {

    void run(final C command);

    boolean isRunnable(final C command);
}
