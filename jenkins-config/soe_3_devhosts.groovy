/*******************************************************************
 * Define all test hosts here. 
 * Format is 'Description':'hostname'
 *******************************************************************/
def hostMap = [
  'Net1_RHEL7':'buildbot1.lab.home.gatwards.org', 
  'Net1_RHEL6':'buildbot2.lab.home.gatwards.org',
]


/*******************************************************************
 * Loop through each host...
 *******************************************************************/

for (desc in hostMap.keySet()) {
  def host = hostMap.get(desc)

  /*******************************************************************
   * Create the Build-Watch jobs for each host
   *******************************************************************/
  freeStyleJob("SOE/Development/Build_${desc}") {
    description("Monitor the build status of ${desc} - ${host}")
    displayName("05: Build ${desc} host")
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
    triggers {
      upstream('Boot_Test_VMs', 'SUCCESS')
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
  freeStyleJob("SOE/Development/Test_${desc}") {
    description("Run functional tests on ${desc} - ${host}")
    displayName("06: Test ${desc} host")
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
for (desc in hostMap.keySet()) {
  joblist = "${joblist}" + "Test_${desc},"
}


/*******************************************************************
 * Create the Test jobs for each host
 *******************************************************************/
freeStyleJob("SOE/Development/Mark_Dev_Done") {
  description("Check successful completion of all Dev tests")
  displayName("07: Check Completion")
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
    shell("""
echo "#####################################################"
echo "#            SOMETHING             #"
echo "#####################################################"
      """)
  }
  publishers {
    extendedEmail {
      recipientList('geoff@gatwards.org')
      triggers {
        success {
          sendTo {
            requester()
          }
        }
      }
    }
  }
}



