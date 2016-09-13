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

# First we'll set some global options if they are not currently there:
git config --global user.name "Jenkins CI/CD"
git config --global user.email jenkins@example.com
git config --global push.default simple

# Next, what we will do here is to merge the current good 'dev' branch into 'master'
cd ${WORKSPACE}/soemaster
git merge ${SOE_COMMIT}

# Replace all instances of SOE_dev_ with SOE_ in each .erb file (snippet call entry + snippet names)
sed -i 's/SOE_dev_/SOE_/g' ${WORKSPACE}/soemaster/kickstarts/*.erb 

# Create version file - read latest git tag, tag+1
# Read current tag list so we can increment the version
tag=$(git describe $(git rev-list --tags --max-count=1))

tagver=$(( $(echo $tag | cut -f2 -d.) + 1 ))

echo tagver=$tagver
TAG="v0.${tagver}"

echo `date '+%H%M%S'` > ${WORKSPACE}/soemaster/version


# Commit the changes, push and tag
git commit -a -m "Automatic promotion by Jenkins"
git push origin HEAD:master
git tag -a ${TAG} -m "Auto Tag by Jenkins"
git push origin --tags


