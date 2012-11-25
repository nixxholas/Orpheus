/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss <aaron@deviant-core.net>
    				Patrick Huy <patrick.huy@frz.cc>
					Matthias Butz <matze@odinms.de>
					Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools.data.output;

import java.awt.Point;
import java.nio.charset.Charset;

import tools.StringUtil;

/**
 * Provides a generic writer of a little-endian sequence of bytes.
 * 
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public class GenericLittleEndianWriter implements LittleEndianWriter {
	private static Charset ASCII = Charset.forName("US-ASCII");
	private ByteOutputStream bos;

	/**
	 * Class constructor - Protected to prevent instantiation with no arguments.
	 */
	protected GenericLittleEndianWriter() {
		// Blah!
	}

	/**
	 * Sets the byte-output stream for this instance of the object.
	 * 
	 * @param bos
	 *            The new output stream to set.
	 */
	void setByteOutputStream(ByteOutputStream bos) {
		this.bos = bos;
	}

	/**
	 * Write an array of bytes to the stream.
	 * 
	 * @param b
	 *            The bytes to write.
	 */
	@Override
	public void write(byte[] b) {
		for (int x = 0; x < b.length; x++) {
			bos.writeByte(b[x]);
		}
	}

	/**
	 * Writes 0 times number of times.
	 * 
	 * @param times
	 *            The number of times to write.
	 */
	public void write0(int times) {
		for (int i = 0; i < times; i++)
			bos.writeByte((byte) 0);
	}

	/**
	 * Write a byte in integer form to the stream.
	 * 
	 * @param b
	 *            The byte as an <code>Integer</code> to write.
	 */
	@Override
	public void writeAsByte(int b) {
		bos.writeByte((byte) b);
	}
	
	@Override
	public void writeAsByte(boolean b) {
		this.writeAsByte(b ? 1 : 0);
	}
	
	@Override
	public void skip(int b) {
		write(new byte[b]);
	}

	/**
	 * Write a short integer to the stream.
	 * 
	 * @param i
	 *            The short integer to write.
	 */
	@Override
	public void writeAsShort(int i) {
		bos.writeByte((byte) (i & 0xFF));
		bos.writeByte((byte) ((i >>> 8) & 0xFF));
	}

	/**
	 * Writes an integer to the stream.
	 * 
	 * @param i
	 *            The integer to write.
	 */
	@Override
	public void writeInt(int i) {
		bos.writeByte((byte) (i & 0xFF));
		bos.writeByte((byte) ((i >>> 8) & 0xFF));
		bos.writeByte((byte) ((i >>> 16) & 0xFF));
		bos.writeByte((byte) ((i >>> 24) & 0xFF));
	}

	/**
	 * Writes an ASCII string the the stream.
	 * 
	 * @param s
	 *            The ASCII string to write.
	 */
	@Override
	public void writeString(String s) {
		write(s.getBytes(ASCII));
	}
	
	@Override
	public void writePaddedString(String s, int padLength) {
		final String padded = StringUtil.getRightPaddedStr(s, '\0', padLength);
		this.writeString(padded);
	}

	/**
	 * Writes a maple-convention ASCII string to the stream.
	 * 
	 * @param s
	 *            The ASCII string to use maple-convention to write.
	 */
	@Override
	public void writeLengthString(String s) {
		writeAsShort((short) s.length());
		writeString(s);
	}

	/**
	 * Writes a null-terminated ASCII string to the stream.
	 * 
	 * @param s
	 *            The ASCII string to write.
	 */
	@Override
	public void writeNullTerminatedString(String s) {
		writeString(s);
		writeAsByte(0);
	}

	/**
	 * Write a long integer to the stream.
	 * 
	 * @param l
	 *            The long integer to write.
	 */
	@Override
	public void writeLong(long l) {
		bos.writeByte((byte) (l & 0xFF));
		bos.writeByte((byte) ((l >>> 8) & 0xFF));
		bos.writeByte((byte) ((l >>> 16) & 0xFF));
		bos.writeByte((byte) ((l >>> 24) & 0xFF));
		bos.writeByte((byte) ((l >>> 32) & 0xFF));
		bos.writeByte((byte) ((l >>> 40) & 0xFF));
		bos.writeByte((byte) ((l >>> 48) & 0xFF));
		bos.writeByte((byte) ((l >>> 56) & 0xFF));
	}

	/**
	 * Writes a 2D 4 byte position information
	 * 
	 * @param s
	 *            The Point position to write.
	 */
	@Override
	public void writeVector(Point s) {
		writeAsShort(s.x);
		writeAsShort(s.y);
	}
}
