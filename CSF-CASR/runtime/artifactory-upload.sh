#!/bin/bash

usage() {
        echo "Please check the Usage of the Script, there were no enough parameters supplied."
        echo "Usage: ArtifactoryUpload.sh localFilePath Repo user password GroupID ArtifactID VersionID"
        exit 1
}

if [ -z "$7" ]; then
        usage
fi

localFilePath="$1"
REPO="$2"
USER="$3"
PSW="$4"
groupId=`echo $5|sed "s/\./\//g"`
artifactId="$6"
versionId="$7"

if [ ! -f "$localFilePath" ]; then
        echo "ERROR: local file $localFilePath does not exists!"
        exit 1
fi

which md5sum || exit $?
which sha1sum || exit $?

md5Value="`md5sum "$localFilePath"`"
md5Value="${md5Value:0:32}"

sha1Value="`sha1sum "$localFilePath"`"
sha1Value="${sha1Value:0:40}"

fileName="`basename "$localFilePath"`"
set -x
if [[ "$fileName" == *.gz ]]
then
    fileName=`basename $fileName .gz`
    fileExt="${fileName##*.}".gz
else
    fileExt="${fileName##*.}"
fi

echo $md5Value $sha1Value $localFilePath
echo "INFO: Uploading $localFilePath to $REPO/$groupId/$artifactId/$versionId/$artifactId-$versionId.$fileExt"

if [ "$USER" != "anonymous" ]; then
    curl -u $USER:$PSW -i -X PUT -H "X-Checksum-MD5: $md5Value" -H "X-Checksum-Sha1: $sha1Value" -T "$localFilePath" "$REPO/$groupId/$artifactId/$versionId/$artifactId-$versionId.$fileExt"
else
    curl -i -X PUT -H "X-Checksum-MD5: $md5Value" -H "X-Checksum-Sha1: $sha1Value" -T "$localFilePath" "$REPO/$groupId/$artifactId/$versionId/$artifactId-$versionId.$fileExt"
fi
