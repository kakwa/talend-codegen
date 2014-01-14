talend-codegen
==============

Command line code generation (job export) plugin for talend

Download
--------

https://github.com/kakwa/talend-codegen/releases

Compiling & Configuring
-----------------------

Build:

```bash
#to build the latest version available
> make

#ls in the out directory
>ls jar/
talend-codegen_5.4.1.jar

#build an older version
>make build_jar_5.3.1

#build directly with ant:
> cat Makefile
build_jar_5.4.1:
    ant build -Dtalend_version=5.4.1 -Dtalend_revision=111943
clean_5.4.1:
    ant clean -Dtalend_version=5.4.1 -Dtalend_revision=111943

#select your version
> ant build -Dtalend_version=5.4.1 -Dtalend_revision=111943
```

And copy `jar/talend-codegen<version>.jar` to the plugins directory of Talend.

or with the talend sources and eclipse:

 * Install eclipse
 * Import talend open studio source code from SVN (http://talendforge.org/svn/tos/tags/release-5_2_2)
 * Import talend-codegen
 * Export as 'Plug-in Development/Deployable plug-ins and fragments'
 * Copy generated .jar to plugins directory of Talend

(svn repo of talend is quite big (~4Go for a given tag)

If you fill lucky, you can try to use mismatched versions :).

Adding a new version of Talend
------------------------------

* Create a new directory `lib/<new version>`
* Download the new Talend version 
* Add the needed jars and .class files from this version in `lib/<new version>/` (you can run `get_needed_jar.sh <plugin dir of talend> <this repo>/lib/<new version>/`
* Add a new entry in the Makefile
* try to build the jar
* eventualy fix it (hint: ./tools/find_jar.sh -h)
* create a tag with only the latest version of the jars/classes

A script doing all these steps automaticaly may come in the futur.

Usage
-----

Invoke talend with the following mandatory command line arguments:
 * -projectDir - the project directory where the project can be found
 * -jobName - name of the job to be exported
 * -targetDir - the directory where the exported job will be placed

Eclipse application arguments
 * -application au.org.emii.talend.codegen.Generator - run the code generation plugin 
 * -nosplash stops the display of the gui splash window
 * --launcher.suppressErrors stops errors being displayed in message boxes - output to stderr instead
 * -data specifies the talend workspace used for building the project - created automatically if it doesn't exist (recommended to ensure a clean build)
 * --clean_component_cache tells TOS to reload external components and rebuild the cache
 
Some optional command line arguments you can have:
 * -version - version of job to be exported
 * -componentDir - location of any custom components used in the job
 * -needLauncher - include launcher script (true/false)
 * -needSystemRoutine - include system outines (true/false)
 * -needUserRoutine - and so on..
 * -needTalendLibraries
 * -needJobItem
 * -needSourceCode
 * -needDependencies
 * -needJobScript
 * -needContext
 * -applyToChildren

Example
-------

This example is taken from our Jenkins build process - $WORKSPACE is the location of the talend project

TOS_DI-linux-gtk-x86 -nosplash --launcher.suppressErrors -data $WORKSPACE/../.talend-workspace --clean_component_cache -application au.org.emii.talend.codegen.Generator -jobName ThreddsExample -projectDir $WORKSPACE -targetDir $WORKSPACE/.talend-build -componentDir /par2/git-repos/talend_components 
 


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/kakwa/talend-codegen/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

