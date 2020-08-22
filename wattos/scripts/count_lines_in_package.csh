wc src/*/*/*.java           | tail -1 > /tmp/t
wc src/*/*/*/*.java         | tail -1 >> /tmp/t
wc src/*/*/*/*/*.java       | tail -1 >> /tmp/t
$SJ/scripts/nawk/average_column -v first=1 -v last=1 /tmp/t
