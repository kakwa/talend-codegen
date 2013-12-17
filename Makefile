all: build_jar_5.4.1

clean: clean_5.4.1

build_jar_5.4.1:
	ant build -Dtalend_version=5.4.1 -Dtalend_revision=111943
clean_5.4.1:
	ant clean -Dtalend_version=5.4.1 -Dtalend_revision=111943
build_jar_5.3.1:
	        ant build -Dtalend_version=5.3.1 -Dtalend_revision=104014
clean_5.3.1:
	        ant clean -Dtalend_version=5.3.1 -Dtalend_revision=104014
build_jar_5.2.1:
	        ant build -Dtalend_version=5.2.1 -Dtalend_revision=95165
clean_5.2.1:
	        ant clean -Dtalend_version=5.2.1 -Dtalend_revision=95165
