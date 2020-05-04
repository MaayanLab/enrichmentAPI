#!/bin/sh

if [ -z "${PREFIX}" ]; then
  echo "deprecation warning: `PREFIX` environment variable should be specified explicitly"
  export PREFIX="/enrichmentapi"
fi

if [ -d "webapps" ]; then
  echo "Cleaning up previous webapps..."
  rm -r webapps
fi

# sub-paths in tomcat defined with this#sub#path.war
WEBAPPS_PATH="webapps/$(echo ${PREFIX} | cut -b2- | sed 's/\//#/g').war"

mkdir -p $(dirname ${WEBAPPS_PATH})

echo "Mounting enrichmentapi on ${PREFIX}..."

# link enrichmentapi into webapps directory
ln -s /enrichmentapi.war "${WEBAPPS_PATH}"

exec "$@"
