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

package jmt.jmva;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;

import jmt.jmva.engine.simDispatcher.DispatcherJMVAschema;

import org.junit.Test;

/**
 * @author Stefano Omini
 */
public class PauseThreadTest {

	private static final int MAX_WAIT_TIME = 15000;

	@Test
	public void testPausing1SecondSleepAndCheckSimProgress() throws InterruptedException, URISyntaxException {

		DispatcherJMVAschema disp = new DispatcherJMVAschema(new File(getClass().getResource("randomModel_open_1.xml").toURI()));

		Thread1 t1 = new Thread1(disp);
		Thread2 t2 = new Thread2(disp);
		t1.start();
		t2.start();
		t2.join(MAX_WAIT_TIME);
		if (t2.isAlive()) {
			t2.finished = true;
			fail("Thread did not finish");
		}
	}

	@Test
	public void testPausing5SecondsSleepAndAbortAllMeasures() throws InterruptedException, URISyntaxException {

		DispatcherJMVAschema disp = new DispatcherJMVAschema(new File(getClass().getResource("randomModel_open_1.xml").toURI()));

		Thread1 t1 = new Thread1(disp);
		Thread3 t3 = new Thread3(disp);
		t1.start();
		t3.start();
		t3.join(MAX_WAIT_TIME);
		if (t3.isAlive()) {
			t3.finished = true;
			fail("Thread did not finish");
		}
	}

}

class Thread1 extends Thread {

	private DispatcherJMVAschema disp;

	public Thread1(DispatcherJMVAschema disp) {
		this.disp = disp;
	}

	@Override
	public void run() {

		disp.solveModel();

	}

}

class Thread2 extends Thread {

	private DispatcherJMVAschema disp;
	boolean finished = false;

	public Thread2(DispatcherJMVAschema disp) {
		this.disp = disp;
	}

	@Override
	public void run() {

		double progress = 0.0;

		while (!finished) {

			progress = disp.checkSimProgress();

			System.out.println("Progress: " + Double.toString(progress));
			disp.refreshTempMeasures();
			disp.printTempMeasures();

			if (progress >= 1.0) {
				finished = true;
				System.out.println("Finished");
				break;
			}

			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

}

class Thread3 extends Thread {

	private DispatcherJMVAschema disp;
	boolean finished = false;

	public Thread3(DispatcherJMVAschema disp) {
		this.disp = disp;
	}

	@Override
	public void run() {

		double progress = 0.0;

		while (!finished) {

			progress = disp.checkSimProgress();

			System.out.println("Progress: " + Double.toString(progress));
			disp.refreshTempMeasures();
			disp.printTempMeasures();

			try {
				sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (disp.abortAllMeasures()) {
				finished = true;
				System.out.println("Finished");
			}

		}

	}

}
