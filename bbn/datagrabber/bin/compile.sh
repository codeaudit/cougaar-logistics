#!/bin/sh

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


TMPFILE1=/tmp/_cmp1_`date +_%Y_%m_%d_%H_%M_%S`
TMPFILE2=/tmp/_cmp2_`date +_%Y_%m_%d_%H_%M_%S`
MARKER=_MARKER_`date +_%Y_%m_%d_%H_%M_%S`

TARGET=$HOME/TOPS/src
if [ $# -ge 1 ]; then
    TARGET=$1
fi

cd $TARGET
find . -name "*.class" -exec rm {} \;
find . -name "*.java" > $TMPFILE1
sed "s/\(.*\)\/.*java/\1/" $TMPFILE1 > $TMPFILE2
rm $TMPFILE1

I=1
LENGTH=`wc -l $TMPFILE2 | awk '{print $1}'`
NEXTDIR=`tail +$I $TMPFILE2 | head -1`
while [ "$I" -le `expr $LENGTH` ]; do
    cd $NEXTDIR
    if [ ! -f $MARKER ]; then
	echo -e "COMPILING: "`pwd`" ... \c" 
        javac -J-mx100M *.java
        echo "Left by compile.sh. Please delete. (JLH)" > $MARKER
        echo DONE
    fi
    I=`expr $I + 1`
    NEXTDIR=`tail +$I $TMPFILE2 | head -1`
    cd $TARGET
done

cd $TARGET
rm $TMPFILE2
find . -name $MARKER -exec rm {} \;

