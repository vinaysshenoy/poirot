package com.vinaysshenoy.poirot;

import com.squareup.javapoet.ClassName;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

import java.io.File;
import java.util.*;

/**
 * Created by vinaysshenoy on 17/01/16.
 */
public final class Utils {

    private Utils() {

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

    public static Map<String, Property> propertyMapFromEntity(Entity entity) {

        final Map<String, Property> propertyMap = new HashMap<>((int) (entity.getProperties().size() * 1.33F));
        for (Property property : entity.getProperties()) {
            propertyMap.put(property.getPropertyName(), property);
        }
        return propertyMap;
    }

    public static AbstractList<String> entityClassList(Schema schema) {

        final AbstractList<String> entityClassList = new ArrayList<>(schema.getEntities().size());
        for (Entity entity : schema.getEntities()) {
            entityClassList.add(entity.getClassName());
        }

        return entityClassList;
    }

    public static AbstractList<String> propertyNameList(Entity entity) {

        final AbstractList<String> propertyNameList = new ArrayList<>(entity.getProperties().size());
        for (Property property : entity.getProperties()) {
            propertyNameList.add(property.getPropertyName());
        }

        return propertyNameList;
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

    public static List<Property> getAddedProperties(Entity prev, Entity cur) {

        final AbstractList<String> prevPropertyNameList = propertyNameList(prev);
        final AbstractList<String> curPropertyNameList = propertyNameList(cur);

        //Remove all properties from the current list that are present in the older list
        curPropertyNameList.removeAll(prevPropertyNameList);
        if (curPropertyNameList.size() > 0) {
            //We have entities that have been added
            final Map<String, Property> propertyMap = propertyMapFromEntity(cur);
            final Iterator<Map.Entry<String, Property>> iterator = propertyMap.entrySet().iterator();
            Map.Entry<String, Property> next;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (!curPropertyNameList.contains(next.getKey())) {
                    iterator.remove();
                }
            }
            return new ArrayList<>(propertyMap.values());
        } else {
            return Collections.emptyList();
        }
    }

    public static List<Property> getRemovedProperties(Entity prev, Entity cur) {

        final AbstractList<String> prevPropertyNameList = propertyNameList(prev);
        final AbstractList<String> curPropertyNameList = propertyNameList(cur);

        //Remove all properties from the current list that are present in the older list
        curPropertyNameList.removeAll(prevPropertyNameList);
        if (curPropertyNameList.size() > 0) {
            //We have entities that have been added
            final Map<String, Property> propertyMap = propertyMapFromEntity(cur);
            final Iterator<Map.Entry<String, Property>> iterator = propertyMap.entrySet().iterator();
            Map.Entry<String, Property> next;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (curPropertyNameList.contains(next.getKey())) {
                    iterator.remove();
                }
            }
            return new ArrayList<>(propertyMap.values());
        } else {
            return Collections.emptyList();
        }
    }


    public static Map<Entity, Entity> getCommonEntitiesAsMap(Schema prev, Schema cur) {

        final AbstractList<String> prevEntityClassList = entityClassList(prev);
        final AbstractList<String> curEntityClassList = entityClassList(cur);

        //Retain all entity classes from the current list that are present in the older list
        curEntityClassList.retainAll(prevEntityClassList);
        if (curEntityClassList.size() > 0) {

            final Map<String, Entity> prevEntityMap = entityMapFromSchema(prev);
            final Map<String, Entity> curEntityMap = entityMapFromSchema(cur);

            final Map<Entity, Entity> commonEntityMap = new HashMap<>((int) (curEntityClassList.size() * 1.33F));
            for (String entityName : curEntityClassList) {
                commonEntityMap.put(prevEntityMap.get(entityName), curEntityMap.get(entityName));
            }
            return commonEntityMap;

        } else {
            return Collections.emptyMap();
        }
    }

    public static List<Entity> getRemoved(Schema prev, Schema cur) {

        final AbstractList<String> prevEntityClassList = entityClassList(prev);
        final AbstractList<String> curEntityClassList = entityClassList(cur);

        //Remove all entity classes from the current list that are present in the older list
        prevEntityClassList.removeAll(curEntityClassList);
        if (prevEntityClassList.size() > 0) {
            //We have entities that have been added
            final Map<String, Entity> entityMap = entityMapFromSchema(prev);
            final Iterator<Map.Entry<String, Entity>> iterator = entityMap.entrySet().iterator();
            Map.Entry<String, Entity> next;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (!prevEntityClassList.contains(next.getKey())) {
                    iterator.remove();
                }
            }
            return new ArrayList<>(entityMap.values());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Ensures that the directories exist
     */
    public static void ensureDirectory(String... filePaths) {

        if (filePaths != null) {
            File file;
            for (String filePath : filePaths) {
                file = new File(filePath);
                if (file.exists() && !file.isDirectory()) {
                    throw new IllegalArgumentException("Given path is not a directory: " + file.getAbsolutePath());
                } else if (!file.exists()) {
                    if (!file.mkdirs()) {
                        throw new IllegalStateException("Could not create directory: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public static String getPropertySqlDef(Property property) {
        if (property.getConstraints() != null) {
            return String.format(Locale.US, "\"%s\" %s %s", property.getColumnName(), property.getColumnType(), property.getConstraints());
        } else {
            return String.format(Locale.US, "\"%s\" %s", property.getColumnName(), property.getColumnType());
        }
    }

    public static List<Property> entityPropertiesWithoutPrimaryKey(Entity entity) {

        final List<Property> properties = new ArrayList<>(entity.getProperties());
        final Iterator<Property> iterator = properties.iterator();

        Property property;
        while (iterator.hasNext()) {
            property = iterator.next();
            if (property.isPrimaryKey()) {
                iterator.remove();
                break;
            }
        }
        return properties;
    }
}
