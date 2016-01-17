package com.vinaysshenoy.poirot;

import com.squareup.javapoet.*;
import de.greenrobot.daogenerator.Schema;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vinaysshenoy on 17/01/16.
 */
class PoirotDbHelperGenerator {

    private final List<Schema> mSchemas;

    /**
     * Create a {@link PoirotDbHelperGenerator} instance with a list of schemas
     *
     * @param schemas A non-{@code null} and non-empty list of schemas for which to generate the helper
     */
    public PoirotDbHelperGenerator(List<Schema> schemas) {

        if (schemas == null || schemas.isEmpty()) {
            throw new IllegalArgumentException("Schemas cannot be null or empty");
        }
        this.mSchemas = new ArrayList<>(schemas);
    }

    public void generateHelper() throws IOException {

        final Schema currentSchema = mSchemas.get(mSchemas.size() - 1);

        final List<JavaFile> filesToCreate = new ArrayList<>();

        filesToCreate.add(createAbstractMigrationFile(currentSchema));
        filesToCreate.add(createDbHelperFile(currentSchema));

        for (JavaFile javaFile : filesToCreate) {
            javaFile.writeTo(System.out);
        }

    }

    private JavaFile createAbstractMigrationFile(Schema currentSchema) {

        final ClassName dbClassName = ClassName.get("android.database.sqlite", "SQLiteDatabase");
        final ClassName abstractMigrationClassName = ClassName.get(currentSchema.getDefaultJavaPackage() + ".helper.migrations", "AbstractMigration");

        final ParameterSpec dbParamSpec = ParameterSpec.builder(dbClassName, "db").build();
        final ParameterSpec versionSpec = ParameterSpec.builder(int.class, "currentVersion").build();

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
                .returns(abstractMigrationClassName)
                .addJavadoc("@return instance of the previous Migration required if the current version is to old for this migration. \nNB: This will only be null if this is the tip of the chain and there are no other earlier migrations.\n")
                .build();

        final MethodSpec applyMigrationSpec = MethodSpec.methodBuilder("applyMigration")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(int.class)
                .addJavadoc("Apply the migration to the given database\n" +
                        "@param $L The database to be updated\n" +
                        "@param $L The current version before migration\n" +
                        "@return the version after migration has been applied\n", dbParamSpec.name, versionSpec.name)
                .build();

        final MethodSpec prepareMigrationSpec = MethodSpec.methodBuilder("prepareMigration")
                .addModifiers(Modifier.PROTECTED)
                .addParameters(Arrays.asList(dbParamSpec, versionSpec))
                .beginControlFlow("if($L < $L())", versionSpec.name, getTargetVersionSpec.name)
                .endControlFlow()
                .build();

        final TypeSpec abstractMigrationHelperSpec = TypeSpec.classBuilder(abstractMigrationClassName.topLevelClassName().simpleName())
                .addModifiers(Modifier.ABSTRACT)
                .addMethods(Arrays.asList(prepareMigrationSpec, applyMigrationSpec, getPreviousMigrationSpec, getMigratedVersionSpec, getTargetVersionSpec))
                .build();

        return JavaFile.builder(currentSchema.getDefaultJavaPackage() + ".helper.migrations", abstractMigrationHelperSpec)
                .build();
    }

    private JavaFile createDbHelperFile(Schema currentSchema) {

        final ClassName contextClassName = ClassName.get("android.content", "Context");
        final ClassName cursorFactoryClassName = ClassName.get("android.database.sqlite", "SQLiteDatabase", "CursorFactory");

        final ParameterSpec contextParameterSpec = ParameterSpec.builder(contextClassName, "context").build();
        final ParameterSpec nameParameterSpec = ParameterSpec.builder(String.class, "name").build();
        final ParameterSpec factoryParameterSpec = ParameterSpec.builder(cursorFactoryClassName, "factory").build();

        final TypeSpec poirotDbHelperSpec = TypeSpec.classBuilder("PoirotDbHelper")
                .superclass(ClassName.get(currentSchema.getDefaultJavaPackage(), "DaoMaster", "OpenHelper"))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameters(Arrays.asList(contextParameterSpec, nameParameterSpec, factoryParameterSpec))
                        .addStatement("super($L, $L, $L)", contextParameterSpec.name, nameParameterSpec.name, factoryParameterSpec.name)
                        .build())
                .build();

        return JavaFile.builder(currentSchema.getDefaultJavaPackage() + ".helper", poirotDbHelperSpec)
                .build();
    }

}
