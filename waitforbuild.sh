#!/bin/bash -x

# Wait for VMs to build
#
# e.g ${WORKSPACE}/scripts/waitforbuild.sh 'hostname'
#

# Load common parameter variables
. $(dirname "${0}")/common.sh

TEST_VM=$1

# we need to wait until all the test machines have been rebuilt by foreman
declare -A vmcopy # declare an associative array to copy our VM array into
vmcopy[$I]=$TEST_VM

WAIT=0
while [[ ${#vmcopy[@]} -gt 0 ]]
do
    sleep 10
    ((WAIT+=10))
    info "Waiting 10 seconds"
    for I in "${vmcopy[@]}"
    do
        echo -n "Checking if test server $I has rebuilt... "
        status=$(ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
            "hammer host info --name $I | \
            grep -e \"Managed.*true\" -e \"Enabled.*true\" -e \"Build.*false\" \
		| wc -l")
        # Check if status is OK, ping reacts and SSH is there, then success!
        if [[ ${status} == 3 ]] && ping -c 1 -q $I && nc -w 1 $I 22
        then
            tell "Success!"
            unset vmcopy[$I]
        else
            tell "Not yet."
        fi
    done
    if [[ ${WAIT} -gt 6000 ]]
    then
        err "Test servers still not rebuilt after 6000 seconds. Exiting."
        exit 1
    fi
done

