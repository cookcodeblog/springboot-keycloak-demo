#!/bin/bash

docker run --name keycloak \
  -d \
  -p 8180:8080 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  quay.io/keycloak/keycloak:12.0.1

# check logs
# docker logs -f keycloak
