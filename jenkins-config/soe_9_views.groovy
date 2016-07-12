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
            firstJob('SOE/SOE_Checkout')
            firstJobLink('job/SOE/view/Dev%20Build%20Pipeline/job/SOE_Checkout/')
        }    
    }
}

buildPipelineView('SOE/Development/Build Pipeline') {
  title 'Dev SOE build pipeline'
  filterBuildQueue()
  displayedBuilds(3)
  alwaysAllowManualTrigger()
  triggerOnlyLatestJob(true)
  showPipelineDefinitionHeader(false)
    configure {
        it / 'gridBuilder'(class: 'au.com.centrumsystems.hudson.plugin.buildpipeline.DownstreamProjectGridBuilder') {
            firstJob('SOE/SOE_Checkout')
            firstJobLink('job/SOE/view/Dev%20Build%20Pipeline/job/SOE_Checkout/')
        }    
    }
}

