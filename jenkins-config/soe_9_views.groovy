/****************************************************************************
 * Create Views
 ****************************************************************************/
buildPipelineView('SOE/Dev Build Pipeline') {
  title 'Dev SOE build pipeline'
  filterBuildQueue()
  displayedBuilds(1)
  alwaysAllowManualTrigger()
  triggerOnlyLatestJob(true) 
  showPipelineDefinitionHeader(false)
    configure { 
        it / 'gridBuilder'(class: 'au.com.centrumsystems.hudson.plugin.buildpipeline.DownstreamProjectGridBuilder') {
            firstJob('SOE/Development/GIT_Checkout')
            firstJobLink('job/SOE/view/test/job/GIT_Checkout/')
        }    
    }
}

