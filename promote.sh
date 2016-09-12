#!/bin/bash

# Promote content from _dev_ to _prod_

# Load common parameter variables
. $(dirname "${0}")/common.sh

#if [[ -z "$1" ]] || [[ ! -d "$1" ]]; then
#  usage "$0 <directory containing kickstart files>"
#  exit ${NOARGS}
#fi
#workdir=$1

if [[ -z ${WORKSPACE} ]] || [[ ! -w ${WORKSPACE} ]]; then
  err "Environment variable 'WORKSPACE' not set or not found"
  exit ${WORKSPACE_ERR}
fi

if [[ -z ${PUSH_USER} ]] || [[ -z ${RSA_ID} ]] || [[ -z ${SATELLITE} ]]; then
  err "PUSH_USER, RSA_ID  or SATELLITE not set or not found"
  exit ${WORKSPACE_ERR}
fi

# setup artefacts environment
#ARTEFACTS=${WORKSPACE}/artefacts/kickstarts
#mkdir -p $ARTEFACTS

# copy erb files from the main SOE directory to a specific kickstart artefact dir (clean temp space)
#rsync -td --out-format="#%n#" --delete-excluded --include=*.erb --exclude=* "${workdir}/" \
#   "${ARTEFACTS}" | grep -e '\.erb#$' | tee -a "${MODIFIED_CONTENT_FILE}"

# Replace all instances of SOE_dev_ with SOE_ in each .erb file (snippet call entry + snippet names)
sed -i 's/SOE_dev_/SOE_/g' ${WORKSPACE}/kickstarts/*.erb 


# Create version file





# sync our kickstart artefect dir with the one in our homedir on Satellite. We delete extraneous 
# kickstarts on the satellite so that we don't keep pushing obsolete kickstarts into satellite
#rsync --delete -va -e "ssh -l ${PUSH_USER} -i /var/lib/jenkins/.ssh/id_rsa" -va \
#    ${ARTEFACTS} ${SATELLITE}:







