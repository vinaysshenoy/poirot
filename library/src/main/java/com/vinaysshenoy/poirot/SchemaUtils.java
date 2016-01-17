package com.vinaysshenoy.poirot;

import com.squareup.javapoet.ClassName;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

import java.util.*;

/**
 * Created by vinaysshenoy on 17/01/16.
 */
public final class SchemaUtils {

    private SchemaUtils() {

    }

    public static ClassName generateMigrationName(String packageName, Schema from, Schema to) {
        return ClassName.get(packageName, String.format(Locale.US, "MigrateV%dToV%d", from.getVersion(), to.getVersion()));
    }

    public static Map<String, Entity> entityMapFromSchema(Schema schema) {

        final Map<String, Entity> entityMap = new HashMap<>((int) (schema.getEntities().size() * 1.33F));
        for (Entity entity : schema.getEntities()) {
            entityMap.put(entity.getClassName(), entity);
        }
        return entityMap;
    }

    public static AbstractList<String> entityClassList(Schema schema) {

        final AbstractList<String> entityClassList = new ArrayList<>(schema.getEntities().size());
        for (Entity entity : schema.getEntities()) {
            entityClassList.add(entity.getClassName());
        }

        return entityClassList;
    }

    public static List<Entity> getAdded(Schema prev, Schema cur) {

        final AbstractList<String> prevEntityClassList = entityClassList(prev);
        final AbstractList<String> curEntityClassList = entityClassList(cur);

        //Remove all entity classes from the current list that are present in the older list
        curEntityClassList.removeAll(prevEntityClassList);
        if (curEntityClassList.size() > 0) {
            //We have entities that have been added
            final Map<String, Entity> entityMap = entityMapFromSchema(cur);
            final Iterator<Map.Entry<String, Entity>> iterator = entityMap.entrySet().iterator();
            Map.Entry<String, Entity> next;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (!curEntityClassList.contains(next.getKey())) {
                    iterator.remove();
                }
            }
            return new ArrayList<>(entityMap.values());
        } else {
            return Collections.emptyList();
        }
    }
}
