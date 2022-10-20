net-socket
==========

For user permissions to bind an ICMP socket on Linux, run 

```
# sysctl -w net.ipv4.ping_group_range="0 65535"
```
and
 
```
# checkmodule -M -m -o ping.mod ping.te
# semodule_package -o ping.pp -m ping.mod
# semodule -i ping.pp
```