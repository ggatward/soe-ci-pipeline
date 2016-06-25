/****************************************************************************
 * Git Checkout
 ****************************************************************************/
freeStyleJob('SOE1/Development/GIT_Checkout') {
  description('Checkout CI scripts and Development SOE GIT branch')
  displayName('01: Checkout GIT Repos')
  blockOnDownstreamProjects()
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

/****************************************************************************
 * Push Kickstarts
 ****************************************************************************/
freeStyleJob('SOE1/Development/Push_Kickstarts') {
  description('Push Kickstarts to Satellite 6')
  displayName('02: Deploy kickstart files to Satellite')
  blockOnDownstreamProjects()
  blockOnUpstreamProjects()
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
  scm {
    cloneWorkspaceSCM {
      parentJobName('SOE1/Development/GIT_Checkout')
      criteria('')
    }
  }
  wrappers {
    preBuildCleanup()
    environmentVariables {
      propertiesFile('scripts/PARAMETERS')
    }
  }
  steps {
    shell('''
      echo "#####################################################"
      echo "#                BUILDING KICKSTARTS                #"
      echo "#####################################################"
      /bin/bash -x ${WORKSPACE}/scripts/pushkickstarts.sh ${WORKSPACE}/soe/kickstarts
    ''')
  }
  publishers {
    downstream('SOE1/Deploy_Puppet_Modules', 'SUCCESS')
    mailer('$EMAIL_TO', true, false)
  }
}


