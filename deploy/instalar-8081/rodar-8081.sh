#!/usr/bin/env bash
cd "$(dirname "$0")/app"
java -jar espetinho-app.jar --server.port=8081
