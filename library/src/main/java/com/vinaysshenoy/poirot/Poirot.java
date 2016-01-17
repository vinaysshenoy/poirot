package com.vinaysshenoy.poirot;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Schema;

import java.io.File;
import java.util.*;

/**
 * Created by vinaysshenoy on 16/01/16.
 */
public class Poirot {

    private static final Comparator<Schema> SCHEMA_COMPARATOR = new Comparator<Schema>() {
        @Override
        public int compare(Schema lhs, Schema rhs) {
            return lhs.getVersion() - rhs.getVersion();
        }
    };

    private final List<Schema> mSchemas;

    private final String mPackageName;

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
    }

    /**
     * Add a {@link Schema} to the list of schemas to be generated
     *
     * @param version   The schema version. Versions should always be increasing, with the current schema having the highest version
     * @param isCurrent Whether the schema is the current one or not
     */
    public Schema create(int version, boolean isCurrent) {

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

        Collections.sort(mSchemas, SCHEMA_COMPARATOR);
        validateSchemas();

        ensureDirectory(currentSchemaOutputDirectory, olderSchemaOutputDirectory);

        final DaoGenerator generator = new DaoGenerator();
        for (int i = 0; i < mSchemas.size(); i++) {
            generator.generateAll(
                    mSchemas.get(i),
                    i == mSchemas.size() - 1 ? currentSchemaOutputDirectory : olderSchemaOutputDirectory
            );
        }
    }

    /**
     * Ensures that the directories exist
     */
    private void ensureDirectory(String... filePaths) {

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
     * Validates that there is only one schema of a particular version
     */
    private void validateSchemas() throws IllegalStateException {

        final HashSet<Integer> versions = new HashSet<>((int) (mSchemas.size() * 1.33F));
        String packageName = null;
        for (Schema schema : mSchemas) {

            if (!isEmpty(packageName) && !packageName.equals(schema.getDefaultJavaPackage())) {
                throw new IllegalStateException("Schemas must not have different package names");
            } else {
                packageName = schema.getDefaultJavaPackage();
            }
            int versionNumber = schema.getVersion();
            if (versions.contains(versionNumber)) {
                throw new IllegalStateException(
                        "Unable to process schema versions, multiple instances with version number : "
                                + versionNumber);
            }
            versions.add(versionNumber);
        }
    }

    private static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }
}
