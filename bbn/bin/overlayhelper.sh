#!/bin/bash

zipname=$1
zipbase=$2
module=$3
cd ../
cp -r data/csmart ${zipbase}/${module}/
cd ${zipbase}/${module}
zip -r ../../${zipname} .
cd ../../
cd bin/
