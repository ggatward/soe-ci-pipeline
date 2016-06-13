#!/bin/bash

# Search for kickstarts within our SOE repo
#
# e.g. ${WORKSPACE}/scripts/kickstart.sh ${WORKSPACE}/soe/kickstarts/ 
#

# Load common parameter variables
. $(dirname "${0}")/common.sh

if [[ -z "$1" ]] || [[ ! -d "$1" ]]; then
  usage "$0 <directory containing kickstart files>"
  exit ${NOARGS}
fi
workdir=$1

if [[ -z ${WORKSPACE} ]] || [[ ! -w ${WORKSPACE} ]]; then
  err "Environment variable 'WORKSPACE' not set or not found"
  exit ${WORKSPACE_ERR}
fi

if [[ -z ${PUSH_USER} ]] || [[ -z ${RSA_ID} ]] || [[ -z ${SATELLITE} ]]; then
  err "PUSH_USER, RSA_ID  or SATELLITE not set or not found"
  exit ${WORKSPACE_ERR}
fi

# setup artefacts environment
ARTEFACTS=${WORKSPACE}/artefacts/kickstarts
mkdir -p $ARTEFACTS

# copy erb files from the main SOE directory to a specific kickstart artefact dir (clean temp space)
rsync -td --out-format="#%n#" --delete-excluded --include=*.erb --exclude=* "${workdir}/" \
   "${ARTEFACTS}" | grep -e '\.erb#$' | tee -a "${MODIFIED_CONTENT_FILE}"

# sync our kickstart artefect dir with the one in our homedir on Satellite. We delete extraneous 
# kickstarts on the satellite so that we don't keep pushing obsolete kickstarts into satellite
#rsync --delete -va -e "ssh -l ${PUSH_USER} -i /var/lib/jenkins/.ssh/id_rsa" -va \
#    ${ARTEFACTS} ${SATELLITE}:kickstarts
rsync --delete -va -e "ssh -l ${PUSH_USER} -i /var/lib/jenkins/.ssh/id_rsa" -va \
    ${ARTEFACTS} ${SATELLITE}:

# either update or create each kickstart in turn
cd ${ARTEFACTS}
for I in *.erb; do
  name=$(sed -n 's/^name:\s*\(.*\)/\1/p' ${I})
  id=0
  id=$(ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
            "/usr/bin/hammer --csv template list --per-page 9999" | grep "${name}" | cut -d, -f1)
  ttype=$(sed -n 's/^kind:\s*\(.*\)/\1/p' ${I})
  if [[ ${id} -ne 0 ]]; then
    ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
      "/usr/bin/hammer template update --id ${id} --file kickstarts/${I} --type ${ttype}"
  else
    ssh -q -l ${PUSH_USER} -i ${RSA_ID} ${SATELLITE} \
      "/usr/bin/hammer template create --file kickstarts/${I} --name \"${name}\" --type ${ttype} --organizations \"${ORG}\""
  fi
done


