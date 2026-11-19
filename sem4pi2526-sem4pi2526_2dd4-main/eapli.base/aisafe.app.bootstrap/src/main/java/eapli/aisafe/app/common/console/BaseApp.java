package eapli.aisafe.app.common.console;

import eapli.aisafe.App;
import eapli.framework.infrastructure.pubsub.EventDispatcher;
import eapli.framework.infrastructure.pubsub.impl.inprocess.service.InProcessPubSub;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseApp {
    final EventDispatcher dispatcher = InProcessPubSub.dispatcher();
    protected static final String SEPARATOR_HR = "=====================================";
    private static final Logger LOGGER = LogManager.getLogger(BaseApp.class);

    public void run(final String[] args) {
        printHeader();
        try {
            doSetupEventHandlers(dispatcher);
            doMain(args);
            printFooter();
        } catch (final Exception e) {
            System.out.println("Something unexpected has happened. Please check the logs.\n");
            LOGGER.error(e);
        } finally {
            try {
                dispatcher.shutdown();
            } catch (final Exception e) {
                LOGGER.error("Unable to cleanup event handlers", e);
            }
        }
        System.exit(0);
    }

    protected void printFooter() {
        System.out.println("\n");
        System.out.println(SEPARATOR_HR);
        System.out.println(appGoodbye());
        System.out.println(SEPARATOR_HR);
    }

    protected void printHeader() {
        System.out.println(SEPARATOR_HR);
        System.out.println(appTitle() + " " + App.VERSION);
        System.out.println(App.COPYRIGHT);
        System.out.println(SEPARATOR_HR);
    }

    protected abstract void doMain(final String[] args);
    protected abstract String appTitle();
    protected abstract String appGoodbye();
    protected abstract void doSetupEventHandlers(EventDispatcher dispatcher);
}
