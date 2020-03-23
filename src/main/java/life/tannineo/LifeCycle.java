package life.tannineo;

/**
 * the basic life cycle of the application
 */
public interface LifeCycle {

    void start() throws Throwable;

    void destroy() throws Throwable;
}
