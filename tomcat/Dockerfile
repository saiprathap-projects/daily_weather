FROM tomcat:9.0

# Clean default apps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy your WAR file
COPY ../target/*.war /usr/local/tomcat/webapps/ROOT.war
