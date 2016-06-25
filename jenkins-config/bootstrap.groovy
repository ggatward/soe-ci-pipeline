
def ci_giturl = 'https://github.com/ggatward/soe-ci-pipeline'
def soe_giturl = 'https://github.com/ggatward/RHEL-SOE'

job("SOE seed job") {
  scm {
    git {
      remote {
        url(ci_giturl)
      }
      branch('development')
      shallowClone(true)
    }
  }
  steps {
    dsl {
      external('jenkins-config/soe_*.groovy')
      removeAction('DELETE')
      removeViewAction('IGNORE')
    }
  }
}
