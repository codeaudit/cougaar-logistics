#! /bin/sh

# <copyright>
#  Copyright 2001 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
# 
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the Cougaar Open Source License as published by
#  DARPA on the Cougaar Open Source Website (www.cougaar.org).
# 
#  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
#  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
#  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
#  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
#  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
#  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
#  PERFORMANCE OF THE COUGAAR SOFTWARE.
# </copyright>


if [ $# -lt 1 ]; then
    echo "Needs jar name."
    exit
fi

UNJAR_DIR=tmp_`date +%m%d%H%M%S`
JARFILE=$1

mkdir $UNJAR_DIR
mv $JARFILE $UNJAR_DIR
cd $UNJAR_DIR
echo "Unjarring $JARFILE..."
jar xf $JARFILE
rm -rf META-INF
rm -rf $JARFILE
echo "Rejarring $JARFILE..."
jar cf $JARFILE *
mv $JARFILE ..
cd ..
echo "Complete. Cleaning up..."
rm -rf $UNJAR_DIR
