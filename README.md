talend-codegen
==============

Command line code generation (job export) plugin for talend

Compiling & Configuring
-----------------------

 * Install eclipse
 * Import talend open studio source code from SVN (http://talendforge.org/svn/tos/tags/release-5_2_2)
 * Import talend-codegen
 * Export as 'Plug-in Development/Deployable plug-ins and fragments'
 * Copy generated .jar to plugins directory of Talend

Usage
-----

Invoke talend with the following mandatory command line arguments:
 * -projectDir - the project directory where the project can be found
 * -jobName - name of the job to be exported
 * -targetDir - the directory where the exported job will be placed
 
Some optional command line arguments you can have:
 * -noSplash
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

