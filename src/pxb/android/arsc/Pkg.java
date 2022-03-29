/*
 * Copyright (c) 2009-2013 Panxiaobo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pxb.android.arsc;

import java.util.TreeMap;

public class Pkg {
    public final int id;
    public String name;
    public TreeMap<Integer, Type> types = new TreeMap<Integer, Type>();
    public int flags;
    public int typeNameXFlags;
    public int keyNamesXFlags;

    public Pkg(int id, String name, int flags) {
        super();
        this.id = id;
        this.name = name;
        this.flags = flags;
    }

    public Type getType(int tid, String name, int entrySize) {
        Type type = types.get(tid);
        if (type != null) {
            if (name != null) {
                if (type.name == null) {
                    type.name = name;
                } else if (!name.endsWith(type.name)) {
                    throw new RuntimeException("Name not match. Expect: " + type.name + ". Found: " + name);
                }
                if (type.specs.length == 0) {
                    type.specs = new ResSpec[entrySize];
                } else if (type.specs.length != entrySize) {
                    throw new RuntimeException();
                }
            }
        } else {
            type = new Type();
            type.id = tid;
            type.name = name;
            type.specs = new ResSpec[entrySize];
            types.put(tid, type);
        }
        return type;
    }

    public void insertType(int tid, String name) {
        Type type = new Type();
        type.id = tid;
        type.name = name;
        type.specs = new ResSpec[0];
        types.put(tid, type);
    }

}