set -e

task="$1"

#Create local properties to fin Android SDK
if [ ! -e "local.properties" ]
then
        echo sdk.dir=$ANDROID_HOME >> local.properties
fi

#Ensure gradle wraper is properly set
gradle wrapper

#Perform all actions
if [ "$task" == "-f" ]; then
        ./gradlew build
        ./gradlew  test
        ./gradlew ZipBundleRelease
        exit 0
fi

#Build project
if [ "$task" == "-b" ]; then
        ./gradlew build
        exit 0
fi

#Run unit tests
if [ "$task" == "-t" ]; then
        ./gradlew  test
        exit 0
fi

#Create zip file with binary and doc
if [ "$task" == "-d" ]; then
        ./gradlew ZipBundleRelease
        exit 0
fi

echo Invalid parameters, please use ‘-b’ to build, ‘-t’ to run tests, ‘-d’ to create zip file with binary and doc or ‘-f’ to perform all actions.
exit 1
