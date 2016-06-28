/****************************************************************************
 * Git Checkout
 ****************************************************************************/
freeStyleJob('SOE/Development/GIT_Checkout') {
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
        url("${CI_GIT_URL}")
      }
      branch('development')
      shallowClone(true)
      createTag(false)
      relativeTargetDir('scripts')
    }
    git {
      remote {
        url("${SOE_GIT_URL}")
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
    downstream('Push_Kickstarts', 'SUCCESS')
    publishCloneWorkspace('**') {
      criteria('Successful')
    }
    mailer('$EMAIL_TO', true, false)
  }
}

/****************************************************************************
 * Push Kickstarts
 ****************************************************************************/
freeStyleJob('SOE/Development/Push_Kickstarts') {
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
      parentJobName('SOE/Development/GIT_Checkout')
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
    downstream('Deploy_Puppet_Modules', 'SUCCESS')
    mailer('$EMAIL_TO', true, false)
  }
}


/****************************************************************************
 * Deploy Puppet Modules
 ****************************************************************************/
freeStyleJob('SOE/Development/Deploy_Puppet_Modules') {
  description('Trigger r10k on Satellite 6 to pull required Puppet modules')
  displayName('03: Deploy SOE puppet modules to Satellite')
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
      parentJobName('SOE/Development/GIT_Checkout')
      criteria('')
    }
  }
  wrappers {
    preBuildCleanup()
    environmentVariables {
      propertiesFile('scripts/PARAMETERS')
      env('R10K_ENV', 'RHEL_SOE_development')
    }
  }
  steps {
    shell('''
echo "#####################################################"
echo "#           DEPLOYING PUPPET MODULES                #"
echo "#####################################################"
/bin/bash -x ${WORKSPACE}/scripts/r10kdeploy.sh
    ''')
  }
  publishers {
    downstream('Boot_Test_VMs', 'SUCCESS')
    mailer('$EMAIL_TO', true, false)
  }
}


/****************************************************************************
 * Reboot Test VMs
 ****************************************************************************/
freeStyleJob('SOE/Development/Boot_Test_VMs') {
  description('Reboot all test VMs to trigger PXE build')
  displayName('04: Reboot test VMs')
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
      parentJobName('SOE/Development/GIT_Checkout')
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
echo "#                REBUILDING TEST VMS                #"
echo "#####################################################"
/bin/bash -x ${WORKSPACE}/scripts/buildtestvms.sh 
    ''')
  }
  publishers {
    mailer('$EMAIL_TO', true, false)
  }
}

