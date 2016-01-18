package com.vinaysshenoy.poirot;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by vinaysshenoy on 16/01/16.
 */
public class Poirot {

    public static final String GENERATED_FILE = "GENERATED FILE! DO NOT MODIFY!";

    private final List<Schema> mSchemas;

    private final String mPackageName;

    private int mLastVersion;

    private int mCurrentVersion;

    /**
     * Create an instance of {@link Poirot} with the package name for the schemas.
     *
     * @param packageName The schema package name. Must not be {@code null} or empty
     */
    public Poirot(String packageName) {
        if (isEmpty(packageName)) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }
        mPackageName = packageName;
        mSchemas = new ArrayList<>();
        mLastVersion = 0;
        mCurrentVersion = 0;
    }

    /**
     * Add a {@link Schema} to the list of schemas to be generated
     *
     * @param version   The schema version. Versions should always be increasing, with the current schema having the highest version
     * @param isCurrent Whether the schema is the current one or not
     */
    public Schema create(int version, boolean isCurrent) {

        if (version < 1) {
            throw new IllegalArgumentException("Cannot generate schemas with version < 1");
        }

        if (isCurrent && mCurrentVersion > version) {
            throw new IllegalArgumentException(version + " cannot be greater than already set current version");
        }

        if (mLastVersion >= version) {
            throw new IllegalArgumentException("Version numbers must always be increasing");
        }
        if (isCurrent) {
            mCurrentVersion = version;
        }
        mLastVersion = version;
        final String schemaPackage = isCurrent ? mPackageName : String.format(Locale.US, "%s.v%d", mPackageName, version);
        final Schema schema = new Schema(version, schemaPackage);
        mSchemas.add(schema);
        return schema;
    }

    /**
     * Generate the schemas. This will sort the schemas in ascending order. The schema with the highest version
     * number will always be selected as the current schema.
     *
     * @param currentSchemaOutputDirectory The directory to generate the DAO objects for the current schema. A good place to put them would be {@code "{Project Folder}/src/main/java-gen"}. Must not be {@code null} or empty.
     * @param olderSchemaOutputDirectory   The directory to generate the DAO objects for the older schemas. A good place to put them would be {@code "{Project Folder}/src/test/java-gen"}. Must not be {@code null} or empty.
     * @throws IllegalArgumentException If either {@code currentSchemaOutputDirectory} or {@code olderSchemaOutputDirectory} is {@code null}
     * @throws IllegalStateException    If there are problems creating the schema. Examine the exception for further details.
     * @throws Exception                If there are problems generating the entities. Examine the exception for further details
     */
    public void generate(String currentSchemaOutputDirectory, String olderSchemaOutputDirectory) throws Exception {

        if (isEmpty(currentSchemaOutputDirectory) || isEmpty(olderSchemaOutputDirectory)) {
            throw new IllegalArgumentException("Either current or older output directories cannot be null or empty");
        }

        if (mSchemas.isEmpty()) {
            throw new IllegalStateException("At least one schema must be added!");
        }

        Utils.ensureDirectory(currentSchemaOutputDirectory, olderSchemaOutputDirectory);

        final DaoGenerator generator = new DaoGenerator();
        for (int i = 0; i < mSchemas.size(); i++) {
            generator.generateAll(
                    mSchemas.get(i),
                    i == mSchemas.size() - 1 ? currentSchemaOutputDirectory : olderSchemaOutputDirectory
            );
        }

        final PoirotDbHelperGenerator helperGenerator = new PoirotDbHelperGenerator(mSchemas);
        helperGenerator.generateHelper(currentSchemaOutputDirectory);

    }

    private static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }
}
