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

NB I have SELinux disabled on the Jenkins server as I ran into too many problems with it enabled and didn't have the time to fix them.

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

    * Build Timeout Plug-in 		(Default plugin)
    * Credentials Binding Plugin	(Default plugin)
    * Git Plugin			(Default plugin)
    * Workspace Cleanup Plugin  	(Default plugin)
    * Job DSL Plugin
    * Environment Injector Plugin
    * Build Pipeline plugin
    * Clone Workspace SCM Plug-in
    * Multiple SCMs Plugin
    * TAP Plugin
    * Post-Build Script Plug-in
    * JobFanIn Plugin
    * Version Number Plug-in

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

* The following manual configuration is needed as a workaround for the Promoted plugin as the actions cannot be scripted via JobDSL yet:




### Git Repository
* Clone the following two git repos:
    * https://github.com/ggatward/soe-ci-pipeline   These are the scripts used to by Jenkins to drive CII
    * https://github.com/ggatward/RHEL-SOE          This is a demo SOE environment
* Push these to a private git remote (or fork to a development branch on github).
_Make sure to create a development branch of the RHEL-SOE and use that in Jenkins - NOT the master branch_
* Create a `jenkins` user with access to the SOE git repositories, and add the public SSH key for the `jenkins` user from the Jenkins server created earlier. This will allow Jenkins to merge and promote the SOE branches as part of the CI flow.
* Configure the SOE_Bootstrap job, and set the `CI_GIT_URL` and `SOE_GIT_URL` String Parameters to reflect the location of the Git repositories for the CI scripts and SOE artefacts respectively, then save the job.




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
* Configure a Compute Resource on the satellite (libvirt, VMWare or RHEV). This will be used to deploy test machines.

* Create a Lifecycle path for the SOE (Library -> SOE Test -> SOE Production)
* Create a Content View called `Server SOE` that contains all required repositories for both RHEL 6 and RHEL 7, and publish this to the Library and the SOE Test environment. Be sure to include any custom repositories.
* Create an Activation Key for both RHEL 6 and RHEL 7, using the Content View from `SOE Test`
* Create a hostgroup (I called mine 'Jenkins Test Servers') that deploys machines on to the Compute Resource that you configured earlier, and uses the activation key that you created. Create a default root password and make a note of it.
* Manually provision the test servers on your defined Compute Resource and deploy them. 
* Ensure that the test servers are configured to boot from network BEFORE boot from local hard drive in thier BIOS configurtion. This is to ensure that when Jenkins triggers Satellite 6 to rebuild the test hosts, they will perform a PXE installation.

These CII scripts use r10k to deploy puppet environments to the Satellite server. Further information on the configuration of r10k and Satellite 6 can be found at https://access.redhat.com/blogs/1169563/posts/2216351
* Install the r10k rubygem on the Satellite 6 server: `gem install r10k`


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

* ....Configure test machine hostnames....
* Configure test hosts in the soe_2_dev.groovy DSL job file

### Getting Started
At this point, you should be good to go. In fact Jenkins may have already kicked off a build for you when you pushed to github.


* Run the SOE_Bootstrap job. This should result in a new job folder named SOE, containing a `Development` and a `Production` project. Each project contains a number of smaller jobs that are chained together to form a pipeline - so we have a SOE project containing a Development pipeline and a Production pipeline.



Develop your build in your DEVELOPMENT branch checkout of RHEL-SOE. 


Make sure that all Test VM's are configured to boot from the network before local HDD. This will ensure that they re-install via PXEboot when triggered to do so by Satellite 6.


