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

* A git repository, containing the 3rd party and in-house packages, kickstarts, puppet modules and BATS scripts. This is where development of the build takes place.
* A Jenkins instance. This is responsible for building artifacts such as RPMs and Puppet modules, Pushing artifacts into the Red Hat Satellite, and orchestrating and reporting tests.
* Red Hat Satellite 6. This acts as the repository for Red Hat-provided and 3rd party packages, kickstarts and puppet modules. Satellite is also used to deploy test clients.
* A virtualisation infrastructure to run test clients. I have used KVM/Libvirt, VMware and RHEV.


## Credit
This is a rehash of the excellent framework created by Nick Strugnell and the RedHatEMEA team.
Their original CI code can be found at https://github.com/RedHatEMEA/soe-ci

## Setup
The following steps should help you get started with CII.

### Jenkins Server

NB I have SELinux disabled on the Jenkins server as I ran into too many problems with it enabled and didn't have the time to fix them.

#### Installation

* From Satellite 6, provision a RHEL 7 server with a minimum of 4GB RAM and 50GB availabile in `/var/lib/jenkins`. The Jenkins host can be a VM.
* Ensure that the Jenkins host is subscribed to the rhel-server-7-rpms and rhel-server-7-satellite-tools-rpms repositories.
* Subscribe the server for to the EPEL and Jenkins repositories. If these are not available on your Satellite 6 server, use the following URLs: [EPEL](https://fedoraproject.org/wiki/EPEL) and [Jenkins](http://pkg.jenkins-ci.org/redhat/)
* Install `httpd`, `mock`, `createrepo`, `git`, `nc` and `puppet` on the system. Puppet should be installed during the provisioning process from Satellite 6, but if it is missing, install it here.
`yum install -y httpd mock createrepo git nc puppet`
* Ensure that httpd is set to start automatically
`systemctl enable httpd; systemctl start httpd`
* Configure `mock` by copying the [rhel-7-x86_64.cfg](https://github.com/ggatward/soe-ci-pipeline/master/rhel-7-x86_64.cfg) file to `/etc/mock` on the jenkins server, and linking ensuring that the link `/etc/mock/default.cfg` points to it.
    * edit the file and replace the placeholder `YOUROWNKEY` with your key as found in the `/etc/yum.repos.d/redhat.repo` file on the Jenkins server.
    * make sure the baseurl points at your Satellite server. The easiest way to do this is to just copy the relevant repo blocks from the Jenkins server's `/etc/yum.repos.d/redhat.repo`
    * if your Jenkins server is able to access the Red Hat CDN, then you can leave the baseurls pointing at
      `https://cdn.redhat.com`





* Install `jenkins`, `tomcat` and Java. If you have setup the Jenkins repo correctly you should be able to simply use yum.
* Start Jenkins and browse to the console at http://jenkinsserver:8080/
* Select the 'Manage Jenkins' link, followed by 'Manage Plugins'. You will need to add the following plugins:
    * Git Plugin
    * Multiple SCMs Plugin
    * TAP Plugin
    * Post-Build Script Plug-in
    * Build Timeout Plug-in
* Restart Jenkins
* Add the `jenkins` user to the `mock` group (`usermod -a -G mock jenkins`). This will allow Jenkins to build RPMs.
* Create `/var/www/html/pub/soe-repo` and `/var/www/html/pub/soe-puppet` and assign their ownership to the `jenkins` user. These will be used as the upstream repositories to publish artefacts to the satellite.
* `su` to the `jenkins` user (`su jenkins -s /bin/bash`) and use `ssh-keygen` to create an ssh keypair. These will be used for authentication to both the git repository, and to the satellite server.
* Create a build plan in Jenkins by creating the directory `/var/lib/jenkins/jobs/SOE` and copying in the  [config.xml] file
* Check that the build plan is visible and correct via the Jenkins UI, you will surely need to adapt the parameter values to your environment.
    * you might need to reload the configuration from disk using 'Manage Jenkins -> Reload Configuration from Disk'.
