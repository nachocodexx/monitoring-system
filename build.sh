readonly TAG=$1
readonly DOCKER_IMAGE=nachocode/monitoring
sbt assembly && docker build -t "$DOCKER_IMAGE:$TAG" .
