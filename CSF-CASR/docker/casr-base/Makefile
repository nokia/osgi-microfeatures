.PHONY: build push release

build:
	cd java8 ; \
	make build ; \
	cd -
	cd java11 ; \
	make build ; \
	cd -

push:
	cd java8 ; \
	make push ; \
	cd -
	cd java11 ; \
	make push ; \
	cd -

clean:
	cd java8 ; \
	make clean ; \
	cd -
	cd java11 ; \
	make clean ; \
	cd -

release: build push

default: build