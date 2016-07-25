#!/bin/bash -x

# Push BATS tests to test VMs
#
# e.g ${WORKSPACE}/scripts/pushtests.sh 'hostname'
#

# Load common parameter variables
. $(dirname "${0}")/common.sh

TESTVM=$1

export SSH_ASKPASS=${WORKSPACE}/scripts/askpass.sh
export DISPLAY=nodisplay
# Root password for test host - Exists as a Jenkins env variable
export ROOTPASS

# copy our tests to the test servers
info "Setting up ssh keys for test server $TESTVM"
sed -i.bak "/^$TESTVM[, ]/d" ${KNOWN_HOSTS} # remove test server from the file

# Copy Jenkins' SSH key to the newly created server(s)
if [ $(sed -e 's/^.*release //' -e 's/\..*$//' /etc/redhat-release) -ge 7 ]; then
  # Only starting with RHEL 7 does ssh-copy-id support -o parameter
  setsid ssh-copy-id -o StrictHostKeyChecking=no -i ${RSA_ID} root@$TESTVM
else # Workaround for RHEL 6 and before
  setsid ssh -o StrictHostKeyChecking=no -i ${RSA_ID} root@$TESTVM 'true'
  setsid ssh-copy-id -i ${RSA_ID} root@$TESTVM
fi

info "Installing bats and rsync on test server $TESTVM"
if ssh -o StrictHostKeyChecking=no -i ${RSA_ID} root@$TESTVM "yum install -y bats rsync"; then
  info "copying tests to test server $TESTVM"
  rsync --delete -va -e "ssh -o StrictHostKeyChecking=no -i ${RSA_ID}" \
    ${WORKSPACE}/soe/tests root@$TESTVM:
else
  err "Couldn't install rsync and bats on '$TESTVM'."
  exit 1
fi

# Pause introduced to allow puppet installation/configuration to complete prior to the tests
sleep 60

# execute the tests in parallel on all test servers
mkdir -p ${WORKSPACE}/test_results
info "Starting TAPS tests on test server $TESTVM"
ssh -o StrictHostKeyChecking=no -i ${RSA_ID} root@$TESTVM \
  'cd tests ; bats -t *.bats' > ${WORKSPACE}/test_results/$TESTVM.tap &

# wait until all backgrounded processes have exited
wait
