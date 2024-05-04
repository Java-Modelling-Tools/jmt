package jmt.engine.log;

public class LoggerStateManager {
    private static final LoggerStateManager instance = new LoggerStateManager();
    private boolean logCalenderOn = false;

    private LoggerStateManager() {}

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
