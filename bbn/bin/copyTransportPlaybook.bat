@echo off
REM This is a really dumb file that distributes the 
REM transportPlaybook file to all transport agents

copy transportPlaybook.txt ../configs/temp/CONUSGround-plays.txt
copy transportPlaybook.txt ../configs/temp/GlobalAir-plays.txt
copy transportPlaybook.txt ../configs/temp/GlobalSea-plays.txt
copy transportPlaybook.txt ../configs/temp/PlanePacker-plays.txt
copy transportPlaybook.txt ../configs/temp/ShipPacker-plays.txt
copy transportPlaybook.txt ../configs/temp/TRANSCOM-plays.txt
copy transportPlaybook.txt ../configs/temp/TheaterGround-plays.txt

