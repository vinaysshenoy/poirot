package com.vinaysshenoy.poirot;

import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by vinaysshenoy on 16/01/16.
 */
public class Generator {

    private static final String PACKAGE_NAME = "com.vinaysshenoy.poirot.db";
    private static final Path OUT_DIR_ROOT = Paths.get("out", "java");
    private static final Path CURRENT_SCHEMA_DIR = OUT_DIR_ROOT.resolve("cur");
    private static final Path OLD_SCHEMA_DIR = OUT_DIR_ROOT.resolve("old");

    public static void main(String[] args) {

        System.out.println("RUNNING!");

        final Poirot poirot = new Poirot(PACKAGE_NAME);
        createV1Schema(poirot);

        try {
            poirot.generate(CURRENT_SCHEMA_DIR.toString(), OLD_SCHEMA_DIR.toString());
            System.out.println("Entities generated!");
        } catch (Exception e) {
            System.out.println("Could not generate entities!");
            e.printStackTrace();
        }
    }

    private static void createV1Schema(Poirot poirot) {

        final Schema v1 = poirot.create(1, false);
        v1.enableKeepSectionsByDefault();

        //Define entities
        final Entity companyEntity = v1.addEntity("Company");
        companyEntity.addIdProperty().autoincrement();
        companyEntity.addStringProperty("companyId").notNull().unique().index();
        companyEntity.addStringProperty("name").notNull();
        companyEntity.addStringProperty("address");

        final Entity functionEntity = v1.addEntity("Function");
        functionEntity.addIdProperty().autoincrement();
        functionEntity.addStringProperty("functionCode").notNull().unique();
        functionEntity.addStringProperty("name").notNull();

        final Entity employeeEntity = v1.addEntity("Employee");
        employeeEntity.addIdProperty().autoincrement();
        employeeEntity.addStringProperty("employeeId").notNull().unique();
        employeeEntity.addStringProperty("designation").notNull().index();
        employeeEntity.addStringProperty("name").notNull();
        employeeEntity.addIntProperty("age");
        employeeEntity.addStringProperty("sex");
        employeeEntity.addDateProperty("dateOfBirth");

        //Define relationships
        final Property functionCompanyId = functionEntity.addLongProperty("companyId").notNull().getProperty();
        functionEntity.addToOne(companyEntity, functionCompanyId);
        companyEntity.addToMany(functionEntity, functionCompanyId);

        final Property employeeFunctionId = employeeEntity.addLongProperty("functionId").notNull().getProperty();
        employeeEntity.addToOne(functionEntity, employeeFunctionId);
        functionEntity.addToMany(employeeEntity, employeeFunctionId);
    }
}
