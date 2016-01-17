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

        final MigrationsGenerator migrationsGenerator = new MigrationsGenerator(mSchemas);
        filesToCreate.addAll(migrationsGenerator.createMigrationsFor());
        filesToCreate.add(createDbHelperFile(currentSchema));

        for (JavaFile javaFile : filesToCreate) {
            javaFile.writeTo(System.out);
        }

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
                .addFileComment(Poirot.GENERATED_FILE)
                .build();
    }

}
