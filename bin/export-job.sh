#!/bin/bash

# most likely you'll need to setup the following variables:
# TALEND_DIR               - talend directory
# TALEND_CUSTOM_COMPONENTS - talend custom components directory
# TALEND_EXEC              - talend executable
# TALEND_BUILD             - where to export the jobs to
# TALEND_WORKSPACE         - talend workspace
# TALEND_REPO              - location of the talend repository to build
#
# for jenkins for instance, you can have:
# TALEND_DIR=/var/lib/jenkins/talend
# TALEND_CODEGEN=$TALEND_DIR/talend-codegen
# TALEND_CUSTOM_COMPONENTS=$TALEND_DIR/talend-components
# TALEND_EXEC=$TALEND_DIR/TOS_DI-linux-gtk-x86_64
# TALEND_BUILD=$WORKSPACE/target
# TALEND_PROJECT_NAME=TSUNAMIBUOYS
# TALEND_WORKSPACE=$WORKSPACE/../.talend-workspace
# TALEND_REPO=$WORKSPACE

# builds a job
# $1 - job name
export_job() {
	local job_name=$1; shift
	mkdir -p $TALEND_BUILD
	$TALEND_EXEC \
		--clean_component_cache --disableShellInput -nosplash \
		-consoleLog --launcher.suppressErrors \
		-data $TALEND_WORKSPACE \
		-componentDir $TALEND_CUSTOM_COMPONENTS \
		-application au.org.emii.talend.codegen.Generator \
		-jobName $job_name \
		-projectDir $TALEND_REPO/$TALEND_PROJECT_NAME \
		-targetDir $TALEND_BUILD
}

# main
# "$@" - jobs to build
main() {
	local -i retval=0
	local failed_jobs=""
	for job in "$@"; do
		echo "Exporting job $job"
		export_job $job
		if [ $? -ne 0 ]; then
			failed_jobs="$failed_jobs $job"
			let retval=$retval+1
		fi
	done

	if [ $retval -ne 0 ]; then
		echo "----------------------"
		echo "Failed to export the following jobs: '$failed_jobs'"
		echo "----------------------"
	fi

	return $retval
}

main "$@"
