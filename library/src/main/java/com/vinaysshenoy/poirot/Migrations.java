package com.vinaysshenoy.poirot;

import com.squareup.javapoet.*;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Index;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

import javax.lang.model.element.Modifier;
import java.util.*;


/**
 * Created by vinaysshenoy on 17/01/16.
 */
public class Migrations {

    private final ClassName mAbstractMigrationClassName;

    private final ClassName mDbClassName;

    private final String mPackageName;

    private final List<Schema> mSchemas;

    private final Schema mCurrentSchema;

    private final ParameterSpec mDbParameterSpec;

    private final ParameterSpec mCurrentVersionParameterSpec;

    private List<EntityRenameDesc> mEntityRenameDescList;

    public Migrations(List<Schema> schemas, List<EntityRenameDesc> entityRenameDescList) {
        this.mSchemas = schemas;
        this.mEntityRenameDescList = entityRenameDescList;
        mCurrentSchema = schemas.get(schemas.size() - 1);
        mPackageName = mCurrentSchema.getDefaultJavaPackage() + ".helper.migrations";
        mAbstractMigrationClassName = ClassName.get(mPackageName, "AbstractMigration");
        mDbClassName = ClassName.get("android.database.sqlite", "SQLiteDatabase");
        mDbParameterSpec = ParameterSpec.builder(mDbClassName, "db").build();
        mCurrentVersionParameterSpec = ParameterSpec.builder(int.class, "currentVersion").build();
    }

    public List<JavaFile> createMigrations() {

        final List<JavaFile> migrationFiles = new ArrayList<>(mSchemas.size());
        migrationFiles.add(createAbstractMigrationFile(mCurrentSchema));

        for (int i = 0; i < mSchemas.size(); i++) {
            if (i == 0) {
                //No need for a migration for the 1st schema version
                continue;
            }

            migrationFiles.add(createMigrationFile(
                    mSchemas.get(i - 1),
                    mSchemas.get(i),
                    i == 1 ? null : mSchemas.get(i - 2)
            ));
        }

        return migrationFiles;
    }

    /**
     * Create a migration from an older schema version to a newer schema version
     *
     * @param from       The {@link Schema} to migrate from
     * @param to         The {@link Schema} to migrate to
     * @param beforeFrom The {@link Schema} that comes before {@code from}
     */
    private JavaFile createMigrationFile(Schema from, Schema to, Schema beforeFrom) {

        final int fromVersion = from.getVersion();
        final int toVersion = to.getVersion();

        if (fromVersion >= toVersion) {
            throw new IllegalArgumentException("Cannot generate a migration from " + fromVersion + " to " + toVersion);
        }

        final ClassName migrationClassName = Utils.generateMigrationName(mPackageName, from, to);

        final MethodSpec getTargetVersionSpec = MethodSpec.methodBuilder("getTargetVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(int.class)
                .addStatement("return $L", fromVersion)
                .build();

        final MethodSpec getMigratedVersionSpec = MethodSpec.methodBuilder("getMigratedVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(int.class)
                .addStatement("return $L", toVersion)
                .build();

        final MethodSpec.Builder getPreviousMigrationBuilder = MethodSpec.methodBuilder("getPreviousMigration")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(mAbstractMigrationClassName);

        MethodSpec getPreviousMigrationSpec;
        if (beforeFrom == null) {
            getPreviousMigrationBuilder.addStatement("return null");
        } else {
            getPreviousMigrationBuilder.addStatement("return new $T()", Utils.generateMigrationName(mPackageName, beforeFrom, from));
        }

        getPreviousMigrationSpec = getPreviousMigrationBuilder.build();

        final MethodSpec applyMigrationSpec = createApplyMethodSpec(from, to);

        final TypeSpec migrationSpec = TypeSpec.classBuilder(migrationClassName.simpleName())
                .superclass(mAbstractMigrationClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethods(Arrays.asList(getTargetVersionSpec, getMigratedVersionSpec, getPreviousMigrationSpec, applyMigrationSpec))
                .build();

        return JavaFile.builder(mPackageName, migrationSpec)
                .addFileComment(Poirot.GENERATED_FILE)
                .build();
    }

    private MethodSpec createApplyMethodSpec(Schema from, Schema to) {

        final MethodSpec.Builder applyMigrationSpecBuilder = MethodSpec.methodBuilder("applyMigration")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addParameters(Arrays.asList(mDbParameterSpec, mCurrentVersionParameterSpec))
                .addStatement("prepareMigration($L,$L)", mDbParameterSpec.name, mCurrentVersionParameterSpec.name);

        handleAddedEntities(from, to, applyMigrationSpecBuilder);
        handleRenamedEntities(from, to, applyMigrationSpecBuilder);
        handleRemovedEntities(from, to, applyMigrationSpecBuilder);
        handleAddedColumns(from, to, applyMigrationSpecBuilder);
        handleAddedIndexes(from, to, applyMigrationSpecBuilder);
        handleRemovedIndexes(from, to, applyMigrationSpecBuilder);

        applyMigrationSpecBuilder.addStatement("return $L()", "getMigratedVersion");

        return applyMigrationSpecBuilder
                .build();
    }

    private void handleRenamedEntities(Schema from, Schema to, MethodSpec.Builder applyMigrationBuilder) {

        final EntityRenameDesc entityRenameDesc = resolveEntityRenameDescription(from, to);
        if (entityRenameDesc != null) {

            final Map<Entity, Entity> renamedEntities = Utils.getRenamed(from, to, entityRenameDesc);
            if (!renamedEntities.isEmpty()) {
                System.out.println(String.format(Locale.US, "Rename %d entities when going from v%d to v%d", renamedEntities.size(), from.getVersion(), to.getVersion()));
            }
            for (Entity entity : renamedEntities.keySet()) {
                applyMigrationBuilder.addStatement(
                        "$L.execSQL($S)",
                        mDbParameterSpec.name,
                        String.format(Locale.US, "ALTER TABLE %s RENAME TO %s", entity.getTableName(), renamedEntities.get(entity).getTableName())
                );
            }

        }
    }

    private void handleRemovedIndexes(Schema from, Schema to, MethodSpec.Builder applyMigrationBuilder) {

        final Map<Entity, Entity> entityMap = Utils.getCommonEntitiesAsMap(from, to);
        for (Map.Entry<Entity, Entity> entityEntry : entityMap.entrySet()) {
            final List<Index> removedIndexes = Utils.getRemovedIndexes(entityEntry.getKey(), entityEntry.getValue());
            if (!removedIndexes.isEmpty()) {
                System.out.println(String.format(Locale.US, "Removed %d indexes when going from v%d to v%d for entity %s", removedIndexes.size(), from.getVersion(), to.getVersion(), entityEntry.getKey().getClassName()));
            }
            removeIndexes(entityEntry.getKey(), removedIndexes, applyMigrationBuilder);
        }
    }

    private void removeIndexes(Entity entity, List<Index> removedIndexes, MethodSpec.Builder applyMigrationBuilder) {

        for (Index removedIndex : removedIndexes) {
            applyMigrationBuilder.addStatement(
                    "$L.execSQL($S)",
                    mDbParameterSpec.name,
                    String.format(Locale.US, "DROP INDEX IF EXISTS %s", removedIndex.getName())
            );
        }

    }

    private void handleAddedIndexes(Schema from, Schema to, MethodSpec.Builder applyMigrationBuilder) {

        final Map<Entity, Entity> entityMap = Utils.getCommonEntitiesAsMap(from, to);
        for (Map.Entry<Entity, Entity> entityEntry : entityMap.entrySet()) {
            final List<Index> addedIndexes = Utils.getAddedIndexes(entityEntry.getKey(), entityEntry.getValue());
            if (!addedIndexes.isEmpty()) {
                System.out.println(String.format(Locale.US, "Added %d indexes when going from v%d to v%d for entity %s", addedIndexes.size(), from.getVersion(), to.getVersion(), entityEntry.getKey().getClassName()));
            }
            addIndexes(entityEntry.getValue(), addedIndexes, applyMigrationBuilder);
        }

    }

    private void addIndexes(Entity entity, List<Index> addedIndexes, MethodSpec.Builder applyMigrationBuilder) {

        for (Index addedIndex : addedIndexes) {
            applyMigrationBuilder.addStatement(
                    "$L.execSQL($S)",
                    mDbParameterSpec.name,
                    String.format(Locale.US, "CREATE INDEX IF NOT EXISTS %s ON %s (\"%s\")", addedIndex.getName(), entity.getTableName(), addedIndex.getProperties().get(0).getColumnName())
            );
        }

    }

    private void handleAddedColumns(Schema from, Schema to, MethodSpec.Builder applyMigrationBuilder) {

        final Map<Entity, Entity> entityMap = Utils.getCommonEntitiesAsMap(from, to);
        for (Map.Entry<Entity, Entity> entityEntry : entityMap.entrySet()) {
            final List<Property> addedProperties = Utils.getAddedProperties(entityEntry.getKey(), entityEntry.getValue());
            if (!addedProperties.isEmpty()) {
                System.out.println(String.format(Locale.US, "Added %d properties when going from v%d to v%d for entity %s", addedProperties.size(), from.getVersion(), to.getVersion(), entityEntry.getKey().getClassName()));
            }
            addColumns(entityEntry.getKey(), addedProperties, applyMigrationBuilder);
        }
    }

    private void addColumns(Entity entity, List<Property> properties, MethodSpec.Builder applyMigrationBuilder) {
        for (Property property : properties) {
            applyMigrationBuilder.addStatement(
                    "$L.execSQL($S)",
                    mDbParameterSpec.name,
                    String.format(Locale.US, "ALTER TABLE \"%s\" ADD COLUMN %s", entity.getTableName(), Utils.getPropertySqlDef(property)));
        }
    }

    private void handleAddedEntities(Schema from, Schema to, MethodSpec.Builder applyMigrationBuilder) {

        final List<Entity> addedEntities = Utils.getAdded(from, to, resolveEntityRenameDescription(from, to));
        if (!addedEntities.isEmpty()) {
            System.out.println(String.format(Locale.US, "Added %d entities when going from v%d to v%d", addedEntities.size(), from.getVersion(), to.getVersion()));
        }
        if (addedEntities.size() > 0) {
            for (Entity addedEntity : addedEntities) {

                applyMigrationBuilder.addStatement("$L.execSQL($S)",
                        mDbParameterSpec.name,
                        String.format(Locale.US, "CREATE TABLE IF NOT EXISTS \"%s\" (%s)", addedEntity.getTableName(), Utils.getPropertySqlDef(addedEntity.getPkProperty()))
                );
                addColumns(addedEntity, Utils.entityPropertiesWithoutPrimaryKey(addedEntity), applyMigrationBuilder);
                addIndexes(addedEntity, addedEntity.getIndexes(), applyMigrationBuilder);
            }
        }
    }

    private void handleRemovedEntities(Schema from, Schema to, MethodSpec.Builder applyMigrationBuilder) {

        final List<Entity> removedEntities = Utils.getRemoved(from, to, resolveEntityRenameDescription(from, to));
        if (!removedEntities.isEmpty()) {
            System.out.println(String.format(Locale.US, "Removed %d entities when going from v%d to v%d", removedEntities.size(), from.getVersion(), to.getVersion()));
        }
        if (removedEntities.size() > 0) {
            for (Entity removedEntity : removedEntities) {
                applyMigrationBuilder.addStatement("$L.execSQL($S)", mDbParameterSpec.name, String.format(Locale.US, "DROP TABLE IF EXISTS \"%s\"", removedEntity.getTableName()));
            }
        }
    }

    private JavaFile createAbstractMigrationFile(Schema currentSchema) {

        final ParameterSpec dbParamSpec = ParameterSpec.builder(mDbClassName, "db").build();
        final ParameterSpec versionParamSpec = ParameterSpec.builder(int.class, "currentVersion").build();

        final MethodSpec getTargetVersionSpec = MethodSpec.methodBuilder("getTargetVersion")
                .addModifiers(Modifier.ABSTRACT, Modifier.PROTECTED)
                .returns(int.class)
                .addJavadoc("@return the target (old) version which will be migrated from\n")
                .build();

        final MethodSpec getMigratedVersionSpec = MethodSpec.methodBuilder("getMigratedVersion")
                .addModifiers(Modifier.ABSTRACT, Modifier.PROTECTED)
                .returns(int.class)
                .addJavadoc("@return the new version which will result from the migration being\n")
                .build();

        final MethodSpec getPreviousMigrationSpec = MethodSpec.methodBuilder("getPreviousMigration")
                .addModifiers(Modifier.ABSTRACT, Modifier.PROTECTED)
                .returns(mAbstractMigrationClassName)
                .addJavadoc("@return instance of the previous Migration required if the current version is to old for this migration. \nNB: This will only be null if this is the tip of the chain and there are no other earlier migrations.\n")
                .build();

        final MethodSpec applyMigrationSpec = MethodSpec.methodBuilder("applyMigration")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(int.class)
                .addParameters(Arrays.asList(dbParamSpec, versionParamSpec))
                .addJavadoc("Apply the migration to the given database\n" +
                        "@param $L The database to be updated\n" +
                        "@param $L The current version before migration\n" +
                        "@return the version after migration has been applied\n", dbParamSpec.name, versionParamSpec.name)
                .build();

        final MethodSpec prepareMigrationSpec = MethodSpec.methodBuilder("prepareMigration")
                .addModifiers(Modifier.PROTECTED)
                .addParameters(Arrays.asList(dbParamSpec, versionParamSpec))
                .beginControlFlow("if($L < $L())", versionParamSpec.name, getTargetVersionSpec.name)
                .addStatement("$L previousMigration = $L()", mAbstractMigrationClassName.simpleName(), getPreviousMigrationSpec.name)
                .beginControlFlow("if(previousMigration == null)")
                .beginControlFlow("if($L != $L())", versionParamSpec.name, getTargetVersionSpec.name)
                .addStatement("throw new $T($S)", IllegalStateException.class, "DB old version != target version")
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("if(previousMigration.$L($L,$L) != $L())", applyMigrationSpec.name, dbParamSpec.name, versionParamSpec.name, getTargetVersionSpec.name)
                .addStatement("throw new $T($S)", IllegalStateException.class, "Error, expected migration parent to update database to appropriate version")
                .endControlFlow()
                .endControlFlow()
                .build();

        final TypeSpec abstractMigrationHelperSpec = TypeSpec.classBuilder(mAbstractMigrationClassName.simpleName())
                .addModifiers(Modifier.ABSTRACT)
                .addMethods(Arrays.asList(prepareMigrationSpec, applyMigrationSpec, getPreviousMigrationSpec, getMigratedVersionSpec, getTargetVersionSpec))
                .build();

        return JavaFile.builder(currentSchema.getDefaultJavaPackage() + ".helper.migrations", abstractMigrationHelperSpec)
                .addFileComment(Poirot.GENERATED_FILE)
                .build();
    }

    private EntityRenameDesc resolveEntityRenameDescription(Schema from, Schema to) {

        final int fromVersion = from.getVersion();
        final int toVersion = to.getVersion();

        for (EntityRenameDesc entityRenameDesc : mEntityRenameDescList) {
            if (fromVersion == entityRenameDesc.getFromVersion() && toVersion == entityRenameDesc.getToVersion()) {
                return entityRenameDesc;
            }
        }

        return null;
    }

}
