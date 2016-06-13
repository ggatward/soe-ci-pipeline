#!/bin/bash -x

# Wait for VMs to build
#
# e.g ${WORKSPACE}/scripts/waitforbuild.sh 'hostname'

# Load common parameter variables
. $(dirname "${0}")/common.sh

TESTVM=$1

# we need to wait until all the test machines have been rebuilt by foreman
buildvm=true

WAIT=0
while [[ ${buildvm} = true ]]; do
  sleep 10
  ((WAIT+=10))
  info "Waiting 10 seconds"
  echo -n "Checking if test server $TESTVM has rebuilt... "
  status=$(ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
      "hammer host info --name $TESTVM | \
       grep -e \"Managed.*true\" -e \"Enabled.*true\" -e \"Build.*false\" | wc -l")
  # Check if status is OK, ping reacts and SSH is there, then success!
  if [[ ${status} == 3 ]] && ping -c 1 -q $TESTVM && nc -w 1 $TESTVM 22; then
    # A PXE install takes at least 5 minutes. Detect if the VM simply rebooted without
    #   rebuilding and return a fail if it did.
#    if [[ ${WAIT} -lt 300 ]]; then
#      err "Test server looks to have simply rebooted without rebuilding. Exiting."
#      exit 1
#    else
      tell "Success!"
      unset buildvm
#    fi
  else
    tell "Not yet."
  fi
  if [[ ${WAIT} -gt 6000 ]]; then
    err "Test servers still not rebuilt after 6000 seconds. Exiting."
    exit 1
  fi
done

