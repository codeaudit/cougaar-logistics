#!/bin/sh

# <copyright>
#  
#  Copyright 2001-2004 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects
#  Agency (DARPA).
# 
#  You can redistribute this software and/or modify it under the
#  terms of the Cougaar Open Source License as published on the
#  Cougaar Open Source Website (www.cougaar.org).
# 
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
#  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
#  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#  
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

