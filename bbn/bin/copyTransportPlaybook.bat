@echo off

REM "<copyright>"
REM " Copyright 2003 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)."
REM ""
REM " This program is free software; you can redistribute it and/or modify"
REM " it under the terms of the Cougaar Open Source License as published by"
REM " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
REM " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
REM " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
REM " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
REM " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
REM " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
REM " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
REM " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
REM " PERFORMANCE OF THE COUGAAR SOFTWARE."
REM "</copyright>"

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

