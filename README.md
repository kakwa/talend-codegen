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

Usage with talend-codegen helper
--------------------------------

This project is shipped with an helper script available in **./bin/talend-codegen**.

This script must be modify to reflect your Talend installation (TALEND_DIR and TALEND_BIN at the beginning of the script)

Generation:
```bash
# With all '-need' options enabled 
$ talend-codegen -p ./MyProjectDir/ -o out/ -j MyJobName -a

# With custom options
$ talend-codegen -p ./MyProjectDir/ -o out/ -j MyJobName -O '-needLauncher=true -needDependencies=true'
```

help:
```
# help
$ talend-codegen -h
usage: talend-codegen -p <project dir> -o <out dir> -j <job name> \
                    [-a] [-O <custom options>] [-T <talend dir>] [-c <comp dir>]

Build Talend project from command line

examples:
 * talend-codegen -p ./MyProjectDir/ -o out/ -j MyJobName -a
 * talend-codegen -p ./MyProjectDir/ -o out/ -j MyJobName -O '-needLauncher=true -needDependencies=true'

arguments:
  -p <project dir>: directory containing the talend project
  -j <job id>: job to export
  -o <out dir>: output directory

optional arguments:
  -a:                  enable all -need* options
  -O <custom options>: custom options (cannot be used with -a)
  -T <talend dir>:     talend install directory (default: /home/pcarpent/TOS_DI-r95165-V5.2.1)
  -c <comp dir>:       location of any custom components used in the job

codegen options (for -O):
 * -version - version of job to be exported
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
```

Usage invoking TOS directly
---------------------------

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
```bash
export JOBNAME=MyJob
export WORKSPACE=/home/projectname/workspace
export PROJECTDIR=/home/projectname/workspace/MYPROJECT
export TARGETDIR=/home/projectname/workspace/.talend-build
export COMPONENTDIR=/home/projectname/custom_components

cp $PROJECTDIR/libs/* /home/TOS_DI-r118616-V5.5.1/lib/java/

/home/TOS_DI-20141024_1545-V5.6.0/TOS_DI-linux-gtk-x86_64 \
    -nosplash --launcher.suppressErrors -data $WORKSPACE \
    -application au.org.emii.talend.codegen.Generator \
    -jobName $JOBNAME -projectDir $PROJECTDIR \
    -targetDir $TARGETDIR -componentDir $COMPONENTDIR
``` 



