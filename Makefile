all: build_jar_5.4.1

clean: clean_5.4.1

build_jar_5.4.1:
	ant build -Dtalend_version=5.4.1 -Dtalend_revision=111943
clean_5.4.1:
	ant clean -Dtalend_version=5.4.1 -Dtalend_revision=111943
