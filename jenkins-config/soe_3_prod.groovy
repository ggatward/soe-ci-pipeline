
/*******************************************************************
 * Define all test hosts here. 
 * Format is 'Description':'hostname'
 *******************************************************************/

def prodHosts = [
  'VMware_RHEL7':'buildbot3.lab.home.gatwards.org', 
  'VMware_RHEL6':'buildbot4.lab.home.gatwards.org',
]


/****************************************************************************
 * Git Checkout
 ****************************************************************************/
freeStyleJob('SOE/Production/Promote') {
  description('Promote SOE elements to production')
  displayName('1. Promote SOE artefacts')
  blockOnDownstreamProjects()
  label('master')
  parameters {
    stringParam('SOE_COMMIT', '111', 'Git Commit ID')
  }
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
/*    cloneWorkspaceSCM {
      parentJobName('SOE/SOE_Checkout')
      criteria('Successful')
    }
*/
    git {
      remote {
        url("${CI_GIT_URL}")
      }
      branch('development')
      extensions {
        relativeTargetDir('scripts')
      }
    }
    git {
      remote {
        url("${SOE_GIT_URL}")
      }
      branch('master')
      extensions {
        relativeTargetDir('soemaster')
      }
    }
  }
  wrappers {
    preBuildCleanup()
    buildUserVars()
    environmentVariables {
      propertiesFile('scripts/PARAMETERS')
    }
  }
  steps {
    shell('''
echo "#####################################################"
echo "#            UPDATING PRODUCTION STRINGS            #"
echo "#####################################################"
/bin/bash -x ${WORKSPACE}/scripts/promote.sh ${SOE_COMMIT}
    ''')
  }
  publishers {
    downstream('Push_Kickstarts', 'SUCCESS')
    publishCloneWorkspace('**') {
      criteria('Successful')
    }
    mailer('${EMAIL_TO}', true, false)
  }
}


/****************************************************************************
 * Push Kickstarts
 ****************************************************************************/
freeStyleJob('SOE/Production/Push_Kickstarts') {
  description('Push Kickstarts to Satellite 6')
  displayName('2. Deploy kickstart files to Satellite')
  blockOnDownstreamProjects()
  blockOnUpstreamProjects()
  label('master')
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
      parentJobName('SOE/Production/Promote')
      criteria('Successful')
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
    mailer('${EMAIL_TO}', true, false)
  }
}


/****************************************************************************
 * Deploy Puppet Modules
 ****************************************************************************/
freeStyleJob('SOE/Production/Deploy_Puppet_Modules') {
  description('Trigger r10k on Satellite 6 to pull required Puppet modules')
  displayName('3. Deploy SOE puppet modules to Satellite')
  blockOnDownstreamProjects()
  blockOnUpstreamProjects()
  label('master')
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
      parentJobName('SOE/Production/Promote')
      criteria('Successful')
    }
  }
  wrappers {
    preBuildCleanup()
    environmentVariables {
      propertiesFile('scripts/PARAMETERS')
      env('R10K_ENV', 'RHEL_SOE_master')
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
    groovyPostBuild {
      script('''
errpattern = ~/ERROR.*/; manager.build.logFile.eachLine{ line -> 
  errmatcher=errpattern.matcher(line) 
  if (errmatcher.find()) { 
    manager.buildFailure() 
  } 
} 
if (manager.build.result.isWorseOrEqualTo(hudson.model.Result.FAILURE)) {
  manager.listener.logger.println("Errors detected in log. Setting build result to FAILED") 
  manager.createSummary("error.gif").appendText("r10k errors detected", false, false, false, "red") 
}
      ''')
      sandbox(false)
    }
    downstream('Boot_Test_VMs', 'SUCCESS')
    mailer('${EMAIL_TO}', true, false)
  }
}


/****************************************************************************
 * Reboot Test VMs
 ****************************************************************************/
freeStyleJob('SOE/Production/Boot_Test_VMs') {
  description('Reboot all test VMs to trigger PXE build')
  displayName('4. Reboot test VMs')
  blockOnDownstreamProjects()
  blockOnUpstreamProjects()
  label('master')
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
      parentJobName('SOE/Production/Promote')
      criteria('Successful')
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
/bin/bash -x ${WORKSPACE}/scripts/buildtestvms.sh PROD
    ''')
  }
  publishers {
    mailer('${EMAIL_TO}', true, false)
  }
}



/*******************************************************************
 * Loop through each host...
 *******************************************************************/

for (desc in prodHosts.keySet()) {
  def host = prodHosts.get(desc)

  /*******************************************************************
   * Create the Build-Watch jobs for each host
   *******************************************************************/
  freeStyleJob("SOE/Production/Build_${desc}") {
    description("Monitor the build status of ${desc} - ${host}")
    displayName("5. Build ${desc} host")
    blockOnDownstreamProjects()
    blockOnUpstreamProjects()
    label('master')
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
    triggers {
      upstream('Boot_Test_VMs', 'SUCCESS')
    }
    scm {
      cloneWorkspaceSCM {
        parentJobName('SOE/Production/Promote')
        criteria('Successful')
      }
    }
    wrappers {
      preBuildCleanup()
      environmentVariables {
        propertiesFile('scripts/PARAMETERS')
      }
    }
    steps {
      shell("""
echo "#####################################################"
echo "#          WAITING FOR BUILD COMPLETION             #"
echo "#####################################################"
/bin/bash -x \${WORKSPACE}/scripts/waitforbuild.sh ${host}
      """)
    }
    publishers {
      downstream("Test_${desc}", 'SUCCESS')
    }
  }


  /*******************************************************************
   * Create the Test jobs for each host
   *******************************************************************/
  freeStyleJob("SOE/Production/Test_${desc}") {
    description("Run functional tests on ${desc} - ${host}")
    displayName("6. Test ${desc} host")
    blockOnDownstreamProjects()
    blockOnUpstreamProjects()
    label('master')
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
        parentJobName('SOE/Production/Promote')
        criteria('Successful')
      }
    }
    wrappers {
      preBuildCleanup()
      environmentVariables {
        propertiesFile('scripts/PARAMETERS')
      }
      credentialsBinding{
        usernamePassword('USERNAME', 'ROOTPASS', 'SOE_ROOT')
      }
    }
    steps {
      shell("""
echo "#####################################################"
echo "#            PUSHING TESTS to TEST VMS              #"
echo "#####################################################"
/bin/bash -x \${WORKSPACE}/scripts/pushtests.sh ${host}
      """)
    }
    publishers {
      tapPublisher {
        failIfNoResults(true)
        failedTestsMarkBuildAsFailure(true)
        planRequired(true)
        validateNumberOfTests(true)
        outputTapToConsole(true)
        enableSubtests(true)
        discardOldReports(false)
        todoIsFailure(false)
        includeCommentDiagnostics(false)
        verbose(false)
        showOnlyFailures(false)
        testResults('test_results/*.tap')
      }
    }
  }

} /* End for loop */

/*
 * Build a comma seperated string of test jobs 
 */

def joblist = ('')
for (desc in prodHosts.keySet()) {
  joblist = "${joblist}" + "Test_${desc},"
}

/*******************************************************************
 * Job that traps successful completion of all tests
 *******************************************************************/
freeStyleJob("SOE/Production/Finish") {
  description("Notify successful completion of all Dev tests. Triggers promotions.")
  displayName("7. Notify Success")
  blockOnDownstreamProjects()
  blockOnUpstreamProjects()
  label('master')
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
  configure { project ->
    project / triggers << 'org.lonkar.jobfanin.FanInReverseBuildTrigger'(plugin: 'job-fan-in@1.0.0') {
      spec()
      upstreamProjects("${joblist}")
      watchUpstreamRecursively('true')
      threshold {
        name('SUCCESS')
        ordinal('0')
        color('BLUE')
        completeBuild('true')
      }
    }
  }

  wrappers {
    preBuildCleanup()
  }
}
