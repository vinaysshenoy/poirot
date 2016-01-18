package com.vinaysshenoy.poirot;

import com.squareup.javapoet.ClassName;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Index;
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

    public static Map<String, Index> indexMapFromEntity(Entity entity) {

        final Map<String, Index> indexMap = new HashMap<>((int) (entity.getIndexes().size() * 1.33F));
        for (Index index : entity.getIndexes()) {
            indexMap.put(index.getName(), index);
        }
        return indexMap;
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

    public static AbstractList<String> indexNameList(Entity entity) {

        final AbstractList<String> indexNameList = new ArrayList<>(entity.getIndexes().size());
        for (Index index : entity.getIndexes()) {
            indexNameList.add(index.getName());
        }

        return indexNameList;
    }

    public static List<Index> getAddedIndexes(Entity prev, Entity cur) {

        final AbstractList<String> prevIndexNameList = indexNameList(prev);
        final AbstractList<String> curIndexNameList = indexNameList(cur);

        //Remove all indexes from the current list that are present in the older list
        curIndexNameList.removeAll(prevIndexNameList);
        if (curIndexNameList.size() > 0) {
            //We have entities that have been added
            final Map<String, Index> indexMap = indexMapFromEntity(cur);
            final Iterator<Map.Entry<String, Index>> iterator = indexMap.entrySet().iterator();
            Map.Entry<String, Index> next;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (!curIndexNameList.contains(next.getKey())) {
                    iterator.remove();
                }
            }
            return new ArrayList<>(indexMap.values());
        } else {
            return Collections.emptyList();
        }
    }

    public static List<Index> getRemovedIndexes(Entity prev, Entity cur) {

        final AbstractList<String> prevIndexNameList = indexNameList(prev);
        final AbstractList<String> curIndexNameList = indexNameList(cur);

        //Remove all indexes from the current list that are present in the older list
        prevIndexNameList.removeAll(curIndexNameList);
        if (prevIndexNameList.size() > 0) {
            //We have entities that have been added
            final Map<String, Index> indexMap = indexMapFromEntity(prev);
            final Iterator<Map.Entry<String, Index>> iterator = indexMap.entrySet().iterator();
            Map.Entry<String, Index> next;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (!prevIndexNameList.contains(next.getKey())) {
                    iterator.remove();
                }
            }
            return new ArrayList<>(indexMap.values());
        } else {
            return Collections.emptyList();
        }
    }

    public static List<Entity> getAdded(Schema prev, Schema cur, EntityRenameDesc entityRenameDesc) {

        final AbstractList<String> prevEntityClassList = entityClassList(prev);
        final AbstractList<String> curEntityClassList = entityClassList(cur);

        if (entityRenameDesc != null) {
            mapOldNamesToNew(prevEntityClassList, entityRenameDesc);
        }

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
        prevPropertyNameList.removeAll(curPropertyNameList);
        if (prevPropertyNameList.size() > 0) {
            //We have entities that have been added
            final Map<String, Property> propertyMap = propertyMapFromEntity(prev);
            final Iterator<Map.Entry<String, Property>> iterator = propertyMap.entrySet().iterator();
            Map.Entry<String, Property> next;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (!prevPropertyNameList.contains(next.getKey())) {
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

    public static void mapNewNamesToOld(AbstractList<String> namesList, EntityRenameDesc entityRenameDesc) {

        final ListIterator<String> entityClassIterator = namesList.listIterator();
        String newEntityName, oldEntityName;
        while (entityClassIterator.hasNext()) {
            newEntityName = entityClassIterator.next();
            oldEntityName = entityRenameDesc.getOriginalName(newEntityName);

            if (oldEntityName != null) {
                entityClassIterator.set(oldEntityName);
            }
        }
    }

    public static void mapOldNamesToNew(AbstractList<String> namesList, EntityRenameDesc entityRenameDesc) {

        final ListIterator<String> entityClassIterator = namesList.listIterator();
        String newEntityName, oldEntityName;
        while (entityClassIterator.hasNext()) {
            oldEntityName = entityClassIterator.next();
            newEntityName = entityRenameDesc.getChangedName(oldEntityName);

            if (newEntityName != null) {
                entityClassIterator.set(newEntityName);
            }
        }
    }

    public static Map<Entity, Entity> getRenamed(Schema prev, Schema cur, EntityRenameDesc entityRenameDesc) {

        final AbstractList<String> prevEntityClassList = entityClassList(prev);

        final Iterator<String> nameIterator = prevEntityClassList.iterator();
        String changedName;
        while (nameIterator.hasNext()) {

            changedName = entityRenameDesc.getChangedName(nameIterator.next());
            if (changedName == null) {
                nameIterator.remove();
            }
        }

        if (prevEntityClassList.size() > 0) {
            //We have entities that have been renamed
            final Map<String, Entity> prevEntityMap = entityMapFromSchema(prev);
            final Map<String, Entity> curEntityMap = entityMapFromSchema(cur);
            final Iterator<Map.Entry<String, Entity>> iterator = prevEntityMap.entrySet().iterator();
            Map.Entry<String, Entity> next;
            while (iterator.hasNext()) {
                next = iterator.next();
                if (!prevEntityClassList.contains(next.getKey())) {
                    iterator.remove();
                }
            }
            List<Entity> oldEntities = new ArrayList<>(prevEntityMap.values());
            final Map<Entity, Entity> renamedEntityMap = new HashMap<>();
            Entity newEntity;

            for (Entity oldEntity : oldEntities) {
                newEntity = curEntityMap.get(entityRenameDesc.getChangedName(oldEntity.getClassName()));
                if (newEntity != null) {
                    renamedEntityMap.put(oldEntity, newEntity);
                }
            }
            return renamedEntityMap;

        } else {
            return Collections.emptyMap();
        }

    }

    public static List<Entity> getRemoved(Schema prev, Schema cur, EntityRenameDesc entityRenameDesc) {

        final AbstractList<String> prevEntityClassList = entityClassList(prev);
        final AbstractList<String> curEntityClassList = entityClassList(cur);

        //Map new names to older names
        if (entityRenameDesc != null) {
            mapNewNamesToOld(curEntityClassList, entityRenameDesc);
        }

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
