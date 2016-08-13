Continuous Integration Pipeline Scripts for Satellite 6
=======================================================
* Author: Geoff Gatward  
* Email: <ggatward@redhat.com>
* Date: 2016-06-06
* Revision: 0.1


## Introduction
Continuous Integration for Infrastructure (CII) is a process by which the Operating System build ("build") component of a Standard Operating Environment (SOE) can be rapidly developed, tested and deployed.

A build is composed of the following components:

* Red Hat provided base packages
* 3rd party and in-house developed packages
* Deployment instructions in the form of kickstart templates
* Configuration instructions in the form of puppet modules
* Test instructions in the form of BATS scripts

The CII system consists of the following components:

* A git repository, containing the CI scripts to drive Jenkins.
* A git repository, containing the kickstarts and BATS scripts.
* Optionally, one or more git repositories, containing custom (in-house) puppet modules.
* A yum repository, containing custom (in-house) RPM packages.
* A Jenkins instance. This is responsible for pushing artifacts into the Red Hat Satellite, and orchestrating and reporting tests.
* The Jenkins instance can also be responsible for building artifacts such as RPMs and Puppet modules, and pushing those artifacts to the relevent repositories (These builds are not covered by this project).
* Red Hat Satellite 6. This acts as the repository for Red Hat-provided and 3rd party packages, kickstarts and puppet modules. Satellite is also used to deploy test clients.
* A virtualisation infrastructure to run test clients. KVM/Libvirt, VMware and RHEV have all been used.
* Optionally, A puppet-forge mirror. This provides access to puppet-forge modules if the environment is disconnected from the internet. In a disconnected environment, consider https://github.com/unibet/puppet-forge-server


## Credit
This is a re-hash of the framework created by Nick Strugnell and the RedHatEMEA team.
Their original CI code can be found at https://github.com/RedHatEMEA/soe-ci

## Setup
The following steps should help you get started with CII.
This set of scripts and the instructions below build a RHEL 6 Server and RHEL 7 Server SOE in parallel using the same artifacts for both releases. If you only require one or the other, that should also work.

### Jenkins Server

* From Satellite 6, provision a RHEL 7 server with a minimum of 4GB RAM and 50GB availabile in `/var/lib/jenkins`. The Jenkins host can be a VM.
* Ensure that the Jenkins host is subscribed to the rhel-server-7-rpms and rhel-server-7-satellite-tools-6.1-rpms repositories.
* Subscribe the server to the EPEL and Jenkins repositories. If these are not available on your Satellite 6 server, use the following URLs:
EPEL https://fedoraproject.org/wiki/EPEL and Jenkins http://pkg.jenkins-ci.org/redhat/
* Install `git`, `nc` and `java-1.8.0-openjdk` on the system. 
* Install `jenkins`. If you have setup the Jenkins repo correctly you should be able to simply use yum.
* Open the firewall for port 8080: `firewall-cmd --permanent --add-port 8080/tcp && firewall-cmd --reload`
* Start Jenkins and browse to the console at http://jenkinsserver:8080/
* You will be prompted to run through the initial setup wizard. Allow Jenkins to install the default plugins.
* Select the 'Manage Jenkins' link, followed by 'Manage Plugins'. You will need to add the following additional plugins:

    * Build Timeout Plugin 		(Default plugin)
    * Credentials Binding Plugin	(Default plugin)
    * Git Plugin			(Default plugin)
    * Workspace Cleanup Plugin  	(Default plugin)
    * Job DSL Plugin
    * Environment Injector Plugin
    * Build Pipeline plugin
    * Clone Workspace SCM Plugin
    * Multiple SCMs Plugin
    * TAP Plugin
    * Post-Build Script Plugin
    * JobFanIn Plugin
    * Version Number Plugin
    * Promoted Builds Plugin
    * Groovy Postbuild Plugin

* To freshen up the Jenkins interface a little, I also recommend the following plugins also be installed:
  
    * Green Balls Plugin
    * Modern Status Plugin
    * Project Build Times Plugin
    * Embeddable Build Status Plugin

* Restart Jenkins
* `su` to the `jenkins` user (`su - jenkins -s /bin/bash`) and use `ssh-keygen` to create an ssh keypair. These will be used for authentication to both the git repository, and to the satellite server.
* In the Jenkins UI, navigate to `Credentials -> System -> Add domain`. Create a new domain called `RHEL Server SOE` and add a `Username with password` parameter with username = root, password = <whatever>, and ID = SOE_ROOT
This is the root password used by Jenkins to access the build test hosts, and we are setting it in the Jenkins credential store to ensure that it is not visible in any of the build jobs that are to be created.
* Create the project build plans in Jenkins by making a directory `/var/lib/jenkins/jobs/SOE_Bootstrap` and copying the `config.xml` file from the `jenkins-config` directory of the CI Git repo to it.
* Ensure that `jenkins` is the owner of the new job: `chown -R jenkins:jenkins /var/lib/jenkins/jobs` 
* Check that the SOE_Bootstrap job is visible and correct via the Jenkins UI (We will need to edit parameters shortly).
    * you might need to reload the configuration from disk using 'Manage Jenkins -> Reload Configuration from Disk'.





### Git Repository
* Clone the following two git repos:
    * https://github.com/ggatward/soe-ci-pipeline   These are the scripts used to by Jenkins to drive CII
    * https://github.com/ggatward/RHEL-SOE          This is a demo SOE environment
* Push these to a private git remote (or fork to a development branch on github).
_Make sure to create a development branch of the RHEL-SOE and use that in Jenkins - NOT the master branch_
* Create a `jenkins` user with access to the SOE git repositories, and add the public SSH key for the `jenkins` user from the Jenkins server created earlier. This will allow Jenkins to merge and promote the SOE branches as part of the CI flow.




### Satellite 6

* Install and register Red Hat Satellite 6 as per the [instructions](https://access.redhat.com/documentation/en-US/Red_Hat_Satellite/6.1/html/Installation_Guide/index.html).
* Enable the following Red Hat repos for both RHEL 6 and RHEL 7:
  - Server Kickstart,
  - Server RPMs,
  - Extra RPMs,
  - Optional RPMs,
  - Common RPMs Server
  - Satellite 6.1 Tools
* Create a sync plan that does a daily sync of the RHEL product and perform an initial sync.
* Create a `jenkins` user on the satellite (both OS and Application)
* Configure hammer for passwordless usage by creating a `~jenkins/.hammer/cli_config.yml` file. [More details here](http://blog.theforeman.org/2013/11/hammer-cli-for-foreman-part-i-setup.html).
* Copy over the public key of the `jenkins` user on the Jenkins server to the `jenkins` user on the satellite and ensure that `jenkins` on the Jenkins server can do passwordless `ssh` to the satellite.
* Create an SSH key for the jenkins user on the Satellite server. This will be needed to allow the user to copy puppet module artefacts to the Satellite capsules.
* Configure a Compute Resource on the satellite (libvirt, VMWare or RHEV). This will be used to deploy test machines.

* Create a Lifecycle path for the SOE (Library -> SOE Test -> SOE Production)
* Create a Content View called `Server SOE` that contains all required repositories for both RHEL 6 and RHEL 7, and publish this to the Library and the SOE Test environment. Be sure to include any custom repositories.
* Create an Activation Key for both RHEL 6 and RHEL 7, using the Content View from `SOE Test`
* Create a hostgroup (I called mine 'Jenkins Test Servers') that deploys machines on to the Compute Resource that you configured earlier, and uses the activation key that you created. Create a default root password and make a note of it.
* Manually provision the test servers on your defined Compute Resource and deploy them. 
* Ensure that the test servers are configured to boot from network BEFORE boot from local hard drive in thier BIOS configurtion. This is to ensure that when Jenkins triggers Satellite 6 to rebuild the test hosts, they will perform a PXE installation.

These CII scripts use r10k to deploy puppet environments to the Satellite server. Further information on the configuration of r10k and Satellite 6 can be found at https://access.redhat.com/blogs/1169563/posts/2216351
* Install the r10k rubygem on the Satellite 6 server: `gem install r10k`
* Create the `/etc/puppet/r10k/environments` directory on the Satellite server AND ALL CAPSULES. Set the permissions and ownership as shown, and add the jenkins user to the apache group.
```
mkdir -p /etc/puppet/r10k/environments
chmod -R 2775 /etc/puppet/r10k
chown -R apache:apache /etc/puppet/r10k
usermod -a -G apache jenkins
```
* Modify the `/etc/puppet/puppet.conf` file on the Satellite and Capsules to include the new path in the `environmentpath` parameter in the `[master]` section:  ([main] in Satellite 6.2)
```
[master]
    ...
    environmentpath = /etc/puppet/environments:/etc/puppet/r10k/environments
    ...
```
* The Satellite/Capsule requires a restart for the updated puppet configuration to take effect.

### Satellite 6 Capsules

* Create a `jenkins` user on the capsule (OS)
* Copy over the public key of the `jenkins` user on the Satellite server to the `jenkins` user on the capsule and ensure that `jenkins` on the Satellite server can do passwordless `ssh` to the capsule.


### Configuration

The PARAMETERS file contains global environment variables that are used by the CII scripts. Please set these variables as per your environment and commit the updated file to your git repository. It will be pulled from git each time the Jenkins job runs (configuration as code)

```
ORG=                   Name of your Organization, as defined in Satellite 6
SATELLITE=             FQDN of the Satellite 6 server
PUPPET_LOCATIONS=      Comma seperated list of locations to enable the SOE puppet environment
PUSH_USER=             Username used to login to Satellite 6
KNOWN_HOSTS=           Location of the PUSH_USER's known_hosts file
RSA_ID=                Location of the public SSH key to use for PUSH_USER
EMAIL_TO=              Space seperated list of email addresses to recieve notifications from Jenkins
```

* Define test hosts
When the SOE Bootstrap job is run to create the SOE project, seperate build and test jobs are dynamically created for each defined test host. There should be as many test hosts as you have seperate environments to build the SOE into.
During the build process, Jenkins will instruct Satellite 6 to set these hosts to build mode and power-cycle them.
If everything has been configured correctly, this will initiate a re-install of each host via PXE kickstart.

To configure the test hosts, locate the file `jenkins-config/soe_2_dev.groovy` within the CII scripts GIT repository.
At the top of this file is a groovy 'map' definition, defining the description and hostname of each test host.
The format must remain as shown below with no spaces in the definition, and the FQDN of each test host.

```
def devHosts = [
  'VMware_RHEL7':'buildbot1.example.org',
  'VMware_RHEL6':'buildbot2.example.org',
  'KVM_RHEL7':'buildbot3.example.org',
  'KVM_RHEL6':'buildbot4.example.org',
]
```

*Whenever hosts are added or deleted from this configuration, the SOE_Bootstrap job must be run to re-generate the Jenkins build and test job definitions.*

* Configure the SOE_Bootstrap job, and set the `CI_GIT_URL` and `SOE_GIT_URL` String Parameters to reflect the location of the Git repositories for the CI scripts and SOE artefacts respectively, then save the job.


### Getting Started

* Run the SOE_Bootstrap job. Verify the Git URLs for both the CI and SOE repositories. This should result in a new job folder named SOE, containing a `Development` and a `Production` project. Each project contains a number of smaller jobs that are chained together to form a pipeline - so we have a SOE project containing a Development pipeline and a Production pipeline.

* The following manual configuration is needed as a workaround for the Promoted plugin as the promotion actions cannot be scripted via JobDSL yet:

    * Open the configuration page for the 'Server SOE' job in the SOE folder 
    * In the 'General' section, tick the 'Promote builds when...' option
    * Configure the Promotion process as follows:
      - Name: `Promoted_to_Production`
      - Icon: Gold Star
      - Criteria: Only when manually approved (Add jenkins users who can perform the approval if required)
      - Criteria: When the following upstream promotions are promoted = `Validated_in_Dev`
      - Actions: Execute Shell
    * Add a second Promotion process and configure it as follows:
      - Name: `Validated_in_Dev`
      - Icon: Green empty star
      - Criteria: When the following downstream projects build successfully = `Development/Finish`
      - Actions: Editable Email Notification
        - Project Recipient List: As required
        - Default Subject: `Successful build and test of Server SOE`
        - Default Content: `Server SOE development build has passed and is ready for promotion to production.`
        - Advanced Settings -> Triggers:
          - Success: Send To Recipient List
    * Save the 'Server SOE' job.
    * Re-run the SOE_Bootstrap job to reset the 'manual modification' flag on the updated job.



Develop your SOE build in your DEVELOPMENT branch checkout of RHEL-SOE. Jenkins will use the development branch whilst building and testing the development SOE. The master branch will be the Production SOE, and will be managed by the Jenkins promotion workflow.


### Building the Development SOE

When you are ready to test the SOE in the DEVELOPMENT environment, simply run the 'Server SOE' job from the SOE folder. Alternatively, you can use the 'Run' button from the 'Dev SOE Pipeline' view.
The build will progress along the pipeline, with all configured test hosts being rebuilt in parallel. Once the hosts are built, they will have the test cases run against them. Only if *ALL* of the hosts test cases pass will the Dev build be marked as successful.
On a successful development build cycle, the main 'Server SOE' job will be marked with a promotion icon indicating that the dev testing was good. At this point it can be manually promoted to the Production environment if desired.


### Promoting the SOE to production

When everything has tested successfully and the 'Validated in Dev' auto-promotion status has been set on the base 'Server SOE' job, the build can be promoted to production my clicking the 'Promotion Status' link in the job, or by clicking on the promotion icon for a specific build.

The promotion process will do the following (using the stored SCM Git checkout that passed the dev testing):
* Create a version number for the SOE
* Perform a string replacement of SOE_DEV with SOE_PROD in all kickstart erb files
* Merge the updated SOE artefacts to the 'master' branch of the SOE repository
* Create a Git Tag of the version number
* Push the production artefects to Satellite 6
* Build and test any configured production sanity test VM's
* Give a final 'All OK' promotion status


