/****************************************************************************
 * Create Views
 ****************************************************************************/
buildPipelineView('SOE1/Dev Build Pipeline') {
  title 'Dev SOE build pipeline'
  filterBuildQueue()
  displayedBuilds(1)
  alwaysAllowManualTrigger()
  selectedJob 'job/SOE1/view/DEV Build Pipeline/job/Development/job/GIT_Checkout/'
}

