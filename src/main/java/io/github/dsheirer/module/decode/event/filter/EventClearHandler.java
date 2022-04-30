package io.github.dsheirer.module.decode.event.filter;

public interface EventClearHandler {
    void onHistoryLimitChanged(int newHistoryLimit);
    void onClearHistoryClicked();
}
