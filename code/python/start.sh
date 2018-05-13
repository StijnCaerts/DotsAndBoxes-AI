(cd dotsandboxes; python3.6 dotsandboxesserver.py 8080) &
python3 agent.py 10001 &
python3 agent.py 10002 &
read -p "Press enter to close all programs."

trap "exit" INT TERM
trap "kill 0" EXIT
