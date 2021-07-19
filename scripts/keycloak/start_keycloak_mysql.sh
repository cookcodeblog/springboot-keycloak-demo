#!/bin/bash

# No volume, only for test!!!

# Create a user defined network
docker network create keycloak-network

# Start a MySQL instance
docker run --name keycloak-mysql \
  -d \
  --net keycloak-network \
  -e MYSQL_DATABASE=keycloak \
  -e MYSQL_USER=keycloak \
  -e MYSQL_PASSWORD=keycloak123 \
  -e MYSQL_ROOT_PASSWORD=keycloak123 \
  mysql:8.0

# Start a Keycloak instance
docker run --name keycloak \
  -d \
  --net keycloak-network \
  -p 8180:8080 \
  -e DB_VENDOR=mysql \
  -e DB_ADDR=keycloak-mysql \
  -e DB_DATABASE=keycloak \
  -e DB_USER=keycloak \
  -e DB_PASSWORD=keycloak123 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  quay.io/keycloak/keycloak:12.0.1

# check logs
# docker logs -f keycloak