/*
 * Copyright (c) 2009-2013 Panxiaobo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pxb.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class StringItems extends ArrayList<StringItem> {
	public static final int UTF8_FLAG = 0x00000100;

	public static String[] read(ByteBuffer in) throws IOException {
		return read(in, null);
	}

	public static String[] read(ByteBuffer in, int[] flag) throws IOException {
        int trunkOffset = in.position() - 8;
        int stringCount = in.getInt();
        int styleCount = in.getInt();
        int flags = in.getInt();
		if (flag != null) {
			flag[0] = flags;
		}
        int stringDataOffset = in.getInt();
        int stylesOffset = in.getInt();
        int offsets[] = new int[stringCount];
        String strings[] = new String[stringCount];
        for (int i = 0; i < stringCount; i++) {
            offsets[i] = in.getInt();
        }

        int base = trunkOffset + stringDataOffset;
		for (int i = 0; i < offsets.length; i++) {
			in.position(base + offsets[i]);
			String s;

			if (0 != (flags & UTF8_FLAG)) {
				u8length(in); // ignored
				int u8len = u8length(in);
				int start = in.position();
				int blength = u8len;
				while (in.get(start + blength) != 0) {
					blength++;
				}
				s = new String(in.array(), start, blength, "UTF-8");
				in.position(start + blength + 1); // ADD
			} else {
				int length = u16length(in);
				s = new String(in.array(), in.position(), length * 2, "UTF-16LE");
				in.position(in.position() + (length + 1) * 2); // ADD
			}
			strings[i] = s;
		}
        return strings;
    }

	static int u16length(ByteBuffer in) {
		int length = in.getShort() & 0xFFFF;
		if (length > 0x7FFF) {
			length = ((length & 0x7FFF) << 8) | (in.getShort() & 0xFFFF);
		}
		return length;
	}

	static int u8length(ByteBuffer in) {
		int len = in.get() & 0xFF;
		if ((len & 0x80) != 0) {
			len = ((len & 0x7F) << 8) | (in.get() & 0xFF);
		}
		return len;
	}

	byte[] stringData;

	public int getSize() {
		return 5 * 4 + this.size() * 4 + stringData.length + 0;// TODO
	}

	public void prepare() throws IOException {
		prepare(0);
	}

	private int flags = 0;
	public void prepare(int flags) throws IOException {
		this.flags = flags;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i = 0;
		int offset = 0;
		baos.reset();
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (StringItem item : this) {
			item.index = i++;
			String stringData = item.data;
			Integer of = map.get(stringData);
			if (of != null) {
				item.dataOffset = of;
			} else {
				item.dataOffset = offset;
				map.put(stringData, offset);
				if ((flags & StringItems.UTF8_FLAG) != 0) {
					int length = stringData.length();
					byte[] data = stringData.getBytes("UTF-8");
					int u8length = data.length;

					if (length > 0x7F) {
						offset++;
						baos.write((length >> 8) | 0x80);
					}
					baos.write(length);

					if (u8length > 0x7F) {
						offset++;
						baos.write((u8length >> 8) | 0x80);
					}
					baos.write(u8length);
					baos.write(data);
					baos.write(0);
					offset += 3 + u8length;
				} else {
					int length = stringData.length();
					byte[] data = stringData.getBytes("UTF-16LE");
					if (length > 0x7FFF) {
						int x = (length >> 16) | 0x8000;
						baos.write(x);
						baos.write(x >> 8);
						offset += 2;
					}
					baos.write(length);
					baos.write(length >> 8);
					baos.write(data);
					baos.write(0);
					baos.write(0);
					offset += 4 + data.length;
				}
			}
		}
		// TODO
		stringData = baos.toByteArray();
	}

	public void write(ByteBuffer out) throws IOException {
		out.putInt(this.size());
		out.putInt(0);// TODO style count
		out.putInt(this.flags);
		out.putInt(7 * 4 + this.size() * 4);
		out.putInt(0);
		for (StringItem item : this) {
			out.putInt(item.dataOffset);
		}
		out.put(stringData);
		// TODO
	}
}
