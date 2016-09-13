#!/bin/bash

# Promote content from _dev_ to _prod_

# Load common parameter variables
. $(dirname "${0}")/common.sh

# Jenkins will pass us the latest good dev commit id
SOE_COMMIT=$1


if [[ -z ${WORKSPACE} ]] || [[ ! -w ${WORKSPACE} ]]; then
  err "Environment variable 'WORKSPACE' not set or not found"
  exit ${WORKSPACE_ERR}
fi

if [[ -z ${PUSH_USER} ]] || [[ -z ${RSA_ID} ]] || [[ -z ${SATELLITE} ]]; then
  err "PUSH_USER, RSA_ID  or SATELLITE not set or not found"
  exit ${WORKSPACE_ERR}
fi

# Jenkins will have already pulled the current 'master' branch for us. At this stage in the workspace we should have:
# /scripts   - the CI scripts that were checked out pre-dev build
# /soe       - the DEV soe that was checked out and tested
# /soemaster - the latest 'master' branch
#
# What we will do here is to merge the current good 'dev' branch into 'master'
cd ${WORKSPACE}/soemaster
git merge ${SOE_COMMIT}


# Replace all instances of SOE_dev_ with SOE_ in each .erb file (snippet call entry + snippet names)
sed -i 's/SOE_dev_/SOE_/g' ${WORKSPACE}/soemaster/kickstarts/*.erb 


# Create version file - read latest git tag, tag+1



# Merge+push, create new tag
cd ${WORKSPACE}/soemaster
git commit -a -m "Automatic promotion by Jenkins"
git push



