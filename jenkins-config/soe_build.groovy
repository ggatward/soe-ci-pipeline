/****************************************************************************
 * Create jobs
 ****************************************************************************/
freeStyleJob('SOE1/Development/GIT_Checkout') {
  properties {
    buildDiscarder {
      strategy {
        logRotator {
          numToKeepStr('5')
          artifactDaysToKeepStr('')
          artifactNumToKeepStr('')
          daysToKeepStr('')
        }
      }
    }
  }
  multiscm {
    git {
      remote {
        url('https://github.com/ggatward/soe-ci-pipeline')
      }
      branch('development')
      shallowClone(true)
      createTag(false)
      relativeTargetDir('scripts')
    }
    git {
      remote {
        url('https://github.com/ggatward/RHEL-SOE')
      }
      branch('development')
      shallowClone(true)
      createTag(false)
      relativeTargetDir('soe')
    }
  }
  wrappers {
    preBuildCleanup()
    environmentVariables {
      propertiesFile('scripts/PARAMETERS')
    }
  }
  publishers {
    downstream('SOE1/Push_Kickstarts', 'SUCCESS')
    publishCloneWorkspace('**') {
      criteria('Successful')
    }
    mailer('$EMAIL_TO', true, false)
  }
}

