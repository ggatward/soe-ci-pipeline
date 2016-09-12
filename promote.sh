#!/bin/bash

# Promote content from _dev_ to _prod_

# Load common parameter variables
. $(dirname "${0}")/common.sh


if [[ -z ${WORKSPACE} ]] || [[ ! -w ${WORKSPACE} ]]; then
  err "Environment variable 'WORKSPACE' not set or not found"
  exit ${WORKSPACE_ERR}
fi

if [[ -z ${PUSH_USER} ]] || [[ -z ${RSA_ID} ]] || [[ -z ${SATELLITE} ]]; then
  err "PUSH_USER, RSA_ID  or SATELLITE not set or not found"
  exit ${WORKSPACE_ERR}
fi


# Replace all instances of SOE_dev_ with SOE_ in each .erb file (snippet call entry + snippet names)
sed -i 's/SOE_dev_/SOE_/g' ${WORKSPACE}/soe/kickstarts/*.erb 


# Create version file - read latest git tag, tag+1



# Merge+push, create new tag




