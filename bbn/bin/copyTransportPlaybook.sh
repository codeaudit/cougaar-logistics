#This is a really dumb file that distributes the 
#transportPlaybook file to all transport agents
echo "This script works even though it complains about white space..."

mkdir tempTransport;

cp transportPlaybook.txt tempTransport/CONUSGround-plays.txt;
cp transportPlaybook.txt tempTransport/GlobalAir-plays.txt;
cp transportPlaybook.txt tempTransport/GlobalSea-plays.txt;
cp transportPlaybook.txt tempTransport/PlanePacker-plays.txt;
cp transportPlaybook.txt tempTransport/ShipPacker-plays.txt;
cp transportPlaybook.txt tempTransport/TRANSCOM-plays.txt;
cp transportPlaybook.txt tempTransport/TheaterGround-plays.txt;