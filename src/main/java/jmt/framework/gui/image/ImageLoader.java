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

package jmt.framework.gui.image;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;

import javax.swing.ImageIcon;

/**
 * <p><b>Name:</b> ImageLoader</p> 
 * <p><b>Description:</b> 
 * A class used to load images. Each subclass must implement the <code>getImageURL</code>
 * method to retrieve URL of resource to be loaded. This class holds a cache of loaded images.
 * Cache size can be adjusted using the <code>maxCache</code> field.
 * </p>
 * <p><b>Date:</b> 23/gen/07
 * <b>Time:</b> 15:09:14</p>
 * @author Bertoli Marco
 * @version 1.0
 */
public abstract class ImageLoader {

	/**
	 * Modifier to be used for mirror images (rotated on y axis)
	 */
	public static final String MODIFIER_MIRROR = "MIRROR";
	/**
	 * Modifier to be used for rotating images (rotated on y axis)
	 */
	public static final String MODIFIER_ROTATE = "ROTATE";
	/**
	 * Modifier to be used for mirror images  and rotating images (rotated on y axis)
	 */
	public static final String MODIFIER_MIRROR_AND_ROTATE = "MIRROR_ROTATE";
	/**
	 * Modifier to be used for pressed button images
	 */
	public static final String MODIFIER_PRESSED = "P";
	/**
	 * Modifier to be used for rollover button images
	 */
	public static final String MODIFIER_ROLLOVER = "RO";
	/**
	 * Modifier to be used for selected (toggled) button images
	 */
	public static final String MODIFIER_SELECTED = "S";
	/**
	 * Template used to derive modified toolbar images
	 */
	private static final String TEMPLATE_IMAGE_NAME = "templateToolbarImage";

	/**
	 * Allowed images extensions
	 */
	private static final String[] EXTENSIONS = new String[]{".gif", ".png", ".jpg"};
	/**
	 * Maximum number of cached elements
	 */
	protected int maxCache = 256;
	/**
	 * Internal caching data structure
	 */
	protected HashMap<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>();

	/**
	 * Returns the url of a given resource
	 *
	 * @param resourceName name of the resource to be retrieved
	 * @return url of the resource or null if it was not found
	 */
	protected abstract URL getImageURL(String resourceName);

	/**
	 * Loads an icon with specified name, and caches it.
	 *
	 * @param iconName name of the icon to be loaded
	 * @return icon if found, null otherwise
	 */
	public ImageIcon loadIcon(String iconName) {
		return this.loadIcon(iconName, (String) null);
	}

	/**
	 * Loads an icon with specified name and modifier, and caches it.
	 *
	 * @param iconName name of the icon to be loaded
	 * @param modifier modifier to be applied
	 * @return icon if found, null otherwise
	 */
	public ImageIcon loadIcon(String iconName, String modifier) {
		return this.loadIcon(iconName,modifier,0.0);
	}

	/**
	 * Loads an icon with specified name and size, caches it and then resizes it.
	 *
	 * @param iconName name of the icon to be loaded
	 * @param size     target dimension of image
	 * @return icon if found, null otherwise
	 */
	public ImageIcon loadIcon(String iconName, Dimension size) {
		return this.loadIcon(iconName, (String) null, size);
	}

	/**
	 * Loads an icon with specified name, modifier and size, caches it and then resizes it.
	 *
	 * @param iconName name of the icon to be loaded
	 * @param modifier modifier to be applied
	 * @param size     target dimension of image
	 * @return icon if found, null otherwise
	 */
	public ImageIcon loadIcon(String iconName, String modifier, Dimension size) {
		ImageIcon icon = loadIcon(iconName, modifier);
		if (icon != null) {
			Image image = icon.getImage();
			image = image.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
			return new ImageIcon(image);
		} else {
			return icon;
		}
	}


	/**
	 * Loads an icon with specified name, modifier and size, caches it and then resizes it.
	 *
	 * @param iconName name of the icon to be loaded
	 * @param modifier modifier to be applied
	 * @param angle    rotation angle of the image in degrees
	 * @return icon if found, null otherwise
	 */
	public ImageIcon loadIcon(String iconName, String modifier, Double angle) {
		if (iconName == null) {
			return null;
		}
		String derivedName = iconName;
		// Applies modifier to icon name if needed
		if (modifier != null) {
			derivedName = deriveIconName(iconName, modifier);
		}
		if(angle != 0.0){
			derivedName = derivedName + "_" + ((int)  Math.floor(angle));
		}

		if (iconCache.containsKey(derivedName)) {
			return iconCache.get(derivedName);
		}

		URL url = null;
		// If image is not found, tries to add extensions
		for (int i = 0; i < EXTENSIONS.length && url == null; i++) {
			url = getImageURL(derivedName + EXTENSIONS[i]);
		}

		ImageIcon icon = null;
		if (url != null) {
			icon = new ImageIcon(url);
		} else if (modifier != null) {
			// Loads base icon without modifiers
			ImageIcon base = loadIcon(iconName);
			if (base != null) {
				// Applies known modifiers
				icon = applyModifier(base, modifier,angle);
			}
		}

		// Clears cache when it is bigger than maxCache. This is not needed but avoids memory leakages...
		if (iconCache.size() > maxCache) {
			iconCache.clear();
		}

		iconCache.put(derivedName, icon);
		return icon;
	}


	/**
	 * Derives an icon name to apply suffixes before extension
	 *
	 * @param iconName name of the icon to be derived
	 * @param modifier suffix to be appended
	 * @return derived name
	 */
	public static String deriveIconName(String iconName, String modifier) {
		int dot = iconName.lastIndexOf('.');
		if (dot < 0) {
			return iconName + modifier;
		} else {
			return iconName.substring(0, dot) + modifier + iconName.substring(dot);
		}
	}

	// --- Methods to apply modifiers to loaded icons ---------------------------------------------------

	/**
	 * Apply known modifiers to given image
	 *
	 * @param base     base image to be modified
	 * @param modifier modifier to be applied
	 * @return modified image
	 */
	protected ImageIcon applyModifier(ImageIcon base, String modifier) {
		return this.applyModifier(base, modifier, 0.0);
	}

	/**
	 * Apply known modifiers to given image
	 *
	 * @param base     base image to be modified
	 * @param modifier modifier to be applied
	 * @param angle rotation angle in degrees
	 * @return modified image
	 */
	protected ImageIcon applyModifier(ImageIcon base, String modifier, Double angle) {
		if (modifier.equals(MODIFIER_MIRROR)) {
			return mirrorImage(base);
		} else if (modifier.equals(MODIFIER_ROTATE)){
			return rotate(base, angle);
		} else if (modifier.equals(MODIFIER_MIRROR_AND_ROTATE)){
			ImageIcon iconMirrored= mirrorImage( base);
			return rotate( iconMirrored, angle);
		} else if (modifier.equals(MODIFIER_ROLLOVER) || modifier.equals(MODIFIER_SELECTED)) {
			ImageIcon background = loadIcon(deriveIconName(TEMPLATE_IMAGE_NAME, modifier));
			if (background != null) {
				return mergeIcons(background, base, new Point(0, 0), new Point(background.getIconWidth(), background.getIconHeight()),
						new Point(0, 0));
			}
		} else if (modifier.equals(MODIFIER_PRESSED)) {
			ImageIcon background = loadIcon(deriveIconName(TEMPLATE_IMAGE_NAME, modifier));
			if (background != null) {
				return mergeIcons(background, base, new Point(0, 0), new Point(background.getIconWidth(), background.getIconHeight()),
						new Point(4, 4));
			}
		}
		// Modifier unsupported or template images not found.
		return base;
	}

	/**
	 * Merge two icons together. Output icon dimension is the same as background one.
	 *
	 * @param background background icon
	 * @param overlay    overlay icon, needs to be transparent (will be moved and cropped)
	 * @param cropMin    minimum crop point for overlay icon (0,0 for no crop)
	 * @param cropMax    maximum crop point for overlay icon (width and height for no crop)
	 * @param move       upper left corner where cropped overlay image should be positioned on background image
	 * @return merged icon
	 */
	protected ImageIcon mergeIcons(ImageIcon background, ImageIcon overlay, Point cropMin, Point cropMax, Point move) {
		BufferedImage out = new BufferedImage(background.getIconWidth(), background.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(background.getImage(), 0, 0, null);
		g.drawImage(overlay.getImage(), move.x, move.y, move.x + cropMax.x - cropMin.x, move.y + cropMax.y - cropMin.y, cropMin.x, cropMin.y,
				cropMax.x, cropMax.y, null);
		return new ImageIcon(out);
	}

	/**
	 * Rotates image on y axis
	 *
	 * @param base image to be modified
	 * @return mirrored image
	 */
	protected ImageIcon mirrorImage(ImageIcon base) {
		BufferedImage out = new BufferedImage(base.getIconWidth(), base.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(base.getImage(), base.getIconWidth(), 0, 0, base.getIconHeight(), 0, 0, base.getIconWidth(), base.getIconHeight(), null);
		return new ImageIcon(out);
	}


	/**
	 * Rotates image on y axis
	 *
	 * @param base  image to be modified
	 * @param angle rotation angle in degrees
	 * @return rotated image
	 *
	 * @author Emma Bortone
	 * Date: April 2020
	 */
	protected ImageIcon rotate(ImageIcon base, Double angle) {
		double radianAngle= Math.toRadians(angle);

		//width and height of the original image
		int widthBase= base.getIconWidth();
		int heightBase= base.getIconHeight();

		//width and height of the new image
		int widthNew= (int) Math.ceil(widthBase * Math.abs(Math.cos(radianAngle)) + heightBase * Math.abs(Math.sin(radianAngle)) );
		int heightNew= (int) Math.ceil(widthBase * Math.abs(Math.sin(radianAngle))+ heightBase * Math.abs(Math.cos(radianAngle)) );

		BufferedImage out = new BufferedImage(widthNew,heightNew, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();

		AffineTransform identity = new AffineTransform();
		AffineTransform trans = new AffineTransform();

		trans.setTransform(identity);

		// translation of the image to the center of the new rectangle
		trans.translate((widthNew-widthBase)/2.0,(heightNew-heightBase)/2.0);
		// rotation of the image
		trans.rotate(radianAngle,widthBase/2.0, heightBase/2.0 );
		g.drawImage(base.getImage(), trans, null);

		return new ImageIcon(out);
	}



}
