package com.vinaysshenoy.poirot;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that is used to describe the renaming of an Entity from one schema to the next
 * Created by vinaysshenoy on 18/01/16.
 */
public class EntityRenameDesc {

    private int mFromVersion;

    private int mToVersion;

    public final Map<String, String> mNameMap;

    public EntityRenameDesc(Builder builder) {
        mNameMap = new HashMap<>(builder.nameMaps);
    }

    /*package*/ void setFromVersion(int fromVersion) {
        this.mFromVersion = fromVersion;
    }

    /*package*/ void setToVersion(int toVersion) {
        this.mToVersion = toVersion;
    }

    public static final class Builder {

        public final Map<String, String> nameMaps;

        public Builder() {
            nameMaps = new HashMap<>();
        }

        public Builder map(String oldEntityName, String newEntityName) {
            nameMaps.put(oldEntityName, newEntityName);
            return this;
        }

        public EntityRenameDesc build() {

            return new EntityRenameDesc(this);
        }

        public void reset() {
            nameMaps.clear();
        }
    }

}
