
def ci-giturl = 'https://github.com/ggatward/soe-ci-pipeline'
def soe-giturl = 'https://github.com/ggatward/RHEL-SOE'

job("SOE seed job") {
  scm {
    git {
      remote {
        url(ci-giturl)
      }
      branch('development')
      shallowClone(true)
    }
  }
  steps {
    dsl {
      external('soe-*.groovy')
      removeAction('DISABLE')
      removeViewAction('IGNORE')
    }
  }
}
