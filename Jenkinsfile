node {
   stage 'Git Checkout'
   checkout([$class: 'GitSCM', branches: [[name: '*/development']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'scripts']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/ggatward/soe-ci-pipeline']]])

   stage 'Stage 2'
   echo 'Hello World 2'
}
