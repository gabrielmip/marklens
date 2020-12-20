#/bin/bash

DIR=/tmp/marklens

mkdir -p $DIR
rm -R $DIR/*
mkdir $DIR/resources
cp resources/{help.txt,stopwords.txt} $DIR/resources
cp target/uberjar/*standalone.jar $DIR
