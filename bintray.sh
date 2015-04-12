#!/usr/bin/env bash
docker tag davidatkins/activemq davidatkins-docker-registry.bintray.io/blueprint/activemq:1.0.0
docker push davidatkins-docker-registry.bintray.io/davidatkins/activemq:1.0.0