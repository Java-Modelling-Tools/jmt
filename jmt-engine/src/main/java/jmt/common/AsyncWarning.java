/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.common;

/**
 * Shows an engine warning from a separate thread after a short delay, so the GUI is up before the popup appears.
 */
public final class AsyncWarning {

	/** Delay before the warning is shown, in milliseconds; tests set it to 0 and restore it. */
	public static volatile long delayMillis = 800;

	private AsyncWarning() {
	}

	/**
	 * Reports the warning to the reporter installed now, after {@link #delayMillis}.
	 * @return the started thread, so tests can join it.
	 */
	public static Thread warn(final String title, final String message) {
		final ErrorReporter reporter = ErrorReporters.get();
		final long delay = delayMillis;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
				reporter.warning(title, message);
			}
		});
		t.setDaemon(true);
		t.start();
		return t;
	}
}
