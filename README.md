# poirot
Adds automatic migration support for Greendao.

## Pre-Alpha and not thoroughly tested. Do NOT use in production unless you like to live on the wild side!

### API Usage coming soon. For now, please check the [Dao Generator](https://github.com/vinaysshenoy/poirot/blob/master/src/main/java/com/vinaysshenoy/poirot/Generator.java) file for more info.

* The generator file will generate a `PoirotDbHelper.java` source file that you can drop in place of GreenDao's Open Helper and it will take care of automatically migrating your database for you.

* Maven support is on the way. For now, clone and use this Generator project to create your entities. Do not depend on your version of the GreenDao generator, use the one that this library depends on.

* When you add a new schema, do NOT change the older schema. Write the new schema from scratch. `Poirot` will examine the diffs between schemas to generate the corressponding migrations.

* This currently uses version `2.1.0` of the GreenDao generator, so link to the corressponding core library in your client app.
