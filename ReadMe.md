# ActiveMQ Blueprint

This is a fork of the activemq docker container provided in the [Fabric8 v3 quickstarts](https://github.com/fabric8io/quickstarts/tree/master/apps/fabric8-mq). This fork has been created to provide a demo project to try out various approaches to docker container definition and development lifecycles. Note that this isn't a true git fork because i've copied a sub module from source to create this standalone project. I've also messed with POM quite a bit.

The major difference between this and the fabric8 version is the use of XML files to configure activemq. This will be provided as a pull request to Fabric8 at some point, see https://github.com/fabric8io/fabric8/issues/3478.

## Pre-Requisites

Before running or building you will need to be running docker, and your DOCKER_HOST environment variable will need to be pointing at that install (e.g. if you run 'docker ps', you should get a response). The easiest way to do this on Windows and Mac is to install and run [boot2docker](http://boot2docker.io/).

## Running

There's a pre-built version of this project on bintray here - https://bintray.com/davidatkins/registry/blueprint%3Aactivemq/1.0.0/view. To run it, execute:

    docker run -Pit davidatkins-docker-registry.bintray.io/blueprint/activemq:1.0.0
  
You should see the output of starting the broker in your current window. Open a new window and type

    docker ps
    
This should confirm that amq is running, and two ports have been forwarded. 

    CONTAINER ID        IMAGE                                                             COMMAND             CREATED             STATUS              PORTS                                              NAMES
    04f290017c10        davidatkins-docker-registry.bintray.io/blueprint/activemq:1.0.0   "/fabric8/run.sh"   3 minutes ago       Up 3 minutes        0.0.0.0:49153->8778/tcp, 0.0.0.0:49154->6162/tcp   happy_lumiere       

6162 is the activemq port. 8778 is a jolokia (JMX over http) port. In this case they've been forwarded to the docker host on ports 49154 and 49153. You can then access these ports from your host machine e.g. the following should return some html:

    curl $(boot2docker ip 2>/dev/null):49153

## Verifying Broker

We can test that activemq server is running by either firing up an activemq consumer, or using Hawtio to connect to jmx over http and take a look.

We'll use Hawtio. Run the following command to download and run a seperate docker container that hosts hawtio. 

    docker run -p 9282:8080 -it fabric8/hawtio

You may see some errors, but this didn't seem to be a problem for my instance. Now connect to the Hawtio web interface using the boot2docker ip and the port forwarded from 8080 on the hawtio docker image. For example, mine was:

    http://192.168.59.103:9282/hawtio

You can now connect to your activemq image's jolkia endpoint by choosing connection and using the following options:

    name: amq
    host: boot2docker ip
    port: whatever port docker maps 8778 to

A new window should pop up and you should see an activemq tab, confirming amq is running

# Building

## Manually

Build using the following

    mvn install docker:build

'mvn install' compiles the code and packages it into a jar. 'docker:build' uses the [maven jolokia plugin](https://github.com/rhuss/docker-maven-plugin) to create a docker image, and copy the project jar and all its dependencies into that image. This uses the fabric8/java docker image as a starting point. Given a class name via the 'MAIN' ENV VAR, this image will automatically run the java class on startup. In our case, this class is io.fabric8.mq.MainUsingSpring.

The image is now in the local docker repository (the one hosted by boot2docker). You can see this by running 

    docker images

## Publishing

We can now publish to a remote docker repository (e.g. bintray). To do this first configure your credentials in '~/.dockercfg' as [described here by bintray](https://bintray.com/docs/usermanual/docker/docker_workingwithdocker.html). This file can be defined on your local machine.

Now run this:

    # before pushing, you need to tag the image with the the target repository name
    docker tag -f blueprint/activemq davidatkins-docker-registry.bintray.io/blueprint/activemq:1.0.0
    # this will push to bin tray
    docker push davidatkins-docker-registry.bintray.io/blueprint/activemq:1.0.0

Of course if pushing to your own bintray, you'll need to change the repository path in your tag and push command.

FYI - this could all be done using the maven plugin. The plugin supports push OOTB. Instead of tagging after build, the config can be changed to build the image with the repository tag in the first place

## Jenkins

All of the above can be done via the bundled Jenkins build job. Easiest approach is to install an instance of the (Jenkins Blueprint)[http://github.com/davidatkins/blueprint-jenkins], read the notes on github to setup git and credentials for your target registry, and then install the jenkins job in this project using:

 cat jenkins-job.xml | curl -L -X POST -H "Content-Type: application/xml" -H "Expect: " --data-binary @- $JENKINS_ENDPOINT/createItem?name=blueprint-activemq

Fixing the Jenkins endpoint, of course.