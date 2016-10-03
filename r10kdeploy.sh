#!/bin/bash
 
# Push Puppet Modules out via r10k
#
# e.g. ${WORKSPACE}/scripts/r10kdeploy.sh
#
 
. $(dirname "${0}")/common.sh
 
if [[ -z ${PUSH_USER} ]] || [[ -z ${SATELLITE} ]] || [[ -z ${RSA_ID} ]]; then
  err "PUSH_USER, SATELLITE or RSA_ID not set or not found"
  exit ${WORKSPACE_ERR}
fi
 
# Pull out the puppet basedir from the r10k.yaml
BASEDIR=$(cat ${WORKSPACE}/scripts/r10k.yaml | grep basedir | awk '{ print $2 }')

# Push the r10k.yaml config from our GIT repo over to Satellite
rsync --delete -va -e "ssh -l ${PUSH_USER} -i ${RSA_ID}" -va \
    ${WORKSPACE}/scripts/r10k.yaml ${SATELLITE}:

# use hammer on the satellite to find all capsules. Then use R10K to push the modules
# into the puppet environmnet directory on each capsule
ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
    "cd /etc/puppet ; r10k deploy environment ${R10K_ENV} -c /var/lib/jenkins/r10k.yaml -pv"

# Clean up extra non-puppet GIT atrefacts that are pulled by r10k
ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
    "cd ${BASEDIR}/${R10K_ENV}; rm -rf tests kickstarts LICENSE README.md"

# Need to fix perms post-deploy (Requires entries in /etc/sudoers.d/jenkins on Satellite)
#ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
#    "sudo /bin/chown -R apache ${BASEDIR}"
#ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
#    "sudo /usr/sbin/restorecon -Fr ${BASEDIR}"

# Clone the updated directory to all capsules
for I in $(ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
    "hammer --csv capsule list" | tail -n +2 | awk -F, '{print $2}'); do
  if [ "${I}" != "${SATELLITE}" ]; then
    ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
       "rsync --delete -va -e \"ssh -q -l ${PUSH_USER} -i ${RSA_ID}\" -va ${BASEDIR}/${R10K_ENV} ${I}:${BASEDIR}"
  fi
done


# See if the environent already exits:
if [ $(ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
      "hammer environment list | awk ' /^[0-9]/ { print $3 }' | grep -c ${R10K_ENV}") -eq 0 ]; then
  ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
      "hammer environment create --name ${R10K_ENV} --locations '${PUPPET_LOCATIONS}' --organizations ${ORG}"
else
  ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
      "hammer environment update --name ${R10K_ENV} --locations '${PUPPET_LOCATIONS}' --organizations ${ORG}"
fi

# Import the puppet classes
ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} "hammer proxy import-classes --id 1 --environment ${R10K_ENV}"

# Output to the logs all classes in the env
ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} "hammer environment info --name ${R10K_ENV}"


