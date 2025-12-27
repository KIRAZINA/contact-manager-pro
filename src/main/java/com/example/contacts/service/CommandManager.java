package com.example.contacts.service;

import com.example.contacts.command.Command;
import com.example.contacts.observer.AuditLogger;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandManager {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private AuditLogger logger;

    public void setLogger(AuditLogger logger) {
        this.logger = logger;
    }

    public void executeCommand(Command cmd) {
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        if (logger != null) logger.log(cmd, "EXECUTE");
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) return;
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        if (logger != null) logger.log(cmd, "UNDO");
    }

    public void redo() {
        if (!canRedo()) return;
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
        if (logger != null) logger.log(cmd, "REDO");
    }
}
