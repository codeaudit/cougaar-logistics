#!/bin/bash

zipname=$1
zipbase=$2
module=$3
cd ../
cp -r data/csmart ${zipbase}/${module}/
cp -r data/ui ${zipbase}/${module}/
cp -r bin/ ${zipbase}/${module}/
cd ${zipbase}/${module}
zip -r ../../${zipname} .
cd ../../
cd bin/
