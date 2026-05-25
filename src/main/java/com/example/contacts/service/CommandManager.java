package com.example.contacts.service;

import com.example.contacts.command.Command;
import com.example.contacts.observer.AuditLogger;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages command execution, undo/redo history, and audit notification.
 *
 * Improvements (see Architectural Review):
 *  - Issue 2.1: History is now bounded by MAX_HISTORY to prevent unbounded heap growth.
 *               When the undo stack exceeds the limit, the oldest (bottom) entry is
 *               silently dropped — the user simply cannot undo that far back.
 *  - Issue 2.4: Commands already carry an {@code executed} flag guard — idempotency
 *               is maintained at the command level.
 */
public final class CommandManager {

    /**
     * Maximum number of commands retained in either history stack.
     * When exceeded, the oldest command is dropped (FIFO eviction).
     * 1 000 operations is enough for any realistic session.
     */
    private static final int MAX_HISTORY = 1_000;

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
        // Evict oldest entry when the stack is full (Issue 2.1)
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        if (logger != null) {
            logger.log(cmd, "EXECUTE");
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) {
            System.out.println("Nothing to undo");
            return;
        }
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        // Evict oldest redo entry when the stack is full
        if (redoStack.size() > MAX_HISTORY) {
            redoStack.removeLast();
        }
        if (logger != null) {
            logger.log(cmd, "UNDO");
        }
    }

    public void redo() {
        if (!canRedo()) {
            System.out.println("Nothing to redo");
            return;
        }
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        if (logger != null) {
            logger.log(cmd, "REDO");
        }
    }

    public int getUndoStackSize() {
        return undoStack.size();
    }

    public int getRedoStackSize() {
        return redoStack.size();
    }

    /** Exposed for tests and diagnostics. */
    public int getMaxHistory() {
        return MAX_HISTORY;
    }
}
