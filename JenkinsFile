node {
   stage 'Git Checkout'
   checkout([$class: 'GitSCM', branches: [[name: '*/development']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '13c1dba5-d57f-415b-a8cc-44fc827b8014', url: 'https://github.com/ggatward/RHEL-soe']]])
   stage 'Stage 2'
   echo 'Hello World 2'
}
