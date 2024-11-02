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
package jmt.jmch.animation;

/**
 * Interface to wrap up all the methods for a graphic component
 *
 * @author Lorenzo Torri
 * Date: 02-sept-2024
 * Time: 14.00
 */
public interface GraphicComponent {
    public void setXPos(int xpos);
    public void setYPos(int ypos);

    /** Return the drawing starting position in the panel of a component*/
    public int getXPos();
    public int getYPos();

    /** Get the total length of a component when drawn */
    public int getTotalLenght();
}
