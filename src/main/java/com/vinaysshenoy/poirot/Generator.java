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

        final EntityRenameDesc.Builder entityRenameDescBuilder = new EntityRenameDesc.Builder();
        final Poirot poirot = new Poirot(PACKAGE_NAME);
        createV1Schema(poirot.create(1, false));
        createV2Schema(poirot.create(2, false));
        createV3Schema(poirot.create(3, false));
        createV4Schema(poirot.create(
                4,
                false,
                entityRenameDescBuilder
                        .reset()
                        .map("Function", "Department")
                        .build()
        ));
        createV5Schema(poirot.create(
                5,
                true,
                entityRenameDescBuilder
                .reset()
                .map("Department", "Function")
                .map("Company", "Organization")
                .build()
        ));

        try {
            poirot.generate(CURRENT_SCHEMA_DIR.toString(), OLD_SCHEMA_DIR.toString());
            System.out.println("Entities generated!");
        } catch (Exception e) {
            System.out.println("Could not generate entities!");
            e.printStackTrace();
        }
    }

    private static void createV5Schema(Schema v5) {

        v5.enableKeepSectionsByDefault();

        //Define entities
        final Entity companyEntity = v5.addEntity("Organization");
        companyEntity.addIdProperty().autoincrement();
        companyEntity.addStringProperty("companyCode").notNull().unique().index();
        companyEntity.addStringProperty("name").notNull();
        companyEntity.addDateProperty("incorporationDate");

        final Entity branchEntity = v5.addEntity("Branch");
        branchEntity.addIdProperty().autoincrement();
        branchEntity.addStringProperty("branchCode").notNull().unique().index();
        branchEntity.addStringProperty("address");

        final Entity functionEntity = v5.addEntity("Function");
        functionEntity.addIdProperty().autoincrement();
        functionEntity.addStringProperty("functionCode").notNull().unique();
        functionEntity.addStringProperty("name");

        final Entity teamEntity = v5.addEntity("Team");
        teamEntity.addIdProperty().autoincrement();
        teamEntity.addStringProperty("teamCode").notNull();
        teamEntity.addStringProperty("name");

        final Entity employeeEntity = v5.addEntity("Employee");
        employeeEntity.addIdProperty().autoincrement();
        employeeEntity.addStringProperty("employeeId").notNull().unique();
        employeeEntity.addStringProperty("designation").notNull().index();
        employeeEntity.addStringProperty("name").notNull();
        employeeEntity.addIntProperty("age");
        employeeEntity.addStringProperty("sex");
        employeeEntity.addDateProperty("dateOfBirth");
        employeeEntity.addDateProperty("dateOfJoining").index().notNull();
        employeeEntity.addStringProperty("address");

        //Define relationships
        final Property functionCompanyId = functionEntity.addLongProperty("companyId").notNull().getProperty();
        functionEntity.addToOne(companyEntity, functionCompanyId);
        companyEntity.addToMany(functionEntity, functionCompanyId);

        final Property branchCompanyId = branchEntity.addLongProperty("companyId").notNull().getProperty();
        branchEntity.addToOne(companyEntity, branchCompanyId);
        companyEntity.addToMany(branchEntity, branchCompanyId);

        final Property teamFunctionId = teamEntity.addLongProperty("functionId").notNull().getProperty();
        teamEntity.addToOne(functionEntity, teamFunctionId);
        functionEntity.addToMany(teamEntity, teamFunctionId);

        final Property employeeTeamId = employeeEntity.addLongProperty("teamId").getProperty();
        employeeEntity.addToOne(teamEntity, employeeTeamId);
        teamEntity.addToMany(employeeEntity, employeeTeamId);

        final Property teamLeadId = teamEntity.addLongProperty("teamLeadId").getProperty();
        teamEntity.addToOne(employeeEntity, teamLeadId);

        final Property employeeCompanyId = employeeEntity.addLongProperty("companyId").notNull().getProperty();
        employeeEntity.addToOne(companyEntity, employeeCompanyId);
        companyEntity.addToMany(employeeEntity, employeeCompanyId);

    }

    private static void createV4Schema(Schema v4) {

        v4.enableKeepSectionsByDefault();

        //Define entities
        final Entity companyEntity = v4.addEntity("Company");
        companyEntity.addIdProperty().autoincrement();
        companyEntity.addStringProperty("companyCode").notNull().unique().index();
        companyEntity.addStringProperty("name").notNull();
        companyEntity.addDateProperty("incorporationDate");

        final Entity branchEntity = v4.addEntity("Branch");
        branchEntity.addIdProperty().autoincrement();
        branchEntity.addStringProperty("branchCode").notNull().unique().index();
        branchEntity.addStringProperty("address");

        final Entity functionEntity = v4.addEntity("Department");
        functionEntity.addIdProperty().autoincrement();
        functionEntity.addStringProperty("functionCode").notNull().unique();
        functionEntity.addStringProperty("name");

        final Entity teamEntity = v4.addEntity("Team");
        teamEntity.addIdProperty().autoincrement();
        teamEntity.addStringProperty("teamCode").notNull();
        teamEntity.addStringProperty("name");

        final Entity employeeEntity = v4.addEntity("Employee");
        employeeEntity.addIdProperty().autoincrement();
        employeeEntity.addStringProperty("employeeId").notNull().unique();
        employeeEntity.addStringProperty("designation").notNull().index();
        employeeEntity.addStringProperty("name").notNull();
        employeeEntity.addIntProperty("age");
        employeeEntity.addStringProperty("sex");
        employeeEntity.addDateProperty("dateOfBirth");
        employeeEntity.addDateProperty("dateOfJoining").index().notNull();
        employeeEntity.addStringProperty("address");

        //Define relationships
        final Property functionCompanyId = functionEntity.addLongProperty("companyId").notNull().getProperty();
        functionEntity.addToOne(companyEntity, functionCompanyId);
        companyEntity.addToMany(functionEntity, functionCompanyId);

        final Property branchCompanyId = branchEntity.addLongProperty("companyId").notNull().getProperty();
        branchEntity.addToOne(companyEntity, branchCompanyId);
        companyEntity.addToMany(branchEntity, branchCompanyId);

        final Property teamFunctionId = teamEntity.addLongProperty("functionId").notNull().getProperty();
        teamEntity.addToOne(functionEntity, teamFunctionId);
        functionEntity.addToMany(teamEntity, teamFunctionId);

        final Property employeeTeamId = employeeEntity.addLongProperty("teamId").getProperty();
        employeeEntity.addToOne(teamEntity, employeeTeamId);
        teamEntity.addToMany(employeeEntity, employeeTeamId);

        final Property teamLeadId = teamEntity.addLongProperty("teamLeadId").getProperty();
        teamEntity.addToOne(employeeEntity, teamLeadId);

        final Property employeeCompanyId = employeeEntity.addLongProperty("companyId").notNull().getProperty();
        employeeEntity.addToOne(companyEntity, employeeCompanyId);
        companyEntity.addToMany(employeeEntity, employeeCompanyId);

    }

    private static void createV3Schema(Schema v3) {

        v3.enableKeepSectionsByDefault();

        //Define entities
        final Entity companyEntity = v3.addEntity("Company");
        companyEntity.addIdProperty().autoincrement();
        companyEntity.addStringProperty("companyCode").notNull().unique().index();
        companyEntity.addStringProperty("name").notNull();
        companyEntity.addDateProperty("incorporationDate");

        final Entity branchEntity = v3.addEntity("Branch");
        branchEntity.addIdProperty().autoincrement();
        branchEntity.addStringProperty("branchCode").notNull().unique().index();
        branchEntity.addStringProperty("address");

        final Entity functionEntity = v3.addEntity("Function");
        functionEntity.addIdProperty().autoincrement();
        functionEntity.addStringProperty("functionCode").notNull().unique();
        functionEntity.addStringProperty("name");

        final Entity teamEntity = v3.addEntity("Team");
        teamEntity.addIdProperty().autoincrement();
        teamEntity.addStringProperty("teamCode").notNull();
        teamEntity.addStringProperty("name");

        final Entity employeeEntity = v3.addEntity("Employee");
        employeeEntity.addIdProperty().autoincrement();
        employeeEntity.addStringProperty("employeeId").notNull().unique();
        employeeEntity.addStringProperty("designation").notNull();
        employeeEntity.addStringProperty("name").notNull();
        employeeEntity.addIntProperty("age");
        employeeEntity.addStringProperty("sex");
        employeeEntity.addDateProperty("dateOfBirth");
        employeeEntity.addDateProperty("dateOfJoining").notNull();
        employeeEntity.addStringProperty("address");

        final Entity orderEntity = v3.addEntity("Order");
        orderEntity.addIdProperty().autoincrement();
        orderEntity.addStringProperty("orderId").notNull().unique().index();
        orderEntity.addDateProperty("orderDate").notNull();

        //Define relationships
        final Property functionCompanyId = functionEntity.addLongProperty("companyId").notNull().getProperty();
        functionEntity.addToOne(companyEntity, functionCompanyId);
        companyEntity.addToMany(functionEntity, functionCompanyId);

        final Property branchCompanyId = branchEntity.addLongProperty("companyId").notNull().getProperty();
        branchEntity.addToOne(companyEntity, branchCompanyId);
        companyEntity.addToMany(branchEntity, branchCompanyId);

        final Property teamFunctionId = teamEntity.addLongProperty("functionId").notNull().getProperty();
        teamEntity.addToOne(functionEntity, teamFunctionId);
        functionEntity.addToMany(teamEntity, teamFunctionId);

        final Property employeeTeamId = employeeEntity.addLongProperty("teamId").getProperty();
        employeeEntity.addToOne(teamEntity, employeeTeamId);
        teamEntity.addToMany(employeeEntity, employeeTeamId);

        final Property teamLeadId = teamEntity.addLongProperty("teamLeadId").getProperty();
        teamEntity.addToOne(employeeEntity, teamLeadId);

        final Property employeeCompanyId = employeeEntity.addLongProperty("companyId").notNull().getProperty();
        employeeEntity.addToOne(companyEntity, employeeCompanyId);
        companyEntity.addToMany(employeeEntity, employeeCompanyId);

    }

    private static void createV2Schema(Schema v2) {

        v2.enableKeepSectionsByDefault();

        //Define entities
        final Entity companyEntity = v2.addEntity("Company");
        companyEntity.addIdProperty().autoincrement();
        companyEntity.addStringProperty("companyCode").notNull().unique().index();
        companyEntity.addStringProperty("name").notNull();
        companyEntity.addStringProperty("address");
        companyEntity.addDateProperty("incorporationDate");

        final Entity functionEntity = v2.addEntity("Function");
        functionEntity.addIdProperty().autoincrement();
        functionEntity.addStringProperty("functionCode").notNull().unique();
        functionEntity.addStringProperty("name").notNull();

        final Entity employeeEntity = v2.addEntity("Employee");
        employeeEntity.addIdProperty().autoincrement();
        employeeEntity.addStringProperty("employeeId").notNull().unique();
        employeeEntity.addStringProperty("designation").notNull().index();
        employeeEntity.addStringProperty("name").notNull();
        employeeEntity.addIntProperty("age");
        employeeEntity.addStringProperty("sex").index();
        employeeEntity.addDateProperty("dateOfBirth");
        employeeEntity.addDateProperty("dateOfJoining").notNull();

        final Entity orderEntity = v2.addEntity("Order");
        orderEntity.addIdProperty().autoincrement();
        orderEntity.addStringProperty("orderId").notNull().unique().index();
        orderEntity.addDateProperty("orderDate").notNull();

        //Define relationships
        final Property functionCompanyId = functionEntity.addLongProperty("companyId").notNull().getProperty();
        functionEntity.addToOne(companyEntity, functionCompanyId);
        companyEntity.addToMany(functionEntity, functionCompanyId);

        final Property employeeFunctionId = employeeEntity.addLongProperty("functionId").notNull().getProperty();
        employeeEntity.addToOne(functionEntity, employeeFunctionId);
        functionEntity.addToMany(employeeEntity, employeeFunctionId);
    }

    private static void createV1Schema(Schema v1) {

        v1.enableKeepSectionsByDefault();

        //Define entities
        final Entity companyEntity = v1.addEntity("Company");
        companyEntity.addIdProperty().autoincrement();
        companyEntity.addStringProperty("companyCode").notNull().unique().index();
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
        employeeEntity.addStringProperty("sex").index();
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
