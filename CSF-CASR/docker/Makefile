.PHONY: buildApps push release

export LATEST_NANO := $(shell python3 scripts/getLatestVersion.py os_base/centos-nano csf-docker-delivered)
export LATEST_JAVA := $(shell python3 scripts/getLatestVersionCentos7.py java_base/8/nano csf-docker-delivered)

ifdef LATEST_JAVA_11_OVERRIDE
   export LATEST_JAVA_11 := $(LATEST_JAVA_11_OVERRIDE)
else
   export LATEST_JAVA_11 := $(shell python3 scripts/getLatestVersionCentos7.py java_base/11/nano csf-docker-delivered)
endif 

export LATEST_CASR := $(shell python3 scripts/getLatestVersion.py casr-base csf-docker-delivered)
export LATEST_CASR_11 := $(shell python3 scripts/getLatestVersion.py casr-base-11 csf-docker-delivered)
export NEXT_CASR := $(shell scripts/nextVersion.sh $(LATEST_CASR))

ifdef NEXT_CASR_11_OVERRIDE
   export NEXT_CASR_11 := $(NEXT_CASR_11_OVERRIDE)
else
   export NEXT_CASR_11 := $(shell scripts/nextVersion.sh $(LATEST_CASR_11))
endif
export ARTIFACTORY_URL := repo.lab.pl.alcatel-lucent.com
export DOCKER_REGISTRY := $(DOCKER_DEPLOY_REPO).$(ARTIFACTORY_URL)
export WORKSPACE := $(shell pwd)
export DATE := $(shell date +%Y%m%d)

# Runs local OBR docker image: needed if we are creating the docker images from the builded java 
runLocalOBR:
ifeq ($(LOCAL), true)
	cd casr-local-obr ; \
	mkdir -p repository ; \
	cp -r ~/.m2/repository/* repository/ ; \
	docker build -t casr-local-obr:latest . ; \
	docker run -d -p 8001:80 -e rewrite_https=false --name casr-local-obr casr-local-obr:latest ; \
	cd -
endif

# Stops and deletes the local OBR docker image
deleteLocalOBR:
ifeq ($(LOCAL), true)
	docker stop casr-local-obr
	docker rm casr-local-obr
	rm -rf casr-local-obr/repository
endif

# Build casr-app images
buildApps: runLocalOBR
ifeq ($(LOCAL), false)
	for dir in casr-app-* ; do \
		cd $$dir ; \
		make OBR_VERSION=$(OBR_VERSION) OBR_REPO=$(OBR_REPO) build ; \
		cd - ; \
	done
else
	for dir in casr-app-* ; do \
		cd $$dir ; \
		make OBR_REPO=local LOCAL_OBR_PORT=8001 buildLocal ; \
		cd - ; \
	done
endif

pushApps:
	for dir in casr-app-* ; do \
		cd $$dir ; \
		make OBR_VERSION=$(OBR_VERSION) push ; \
		cd - ; \
	done

# Builds casr-base
buildBase:
	cd casr-base ; \
	make build ; \
	cd -

pushBase:
	cd casr-base ; \
	make push ; \
	cd -

# Clean up
clean: deleteLocalOBR
	cd casr-base ; \
	make clean ; \
	cd -
	for dir in casr-app-* ; do \
		cd $$dir ; \
		make clean ; \
		cd - ; \
	done
	rm -f pushlist

release: buildApps push

default: buildApps