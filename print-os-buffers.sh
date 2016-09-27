#!/bin/bash
# prints the os buffers for the in and out connections (on macOS)
watch -d -n 1 'netstat -n -p tcp | grep 127 | grep 8080'