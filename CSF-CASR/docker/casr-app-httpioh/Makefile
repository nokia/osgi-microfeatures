.PHONY: build push release

buildLocal:
	sed -e "s,CASR_BASE,${LATEST_CASR_11},g;s,NANO,${LATEST_NANO},g" dockerfile.sh.tmpl > dockerfile.sh
	chmod +x dockerfile.sh
	./dockerfile.sh --name=http-ioh \
	            	--version=1.0.0 \
	            	--localport=$(LOCAL_OBR_PORT) \
	            	--obr=$(OBR_REPO) \
	            	--features="runtime.felix;lib.log.log4j:1.0.0;config.advanced;ioh.mux.http;metering.exporter.prometheus;ioh.server.jersey" \
					--logger="rootLogger=WARN"

build:
	sed -e "s,CASR_BASE,${LATEST_CASR_11},g;s,NANO,${LATEST_NANO},g" dockerfile.sh.tmpl > dockerfile.sh
	chmod +x dockerfile.sh
	./dockerfile.sh --name=http-ioh \
	            	--version=1.0.0 \
	            	--casr=$(OBR_VERSION) \
	            	--obr=$(OBR_REPO) \
	            	--features="runtime.felix;lib.log.log4j:1.0.0;config.advanced;ioh.mux.http;metering.exporter.prometheus;ioh.server.jersey" \
					--logger="rootLogger=WARN"

push:
	echo "$(ARTIFACTORY_BUILD_LOGIN_PSW)" | docker login -u $(ARTIFACTORY_BUILD_LOGIN_USR) --password-stdin $(DOCKER_REGISTRY)
	docker tag casr-app-httpioh:latest $(DOCKER_REGISTRY)/casr-app-httpioh:$(OBR_VERSION)-$(DATE)
	echo "$(DOCKER_REGISTRY)/casr-app-httpioh:$(OBR_VERSION)-$(DATE)" >> $(WORKSPACE)/pushlist

clean:
	rm -f dockerfile.sh

release: build push

default: build
