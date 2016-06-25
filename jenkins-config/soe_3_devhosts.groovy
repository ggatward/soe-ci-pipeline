/*******************************************************************
 * Define all test hosts here. 
 * Format is 'Description':'hostname'
 *******************************************************************/
def hostMap = [
  'Net1 RHEL7':'buildbot1.lab.home.gatwards.org', 
  'Net2 RHEL6':'buildbot2.lab.home.gatwards.org',
]


/*******************************************************************
 * Create the host-build jobs for each host
 *******************************************************************/

for (desc in hostMap.keySet()) {
  def host = hostMap.get(desc)

  freeStyleJob("SOE1/Development/Build_${desc}") {
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
      shell("""
echo "#####################################################"
echo "#            PUSHING TESTS to TEST VMS              #"
echo "#####################################################"
/bin/bash -x \${WORKSPACE}/scripts/waitforbuild.sh ${host}
      """)
    }
    publishers {
      downstream("Test_${desc}", 'SUCCESS')
      mailer('$EMAIL_TO', true, false)
    }
  }




} /* End for loop */


