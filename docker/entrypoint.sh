#!/bin/sh

if [ -z "${PREFIX}" ]; then
  echo "deprecation warning: `PREFIX` environment variable should be specified explicitly"
  export PREFIX="/enrichmentapi"
fi

if [ -d "webapps" ]; then
  echo "Cleaning up previous webapps..."
  rm -r webapps
fi
mkdir -p webapps

echo "Mounting enrichmentapi on ${PREFIX}..."

# link enrichmentapi into webapps directory
ln -s /enrichmentapi.war webapps${PREFIX}.war

exec "$@"
