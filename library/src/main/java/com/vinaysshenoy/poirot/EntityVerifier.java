package com.vinaysshenoy.poirot;

import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.vinaysshenoy.poirot.Utils.resolveEntityRenameDescription;

/**
 * Class that verifies entities and checks whether there are any problems
 * Created by vinaysshenoy on 21/01/16.
 */
public class EntityVerifier {

    private final List<Schema> mSchemas;

    private final List<EntityRenameDesc> mEntityRenameDescList;

    public EntityVerifier(List<Schema> schemas, List<EntityRenameDesc> entityRenameDescs) {
        this.mSchemas = schemas;
        this.mEntityRenameDescList = entityRenameDescs;
    }

    public void verify() throws VerificationFailedException {

        verifyPropertiesNotChanged();
    }

    /**
     * Since SQLite does not support changing column constraints, this method verifies that
     * a property's constraints are not changing in succeeding schemas
     */
    private void verifyPropertiesNotChanged() throws VerificationFailedException {

        Schema from, to;
        Map<Entity, Entity> commonEntities;
        Map<Property, Property> commonProperties;
        for (int i = 0; i < mSchemas.size(); i++) {
            if (i == 0) {
                //No need to check for the 1st schema
                continue;
            }

            to = mSchemas.get(i);
            from = mSchemas.get(i - 1);
            commonEntities = Utils.getCommonEntitiesAsMap(from, to, resolveEntityRenameDescription(from, to, mEntityRenameDescList));
            for (Map.Entry<Entity, Entity> entityEntry : commonEntities.entrySet()) {
                commonProperties = Utils.getCommonPropertiesAsMap(entityEntry.getKey(), entityEntry.getValue());
                for (Map.Entry<Property, Property> propertyEntry : commonProperties.entrySet()) {
                    if (!Utils.areEquivalent(propertyEntry.getKey(), propertyEntry.getValue())) {
                        throw new VerificationFailedException(
                                String.format(Locale.US,
                                        "Property '%s' on Entity '%s' constraints changed when going from schema %d to schema %d",
                                        propertyEntry.getKey().getPropertyName(), entityEntry.getValue().getClassName(), from.getVersion(), to.getVersion()
                                )
                        );
                    }
                }

            }

        }
    }

    public class VerificationFailedException extends RuntimeException {

        public VerificationFailedException() {
            super();
        }

        public VerificationFailedException(String message) {
            super(message);
        }

        public VerificationFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public VerificationFailedException(Throwable cause) {
            super(cause);
        }

        protected VerificationFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }


}
