#
# <copyright>
#  
#  Copyright 1997-2004 BBNT Solutions, LLC
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
#

# This script generates an xml test file to test the clusters. 


#!opt/bin/perl
$taskNum=1;
$id=1;
$white=0;
$promptIndent=0;

#default stuff
$in="";
$verb = "Transport";
$ichoice = "y";
$item="TOPS_TANK";
$itemNum=4;
$choiceM="a";
$choiceMore="n";
$code="XYZF";
$Lat="45.0";
$Long="45.0";
$yr="2000";
$mth="6";
$day="1";
$hr="12";
$min="00";
$sec="00";
$role = "Transit";
$moreI = "y";
$oName="foo";
$wChoice="y";
$wItem="C5";

$moreTasks = "y";

# get file name
print ("Default values are in brackets. []\n");

print ("Output File name? [",$oName,"] ");
chop($in = <STDIN>);
if ($in ne "") {
  $oName = $in;
}
open(OUT,  ">".$oName);


print OUT (qq(<?xml version="1.0"?>\n\n));
print OUT (qq(<!DOCTYPE tasklist SYSTEM "ClusterInput.dat.dtd" []>\n\n));

print OUT ("<tasklist>\n");
incIndent();

while ($moreTasks eq "y") {
  indent();
  print OUT (qq(<task id="task-), $taskNum, qq(">\n));
  incIndent();

  # verb
  pIndent();
  print ("Verb (i.e. Transport)? [",$verb,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $verb = $in;
  }
  indent();
  print OUT ("<verb>",$verb,"</verb>\n");

  # direct object
  indent();
  print OUT ("<directobject>\n");
  
  incIndent();
  incPIndent();
  fillSingleGroup();
  decPIndent();
  decIndent();

  indent();
  print OUT ("</directobject>\n");


  ## with
  pIndent();
  print ("Create with preposition (y/n)? [",$wChoice,"] ");
  $valid = 0;
  while ($valid == 0) {
    chop ($in = <STDIN>);
    if ($in ne "") {
      $wChoice = $in;
    }
    if (($wChoice eq "y") || ($wChoice eq "n")) {
      $valid = 1;
    }
    if ($valid == 0) {
      pIndent();
      print ("Invalid, try again.\n");
    }
  }
  
  if ($wChoice eq "y") {
    
    pIndent();
    print ("With preposition creation:\n");
    incPIndent();

    # do the with fill in
    indent();
    print OUT ("<with>\n");
    
    incIndent();
    fillWith();
    decIndent();
    
    indent();
    print OUT ("</with>\n");
    
    decPIndent();
  }


  ## end with
  
  #### Itinerary ###
  # at this point the itinerary is optional.
  
  pIndent();
  print ("Create itinerary (y/n)? [",$ichoice,"] ");
  $valid = 0;
  while ($valid == 0) {
    chop ($in = <STDIN>);
    if ($in ne "") {
      $ichoice = $in;
    }
    if (($ichoice eq "y") || ($ichoice eq "n")) {
      $valid = 1;
    }
    if ($valid == 0) {
      pIndent();
      print ("Invalid, try again.\n");
    }
  }
  
  if ($ichoice eq "y") {
    # do the itinerary fill in
    itinerary();
  } else {
    
    ### end itinerary ###
    
    ### do the other date stuff 
    # location
    # from
    pIndent();
    print ("from:\n");
    
    indent();
    print OUT ("<!-- PrepPhrase -->\n");
    indent();
    print OUT ("<from>\n");
    incIndent();
    location();
    decIndent();
    indent();
    print OUT ("</from>\n");
    indent();
    print OUT ("<!-- /PrepPhrase -->\n");
  
    # to
    pIndent();
    print ("to:\n");
    
    indent();
    print OUT ("<!-- PrepPhrase -->\n");
    indent();
    print OUT ("<to>\n");
    incIndent();
    location();
    decIndent();
    indent();
    print OUT ("</to>\n");
    indent();
    print OUT ("<!-- /PrepPhrase -->\n");
    
  }



  ## keep doing the date preferences for now.
  # date preferences
  pIndent();
  print ("Start or Ready At Date:\n");
  
  indent();
  print OUT ("<!-- Preference -->\n");
  indent();
  print OUT ("<startdate>\n");
  
  incIndent();
  indent();
  print OUT ("<readyatdate>\n");

  incIndent();
  date();
  decIndent();

  indent();
  print OUT ("</readyatdate>\n");
  decIndent();

  indent();
  print OUT ("</startdate>\n");
  indent();
  print OUT ("<!-- /Preference -->\n");


  #######
  
  pIndent();
  print ("End Date:\n");
  
  indent();
  print OUT ("<!-- Preference -->\n");
  indent();
  
  print OUT ("<enddate>\n");
  incIndent();
  
  incPIndent();
  pIndent();
  print ("Early Date:\n");
  
  indent();
  print OUT ("<earlydate>\n");
  incIndent();
  
  date();

  decIndent();
  indent();
  print OUT ("</earlydate>\n");
  
  #######

  pIndent();
  print ("Best Date:\n");
  
  indent();
  print OUT ("<bestdate>\n");
  incIndent();

  date();

  decIndent();
  indent();
  print OUT ("</bestdate>\n");

  #######

  pIndent();
  print ("Late Date:\n");
  
  indent();
  print OUT ("<latedate>\n");
  incIndent();

  date();

  decIndent();
  indent();
  print OUT ("</latedate>\n");

  decPIndent();
  decIndent();
  indent();
  print OUT ("</enddate>\n");
  indent();
  print OUT ("<!-- /Preference -->\n");

  #######
  
  decIndent();
  indent();
  print OUT ("</task>\n");

  #### now see if there are more tasks to do
  pIndent();
  print ("More tasks (y/n)? [",$moreTasks,"] ");
  $valid = 0;
  while ($valid == 0) {
    chop ($in = <STDIN>);
    if ($in ne "") {
      $moreTasks = $in;
    }
    if (($moreTasks eq "y") || ($moreTasks eq "n")) {
      $valid = 1;
    }
    if ($valid == 0) {
      pIndent();
      print ("Invalid, try again.\n");
    }
  }
  
  $taskNum++; # increment task number
  
}

decIndent();
indent();
print OUT ("</tasklist>\n");


close(OUT);




####################



# helper sub routines

sub indent {
  # indent the lines the required amount as determined by the variable $white
  print OUT "  " x $white;
}

sub decIndent {
  # decrement indent amount
  $white--;
}

sub incIndent {
  # increment indent amount
  $white++;
}

sub pIndent {
  # indent the prompt lines
  print "  " x $promptIndent;
}

sub decPIndent {
  # decrement promptindent amount
  $promptIndent--;
}

sub incPIndent {
  # increment prompt indent amount
  $promptIndent++;
}


sub idInc {
  # increment the id number
  $id++;
}

sub printId {
  # print id number
  print OUT $id;
}

sub fillAsset {
  # fill in the asset section
  incPIndent();
  pIndent();
  print ("Asset creation: \n");
  pIndent();
  print ("item (i.e. TOPS_TANK)? [",$item,"] "); 
  chop($in = <STDIN>);
  if ($in ne "") {
    $item = $in;
  }
  pIndent();
  print ("how many ",$item, "? [",$itemNum,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $itemNum = $in;
  }

  for ($i = 1; $i <= $itemNum; $i++) {
    indent();
    print OUT (qq(<asset id="), $item,"-");
    printId();
    idInc();
    print OUT (qq(">),$item,"</asset>\n");
  }
  decPIndent();
}


sub fillAggregateAsset {
  # fill aggregate asset
  incPIndent();
  pIndent();
  print ("Aggregate asset creation: \n");
  pIndent();
  print ("item (i.e. TOPS_TANK)? [",$item,"] "); 
  chop($in = <STDIN>);
  if ($in ne "") {
    $item = $in;
  }

  pIndent();
  print ("how many ",$item, "? [",$itemNum,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $itemNum = $in;
  }

  indent();
  print OUT (qq(<aggregateasset quantity="), $itemNum,,qq(">),$item,"</aggregateasset>\n");
  decPIndent();
}

sub fillAssetGroup {
  # fill in an asset group
  incPIndent();
  pIndent();
  print ("Asset group creation: \n");
  indent();
  print OUT (qq(<assetgroup id="ag-));
  printId();
  idInc();
  print OUT (qq(">\n)); 
  incIndent();
  
  fillMultiGroup();

  decIndent();
  indent();
  print OUT ("</assetgroup>\n");
  decPIndent();
}

sub fillMultiGroup {
  pIndent();
  print ("What type of objects? The choices are:\n");
  $valid = 0;
  while ($valid == 0) {
    pIndent();
    print ("asset(a), aggregate asset(g), asset group (r) [",$choiceM,"] ");
    chop ($in = <STDIN>);
    if ($in ne "") {
      $choiceM = $in;
    }
    if (($choiceM eq "a") || ($choiceM eq "g") || ($choiceM eq "r")) {
      $valid = 1;
    }
    if ($valid == 0) {
      pIndent();
      print ("Invalid, try again.\n");
    }
  }
  
  if ($choiceM eq "a") {
      fillAsset();
  } elsif ($choiceM eq "g") {
      fillAggregateAsset();
  } elsif ($choiceM eq "r") {
      fillAssetGroup();
  }


  
  pIndent();
  print ("More objects (y/n)? [",$choiceMore,"] ");
  $valid = 0;
  while ($valid == 0) {
    chop ($in = <STDIN>);
    if ($in ne "") {
      $choiceMore = $in;
    }
    if (($choiceMore eq "y") || ($choiceMore eq "n")) {
      $valid = 1;
    }
    if ($valid == 0) {
      pIndent();
      print ("Invalid, try again.\n");
    }
  }
  
  if ($choiceMore eq "y") {
    fillMultiGroup();
  }
}

sub fillSingleGroup {
  pIndent();
  print ("What type of object? The choices are:\n");
  $valid = 0;
  while ($valid == 0) {
    pIndent();
    print ("asset(a), aggregate asset(g), asset group (r) [",$choiceM,"] ");
    chop ($in = <STDIN>);
    if ($in ne "") {
      $choiceM = $in;
    }
    if (($choiceM eq "a") || ($choiceM eq "g") || ($choiceM eq "r")) {
      $valid = 1;
    }
    if ($valid == 0) {
      pIndent();
      print ("Invalid, try again.\n");
    }
  }
  
  if ($choiceM eq "a") {
      fillAsset();
  } elsif ($choiceM eq "g") {
      fillAggregateAsset();
  } elsif ($choiceM eq "r") {
      fillAssetGroup();
  }
}

sub location {
  # input a location

  incPIndent();
  pIndent();
  print ("location code(i.e. XYZF)? [",$code,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $code = $in;
  }
  pIndent();
  print ("latitude of ", $code, " (i.e. 45.0)? [",$Lat,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $Lat = $in;
  }
  pIndent();
  print ("longitude of ", $code, " (i.e. 45.0)? [",$Long,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $Long = $in;
  }

  indent();
  print OUT ("<geoloc>",$code,"</geoloc>\n");
  indent();
  print OUT ("<latitude>",$Lat,"</latitude>\n");
  indent();
  print OUT ("<longitude>",$Long,"</longitude>\n");
  decPIndent();
}

sub date {
  # input a date

  incPIndent();
  pIndent();
  print ("year? [",$yr,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $yr = $in;
  }
  pIndent();
  print ("month? [",$mth,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $mth = $in;
  }
  pIndent();
  print ("day? [",$day,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $day = $in;
  }
  pIndent();
  print ("hour? [",$hr,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $hr = $in;
  }
  pIndent();
  print ("minute? [",$min,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $min = $in;
  }
  pIndent();
  print ("second? [",$sec,"] ");
  chop($in = <STDIN>);
  if ($in ne "") {
    $sec = $in;
  }

  indent();
  print OUT ("<year>",$yr,"</year>\n");
  indent();
  print OUT ("<month>",$mth,"</month>\n");
  indent();
  print OUT ("<day>",$day,"</day>\n");
  indent();
  print OUT ("<hour>",$hr,"</hour>\n");
  indent();
  print OUT ("<minute>",$min,"</minute>\n");
  indent();
  print OUT ("<second>",$sec,"</second>\n");

  decPIndent();
}

  
sub itinerary {
  # fill in itenerary

  $itNum = 1;
  indent();
  print OUT ("<ItineraryOf>\n");
  incIndent();
  
  # repeat until itinerary is done
  $moreI = "y";
  while ($moreI eq "y") {
    indent();
    print OUT (qq(<ItineraryElement leg="), $itNum, qq(">\n));
    incIndent();

    pIndent();
    print ("Start location for leg: ", $itNum,"\n");

    indent();
    print OUT ("<StartLocation>\n");
    incIndent();

    location();

    decIndent();
    indent();
    print OUT ("</StartLocation>\n");

    pIndent();
    print ("Start date for leg: ", $itNum,"\n");
  
    indent();
    print OUT ("<StartDate>\n");
    incIndent();

    date();

    decIndent();
    indent();
    print OUT ("</StartDate>\n");

    pIndent();
    print ("End location for leg: ", $itNum,"\n");

    indent();
    print OUT ("<EndLocation>\n");
    incIndent();

    location();

    decIndent();
    indent();
    print OUT ("</EndLocation>\n");

    pIndent();
    print ("End date for leg: ", $itNum,"\n");
  
    indent();
    print OUT ("<EndDate>\n");
    incIndent();

    date();

    decIndent();
    indent();
    print OUT ("</EndDate>\n");
  
    pIndent();
    print ("Role of leg: ", $itNum, " (i.e. Transit, Load, Unload, Fuel)? [",$role,"] ");
    chop($in = <STDIN>);
    if ($in ne "") {
      $role = $in;
    }

    indent();
    print OUT ("<Role> ", $role,, " </Role>\n");

    decIndent();
    indent();
    print OUT ("</ItineraryElement>\n");

    #more itinerary elements?
    pIndent();
    print ("More elements (y/n)? [",$moreI,"] ");
    $valid = 0;
    while ($valid == 0) {
      chop ($in = <STDIN>);
      if ($in ne "") {
	$moreI = $in;
      }
      if (($moreI eq "y") || ($moreI eq "n")) {
	$valid = 1;
      }
      if ($valid == 0) {
	pIndent();
	print ("Invalid, try again.\n");
      }
    }
    $itNum++;
  }

  decIndent();
  indent();
  print OUT ("</ItineraryOf>\n");

    
}

sub fillWith {
  # fill in the with section with an asset
  incPIndent();
  pIndent();
  print ("Asset creation: \n");
  pIndent();
  print ("item (i.e. C5)? [",$wItem,"] "); 
  chop($in = <STDIN>);
  if ($in ne "") {
    $wItem = $in;
  }

  indent();
  print OUT (qq(<asset id="), $wItem,"-");
  printId();
  idInc();
  print OUT (qq(">),$wItem,"</asset>\n");

  decPIndent();
}
