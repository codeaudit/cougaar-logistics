@echo off
REM This is a really dumb file that distributes the 
REM transportPlaybook file to all transport agents

mkdir tempTransport

copy transportPlaybook.txt tempTransport/CONUSGround-plays.txt
copy transportPlaybook.txt tempTransport/GlobalAir-plays.txt
copy transportPlaybook.txt tempTransport/GlobalSea-plays.txt
copy transportPlaybook.txt tempTransport/PlanePacker-plays.txt
copy transportPlaybook.txt tempTransport/ShipPacker-plays.txt
copy transportPlaybook.txt tempTransport/TRANSCOM-plays.txt
copy transportPlaybook.txt tempTransport/TheaterGround-plays.txt

