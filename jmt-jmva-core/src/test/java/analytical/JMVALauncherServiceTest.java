package analytical;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JMVALauncherServiceTest {

	@AfterEach
	void unregister() {
		JMVALauncherService.Registry.set(null);
	}

	@Test
	void launchWithoutRegisteredServiceIsAnIllegalState() {
		JMVALauncherService.Registry.set(null);
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> JMVALauncherService.Registry.launch(new ExactModel()));
		assertTrue(e.getMessage().contains("not registered"), e.getMessage());
	}

	@Test
	void launchDelegatesToTheRegisteredServiceWithTheSameModel() {
		ExactModel[] seen = new ExactModel[1];
		JMVALauncherService.Registry.set(m -> seen[0] = m);
		ExactModel m = new ExactModel();
		JMVALauncherService.Registry.launch(m);
		assertSame(m, seen[0]);
	}

	@Test
	void latestRegistrationWins() {
		int[] calls = new int[2];
		JMVALauncherService.Registry.set(m -> calls[0]++);
		JMVALauncherService.Registry.set(m -> calls[1]++);
		JMVALauncherService.Registry.launch(new ExactModel());
		assertArrayEquals(new int[] { 0, 1 }, calls);
	}
}
