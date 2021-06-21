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

package jmt.gui.jsimgraph.template;

import java.io.File;

/**
 * @author S Jiang
 * 
 */
public class TemplateData {

	//available in both manifest and index.xml
	private String name;
	private String fileName;
	private String author;
	private String version;
	private String date;
	private String shortDescription;
	private String toolTip;
	private String updateAddress;
	private String descriptionAddress;
	//refer to the .jar file
	private File file;
	//not available in manifest
	//use md5 checksum function
	private String md5; 

	public TemplateData() {

	}

	public TemplateData(String fileName, String shortDescription, String updateAddress) {
		this.fileName = fileName;
		this.shortDescription = shortDescription;
		this.updateAddress = updateAddress;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String description) {
		this.shortDescription = description;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getUpdateAddress() {
		return updateAddress;
	}

	public void setUpdateAddress(String address) {
		this.updateAddress = address;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDescriptionAddress() {
		return descriptionAddress;
	}

	public void setDescriptionAddress(String descriptionAddress) {
		this.descriptionAddress = descriptionAddress;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getToolTip() {
		return toolTip;
	}

	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
	}

}
