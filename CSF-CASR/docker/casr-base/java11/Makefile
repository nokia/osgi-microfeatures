.PHONY: build push release

build:
	sed -e "s,CENTOS_JAVA11,${LATEST_JAVA_11},g" dockerfile.sh.tmpl > dockerfile.sh
	chmod +x dockerfile.sh
	./dockerfile.sh

push:
	echo "$(ARTIFACTORY_BUILD_LOGIN_PSW)" | docker login -u $(ARTIFACTORY_BUILD_LOGIN_USR) --password-stdin $(DOCKER_REGISTRY)
	docker tag casr-base-11:latest $(DOCKER_REGISTRY)/casr-base-11:$(NEXT_CASR_11)
	echo "$(DOCKER_REGISTRY)/casr-base-11:$(NEXT_CASR_11)" >> $(WORKSPACE)/pushlist

clean:
	rm -f dockerfile.sh

release: build push

default: build