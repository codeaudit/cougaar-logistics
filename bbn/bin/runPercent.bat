@echo OFF

if [%5] == [] (
  echo Usage: runPercent.bat [Host] [database name] [DB Username] [DB Password] [runid]
  GOTO L_END
) 

REM example usage : ./runPercent.bat localhost grabber root "" 221

echo java -classpath ".;%COUGAAR_INSTALL_PATH%/sys/mm-mysql-2.jar" Percent %1 %2 %3 %4 %5
java -classpath ".;%COUGAAR_INSTALL_PATH%/sys/mm-mysql-2.jar" Percent %1 %2 %3 %4 %5

:L_END
