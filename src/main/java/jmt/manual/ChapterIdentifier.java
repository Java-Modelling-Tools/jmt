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

package jmt.manual;

/**
 * <p>Title: Chapter Identifier</p>
 * <p>Description: This enum provides a list of identifiers associated with the
 * chapters of the JMT manual.</p>
 *
 * @author Lulai Zhu
 * Date: 16-08-2018
 * Time: 18.00.00
 */
public enum ChapterIdentifier {

	INTRO("1 Intro"),
	JMVA("2 JMVA"),
	JSIMgraph("3 JSIMgraph"),
	JSIMwiz("4 JSIMwiz"),
	JMCH("5 JMCH"),
	JABA("6 JABA"),
	JWAT("7 JWAT");

	private String prefix;

	private ChapterIdentifier(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}

}
