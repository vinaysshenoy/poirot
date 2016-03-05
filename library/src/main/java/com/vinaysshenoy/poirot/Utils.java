package com.vinaysshenoy.poirot;

import com.squareup.javapoet.ClassName;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Index;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

import java.io.File;
import java.util.*;

/**
 * Internal Utility class for working with {@link Schema}, {@link Entity} and {@link Property} objects
 * <p/>
 * Created by vinaysshenoy on 17/01/16.
 */
final class Utils {

    private Utils() {

    }

    /**
     * Generates the migration class name for migrating from one DB version to the other
     *
     * @param packageName The name of the package the generated DB classes should reside in
     * @param from        The Schema from which the DB is migrating from
     * @param to          The Schema to which the DB is migrating
     * @return The Classname of the migration
     */
    public static ClassName generateMigrationName(String packageName, Schema from, Schema to) {
        return ClassName.get(packageName, String.format(Locale.US, "MigrateV%dToV%d", from.getVersion(), to.getVersion()));
    }

    /**
     * Creates a Map of the entity names to the entities from a schema
     *
     * @param schema The schema for which to generate the map
     * @return The Map of entities
     */
    public static Map<String, Entity> entityMapFromSchema(Schema schema) {

        final Map<String, Entity> entityMap = new HashMap<>((int) (schema.getEntities().size() * 1.33F));
        for (Entity entity : schema.getEntities()) {
            entityMap.put(entity.getClassName(), entity);
        }
        return entityMap;
    }

    /**
     * Creates a map of the property names to the properties for a schema
     *
     * @param entity The entity for which to generate the map
     * @return The Map of properties
     */
    public static Map<String, Property> propertyMapFromEntity(Entity entity) {

        final Map<String, Property> propertyMap = new HashMap<>((int) (entity.getProperties().size() * 1.33F));
        for (Property property : entity.getProperties()) {
            propertyMap.put(property.getPropertyName(), property);
        }
        return propertyMap;
    }

    /**
     * Creates a map of the index names to the indexes(?) for a schema
     *
     * @param entity The entity for which to generate the map
     * @return The map of indexes
     */
    public static Map<String, Index> indexMapFromEntity(Entity entity) {

        final Map<String, Index> indexMap = new HashMap<>((int) (entity.getIndexes().size() * 1.33F));
        for (Index index : entity.getIndexes()) {
            indexMap.put(index.getName(), index);
        }
        return indexMap;
    }

    /**
     * Get a list of the entities from the schema
     * <p/>
     * Note: This was added because we specifically need an Abstract list
     *
     * @param schema The schema for which to generate the list
     * @return The list of entities
     */
    public static AbstractList<String> entityClassList(Schema schema) {

        final AbstractList<String> entityClassList = new ArrayList<>(schema.getEntities().size());
        for (Entity entity : schema.getEntities()) {
            entityClassList.add(entity.getClassName());
        }

        return entityClassList;
    }

    /**
     * Get a list of the properties from the entitiy
     * <p/>
     * Note: This was added because we specifically need an Abstract list
     *
     * @param entity The entity for which to generate the list
     * @return The list of properties
     */
    public static AbstractList<String> propertyNameList(Entity entity) {

        final AbstractList<String> propertyNameList = new ArrayList<>(entity.getProperties().size());
        for (Property property : entity.getProperties()) {
            propertyNameList.add(property.getPropertyName());
        }

        return propertyNameList;
    }

    /**
     * Get a list of the indexes from the entitiy
     * <p/>
     * Note: This was added because we specifically need an Abstract list
     *
     * @param entity The entity for which to generate the list
     * @return The list of indexes
     */
    public static AbstractList<String> indexNameList(Entity entity) {

        final AbstractList<String> indexNameList = new ArrayList<>(entity.getIndexes().size());
        for (Index index : entity.getIndexes()) {
            indexNameList.add(index.getName());
        }

        return indexNameList;
    }

    /**
     * Get a list of the indexes that were added from going from one entity to the other
     *
     * @param prev The entity from which we are migrating
     * @param cur  The entity to which we are migrating
     * @return The list of added indexes
     */
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

    /**
     * Get a list of the indexes that were removed from going from one entity to the other
     *
     * @param prev The entity from which we are migrating
     * @param cur  The entity to which we are migrating
     * @return The list of removed indexes
     */
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

    /**
     * Get a list of the entities that were added from going from one schema to the other
     *
     * @param prev             The schema from which we are migrating
     * @param cur              The schema to which we are migrating
     * @param entityRenameDesc {@link EntityRenameDesc} to denote if the name of any enity has changed when going from {@code prev} to {@code cur}
     * @return The list of added indexes
     */
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

    /**
     * Get a list of the properties that were added when going from one entity to the other
     *
     * @param prev The entity from which we are migrating
     * @param cur  The entity to which we are migrating
     * @return The list of added properties
     */
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

    /**
     * Get a list of the properties that were removed when going from one entity to the other
     *
     * @param prev The entity from which we are migrating
     * @param cur  The entity to which we are migrating
     * @return The list of removed properties
     */
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

    /**
     * Get a Map of the common entites between two schemas
     *
     * @param prev             The schema from which we are migrating
     * @param cur              The schema to which we are migrating
     * @param entityRenameDesc The {@link EntityRenameDesc} to denote if the name of any enity has changed when going from {@code prev} to {@code cur}
     * @return The map of common entities
     */
    public static Map<Entity, Entity> getCommonEntitiesAsMap(Schema prev, Schema cur, EntityRenameDesc entityRenameDesc) {

        final AbstractList<String> prevEntityClassList = entityClassList(prev);
        final AbstractList<String> curEntityClassList = entityClassList(cur);

        if (entityRenameDesc != null) {
            mapOldNamesToNew(prevEntityClassList, entityRenameDesc);
        }

        //Retain all entity classes from the current list that are present in the older list
        curEntityClassList.retainAll(prevEntityClassList);
        if (curEntityClassList.size() > 0) {

            final Map<String, Entity> prevEntityMap = entityMapFromSchema(prev);
            final Map<String, Entity> curEntityMap = entityMapFromSchema(cur);

            final Map<Entity, Entity> commonEntityMap = new HashMap<>((int) (curEntityClassList.size() * 1.33F));
            for (String entityName : curEntityClassList) {
                commonEntityMap.put(
                        prevEntityMap.get(entityRenameDesc != null && entityRenameDesc.isChanged(entityName) ? entityRenameDesc.getOriginalName(entityName) : entityName),
                        curEntityMap.get(entityName)
                );
            }
            return commonEntityMap;

        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Converts a list of names of a newer entity to the older one, based on the {@link EntityRenameDesc} provided
     *
     * @param namesList        The list of names belonging to a newer Entity
     * @param entityRenameDesc The {@link EntityRenameDesc} to denote if the name of any entity has changed
     */
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

    /**
     * Converts a list of names of an older entity to a newer one, based on the {@link EntityRenameDesc} provided
     *
     * @param namesList        The list of names belonging to a older Entity
     * @param entityRenameDesc The {@link EntityRenameDesc} {@link EntityRenameDesc} to denote if the name of any entity has changed
     */
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

    /**
     * Get a map of the entities that have been renamed when going from one schema to the next
     *
     * @param prev             The schema from which we are migrating
     * @param cur              The schema to which we are migrating
     * @param entityRenameDesc The {@link EntityRenameDesc} to denote if the name of any enity has changed when going from {@code prev} to {@code cur}
     * @return The map of old to new entities
     */
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

    /**
     * Get a list of the entities that were removed from going from one schema to the next
     *
     * @param prev             The The schema from which we are migrating
     * @param cur              The schema to which we are migrating
     * @param entityRenameDesc The {@link EntityRenameDesc} to denote if the name of any enity has changed when going from {@code prev} to {@code cur}
     * @return The list of removed entities
     */
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
     *
     * @param filePaths The directories to create
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

    /**
     * Creates a formatted SQL string for a Property
     *
     * @param property The property for which to generate an SQL definition
     * @return The formatted SQL string
     */
    public static String getPropertySqlDef(Property property) {
        if (property.getConstraints() != null) {
            return String.format(Locale.US, "\"%s\" %s %s", property.getColumnName(), property.getColumnType(), property.getConstraints());
        } else {
            return String.format(Locale.US, "\"%s\" %s", property.getColumnName(), property.getColumnType());
        }
    }

    /**
     * Gets a list of properties from the entity, excluding the primary key property
     *
     * @param entity The entity for which to generate the list
     * @return The list of properties, sans the Primary Key property
     */
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


    /**
     * Checks whether the SQL definitions of two properties are equivalent or not
     *
     * @param p1 The first property
     * @param p2 The second property
     * @return {@code true} if the equivalent, {@code false} otherwise
     */
    public static boolean areEquivalent(Property p1, Property p2) {
        return getPropertySqlDef(p1).equals(getPropertySqlDef(p2));
    }

    public static Map<Property, Property> getCommonPropertiesAsMap(Entity prev, Entity cur) {

        final AbstractList<String> prevPropertyNameList = propertyNameList(prev);
        final AbstractList<String> curPropertyNameList = propertyNameList(cur);

        //Retain all properties from the current list that are present in the older list
        curPropertyNameList.retainAll(prevPropertyNameList);
        if (curPropertyNameList.size() > 0) {

            final Map<String, Property> prevPropertyMap = propertyMapFromEntity(prev);
            final Map<String, Property> curPropertyMap = propertyMapFromEntity(cur);

            final Map<Property, Property> commonPropertyMap = new HashMap<>((int) (curPropertyNameList.size() * 1.33F));
            for (String propertyName : curPropertyNameList) {
                commonPropertyMap.put(prevPropertyMap.get(propertyName), curPropertyMap.get(propertyName));
            }
            return commonPropertyMap;

        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Fetches the right {@link EntityRenameDesc} for mapping entites when moving between schemas
     *
     * @param from              The Schema from which the DB is migrating from
     * @param to                The Schema to which the DB is migrating
     * @param entityRenameDescs The list of entity rename descriptors
     * @return The right Entity rename descriptor, or {@code null} if no entity has been renamed
     */
    public static EntityRenameDesc resolveEntityRenameDescription(Schema from, Schema to, List<EntityRenameDesc> entityRenameDescs) {

        final int fromVersion = from.getVersion();
        final int toVersion = to.getVersion();

        for (EntityRenameDesc entityRenameDesc : entityRenameDescs) {
            if (fromVersion == entityRenameDesc.getFromVersion() && toVersion == entityRenameDesc.getToVersion()) {
                return entityRenameDesc;
            }
        }

        return null;
    }

    /**
     * Finds the succeeding entity for a given Entity in the succeeding schema
     *
     * @param entity           The entity for which to find the succeeding entity
     * @param nextSchema       The succeeding schema in which to find the succeeding entity
     * @param entityRenameDesc The entity rename descriptor
     * @return The succeeding entitiy, or {@code null} if none exist
     */
    public static Entity succeeding(Entity entity, Schema nextSchema, EntityRenameDesc entityRenameDesc) {

        final Map<String, Entity> succeedingEntityMap = entityMapFromSchema(nextSchema);
        Entity succeeding = null;
        if (entityRenameDesc != null) {
            final String succeedingName = entityRenameDesc.getChangedName(entity.getClassName());
            if (succeedingName != null) {
                succeeding = succeedingEntityMap.get(succeedingName);
            }
        }
        if (succeeding == null) {
            succeeding = succeedingEntityMap.get(entity.getClassName());
        }
        return succeeding;
    }
}
