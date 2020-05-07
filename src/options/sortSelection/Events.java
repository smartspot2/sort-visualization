package options.sortSelection;

import javafx.event.Event;
import javafx.event.EventType;

public class Events {
    public static final EventType<SortSelect> SORT_SELECT = new EventType<>("SORT_SELECT");

    public static class SortSelect extends Event {
        public SortSelect(EventType<? extends Event> eventType) {
            super(eventType);
        }
    }
}
