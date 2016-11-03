package com.nike.cerberus.command;

import com.nike.cerberus.operation.Operation;

/**
 * Interface implemented by all commands available via the CLI.
 */
public interface Command {

    String getCommandName();

    Class<? extends Operation<?>> getOperationClass();
}
