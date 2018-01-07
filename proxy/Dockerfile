FROM gradle:jdk8

ENV GRADLE_USER_HOME=~/.gradle
COPY . /usr/src/proxy
WORKDIR /usr/src/proxy
RUN gradle installDist

WORKDIR /usr/src/proxy/build/install/proxy
EXPOSE 8080 8080
CMD ["./proxy", "-i", "0.0.0.0", "-p", "8080", "-b", "50", "-t", "100"]
