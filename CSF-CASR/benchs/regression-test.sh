#!/bin/bash
#set this hostname
HOSTNAME=`hostname --short`

# Set Graphite host
IP_chine=139.54.131.117
IP_mordor=139.54.131.190
IP_rohan=139.54.131.191
IP_moria=139.54.131.192

#Testsnames

TestNameCASF=TestCASFSmallsmallNreq_50K_`date +'%Y-%m-%d_%H:%M:%S'`
TestNameCASFHTTPS=TestCASFHTTPSSmallsmallNreq_50K_`date +'%Y-%m-%d_%H:%M:%S'`
TestNameCASRJaxrs=TestCASRSJaxrsmallNreq_66K_`date +'%Y-%m-%d_%H:%M:%S'`
TestNameCASRJaxrss=TestCASRSSJaxrsmallNreq_66K_`date +'%Y-%m-%d_%H:%M:%S'`
TestNameCASRJetty=TestCASRJettySmallNreq_40K_`date +'%Y-%m-%d_%H:%M:%S'`
TestNameCASRJettyHTTPS=TestCASRJettyHTTPSSmallNreq_40K_`date +'%Y-%m-%d_%H:%M:%S'`
TestNameCJEE=TestJbossSmallNreq_30_`date +'%Y-%m-%d_%H:%M:%S'`

#function help 

display_usage() {
        echo "This script must be run with 5  arguments. For the first one is the name of the server to bench, choose it from this list"
        echo "CASRJAXRS, CASRJAXRSS, CASRJETTY, CASRJETTYHTTPS, CASF, CASFHTTPS, CJEE"
        echo "The 2 argument is the number of requests to inject per second"
        echo "the 3 argument is the address IP of Server"
        echo "the 4 argument is the address IP of Injector"
        echo "the 5 argument is the duration of the test in minute"
        echo
        echo "Example:"
        echo "./regression-test.sh CASRJAXRS 1000 139.54.131.190 139.54.131.117 5"
}

# if no arguments supplied, display usage

       if [ $# != 5 ]
       then
               echo "at least 5 args required"
               display_usage
               exit
       fi

#if argument different to CASR JAXRS, display usage
        if  [ $1 != 'CASRJAXRS' ] && [ $1 != 'CASRJAXRSS' ] && [ $1 != 'CASRJETTY' ] && [ $1 != 'CASRJETTYHTTPS' ] &&[ $1 != 'CASF' ] && [ $1 != 'CASFHTTPS' ] &&[ $1 != 'CJEE' ]
        then
              display_usage
              exit
        fi
# check whether user had supplied -h or --help . If yes display usage
        if [[ ( $# == "--help") ||  $# == "-h" ]]
        then
                display_usage
                exit
        fi

echo "###############################################################################################################"
echo "You have choose $1 server to bench "
echo "###############################################################################################################"


# ssh to mordor and start the WebServer
echo "----------------------------------------------------------------------------------------------------------------"
  echo "This script is about to connect to mordor server"
echo "----------------------------------------------------------------------------------------------------------------"
  sshpass -p "nxuser" ssh -o StrictHostKeyChecking=no nxuser@$3 <<EOF

echo "###############################################################################################################"
  echo "Welcome to Mordor"
echo "###############################################################################################################"

  sudo pkill -f Bootstrap
  sleep 3s
  sudo pkill -f jboss-modules.jar
  sleep 3s
  sudo pkill -f FelixLauncher
  sleep 3s

echo "----------------------------------------------------------------------------------------------------------------"
  echo "all web servers are stoped"
echo "----------------------------------------------------------------------------------------------------------------"

    if [ "$1" = "CASF" ]; then
       cd /opt/tomcat
       sudo ./service.sh start
    elif [ "$1" = "CASFHTTPS" ]; then
       cd /opt/tomcat
       sudo ./service.sh start
    elif [ "$1" = "CASRJAXRS" ]; then
        cd /data1/server/jax-rs-server-18.7.1-unsecure-1.0.0
        ./start.sh 
    elif [ "$1" = "CASRJAXRSS" ]; then
        cd /data1/server/jax-rs-server-18.7.1-secure-1.0.0
        ./start.sh 
    elif [ "$1" = "CASRJETTY" ]; then
         cd /data1/server/CASRF/CASR_FASTER-1.0
         ./start.sh&      
         pwd 
     elif [ "$1" = "CASRJETTYHTTPS" ]; then
         cd /data1/server/CASR-HTTPS-1.0.0
          ./start.sh&      
         pwd 
   elif [ "$1" = "CJEE" ]; then
         cd /opt/wildfly
         sudo ./service.sh start
    fi 
  sleep 5s

echo "###############################################################################################################"
  echo  "The $1 server is started "
echo "###############################################################################################################"

  sudo jps
EOF
 sleep 3s
# SSH to the Injector, choose the test script and run it

echo "###############################################################################################################"
  echo "This script is about to connect to the injector server"
echo "###############################################################################################################"
 sshpass -p "nxuser" ssh -o StrictHostKeyChecking=no nxuser@$4 <<EOF
echo "----------------------------------------------------------------------------------------------------------------"
  echo "Welcome to Chine"
echo "----------------------------------------------------------------------------------------------------------------"

pkill -f gatling

 cd /data1/a5350/gatling/bin/
#echo JAVA_OPTS=”users=["$2"]” >> gatling.sh
#echo DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS} -users=[$2]" >> gatling.sh
 #export JAVA_OPTS=”users=["$2"]” 
echo "###############################################################################################################"
 echo "you have choose the $1 server to test" 
echo "###############################################################################################################"

    if [ "$1" = "CASF" ]; then
      pwd

    JAVA_OPTS="-Dusers="$2" -Dramp="$5"" ./gatling.sh -s computerdatabase.advanced.TestTomcatSmallNreq -on $TestNameCASF

    elif [ "$1" = "CASFHTTPS" ]; then
    JAVA_OPTS="-Dusers="$2" -Dramp="$5"" ./gatling.sh -s computerdatabase.advanced.TestTomcatHTTPS -on $TestNameCASFHTTPS

    elif [ "$1" = "CASRJAXRS" ]; then
    JAVA_OPTS="-Dusers="$2" -Dramp="$5"" ./gatling.sh -s computerdatabase.advanced.TestCASRSmallJaxrs -on $TestNameCASRJaxrs

    elif [ "$1" = "CASRJAXRSS" ]; then
    JAVA_OPTS="-Dusers="$2" -Dramp="$5"" ./gatling.sh -s computerdatabase.advanced.TestAnselmeJaxrs -on $TestNameCASRJaxrss

    elif [ "$1" = "CASRJETTY" ]; then
    JAVA_OPTS="-Dusers="$2" -Dramp="$5"" ./gatling.sh -s computerdatabase.advanced.TestCASRSmallNreq -on $TestNameCASRJetty
    
    elif [ "$1" = "CASRJETTYHTTPS" ]; then
    JAVA_OPTS="-Dusers="$2" -Dramp="$5"" ./gatling.sh -s computerdatabase.advanced.TestCASRJettyHTTPS -on $TestNameCASRJettyHTTPS

    elif [ "$1" = "CJEE" ]; then
    JAVA_OPTS="-Dusers="$2" -Dramp="$5"" ./gatling.sh -s computerdatabase.advanced.TestJbossSmallNreq -on $TestNameCJEE
    fi  


EOF

#Take a snapshot of grafana metrics 
#echo "###############################################################################################################"
#echo " We are going to take a snapshot of grafana metrics "
#echo "###############################################################################################################"


#echo "username=admin&password=admin" | firefox "http://$4:3000/dashboard/db/chine?refresh=5s&orgId=1" -post_data &
#sleep 5
#xdotool key F11
#sleep 30s

#DATE=$(date +%Y-%m-%d-%H:%M:%S)
#gnome-screenshot -f /home/soukaina/Documents/grafana/grafana-result_$1_$DATE.png

#echo "###############################################################################################################"
#echo "The grafana snapshot is available: /home/soukaina/Documents/grafana/grafana-result_$1_$DATE.png"
#echo "###############################################################################################################"


#gnome-screenshot -f /home/user/Downloads/Screenshot-$DATE.png
#scrot  'grafana-result_'+$i+'_`date +'%Y-%m-%d_%H:%M:%S'`.png' -e 'mv $f /home/soukaina/Documents/grafana'
