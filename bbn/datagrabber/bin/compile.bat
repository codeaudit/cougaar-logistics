
COMPROOT=$1

if [ -z $COMPROOT ]
then
  COMPROOT=`pwd`
fi

echo Compiling everything below $COMPROOT

for i in `fnd $COMPROOT -type d -a \! -name '*CVS' `
do
  DDIR=`echo $i | sed -f ./slashify.sed`
  DDIR=d:${DDIR##\\cygdrive\\d}
  echo Compiling in $DDIR
  javac "$DDIR\\*.java"
done
