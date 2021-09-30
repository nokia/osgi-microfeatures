if [ -e "/casr/debug" ]; then
  exit 0
else
  echo -e '\x1dclose\x0d' | telnet localhost 17000
fi