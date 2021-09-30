#!/bin/bash
# Promotes a given group.id stored in csf-mvn-candidates-local to csf-mvn-delivered-local
#

function usage {
	echo "$0 -a artifactoryRootUrl -g groups -c candidateRepo -r releaseRepo"
	echo "Example:"
	echo "$0 -u admin -p admin123 -a https://repo.lab.pl.alcatel-lucent.com -g \"com.nokia.casr com.nokia.cjdi com.nokia.cdlb\" -c csf-mvn-candidates-local -r csf-mvn-delivered-local"
	exit 1
}

[ "$TMPDIR" = "" ] && TMPDIR="/tmp"

# temp file used to get candidate artifacts
CURLOUT=$TMPDIR/$$.candidates
rm -f $CURLOUT
[ $? != 0 ] && echo "***Cannot clean temp file: $CURLOUT" && exit 2

# directory where files are downloaded
DOWNLOAD=$TMPDIR/$$.download
rm -rf $DOWNLOAD
[ $? != 0 ] && echo "***Cannot clean temp download dir: $DOWNLOAD" && exit 2
mkdir -p $DOWNLOAD

# Check parameters
while  [ ! $# = 0 ]
do
case $1 in
	-h|-help|--help)
	    usage
	    ;;
	-u)
	    shift
	    user=$1
	    ;;

	-p)
	    shift
	    password=$1
	    ;;
	-a)
	    shift
	    artifactoryUrl=$1
	    ;;
	-g)
	    shift
	    group="$1"
	    ;;
	-c)
	    shift
	    candidateRepo=$1
	    ;;
	-r)
	    shift
	    releaseRepo=$1
	    ;;
	*)
	    usage
	    ;;
esac
shift
done

[ "$artifactoryUrl" == "" ] && echo "*** Missing -a option" && exit 2
[ "$group" == "" ] && echo "*** Missing -g option" && exit 2
[ "$user" == "" ] && echo "*** Missing -u option" && exit 2
[ "$password" == "" ] && echo "*** Missing -p option" && exit 2
[ "$candidateRepo" == "" ] && echo "*** Missing -c option" && exit 2
[ "$releaseRepo" == "" ] && echo "*** Missing -r option" && exit 2

rm -fr $DOWNLOAD
mkdir -p $DOWNLOAD
cd $DOWNLOAD

for g in $group; do
	curl -s -o $CURLOUT -k $artifactoryUrl/api/search/gavc?g=$g\&repos=$candidateRepo
	l=$(cat $CURLOUT | wc -l)
	[ $l -le 3 ] && echo "***No artifacts found, nothing to promote" && exit 0
	# Get urls
	awk -F\" '{ print $4 }' $CURLOUT > ${CURLOUT}.url

	for file in $(cat ${CURLOUT}.url); do
	    dir=$(dirname $(echo $file | sed "s:.*$candidateRepo/::"))
	    mkdir -p $dir
	    cd $dir
	    echo "Downloading $file ..."
	    wget --no-check-certificate -q $file
	    cd - > /dev/null 2>&1
	done
done
echo

cd $DOWNLOAD
for artifact in `find . -type f`; do
    # remove ./ 
    artifact=`echo $artifact | sed 's/..//'`
    echo "Promoting $artifact ..."
    curl -X POST -u${user}:${password} ${artifactoryUrl}/api/copy/${candidateRepo}/$artifact/?to=${releaseRepo}/$artifact
    echo
    echo
done

rm -f $CURLOUT
rm -rf $DOWNLOAD

