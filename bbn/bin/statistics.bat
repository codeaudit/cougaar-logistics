@echo OFF

if [%5] == [] (
  echo Usage: statistics.bat [Host] [database name] [DB Username] [DB Password] [runid]
  GOTO L_END
) 

REM example usage : ./runPercent.bat localhost grabber root "" 221

%COUGAAR_INSTALL_PATH%\csmart\data\database\scripts\sed.exe "s/XXX/%5/g" people.sql > peoplerunid.sql

REM echo "mysql -h%1 -u%3 -p%4 %2 < peoplerunid.sql"
REM echo "pax per unit"
mysql -h%1 -u%3 -p%4 %2 < peoplerunid.sql

DEL peoplerunid.sql

%COUGAAR_INSTALL_PATH%\csmart\data\database\scripts\sed.exe "s/XXX/%5/g" items.sql > itemsrunid.sql

REM echo "mysql -h%1 -u%3 -p%4 %2 < itemsrunid.sql"
REM echo "items per unit"
mysql -h%1 -u%3 -p%4 %2 < itemsrunid.sql

DEL itemsrunid.sql

%COUGAAR_INSTALL_PATH%\csmart\data\database\scripts\sed.exe "s/XXX/%5/g" tons.sql > tonsrunid.sql

REM echo "mysql -h%1 -u%3 -p%4 %2 < tonsrunid.sql"
REM echo "tons per unit"
mysql -h%1 -u%3 -p%4 %2 < tonsrunid.sql

DEL tonsrunid.sql

:L_END
