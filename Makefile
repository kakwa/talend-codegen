all: build_jar_5.6.0

clean: clean_5.6.0

build_jar_5.6.0:
	ant build -Dtalend_version=5.6.0 -Dtalend_revision=20141024_1545
clean_5.6.0:
	ant clean -Dtalend_version=5.6.0 -Dtalend_revision=20141024_1545

build_jar_5.5.1:
	ant build -Dtalend_version=5.5.1 -Dtalend_revision=r118616
clean_5.5.1:
	ant clean -Dtalend_version=5.5.1 -Dtalend_revision=r118616
build_jar_5.4.1:
	ant build -Dtalend_version=5.4.1 -Dtalend_revision=r111943
clean_5.4.1:
	ant clean -Dtalend_version=5.4.1 -Dtalend_revision=r111943
build_jar_5.3.1:
	        ant build -Dtalend_version=5.3.1 -Dtalend_revision=r104014
clean_5.3.1:
	        ant clean -Dtalend_version=5.3.1 -Dtalend_revision=r104014
build_jar_5.2.1:
	        ant build -Dtalend_version=5.2.1 -Dtalend_revision=r95165
clean_5.2.1:
	        ant clean -Dtalend_version=5.2.1 -Dtalend_revision=r95165
