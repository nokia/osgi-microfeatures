function wait_str() {
 	local file="$1"; shift
  	local search_term="$1"; shift
  	local wait_time="${1:-5m}"; shift # 5 minutes as default timeout
  	(timeout $wait_time tail -F -n1000000000 "$file" &) | grep -q "$search_term" && return 0
  	echo "Timeout of $wait_time reached. Unable to find '$search_term' in '$file'"
  	cat $file
  	exit 1
}


# check test result using client log file.
CLIENT_LOGFILE=../runtimes/client/var/log/csf.runtime__component.instance/msg.log
SERVER_LOGFILE=../runtimes/server/var/log/csf.runtime__component.instance/msg.log

echo "Waiting for test results"
wait_str $CLIENT_LOGFILE "Junit4Osgi: Tests done: passed: 1" 30


