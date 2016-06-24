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
  job("SOE1/Test ${desc} - ${host}") {
    steps {
      shell("echo Test ${desc} - ${host}")
    }
  }
}


