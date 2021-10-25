#!/bin/bash
#

#
# create a proxy repo to https://repo1.maven.org/maven2/
#
curl --noproxy "*" -v -u admin:admin123 -X POST "http://localhost:8081/service/rest/v1/repositories/maven/proxy" -H "accept: application/json" -H "Content-Type: application/json" -d @-<<EOF
{
  "name": "microfeatures-central",
  "url": "http://localhost:8081/repository/microfeatures-central",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true,
    "writePolicy": "ALLOW"
  },
  "cleanup": null,
  "proxy": {
    "remoteUrl": "https://repo1.maven.org/maven2",
    "contentMaxAge": -1,
    "metadataMaxAge": 1440
  },
  "negativeCache": {
    "enabled": false,
    "timeToLive": 1440
  },
  "httpClient": {
    "blocked": false,
    "autoBlock": false,
    "connection": {
      "retries": null,
      "userAgentSuffix": null,
      "timeout": null,
      "enableCircularRedirects": false,
      "enableCookies": false
    },
    "authentication": null
  },
  "routingRuleName": null,
  "maven": {
    "versionPolicy": "RELEASE",
    "layoutPolicy": "STRICT"
  },
  "format": "maven2",
  "type": "proxy"
}
EOF

#
# create a proxy repo to https://packages.confluent.io/maven
#
curl --noproxy "*" -v -u admin:admin123 -X POST "http://localhost:8081/service/rest/v1/repositories/maven/proxy" -H "accept: application/json" -H "Content-Type: application/json" -d @-<<EOF
{
  "name": "microfeatures-confluent",
  "url": "http://localhost:8081/repository/microfeatures-confluent",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true,
    "writePolicy": "ALLOW"
  },
  "cleanup": null,
  "proxy": {
    "remoteUrl": "https://packages.confluent.io/maven",
    "contentMaxAge": -1,
    "metadataMaxAge": 1440
  },
  "negativeCache": {
    "enabled": false,
    "timeToLive": 1440
  },
  "httpClient": {
    "blocked": false,
    "autoBlock": false,
    "connection": {
      "retries": null,
      "userAgentSuffix": null,
      "timeout": null,
      "enableCircularRedirects": false,
      "enableCookies": false
    },
    "authentication": null
  },
  "routingRuleName": null,
  "maven": {
    "versionPolicy": "RELEASE",
    "layoutPolicy": "STRICT"
  },
  "format": "maven2",
  "type": "proxy"
}
EOF

#
# create a hosted repo to microfeatures-local
#
curl --noproxy "*" -v -u admin:admin123 -X POST "http://localhost:8081/service/rest/v1/repositories/maven/hosted" -H "accept: application/json" -H "Content-Type: application/json" -d @-<<EOF
{
  "name": "microfeatures-local",
  "url": "http://localhost:8081/repository/microfeatures-local",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true,
    "writePolicy": "ALLOW_ONCE"
  },
  "cleanup": null,
  "maven": {
    "versionPolicy": "RELEASE",
    "layoutPolicy": "STRICT"
  },
  "format": "maven2",
  "type": "hosted"
}
EOF

#
# create a group "microfeatures" repo regrouping above repositories
#
curl --noproxy "*" -v -u admin:admin123 -X POST "http://localhost:8081/service/rest/v1/repositories/maven/group" -H "accept: application/json" -H "Content-Type: application/json" -d @-<<EOF
{
  "name": "microfeatures",
  "format": "maven2",
  "url": "http://localhost:8081/repository/microfeatures",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true
  },
  "group": {
    "memberNames": [
      "microfeatures-local",
      "microfeatures-central",
      "microfeatures-confluent"
    ]
  },
  "type": "group"
}
EOF
