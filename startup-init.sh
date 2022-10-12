#!/bin/bash -eu
set -e
#    This Source Code Form is subject to the terms of the Mozilla Public License,
#    v. 2.0. If a copy of the MPL was not distributed with this file, You can
#    obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
#    the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
#
#    Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
#    graphic logo is a trademark of OpenMRS Inc.

echo "Initiating OpenMRS startup"

# This startup script is responsible for fully preparing the OpenMRS Tomcat environment.

export OMRS_HOME="/openmrs"
OMRS_WEBAPP_NAME=${OMRS_WEBAPP_NAME:-openmrs}

# Each of these mounted directories are used to populate expected configurations on the server, defined here

OMRS_DATA_DIR="$OMRS_HOME/data"
export OMRS_MODULES_DIR="$OMRS_DATA_DIR/modules"
OMRS_OWA_DIR="$OMRS_DATA_DIR/owa"
OMRS_CONFIG_DIR="$OMRS_DATA_DIR/configuration"

OMRS_SERVER_PROPERTIES_FILE="$OMRS_HOME/$OMRS_WEBAPP_NAME-server.properties"
OMRS_RUNTIME_PROPERTIES_FILE="$OMRS_DATA_DIR/$OMRS_WEBAPP_NAME-runtime.properties"

TOMCAT_DIR="/usr/local/tomcat"
TOMCAT_WEBAPPS_DIR="$TOMCAT_DIR/webapps"
TOMCAT_WORK_DIR="$TOMCAT_DIR/work"
TOMCAT_TEMP_DIR="$TOMCAT_DIR/temp"
TOMCAT_SETENV_FILE="$TOMCAT_DIR/bin/setenv.sh"

echo "Clearing out existing directories of any previous artifacts"

rm -fR $TOMCAT_WEBAPPS_DIR/*
#rm -fR $OMRS_MODULES_DIR/*
rm -fR $OMRS_OWA_DIR/*
rm -fR $OMRS_CONFIG_DIR/*
rm -fR $TOMCAT_WORK_DIR/*
rm -fR $TOMCAT_TEMP_DIR/*

# Setup database configuration properties
OMRS_CONFIG_DATABASE="${OMRS_CONFIG_DATABASE:-mysql}"
OMRS_CONFIG_CONNECTION_SERVER="${OMRS_CONFIG_CONNECTION_SERVER:-localhost}"
OMRS_CONFIG_CONNECTION_DATABASE="${OMRS_CONFIG_CONNECTION_DATABASE:-openmrs}"
OMRS_CONFIG_CONNECTION_EXTRA_ARGS="${OMRS_CONFIG_CONNECTION_EXTRA_ARGS-}"
OMRS_CONFIG_ADD_DEMO_DATA=${OMRS_CONFIG_ADD_DEMO_DATA:-false}
OMRS_CONFIG_ADMIN_USER_PASSWORD=${OMRS_CONFIG_ADMIN_USER_PASSWORD:-Admin123}
OMRS_CONFIG_CONNECTION_PASSWORD=${OMRS_CONFIG_CONNECTION_PASSWORD:-openmrs}
OMRS_CONFIG_CONNECTION_USERNAME=${OMRS_CONFIG_CONNECTION_USERNAME:-openmrs}
OMRS_CONFIG_AUTO_UPDATE_DATABASE=${OMRS_CONFIG_AUTO_UPDATE_DATABASE:-true}
OMRS_CONFIG_CREATE_TABLES=${OMRS_CONFIG_CREATE_TABLES:-false}
OMRS_CONFIG_CREATE_DATABASE_USER=${OMRS_CONFIG_CREATE_DATABASE_USER:-false}
OMRS_CONFIG_HAS_CURRENT_OPENMRS_DATABASE=${OMRS_CONFIG_HAS_CURRENT_OPENMRS_DATABASE:-true}
OMRS_CONFIG_INSTALL_METHOD=${OMRS_CONFIG_INSTALL_METHOD:-auto}
OMRS_CONFIG_MODULE_WEB_ADMIN=${OMRS_CONFIG_MODULE_WEB_ADMIN:-true}

if [[ -z $OMRS_CONFIG_DATABASE || "$OMRS_CONFIG_DATABASE" == "mysql" ]]; then
  OMRS_CONFIG_JDBC_URL_PROTOCOL=mysql
  OMRS_CONFIG_CONNECTION_DRIVER_CLASS="${OMRS_CONFIG_CONNECTION_DRIVER_CLASS:-com.mysql.jdbc.Driver}"
  OMRS_CONFIG_CONNECTION_PORT="${OMRS_CONFIG_CONNECTION_PORT:-3306}"
  OMRS_CONFIG_CONNECTION_ARGS="${OMRS_CONFIG_CONNECTION_ARGS:-?autoReconnect=true&sessionVariables=default_storage_engine=InnoDB&useUnicode=true&characterEncoding=UTF-8}"
elif [[ "$OMRS_CONFIG_DATABASE" == "postgresql" ]]; then
  OMRS_CONFIG_JDBC_URL_PROTOCOL=postgresql
  OMRS_CONFIG_CONNECTION_DRIVER_CLASS="${OMRS_CONFIG_CONNECTION_DRIVER_CLASS:-org.postgresql.Driver}"
  OMRS_CONFIG_CONNECTION_PORT="${OMRS_CONFIG_CONNECTION_PORT:-5432}"
else
  echo "Unknown database type $OMRS_CONFIG_DATABASE. Using properties for MySQL"
  OMRS_CONFIG_JDBC_URL_PROTOCOL=mysql
  OMRS_CONFIG_CONNECTION_DRIVER_CLASS="${OMRS_CONFIG_CONNECTION_DRIVER_CLASS:-com.mysql.jdbc.Driver}"
  OMRS_CONFIG_CONNECTION_PORT="${OMRS_CONFIG_CONNECTION_PORT:-3306}"
  OMRS_CONFIG_CONNECTION_ARGS="${OMRS_CONFIG_CONNECTION_ARGS:-?autoReconnect=true&sessionVariables=default_storage_engine=InnoDB&useUnicode=true&characterEncoding=UTF-8}"
fi

# Build the JDBC URL using the above properties
OMRS_CONFIG_CONNECTION_URL="${OMRS_CONFIG_CONNECTION_URL:-jdbc:${OMRS_CONFIG_JDBC_URL_PROTOCOL}://${OMRS_CONFIG_CONNECTION_SERVER}:${OMRS_CONFIG_CONNECTION_PORT}/${OMRS_CONFIG_CONNECTION_DATABASE}${OMRS_CONFIG_CONNECTION_ARGS}${OMRS_CONFIG_CONNECTION_EXTRA_ARGS}}"

echo "Writing out $OMRS_SERVER_PROPERTIES_FILE"

cat > "$OMRS_SERVER_PROPERTIES_FILE" << EOF
add_demo_data=${OMRS_CONFIG_ADD_DEMO_DATA}
admin_user_password=${OMRS_CONFIG_ADMIN_USER_PASSWORD}
auto_update_database=${OMRS_CONFIG_AUTO_UPDATE_DATABASE}
connection.driver_class=${OMRS_CONFIG_CONNECTION_DRIVER_CLASS}
connection.username=${OMRS_CONFIG_CONNECTION_USERNAME}
connection.password=${OMRS_CONFIG_CONNECTION_PASSWORD}
connection.url=${OMRS_CONFIG_CONNECTION_URL}
create_database_user=${OMRS_CONFIG_CREATE_DATABASE_USER}
create_tables=${OMRS_CONFIG_CREATE_TABLES}
has_current_openmrs_database=${OMRS_CONFIG_HAS_CURRENT_OPENMRS_DATABASE}
install_method=${OMRS_CONFIG_INSTALL_METHOD}
module_web_admin=${OMRS_CONFIG_MODULE_WEB_ADMIN}
module.allow_web_admin=${OMRS_CONFIG_MODULE_WEB_ADMIN}
EOF

if [ -f $OMRS_RUNTIME_PROPERTIES_FILE ]; then
  echo "Found existing runtime properties file at $OMRS_RUNTIME_PROPERTIES_FILE.  Overwriting with $OMRS_SERVER_PROPERTIES_FILE"
  cp "$OMRS_SERVER_PROPERTIES_FILE" "$OMRS_RUNTIME_PROPERTIES_FILE"
fi

echo "Writing out $TOMCAT_SETENV_FILE file"

OMRS_JAVA_MEMORY_OPTS=${OMRS_JAVA_MEMORY_OPTS:-"-XX:NewSize=128m"}
OMRS_JAVA_SERVER_OPTS=${OMRS_JAVA_SERVER_OPTS:-"-Dfile.encoding=UTF-8 -server -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -Djava.awt.headlesslib=true"}

JAVA_OPTS="$OMRS_JAVA_SERVER_OPTS"
CATALINA_OPTS="$OMRS_JAVA_MEMORY_OPTS -DOPENMRS_INSTALLATION_SCRIPT=$OMRS_SERVER_PROPERTIES_FILE -DOPENMRS_APPLICATION_DATA_DIRECTORY=$OMRS_DATA_DIR/"

cat > $TOMCAT_SETENV_FILE << EOF
export JAVA_OPTS="$JAVA_OPTS"
export CATALINA_OPTS="$CATALINA_OPTS"
EOF

if [ -n "${OMRS_DEV_DEBUG_PORT-}" ]; then
  echo "Enabling debugging on port $OMRS_DEV_DEBUG_PORT"
  CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$OMRS_DEV_DEBUG_PORT"
fi

echo "Waiting for database to initialize..."

/usr/local/tomcat/wait-for-it.sh --timeout=3600 "${OMRS_CONFIG_CONNECTION_SERVER}:${OMRS_CONFIG_CONNECTION_PORT}"
