package com.vinaysshenoy.poirot;

import com.squareup.javapoet.*;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

    public Migrations(List<Schema> schemas) {
        this.mSchemas = schemas;
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
                .addStatement("$L($L,$L)", "prepareMigration", mDbParameterSpec.name, mCurrentVersionParameterSpec.name);


        final List<Entity> addedEntities = Utils.getAdded(from, to);
        System.out.println(String.format(Locale.US, "Added %d entities when going from v%d to v%d", addedEntities.size(), from.getVersion(), to.getVersion()));
        if (addedEntities.size() > 0) {
            for (Entity addedEntity : addedEntities) {

                final Property pkProperty = addedEntity.getPkProperty();
                final String pkColumnDef = String.format(Locale.US, "\"%s\" %s %s", pkProperty.getColumnName(), pkProperty.getColumnType(), pkProperty.getConstraints());
                applyMigrationSpecBuilder.addStatement("$L.execSQL($S)",
                        mDbParameterSpec.name,
                        String.format(Locale.US, "CREATE TABLE \"%s\" (%s);", addedEntity.getTableName(), pkColumnDef)
                );
            }
        }

        final List<Entity> removedEntities = Utils.getRemoved(from, to);
        System.out.println(String.format(Locale.US, "Removed %d entities when going from v%d to v%d", removedEntities.size(), from.getVersion(), to.getVersion()));
        if (removedEntities.size() > 0) {
            for (Entity removedEntity : removedEntities) {
                applyMigrationSpecBuilder.addStatement("$L.execSQL($S)", mDbParameterSpec.name, String.format(Locale.US, "DROP TABLE IF EXISTS \"%s\"", removedEntity.getTableName()));
            }
        }


        applyMigrationSpecBuilder.addStatement("return $L()", "getMigratedVersion");

        return applyMigrationSpecBuilder
                .build();
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

}
