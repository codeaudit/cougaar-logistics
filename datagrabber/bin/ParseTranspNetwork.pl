#!/usr/local/bin/perl

# Script file to transform MTMC format to our format

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


$node_object_id = 101;
$link_object_id = 100;
$graph_object_id = 104;

sub open_and_skip_header 
  {
    local ($filename) = @_;
    open (FILE, $filename) || die "Can't open $filename";
    while (<FILE>) {
      @fields = split (/,/);
      last if (@fields[0] eq "\"ID\"");
    }
  }

open_and_skip_header ("nhpn22lk.txt");
$link_id = 1;
$node_index = 0;
@nodes = ();

while ($line = <FILE>) {
  ($ID,$Fnode,$Tnode,$Stfips,$Ctfips,$Sign1,$Sign2,$Sign3,$Lname,$Miles,$Lanes,$Acontrol,$Median,$Surface,$Fclass,$Rucode,$Nhs,$Imputed_sp,$NumPoints) = split (/,/, $line);

  print "first parse: link $link_id...\n";
  for ($i = 0; $i < $NumPoints; $i++) {
    $longlat = <FILE>;
    chop $longlat; chop $longlat;
 
    if ($i == 0){
      ($long[$Fnode],$lat[$Fnode]) = split (/,/, $longlat);
    }

    if ($i == ($NumPoints-1)){
      ($long[$Tnode],$lat[$Tnode]) = split (/,/, $longlat);
    }
  }

  if ($lat[$Fnode] <38 && $lat[$Fnode] >31 && $lat[$Tnode]<38 && $lat[$Tnode] >31 && $long[$Fnode] >-85 && $long[$Tnode]>-85 && $long[$Fnode] <-81 && $long[$Tnode]<-81){
      $links[$Fnode] .= "$link_id ";
      $links[$Tnode] .= "$link_id ";
      $valid_link[$ID] = 1;
  } else {
      $valid_link[$ID] = 0;
  }
  $link_id++;
}
close (FILE);

for ($i = 1; $i <= @links; $i++){
  @links2 = split (/ /, $links[$i]);
    
  if (($#links2 + 1) != 0) {
#    $nodes[$node_index] = $i;
    $nodes[$i] = $node_index;
#    print "$node_index:  $nodes[$node_index]\n";
    $node_index++;
  } else {
    $nodes[$i] = -1;
  }

}

open (FILE2, "> nhpn22.script.xml") || die "Cant open nhpn22.script";
print FILE2 "<!-- File containing south east region of the conus graph objects -->\n\n";

open_and_skip_header ("nhpn22lk.txt");
$node_index = 0;

print FILE2 "<network>\n";

print FILE2 "\n<!-- **************Nodes**************--> \n";

for ($i = 1; $i <= @nodes; $i++){
  print "parsing node $i\n";
  if ($nodes[$i] == $node_index){
    print FILE2 "<node id=\"n-$i\"\n";
    print FILE2 "      name=\"$i\"\n";
    print FILE2 "      geoloc=\"\"\n";
    print FILE2 "      latitude=\"$lat[$i]\"\n";
    print FILE2 "      longitude=\"$long[$i]\" />\n";
    #print out the link lists
    #print FILE2 "  <ll>\n";
    #@links3 = split (/ /, $links[$i]);
    #foreach $link (@links3) {
    #  print FILE2 "    <lr id=l-\"$link\"/>\n";
    #}
    #print FILE2 " </ll>\n\n";
    $node_index++;
  }
}
  print FILE2 "\n<!-- **************Links**************--> \n";
    
  while ($line = <FILE>) {
    ($ID,$Fnode,$Tnode,$Stfips,$Ctfips,$Sign1,$Sign2,$Sign3,$Lname,$Miles,$Lanes,$Acontrol,$Median,$Surface,$Fclass,$Rucode,$Nhs,$Imputed_sp,$NumPoints) = split (/,/, $line);
    
    if ($valid_link[$ID]){    
      print "Second parse: link $ID\n";
      
      #Lets print out the XML Link files
      print FILE2 "\n<link id=\"l-$ID\"\n";
      print FILE2 "        name=$Sign2\n";
      printf FILE2 "        source=\"n-%d\"\n",$Fnode;
      printf FILE2 "        destination=\"n-%d\"\n",$Tnode;
      print FILE2 "        length=\"$Miles\"\n";
      print FILE2 "        speed=\"$Imputed_sp\"\n";
      print FILE2 "        weight=\"1000000\"\n";
      print FILE2 "        capacity=\"1000000\" > \n";
      print FILE2 "  <roadlinkprop\n" 
      print FILE2 "       direction=\"-1\"\n";
      print FILE2 "       urban=\"-1\"\n";
      print FILE2 "       num_lanes=\"$Lanes\"\n";
      print FILE2 "       max_height=\"-1\"\n";
      print FILE2 "       max_width=\"-1\"\n";
      print FILE2 "       link_ID=\"$ID\"\n";
      print FILE2 "       state_code=\"-1\"\n";
      print FILE2 "       route_1=$Sign2\n";
      print FILE2 "       route_2=$Sign3\n";
      print FILE2 "       median=\"$Median\"\n";
      print FILE2 "       access=\"$Acontrol\"\n";
      print FILE2 "       trkrte=\"-1\"\n";
      print FILE2 "       f_class=\"$Fclass\"\n";
      print FILE2 "       convoy_speed=\"-1\"\n";
      print FILE2 "       convoy_travel_time=\"-1\"\n";
      print FILE2 "       num_under_hs20=\"$Nhs\" /> \n";
      print FILE2 " </link>\n";
    }
  }


$graph_id = 1;
print FILE2 "<!-------------- Now print the graph ------------>\n\n";
print FILE2 " <TransportationNetwork id=\"tn-$graph_id\">\n";
$node_index = 0;
for ($i = 1; $i <= @nodes; $i++) 
  {
    if ($nodes[$i] == $node_index) {
      print FILE2 "     <noderef id=\"n-$i\"/>\n";
      $node_index++
    }
  }
print FILE2 " </TransportationNetwork>\n";
print FILE2 "</network>\n";
close (FILE);
close (FILE2);
    






