#!/bin/bash

VBoxManage controlvm small-group-0 poweroff
VBoxManage unregistervm small-group-0

echo Removing nodes...
rm -rf ~/.vmfest/nodes

echo Done.
