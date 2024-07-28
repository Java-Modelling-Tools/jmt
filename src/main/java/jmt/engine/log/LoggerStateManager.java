package jmt.engine.log;

public class LoggerStateManager {
    private static final LoggerStateManager instance = new LoggerStateManager();
    private boolean logCalenderOn = false;
    //a list of strong of allowed events
    private static String[] allowedEvents = {};
    private LoggerStateManager() {}

    public static void addAllowedEvent(String event) {
        if (event == null) {
            return;
        }
        for (String allowedEvent : allowedEvents) {
            if (allowedEvent.equals(event)) {
                return;
            }
        }
        String[] newAllowedEvents = new String[allowedEvents.length + 1];
        System.arraycopy(allowedEvents, 0, newAllowedEvents, 0, allowedEvents.length);
        newAllowedEvents[allowedEvents.length] = event;
        allowedEvents = newAllowedEvents;
    }

    public static void deleteAllowedEvent(String event) {
        if (event == null) {
            return;
        }
        for (int i = 0; i < allowedEvents.length; i++) {
            if (allowedEvents[i].equals(event)) {
                String[] newAllowedEvents = new String[allowedEvents.length - 1];
                System.arraycopy(allowedEvents, 0, newAllowedEvents, 0, i);
                System.arraycopy(allowedEvents, i + 1, newAllowedEvents, i, allowedEvents.length - i - 1);
                allowedEvents = newAllowedEvents;
                return;
            }
        }
    }

    public static void clearAllowedEvents() {
        allowedEvents = new String[0];
    }

    public static String[] getAllowedEvents() {
        return allowedEvents;
    }

    public static LoggerStateManager getInstance() {
        return instance;
    }

    public void setLogCalenderOn(boolean logCalenderOn) {
        this.logCalenderOn = logCalenderOn;
    }

    public boolean isLogCalenderOn() {
        return logCalenderOn;
    }
}
