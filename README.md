talend-codegen
==============

Command line code generation (job build/export) plugin for talend

Compiling & Configuring
-----------------------

Build:

```bash
#to build the latest version available
> make

#ls in the out directory
>ls jar/talend-codegen_5.6.0.jar

#build an older version
>make build_jar_5.4.1

#build directly with ant:
> cat Makefile
build_jar_5.6.0:
	ant build -Dtalend_version=5.6.0 -Dtalend_revision=20141024_1545
clean_5.6.0:
	ant clean -Dtalend_version=5.6.0 -Dtalend_revision=20141024_1545

#select your version
> ant build -Dtalend_version=5.5.1 -Dtalend_revision=r118616
```

And copy `jar/talend-codegen<version>.jar` to the plugins directory of Talend.

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
```
export JOBNAME=MyJob
export WORKSPACE=/home/projectname/workspace
export PROJECTDIR=/home/projectname/workspace/MYPROJECT
export TARGETDIR=/home/projectname/workspace/.talend-build
export COMPONENTDIR=/home/projectname/custom_components

cp $PROJECTDIR/libs/* /home/TOS_DI-r118616-V5.5.1/lib/java/

/home/TOS_DI-20141024_1545-V5.6.0/TOS_DI-linux-gtk-x86_64 -nosplash --launcher.suppressErrors -data $WORKSPACE -application au.org.emii.talend.codegen.Generator -jobName $JOBNAME -projectDir $PROJECTDIR -targetDir $TARGETDIR -componentDir $COMPONENTDIR
``` 



