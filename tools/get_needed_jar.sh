#!/bin/sh


if ! [ -d "$1" ] || ! [ -d "$2" ]
then
    echo "usage: `basename $0` <plugin dir of talend> <this repo>/lib/<version>/"
    exit 1
fi

stuff="org.apache.log4j
org.eclipse.ui.ide 
org.apache.log4j
org.eclipse.ui.workbench 
org.eclipse.core.jobs
org.talend.commons.runtime 
org.eclipse.core.resources
org.talend.common.ui.runtime 
org.eclipse.core.runtime
org.talend.core 
org.eclipse.emf.common
org.talend.core.repository 
org.eclipse.emf.ecore
org.talend.core.runtime 
org.eclipse.equinox.app
org.talend.designer.codegen 
org.eclipse.equinox.common
org.talend.model 
org.eclipse.jface 
org.talend.repository 
org.eclipse.osgi"


for js in `echo $stuff`
do
    cp -r $1/$js* $2 
done
