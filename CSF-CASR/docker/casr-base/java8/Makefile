.PHONY: build push release

build:
	sed -e "s,CENTOS_JAVA8,${LATEST_JAVA},g" dockerfile.sh.tmpl > dockerfile.sh
	chmod +x dockerfile.sh
	./dockerfile.sh

push:
	echo "$(ARTIFACTORY_BUILD_LOGIN_PSW)" | docker login -u $(ARTIFACTORY_BUILD_LOGIN_USR) --password-stdin $(DOCKER_REGISTRY)
	docker tag casr-base:latest $(DOCKER_REGISTRY)/casr-base:$(NEXT_CASR)
	echo "$(DOCKER_REGISTRY)/casr-base:$(NEXT_CASR)" >> $(WORKSPACE)/pushlist

clean:
	rm -f dockerfile.sh

release: build push

default: build