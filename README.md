# Poirot

## Automagical database migrations for [GreenDao](http://greenrobot.org/greendao/)

Adding migrations in Android for databases is tricky and involves a lot a human effort. There is huge scope for human error and screwed up migrations. Poirot aims to fix that by attempting to automatically generating migrations where it can, and by forcing certain practices upon the user

### Greendao Version - 2.1.0

### Quick Usage
1. Replace the GreenDao-Generator dependency in your generator project with Poirot.
2. Define your entities using Poirot
3. Generate your Dao classes using Poirot
4. Use the generate `PoirotDbHelper.java` and your migrations will be handled automatically

### Features
Poirot handles
- Addition, deletion and renaming of tables
- Addition of columns
- Addition and removal of Indexes

Changing of field constraints, renaming and dropping of fields is not supported by SQLite, hence Poirot does not support it and takes steps to ensure that these actions are not allowed.

### Usage
#### Include the dependency in your Generator project
##### Maven
```xml
<dependency>
    <groupId>com.vinaysshenoy</groupId>
    <artifactId>poirot</artifactId>
    <version>1.0.0</version>
</dependency>
```
##### Gradle
```groovy
compile 'com.vinaysshenoy:poirot:1.0.0'
```

#### Create Entities
In Poirot, instead of modifying your entity, you create a whole new set of entities for each version. This enables Poirot to examine the differences between successive schemas and generate code.
```java
//The root package for all the generated source
final Poirot poirot = new Poirot("com.poirot.example.db");

//Used for describing change in Entity names across schemas
final EntityRenameDesc.Builder entityRenameDescBuilder = new EntityRenameDesc.Builder();

createV1Schema(poirot.create(1, false));
createV2Schema(poirot.create(2, false));
createV3Schema(poirot.create(3, false));
//The current version has to be marked true
createV4Schema(poirot.create(4, true, entityRenameDescBuilder
                        .reset()
                        .map("Function", "Department") //Function Entity from < v3 has been renamed to Department in v4
                        .build()));
```
`poirot.create()` returns a standard GreenDao `Schema`, so you can use it as usual.

#### Generate your entities
After defining your entities, you generate them by calling `poirot.generate()`.
```java
poirot.generate("out/java/cur", "out/java/old");
```
The current DAO classes are generated under the "cur" directory, while the older classes are generated under the "old" directory. This allows you to test the migrations using your testing framework if you wish.

#### Use the generated DbHelper
Along with the entities, a `PoirotDbHelper.java` will be generated under `out/java/cur/com/poirot/example/db/helper`. All you need to do is use this class instead of GreenDao's default `OpenHelper` and you have automatic migrations.

### Credits
1. [GreenDao](http://greenrobot.org/greendao/), for an awesome ORM
2. The core migration class structure was described in a StackOverflow [post] (http://stackoverflow.com/questions/13373170/greendao-schema-update-and-data-migration). The core website is no longer available but the archives are present [here](https://web.archive.org/web/20140215121239/http://www.androidanalyse.com/greendao-schema-generation/).
3. The amazing devs at Square for creating [javapoet](https://github.com/square/javapoet), without which the code generation for Poirot would've taken orders of magnitude longer
