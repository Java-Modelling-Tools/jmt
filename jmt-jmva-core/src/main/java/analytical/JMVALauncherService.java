package analytical;

/**
 * Factory for launching the JMVA wizard given an already-populated analytical.ExactModel.
 * Registered at runtime by jmt-app so that jmt-jsimg-gui's Mediator can open
 * the JMVA tool without a compile-time dependency on JMVAWizard.
 */
public interface JMVALauncherService {

    void launch(ExactModel model);

    final class Registry {
        private static JMVALauncherService instance;

        public static void set(JMVALauncherService s) { instance = s; }

        public static void launch(ExactModel model) {
            if (instance == null) throw new IllegalStateException(
                "analytical.JMVALauncherService not registered — jmt-app not on classpath?");
            instance.launch(model);
        }
    }
}
