node {
   stage 'Git Checkout'
   checkout([$class: 'GitSCM', branches: [[name: '*/development']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'scripts']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/ggatward/soe-ci-pipeline']]])

   checkout([$class: 'GitSCM', branches: [[name: '*/development']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'soe']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/ggatward/RHEL-SOE']]])

   stage 'Push Kickstarts'
   sh '''echo "#####################################################"
   echo "#                BUILDING KICKSTARTS                #"
   echo "#####################################################"
   /bin/bash -x ${WORKSPACE}/scripts/kickstartbuild.sh ${WORKSPACE}/soe/kickstarts'''

   stage '3'
   echo 'boo'
}
