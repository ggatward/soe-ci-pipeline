#!/bin/bash
 
# Push Puppet Modules out via r10k
#
# e.g. ${WORKSPACE}/scripts/r10kdeploy.sh
#
 
. $(dirname "${0}")/common.sh
 
if [[ -z ${PUSH_USER} ]] || [[ -z ${SATELLITE} ]] || [[ -z ${R10K_USER} ]]; then
  err "PUSH_USER, R10K_USER or SATELLITE not set or not found"
  exit ${WORKSPACE_ERR}
fi
 
# Push the r10k.yaml config from our GIT repo over to Satellite
rsync --delete -va -e "ssh -l ${PUSH_USER} -i /var/lib/jenkins/.ssh/id_rsa" -va \
    y10k.yaml ${SATELLITE}:

# use hammer on the satellite to find all capsules. Then use R10K to push the modules
# into the puppet environmnet on each capsule
for I in $(ssh -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
      "hammer --csv capsule list" | tail -n +2 | awk -F, '{print $2}'); do
  ssh -l ${R10K_USER} -i ${RSA_ID} ${I} \
      "cd /etc/puppet ; r10k deploy environment ${R10K_ENV} -c /var/lib/jenkins/r10k.yaml -pv"
done
