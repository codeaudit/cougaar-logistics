#Provide csv file containing file names to be compared
#Parse baseline file and current file and then compare them

# <copyright>
#  
#  Copyright 2004 BBNT Solutions, LLC
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



$line_not_match_count = 0;
$data_groups_not_compared = 0;
$tolerance = 0.001;
$NUMBER_FILES_NOT_FOUND = 0;


while(<>) {
	$FILE_NOT_FOUND_FLAG = 0;

	#get file names
	@file_names = split(/,/);
	$Baseline_file_name = @file_names[0];
	$Current_file_name = @file_names[1];

	#remove return character from file name
	chop ($Current_file_name);
	
	#Reset the file mid point flags so that certain conditional statements
	#will only evaluate as true while in the upper half of the file being scanned
	$Baseline_FMPR = 0;
	$Current_FMPR = 0;
	
#Start and End points (actually line numbers) of the data groups must be reinitialized before scanning the files

	#Inventory Data Start and end points
	$Baseline_Inventory_Level_sp = 0;
	$Baseline_Inventory_Level_ep = 0;
	$Current_Inventory_Level_sp = 0;
	$Current_Inventory_Level_ep = 0;

	#Demand Projection Data Start and End points
	$Baseline_Demand_Projection_sp = 0;
	$Baseline_Demand_Projection_ep = 0;
	$Current_Demand_Projection_sp = 0;
	$Current_Demand_Projection_ep = 0;

	#Demand Projection Response Data Start and End Points
	$Baseline_Demand_Projection_Response_sp = 0;
	$Baseline_Demand_Projection_Response_ep = 0;
	$Current_Demand_Projection_Response_sp = 0;
	$Current_Demand_Projection_Response_ep = 0;

	#Demand Requisition Data Start and End Points
	$Baseline_Demand_Requisition_sp = 0;
	$Baseline_Demand_Requisition_ep = 0;
	$Current_Demand_Requisition_sp = 0;
	$Current_Demand_Requisition_ep = 0;

	#Demand Requisition Response Data Start and End Points
	$Baseline_Demand_Requisition_Response_sp = 0;
	$Baseline_Demand_Requisition_Response_ep = 0;
	$Current_Demand_Requisition_Response_sp = 0;
	$Current_Demand_Requisition_Response_ep = 0;

	#Refill Projection Data Start and End points
	$Baseline_Refill_Projection_sp = 0;
	$Baseline_Refill_Projection_ep = 0;
	$Current_Refill_Projection_sp = 0;
	$Current_Refill_Projection_ep = 0;

	#Refill Projection Response Data Start and End Points
	$Baseline_Refill_Projection_Response_sp = 0;
	$Baseline_Refill_Projection_Response_ep = 0;
	$Current_Refill_Projection_Response_sp = 0;
	$Current_Refill_Projection_Response_ep = 0;

	#Refill Requisition Data Start and End Points
	$Baseline_Refill_Requisition_sp = 0;
	$Baseline_Refill_Requisition_ep = 0;
	$Current_Refill_Requisition_sp = 0;
	$Current_Refill_Requisition_ep = 0;

	#Refill Requisition Response Data Start and End Points
	$Baseline_Refill_Requisition_Response_sp = 0;
	$Baseline_Refill_Requisition_Response_ep = 0;
	$Current_Refill_Requisition_Response_sp = 0;
	$Current_Refill_Requisition_Response_ep = 0;


#Clear data each time through the while loop

	@Baseline_Inventory_Level_list = ();
	@Current_Inventory_Level_list =();

	@Baseline_Demand_Projection_list = ();
	@Current_Demand_Projection_list = ();

	@Baseline_Demand_Projection_Response_list = ();
	@Current_Demand_Projection_Response_list =();

	@Baseline_Demand_Requisition_list = ();
	@Current_Demand_Requisition_list = ();

	@Baseline_Demand_Requisition_Response_list = ();
	@Current_Demand_Requisition_Response_list = ();
	
	@Baseline_Refill_Projection_list = ();
	@Current_Refill_Projection_list =();

	@Baseline_Refill_Projection_Response_list = ();
	@Current_Refill_Projection_Response_list =();

	@Baseline_Refill_Requisition_list = ();
	@Current_Refill_Requisition_list = ();

	@Baseline_Refill_Requisition_Response_list = ();
	@Current_Refill_Requisition_Response_list = ();

#Clear Array data flags

	$Baseline_Inventory_Level_has_no_data = 0;
	$Current_Inventory_Level_has_no_data = 0;
	
	$Baseline_Demand_Projection_has_no_data = 0;
	$Current_Demand_Projection_has_no_data = 0;

	$Baseline_Demand_Projection_Response_has_no_data = 0;
	$Current_Demand_Projection_Response_has_no_data = 0;

	$Baseline_Demand_Requisition_has_no_data = 0;
	$Current_Demand_Requisition_has_no_data = 0;

	$Baseline_Demand_Requisition_Response_has_no_data = 0;
	$Current_Demand_Requisition_Response_has_no_data = 0;

	$Baseline_Refill_Projection_has_no_data = 0;
	$Current_Refill_Projection_has_no_data = 0;

	$Baseline_Refill_Projection_Response_has_no_data = 0;
	$Current_Refill_Projection_Response_has_no_data = 0;

	$Baseline_Refill_Requisition_has_no_data = 0;
	$Current_Refill_Requisition_has_no_data = 0;

	$Baseline_Refill_Requisition_Response_has_no_data = 0;
	$Current_Refill_Requisition_Response_has_no_data = 0;

#Start parsing Baseline file
	
#	print ("#########################################################################################\n");
#	print ("Looking for BASELINE file.......$Baseline_file_name\n");
#	print ("#########################################################################################\n");

#Set File being analysed for data gathering subroutine
$FILE_type = "Baseline";

	#Make sure file exists before attempting to parse it
	if (-e $Baseline_file_name){
#		print ("File exists........\n");
		open (BASELINE_FILE, "$Baseline_file_name");

		#Read in all of the lines of the file at once into an array of strings
		@lines_baseline_file = <BASELINE_FILE>;
		
		#Loop through each line of the array and search for the various Start/End points
		for ($i = 0; $i < @lines_baseline_file; $i++){
			
			#File Mid Point Reached?
			if (@lines_baseline_file[$i] =~ /<INVENTORY_HEADER_GUI/) {
				$Baseline_FMPR = 1;		
			}

			#Find Start and End points of Inventory data
			if ((@lines_baseline_file[$i] =~ /<START TIME,END TIME,REORDER LEVEL,INVENTORY LEVEL, TARGET LEVEL>/) && (@lines_baseline_file[$i+1] !~ /<\/INVENTORY_HEADER_READABLE>/)) {
				$Baseline_Inventory_Level_sp = $i+1;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<START TIME,END TIME,REORDER LEVEL,INVENTORY LEVEL, TARGET LEVEL>/) && (@lines_baseline_file[($i+1)] =~ /<\/INVENTORY_HEADER_READABLE>/)) {
				$Baseline_Inventory_Level_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/INVENTORY_HEADER_READABLE>/) && ($Baseline_Inventory_Level_sp != 0)) {
				$Baseline_Inventory_Level_ep = $i-2;
			}
			
			#Find Start and End points of Demand Projection
			if ((@lines_baseline_file[$i] =~ /<COUNTED_PROJECTWITHDRAW_TASKS type=PROJTASKS>/) && ($Baseline_Demand_Projection_sp == 0) && (@lines_baseline_file[$i+2] !~ /<\/COUNTED_PROJECTWITHDRAW_TASKS>/)){
				$Baseline_Demand_Projection_sp = $i+2;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<COUNTED_PROJECTWITHDRAW_TASKS type=PROJTASKS>/) && (@lines_baseline_file[($i+2)] =~ /<\/COUNTED_PROJECTWITHDRAW_TASKS>/)) {
				$Baseline_Demand_Projection_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/COUNTED_PROJECTWITHDRAW_TASKS>/) && ($Baseline_Demand_Projection_sp != 0) && ($Baseline_Demand_Projection_ep == 0)) {
				$Baseline_Demand_Projection_ep = $i-1;
			}

			#Find Start and End points of Demand Projection Response
			if ((@lines_baseline_file[$i] =~ /<COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS type=PROJ_ARS>/) && ($Baseline_Demand_Projection_Response_sp == 0) && (@lines_baseline_file[$i+2] !~ /<\/COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS>/)){
				$Baseline_Demand_Projection_Response_sp = $i+2;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS type=PROJ_ARS>/) && (@lines_baseline_file[($i+2)] =~ /<\/COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS>/)) {
				$Baseline_Demand_Projection_Response_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS>/) && ($Baseline_Demand_Projection_Response_sp != 0) && ($Baseline_Demand_Projection_Response_ep == 0)) {
				$Baseline_Demand_Projection_Response_ep = $i-1;
			}
			
			#Find Start and End points of Demand Requisition
			if ((@lines_baseline_file[$i] =~ /<WITHDRAW_TASKS type=TASKS>/) && ($Baseline_Demand_Requisition_sp == 0) && (@lines_baseline_file[$i+2] !~ /<\/WITHDRAW_TASKS>/)) {
				$Baseline_Demand_Requisition_sp = $i+2;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<WITHDRAW_TASKS type=TASKS>/) && (@lines_baseline_file[($i+2)] =~ /<\/WITHDRAW_TASKS>/)) {
				$Baseline_Demand_Requisition_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/WITHDRAW_TASKS>/) && ($Baseline_Demand_Requisition_sp != 0) && ($Baseline_Demand_Requisition_ep == 0)) {
				$Baseline_Demand_Requisition_ep = $i-1;
			}
			
			#Find Start and End points of Demand Requisition Response
			if ((@lines_baseline_file[$i] =~ /<WITHDRAW_TASK_ALLOCATION_RESULTS type=ARS>/) && ($Baseline_Demand_Requisition_Response_sp == 0) && (@lines_baseline_file[$i+2] !~ /<\/WITHDRAW_TASK_ALLOCATION_RESULTS>/)) {
				$Baseline_Demand_Requisition_Response_sp = $i+2;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<WITHDRAW_TASK_ALLOCATION_RESULTS type=ARS>/) && (@lines_baseline_file[($i+2)] =~ /<\/WITHDRAW_TASK_ALLOCATION_RESULTS>/)) {
				$Baseline_Demand_Requisition_Response_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/WITHDRAW_TASK_ALLOCATION_RESULTS>/) && ($Baseline_Demand_Requisition_Response_sp != 0) && ($Baseline_Demand_Requisition_Response_ep == 0)) {
				$Baseline_Demand_Requisition_Response_ep = $i-1;
			}
			
			#Find Start and End points of Refill Projection
			if ((@lines_baseline_file[$i] =~ /<RESUPPLY_PROJECTSUPPLY_TASKS type=PROJTASKS>/) && ($Baseline_Refill_Projection_sp == 0) && (@lines_baseline_file[$i+2] !~ /<\/RESUPPLY_PROJECTSUPPLY_TASKS>/)) {
				$Baseline_Refill_Projection_sp = $i+2;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<RESUPPLY_PROJECTSUPPLY_TASKS type=PROJTASKS>/) && (@lines_baseline_file[($i+2)] =~ /<\/RESUPPLY_PROJECTSUPPLY_TASKS>/)) {
				$Baseline_Refill_Projection_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/RESUPPLY_PROJECTSUPPLY_TASKS>/) && ($Baseline_Refill_Projection_sp != 0) && ($Baseline_Refill_Projection_ep == 0)) {
				$Baseline_Refill_Projection_ep = $i-1;
			}

			#Find Start and End points of Refill Projection Response
			if ((@lines_baseline_file[$i] =~ /<RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS type=PROJ_ARS>/) && ($Baseline_Refill_Projection_Response_sp == 0) && (@lines_baseline_file[$i+2] !~ /<\/RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS>/)){
				$Baseline_Refill_Projection_Response_sp = $i+2;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS type=PROJ_ARS>/) && (@lines_baseline_file[($i+2)] =~ /<\/RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS>/)) {
				$Baseline_Refill_Projection_Response_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS>/) && ($Baseline_Refill_Projection_Response_sp != 0) && ($Baseline_Refill_Projection_Response_ep == 0)) {
				$Baseline_Refill_Projection_Response_ep = $i-1;
			}
			
			#Find Start and End points of Refill Requisition
			if ((@lines_baseline_file[$i] =~ /<RESUPPLY_SUPPLY_TASKS type=TASKS>/) && ($Baseline_Refill_Requisition_sp == 0) && (@lines_baseline_file[$i+2] !~ /<\/RESUPPLY_SUPPLY_TASKS>/)){
				$Baseline_Refill_Requisition_sp = $i+2;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<RESUPPLY_SUPPLY_TASKS type=TASKS>/) && (@lines_baseline_file[($i+2)] =~ /<\/RESUPPLY_SUPPLY_TASKS>/)) {
				$Baseline_Refill_Requisition_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/RESUPPLY_SUPPLY_TASKS>/) && ($Baseline_Refill_Requisition_sp != 0) && ($Baseline_Refill_Requisition_ep == 0)) {
				$Baseline_Refill_Requisition_ep = $i-1;
			}
	
			#Find Start and End points of Refill Requisition Response
			if ((@lines_baseline_file[$i] =~ /<RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS type=ARS>/) && ($Baseline_Refill_Requisition_Response_sp == 0) && (@lines_baseline_file[$i+2] !~ /<\/RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS>/)){
				$Baseline_Refill_Requisition_Response_sp = $i+2;
			} elsif (($Baseline_FMPR == 0) && (@lines_baseline_file[$i] =~ /<RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS type=ARS>/) && (@lines_baseline_file[($i+2)] =~ /<\/RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS>/)) {
				$Baseline_Refill_Requisition_Response_has_no_data = 1;
			} elsif ((@lines_baseline_file[$i] =~ /<\/RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS>/) && ($Baseline_Refill_Requisition_Response_sp != 0) && ($Baseline_Refill_Requisition_Response_ep == 0)) {
				$Baseline_Refill_Requisition_Response_ep = $i-1;
			}

		}
		
		#Gather up only the Inventory Level data that is to be compared
		if ($Baseline_Inventory_Level_has_no_data == 0) {
		@Baseline_Inventory_Level_list = Gather_Data ($Baseline_Inventory_Level_sp, $Baseline_Inventory_Level_ep, (1, 2, 3, 4, 5));
		}

		#Gather up only the Demand Projection data that is to be compared
		if ($Baseline_Demand_Projection_has_no_data == 0) {
		@Baseline_Demand_Projection_list = Gather_Data ($Baseline_Demand_Projection_sp, $Baseline_Demand_Projection_ep, (4, 5, 6, 7));
		}

		#Gather up only the Demand Projection Response data that is to be compared
		if ($Baseline_Demand_Projection_Response_has_no_data == 0) {
		@Baseline_Demand_Projection_Response_list = Gather_Data ($Baseline_Demand_Projection_Response_sp, $Baseline_Demand_Projection_Response_ep, (4, 7, 8, 9));
		}

		#Gather up only the Demand Requisition data that is to be compared
		if ($Baseline_Demand_Requisition_has_no_data == 0) {
		@Baseline_Demand_Requisition_list = Gather_Data ($Baseline_Demand_Requisition_sp, $Baseline_Demand_Requisition_ep, (4, 5, 6, 7));
		}

		#Gather up only the Demand Requisition Response data that is to be compared
		if ($Baseline_Demand_Requisition_Response_has_no_data == 0) {
		@Baseline_Demand_Requisition_Response_list = Gather_Data ($Baseline_Demand_Requisition_Response_sp, $Baseline_Demand_Requisition_Response_ep, (4, 7, 8, 9));
		}

		#Gather up only the Refill Projection data that is to be compared
		if ($Baseline_Refill_Projection_has_no_data == 0) {
		@Baseline_Refill_Projection_list = Gather_Data ($Baseline_Refill_Projection_sp, $Baseline_Refill_Projection_ep, (4, 5, 6, 7));
		}

		#Gather up only the Refill Projection Response data that is to be compared
		if ($Baseline_Refill_Projection_Response_has_no_data == 0) {
		@Baseline_Refill_Projection_Response_list = Gather_Data ($Baseline_Refill_Projection_Response_sp, $Baseline_Refill_Projection_Response_ep, (4, 7, 8, 9));
		}

		#Gather up only the Refill Requisition data that is to be compared
		if ($Baseline_Refill_Requisition_has_no_data == 0) {
		@Baseline_Refill_Requisition_list = Gather_Data ($Baseline_Refill_Requisition_sp, $Baseline_Refill_Requisition_ep, (4, 5, 6, 7));
		}

		#Gather up only the Refill Requisition Response data that is to be compared
		if ($Baseline_Refill_Requisition_Response_has_no_data == 0) {
		@Baseline_Refill_Requisition_Response_list = Gather_Data ($Baseline_Refill_Requisition_Response_sp, $Baseline_Refill_Requisition_Response_ep, (4, 7, 8, 9));
		}

	
	}else{
		print("Can not find file named ", $Baseline_file_name,
	"comparison fails by default\n");
		$FILE_NOT_FOUND_FLAG = 1;
		$NUMBER_FILES_NOT_FOUND++;
	}
		
#Start parsing Current file

#	print ("#########################################################################################\n");
#	print ("Looking for CURRENT file.......$Current_file_name\n");
#	print ("#########################################################################################\n");

#Set File being analysed for data gathering subroutine
$FILE_type = "Current";
	
	if (-e $Current_file_name){
#		print ("File exists........\n");
		open (CURRENT_FILE, "$Current_file_name");

		@lines_current_file = <CURRENT_FILE>;
	
		for ($i = 0; $i < @lines_current_file; $i++){
			
			#File Mid Point Reached? 
			#Can't allow certain conditional statements to be evaluated after this point is reached

			if (@lines_current_file[$i] =~ /<INVENTORY_HEADER_GUI/) {
				$Current_FMPR = 1;		
			}

			#Find Start and End points of Inventory data
			if ((@lines_current_file[$i] =~ /<START TIME,END TIME,REORDER LEVEL,INVENTORY LEVEL, TARGET LEVEL>/) && (@lines_current_file[($i+1)] !~ /<\/INVENTORY_HEADER_READABLE>/)) {
				$Current_Inventory_Level_sp = $i+1;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<START TIME,END TIME,REORDER LEVEL,INVENTORY LEVEL, TARGET LEVEL>/) && (@lines_current_file[($i+1)] =~ /<\/INVENTORY_HEADER_READABLE>/)) {
				$Current_Inventory_Level_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/INVENTORY_HEADER_READABLE>/) && ($Current_Inventory_Level_sp != 0)){
				$Current_Inventory_Level_ep = $i-2;
			}

			#Find Start and End points of Demand Projection
			if ((@lines_current_file[$i] =~ /<COUNTED_PROJECTWITHDRAW_TASKS type=PROJTASKS>/) && ($Current_Demand_Projection_sp == 0) && (@lines_current_file[($i+2)] !~ /<\/COUNTED_PROJECTWITHDRAW_TASKS>/)){
				$Current_Demand_Projection_sp = $i+2;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<COUNTED_PROJECTWITHDRAW_TASKS type=PROJTASKS>/) && (@lines_current_file[($i+2)] =~ /<\/COUNTED_PROJECTWITHDRAW_TASKS>/)) {
				$Current_Demand_Projection_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/COUNTED_PROJECTWITHDRAW_TASKS>/) && ($Current_Demand_Projection_sp != 0) && ($Current_Demand_Projection_ep == 0)) {
				$Current_Demand_Projection_ep = $i-1;
			}

			#Find Start and End points of Demand Projection Response
			if ((@lines_current_file[$i] =~ /<COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS type=PROJ_ARS>/) && ($Current_Demand_Projection_Response_sp == 0) && (@lines_current_file[($i+2)] !~ /<\/COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS>/)){
				$Current_Demand_Projection_Response_sp = $i+2;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS type=PROJ_ARS>/) && (@lines_current_file[($i+2)] =~ /<\/COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS>/)) {
				$Current_Demand_Projection_Response_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS>/) && ($Current_Demand_Projection_Response_sp != 0) && ($Current_Demand_Projection_Response_ep == 0)) {
				$Current_Demand_Projection_Response_ep = $i-1;
			}

			#Find Start and End points of Demand Requisition
			if ((@lines_current_file[$i] =~ /<WITHDRAW_TASKS type=TASKS>/) && ($Current_Demand_Requisition_sp == 0) && (@lines_current_file[($i+2)] !~ /<\/WITHDRAW_TASKS>/)) {
				$Current_Demand_Requisition_sp = $i+2;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<WITHDRAW_TASKS type=TASKS>/) && (@lines_current_file[($i+2)] =~ /<\/WITHDRAW_TASKS>/)) {
				$Current_Demand_Requisition_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/WITHDRAW_TASKS>/) && ($Current_Demand_Requisition_sp != 0) && ($Current_Demand_Requisition_ep == 0)) {
				$Current_Demand_Requisition_ep = $i-1;
			}
			
			#Find Start and End points of Demand Requisition Response
			if ((@lines_current_file[$i] =~ /<WITHDRAW_TASK_ALLOCATION_RESULTS type=ARS>/) && ($Current_Demand_Requisition_Response_sp == 0) && (@lines_current_file[($i+2)] !~ /<\/WITHDRAW_TASK_ALLOCATION_RESULTS>/)){
				$Current_Demand_Requisition_Response_sp = $i+2;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<WITHDRAW_TASK_ALLOCATION_RESULTS type=ARS>/) && (@lines_current_file[($i+2)] =~ /<\/WITHDRAW_TASK_ALLOCATION_RESULTS>/)) {
				$Current_Demand_Requisition_Response_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/WITHDRAW_TASK_ALLOCATION_RESULTS>/) && ($Current_Demand_Requisition_Response_sp != 0) && ($Current_Demand_Requisition_Response_ep == 0)) {
				$Current_Demand_Requisition_Response_ep = $i-1;
			}

			#Find Start and End points of Refill Projection
			if ((@lines_current_file[$i] =~ /<RESUPPLY_PROJECTSUPPLY_TASKS type=PROJTASKS>/) && ($Current_Refill_Projection_sp == 0) && (@lines_current_file[($i+2)] !~ /<\/RESUPPLY_PROJECTSUPPLY_TASKS>/)){
				$Current_Refill_Projection_sp = $i+2;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<RESUPPLY_PROJECTSUPPLY_TASKS type=PROJTASKS>/) && (@lines_current_file[($i+2)] =~ /<\/RESUPPLY_PROJECTSUPPLY_TASKS>/)) {
				$Current_Refill_Projection_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/RESUPPLY_PROJECTSUPPLY_TASKS>/) && ($Current_Refill_Projection_sp != 0) && ($Current_Refill_Projection_ep == 0)) {
				$Current_Refill_Projection_ep = $i-1;
			}

			#Find Start and End points of Refill Projection Response
			if ((@lines_current_file[$i] =~ /<RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS type=PROJ_ARS>/) && ($Current_Refill_Projection_Response_sp == 0) && (@lines_current_file[($i+2)] !~ /<\/RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS>/)){
				$Current_Refill_Projection_Response_sp = $i+2;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS type=PROJ_ARS>/) && (@lines_current_file[($i+2)] =~ /<\/RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS>/)) {
				$Current_Refill_Projection_Response_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS>/) && ($Current_Refill_Projection_Response_sp != 0) && ($Current_Refill_Projection_Response_ep == 0)) {
				$Current_Refill_Projection_Response_ep = $i-1;
			}

			#Find Start and End points of Refill Requisition
			if ((@lines_current_file[$i] =~ /<RESUPPLY_SUPPLY_TASKS type=TASKS>/) && ($Current_Refill_Requisition_sp == 0) && (@lines_current_file[($i+2)] !~ /<\/RESUPPLY_SUPPLY_TASKS>/)){
				$Current_Refill_Requisition_sp = $i+2;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<RESUPPLY_SUPPLY_TASKS type=TASKS>/) && (@lines_current_file[($i+2)] =~ /<\/RESUPPLY_SUPPLY_TASKS>/)) {
				$Current_Refill_Requisition_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/RESUPPLY_SUPPLY_TASKS>/) && ($Current_Refill_Requisition_sp != 0) && ($Current_Refill_Requisition_ep == 0)) {
				$Current_Refill_Requisition_ep = $i-1;
			}
			
			#Find Start and End points of Refill Requisition Response
			if ((@lines_current_file[$i] =~ /<RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS type=ARS>/) && ($Current_Refill_Requisition_Response_sp == 0) && (@lines_current_file[($i+2)] !~ /<\/RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS>/)){
				$Current_Refill_Requisition_Response_sp = $i+2;
			} elsif (($Current_FMPR == 0) && (@lines_current_file[$i] =~ /<RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS type=ARS>/) && (@lines_current_file[($i+2)] =~ /<\/RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS>/)) {
				$Current_Refill_Requisition_Response_has_no_data = 1;
			} elsif ((@lines_current_file[$i] =~ /<\/RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS>/) && ($Current_Refill_Requisition_Response_sp != 0) && ($Current_Refill_Requisition_Response_ep == 0)) {
				$Current_Refill_Requisition_Response_ep = $i-1;
			}
			
			

		}

		#Gather up only the Inventory Level data that is to be compared
		if ($Current_Inventory_Level_has_no_data == 0) {
		@Current_Inventory_Level_list = Gather_Data ($Current_Inventory_Level_sp, $Current_Inventory_Level_ep, (1, 2, 3, 4, 5));
		}

		#Gather up only the Demand Projection data that is to be compared
		if ($Current_Demand_Projection_has_no_data == 0) {
		@Current_Demand_Projection_list = Gather_Data ($Current_Demand_Projection_sp, $Current_Demand_Projection_ep, (4, 5, 6, 7));
		}

		#Gather up only the Demand Projection Response data that is to be compared
		if ($Current_Demand_Projection_Response_has_no_data == 0) {
		@Current_Demand_Projection_Response_list = Gather_Data ($Current_Demand_Projection_Response_sp, $Current_Demand_Projection_Response_ep, (4, 7, 8, 9));
		}

		#Gather up only the Demand Requisition data that is to be compared
		if ($Current_Demand_Requisition_has_no_data == 0) {
		@Current_Demand_Requisition_list = Gather_Data ($Current_Demand_Requisition_sp, $Current_Demand_Requisition_ep, (4, 5, 6, 7));
		}

		#Gather up only the Demand Requisition Response data that is to be compared
		if ($Current_Demand_Requisition_Response_has_no_data == 0) {
		@Current_Demand_Requisition_Response_list = Gather_Data ($Current_Demand_Requisition_Response_sp, $Current_Demand_Requisition_Response_ep, (4, 7, 8, 9));
		}

		if ($Current_Refill_Projection_has_no_data == 0) {
		@Current_Refill_Projection_list = Gather_Data ($Current_Refill_Projection_sp, $Current_Refill_Projection_ep, (4, 5, 6, 7));
		}

		#Gather up only the Refill Projection Response data that is to be compared
		if ($Current_Refill_Projection_Response_has_no_data == 0) {
		@Current_Refill_Projection_Response_list = Gather_Data ($Current_Refill_Projection_Response_sp, $Current_Refill_Projection_Response_ep, (4, 7, 8, 9));
		}

		#Gather up only the Refill Requisition data that is to be compared
		if ($Current_Refill_Requisition_has_no_data == 0) {
		@Current_Refill_Requisition_list = Gather_Data ($Current_Refill_Requisition_sp, $Current_Refill_Requisition_ep, (4, 5, 6, 7));
		}

		#Gather up only the Refill Requisition Response data that is to be compared
		if ($Current_Refill_Requisition_Response_has_no_data == 0) {
		@Current_Refill_Requisition_Response_list = Gather_Data ($Current_Refill_Requisition_Response_sp, $Current_Refill_Requisition_Response_ep, (4, 7, 8, 9));
		}


	}else{
		print("Can not find file named ", $Current_file_name, " comparison fails by default\n");
		$FILE_NOT_FOUND_FLAG = 1;
		$NUMBER_FILES_NOT_FOUND++;
	}
	
#Start comparisons of the various data sets

     if ($FILE_NOT_FOUND_FLAG != 1){
	#Inventory comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Inventory lines of Baseline file and Current File....\n");
	if (($Baseline_Inventory_Level_has_no_data == 0) && ($Current_Inventory_Level_has_no_data == 0)) {


	$local_line_not_match_count = 0;
	   if (($Baseline_Inventory_Level_ep-$Baseline_Inventory_Level_sp) == ($Current_Inventory_Level_ep-$Current_Inventory_Level_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Inventory_Level_lines = $Baseline_Inventory_Level_ep - $Baseline_Inventory_Level_sp;
		for ($j = 0; $j <= $Max_Inventory_Level_lines; $j++){
			if ($Baseline_Inventory_Level_list[$j][1] ne $Current_Inventory_Level_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Inventory Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ": Line# ",  $Baseline_Inventory_Level_list[$j][0]+1,    "  $Baseline_Inventory_Level_list[$j][1]\n");
				print ($Current_file_name, ":  Line# ",  $Current_Inventory_Level_list[$j][0]+1,     "  $Current_Inventory_Level_list[$j][1]\n\n");
			
			}
			if ($Baseline_Inventory_Level_list[$j][2] ne $Current_Inventory_Level_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Inventory End Date Fields don't match\n\n");
				print ($Baseline_file_name, ": Line# ",   $Baseline_Inventory_Level_list[$j][0]+1,    "  $Baseline_Inventory_Level_list[$j][2]\n");
				print ($Current_file_name, ":  Line# ",   $Current_Inventory_Level_list[$j][0]+1,     "  $Current_Inventory_Level_list[$j][2]\n\n");
			
			}
			$Inventory_Reorder_Level_difference = abs($Baseline_Inventory_Level_list[$j][3] - $Current_Inventory_Level_list[$j][3]);
			if ($Inventory_Reorder_Level_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Inventory Reorder Level Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Inventory_Level_list[$j][0]+1,    "  $Baseline_Inventory_Level_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Inventory_Level_list[$j][0]+1,     "  $Current_Inventory_Level_list[$j][3]\n\n");	
			}
			$Inventory_Level_difference = abs($Baseline_Inventory_Level_list[$j][4] - $Current_Inventory_Level_list[$j][4]);
			if ($Inventory_Level_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Inventory Level Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Inventory_Level_list[$j][0]+1,    "  $Baseline_Inventory_Level_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Inventory_Level_list[$j][0]+1,     "  $Current_Inventory_Level_list[$j][4]\n\n");
			
			}
			$Inventory_Target_Level_difference = abs($Baseline_Inventory_Level_list[$j][5] - $Current_Inventory_Level_list[$j][5]);
			if ($Inventory_Target_Level_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Inventory Target Level Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Inventory_Level_list[$j][0]+1,    "  $Baseline_Inventory_Level_list[$j][5]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Inventory_Level_list[$j][0]+1,     "  $Current_Inventory_Level_list[$j][5]\n\n");
			
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		}
	   }else{
#		print ($Baseline_file_name, " and ",
#	   $Current_file_name, " do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }

	} elsif (($Baseline_Inventory_Level_has_no_data == 1) || ($Current_Inventory_Level_has_no_data == 1)) {
			if ($Baseline_Inventory_Level_has_no_data == 1) {
				print ($Baseline_file_name, 
				       "\nNo BASELINE Inventory Level Data available for comparison\n");
			}
			if ($Current_Inventory_Level_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Inventory Level Data available for comparison\n\n");
			}
	}

	#Demand Projection comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Demand_Projection lines of Baseline file and Current File....\n");
	if (($Baseline_Demand_Projection_has_no_data == 0) && ($Current_Demand_Projection_has_no_data == 0)) {

	@Baseline_Demand_Projection_list = sort by_unit_then_by_date @Baseline_Demand_Projection_list;
	@Current_Demand_Projection_list = sort by_unit_then_by_date @Current_Demand_Projection_list;
	
	$local_line_not_match_count = 0;
	   if (($Baseline_Demand_Projection_ep-$Baseline_Demand_Projection_sp) == ($Current_Demand_Projection_ep-$Current_Demand_Projection_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Demand_Projection_lines = $Baseline_Demand_Projection_ep - $Baseline_Demand_Projection_sp;
		for ($j = 0; $j <= $Max_Demand_Projection_lines; $j++){
			#print ("List line $j $Current_Demand_Projection_list[$j][0] $Current_Demand_Projection_list[$j][1] $Current_Demand_Projection_list[$j][2] $Current_Demand_Projection_list[$j][3] $Current_Demand_Projection_list[$j][4]\n");
			#print ("List line $j $Baseline_Demand_Projection_list[$j][0] $Baseline_Demand_Projection_list[$j][1] $Baseline_Demand_Projection_list[$j][2] $Baseline_Demand_Projection_list[$j][3] $Baseline_Demand_Projection_list[$j][4]\n");
			if ($Baseline_Demand_Projection_list[$j][1] ne $Current_Demand_Projection_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Projection Unit Name Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Projection_list[$j][0]+1,    "  $Baseline_Demand_Projection_list[$j][1]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Projection_list[$j][0]+1,     "  $Current_Demand_Projection_list[$j][1]\n\n");	
			}
			if ($Baseline_Demand_Projection_list[$j][2] ne $Current_Demand_Projection_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Projection Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Projection_list[$j][0]+1,    "  $Baseline_Demand_Projection_list[$j][2]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Projection_list[$j][0]+1,     "  $Current_Demand_Projection_list[$j][2]\n\n");	
			}
			if ($Baseline_Demand_Projection_list[$j][3] ne $Current_Demand_Projection_list[$j][3]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Projection End Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Projection_list[$j][0]+1,    "  $Baseline_Demand_Projection_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Projection_list[$j][0]+1,     "  $Current_Demand_Projection_list[$j][3]\n\n");	
			}
			$Demand_Projection_difference = abs($Baseline_Demand_Projection_list[$j][4] - $Current_Demand_Projection_list[$j][4]);
			if ($Demand_Projection_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Projection Daily Rate Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Projection_list[$j][0]+1,    "  $Baseline_Demand_Projection_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Projection_list[$j][0]+1,     "  $Current_Demand_Projection_list[$j][4]\n\n");	
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		}
 
	   }else{
#		print ($Baseline_file_name, " and ",
#	$Current_file_name, " do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }

	} elsif (($Baseline_Demand_Projection_has_no_data == 1) || ($Current_Demand_Projection_has_no_data == 1)) {
			if ($Baseline_Demand_Projection_has_no_data == 1) {
				print ($Baseline_file_name, 
				       "\nNo BASELINE Demand Projection Data available for comparison\n");
			}
			if ($Current_Demand_Projection_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Demand Projection Data available for comparison\n\n");
			}
	}

	#Demand Projection Response comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Demand_Projection_Response lines of Baseline file and Current File....\n");
	if (($Baseline_Demand_Projection_Response_has_no_data == 0) && ($Current_Demand_Projection_Response_has_no_data == 0)) {

	@Baseline_Demand_Projection_Response_list = sort by_unit_then_by_date @Baseline_Demand_Projection_Response_list;
	@Current_Demand_Projection_Response_list = sort by_unit_then_by_date @Current_Demand_Projection_Response_list;
	
	$local_line_not_match_count = 0;
	   if (($Baseline_Demand_Projection_Response_ep-$Baseline_Demand_Projection_Response_sp) == ($Current_Demand_Projection_Response_ep-$Current_Demand_Projection_Response_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Demand_Projection_Response_lines = $Baseline_Demand_Projection_Response_ep - $Baseline_Demand_Projection_Response_sp;
		for ($j = 0; $j <= $Max_Demand_Projection_Response_lines; $j++){
			if ($Baseline_Demand_Projection_Response_list[$j][1] ne $Current_Demand_Projection_Response_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Projection Response Unit Name Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Projection_Response_list[$j][0]+1,    "  $Baseline_Demand_Projection_Response_list[$j][1]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Projection_Response_list[$j][0]+1,     "  $Current_Demand_Projection_Response_list[$j][1]\n\n");	
			}
			if ($Baseline_Demand_Projection_Response_list[$j][2] ne $Current_Demand_Projection_Response_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Projection Response Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Projection_Response_list[$j][0]+1,    "  $Baseline_Demand_Projection_Response_list[$j][2]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Projection_Response_list[$j][0]+1,     "  $Current_Demand_Projection_Response_list[$j][2]\n\n");	
			}
			if ($Baseline_Demand_Projection_Response_list[$j][3] ne $Current_Demand_Projection_Response_list[$j][3]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Projection Response End Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Projection_Response_list[$j][0]+1,    "  $Baseline_Demand_Projection_Response_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Projection_Response_list[$j][0]+1,     "  $Current_Demand_Projection_Response_list[$j][3]\n\n");	
			}
			$Demand_Projection_Response_difference = abs($Baseline_Demand_Projection_Response_list[$j][4] - $Current_Demand_Projection_Response_list[$j][4]);
			if ($Demand_Projection_Response_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Projection Response Daily Rate Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Projection_Response_list[$j][0]+1,    "  $Baseline_Demand_Projection_Response_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Projection_Response_list[$j][0]+1,     "  $Current_Demand_Projection_Response_list[$j][4]\n\n");	
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		}

 
	   }else{
#		print ("Files do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }

	} elsif (($Baseline_Demand_Projection_Response_has_no_data == 1) || ($Current_Demand_Projection_Response_has_no_data == 1)) {
			if ($Baseline_Demand_Projection_Response_has_no_data == 1) {
				print ($Baseline_file_name, 
				       "\nNo BASELINE Demand Projection Response Data available for comparison\n");
			}
			if ($Current_Demand_Projection_Response_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Demand Projection Response Data available for comparison\n\n");
			}
	}

	#Demand Requisition comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Demand_Requisition lines of Baseline file and Current File....\n");
	if (($Baseline_Demand_Requisition_has_no_data == 0) && ($Current_Demand_Requisition_has_no_data == 0)) {

	@Baseline_Demand_Requisition_list = sort by_unit_then_by_date @Baseline_Demand_Requisition_list;
	@Current_Demand_Requisition_list = sort by_unit_then_by_date @Current_Demand_Requisition_list;
	
	$local_line_not_match_count = 0;
	   if (($Baseline_Demand_Requisition_ep-$Baseline_Demand_Requisition_sp) == ($Current_Demand_Requisition_ep-$Current_Demand_Requisition_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Demand_Requisition_lines = $Baseline_Demand_Requisition_ep - $Baseline_Demand_Requisition_sp;
		for ($j = 0; $j <= $Max_Demand_Requisition_lines; $j++){
			if ($Baseline_Demand_Requisition_list[$j][1] ne $Current_Demand_Requisition_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Requisition Unit Name Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Requisition_list[$j][0]+1,    "  $Baseline_Demand_Requisition_list[$j][1]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Requisition_list[$j][0]+1,     "  $Current_Demand_Requisition_list[$j][1]\n\n");	
			}
			if ($Baseline_Demand_Requisition_list[$j][2] ne $Current_Demand_Requisition_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Requisition Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Requisition_list[$j][0]+1,    "  $Baseline_Demand_Requisition_list[$j][2]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Requisition_list[$j][0]+1,     "  $Current_Demand_Requisition_list[$j][2]\n\n");	
			}
			if ($Baseline_Demand_Requisition_list[$j][3] ne $Current_Demand_Requisition_list[$j][3]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Requisition End Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Requisition_list[$j][0]+1,    "  $Baseline_Demand_Requisition_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Requisition_list[$j][0]+1,     "  $Current_Demand_Requisition_list[$j][3]\n\n");	
			}
			$Demand_Requisition_difference = abs($Baseline_Demand_Requisition_list[$j][4] - $Current_Demand_Requisition_list[$j][4]);
			if ($Demand_Requisition_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Requisition Daily Rate Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Requisition_list[$j][0]+1,    "  $Baseline_Demand_Requisition_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Requisition_list[$j][0]+1,     "  $Current_Demand_Requisition_list[$j][4]\n\n");	
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		} 
	   }else{
#		print ($Baseline_file_name, " and ",
#	$Current_file_name, " do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }

	} elsif (($Baseline_Demand_Requisition_has_no_data == 1) || ($Current_Demand_Requisition_has_no_data == 1)) {
			if ($Baseline_Demand_Requisition_has_no_data == 1) {
			    print ($Baseline_file_name, 
				   "\nNo BASELINE Demand Requisition Data available for comparison\n");
			}
			if ($Current_Demand_Requisition_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Demand Requisition Data available for comparison\n\n");
			}
	}

	#Demand Requisition Response comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Demand_Requisition_Response lines of Baseline file and Current File....\n");
	if (($Baseline_Demand_Requisition_Response_has_no_data == 0) && ($Current_Demand_Requisition_Response_has_no_data == 0)) {

	@Baseline_Demand_Requisition_Response_list = sort by_unit_then_by_date @Baseline_Demand_Requisition_Response_list;
	@Current_Demand_Requisition_Response_list = sort by_unit_then_by_date @Current_Demand_Requisition_Response_list;
	
	$local_line_not_match_count = 0;
	   if (($Baseline_Demand_Requisition_Response_ep-$Baseline_Demand_Requisition_Response_sp) == ($Current_Demand_Requisition_Response_ep-$Current_Demand_Requisition_Response_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Demand_Requisition_Response_lines = $Baseline_Demand_Requisition_Response_ep - $Baseline_Demand_Requisition_Response_sp;
		for ($j = 0; $j <= $Max_Demand_Requisition_Response_lines; $j++){
			if ($Baseline_Demand_Requisition_Response_list[$j][1] ne $Current_Demand_Requisition_Response_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Requisition Response Unit Name Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Requisition_Response_list[$j][0]+1,    "  $Baseline_Demand_Requisition_Response_list[$j][1]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Requisition_Response_list[$j][0]+1,     "  $Current_Demand_Requisition_Response_list[$j][1]\n\n");	
			}
			if ($Baseline_Demand_Requisition_Response_list[$j][2] ne $Current_Demand_Requisition_Response_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Requisition Response Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Requisition_Response_list[$j][0]+1,    "  $Baseline_Demand_Requisition_Response_list[$j][2]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Requisition_Response_list[$j][0]+1,     "  $Current_Demand_Requisition_Response_list[$j][2]\n\n");	
			}
			if ($Baseline_Demand_Requisition_Response_list[$j][3] ne $Current_Demand_Requisition_Response_list[$j][3]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Requisition Response End Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Requisition_Response_list[$j][0]+1,    "  $Baseline_Demand_Requisition_Response_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Requisition_Response_list[$j][0]+1,     "  $Current_Demand_Requisition_Response_list[$j][3]\n\n");	
			}
			$Demand_Requisition_Response_difference = abs($Baseline_Demand_Requisition_Response_list[$j][4] - $Current_Demand_Requisition_Response_list[$j][4]);
			if ($Demand_Requisition_Response_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Demand Requisition Response Daily Rate Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Demand_Requisition_Response_list[$j][0]+1,    "  $Baseline_Demand_Requisition_Response_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Demand_Requisition_Response_list[$j][0]+1,     "  $Current_Demand_Requisition_Response_list[$j][4]\n\n");	
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		}

 
	   }else{
#		print ("Files do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }

	} elsif (($Baseline_Demand_Requisition_Response_has_no_data == 1) || ($Current_Demand_Requisition_Response_has_no_data == 1)) {
			if ($Baseline_Demand_Requisition_Response_has_no_data == 1) {
				print ($Baseline_file_name, 
				       "\nNo BASELINE Demand Requisition Response Data available for comparison\n");
			}
			if ($Current_Demand_Requisition_Response_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Demand Requisition Response Data available for comparison\n\n");
			}
	}

	#Refill Projection comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Refill_Projection lines of Baseline file and Current File....\n");
	if (($Baseline_Refill_Projection_has_no_data == 0) && ($Current_Refill_Projection_has_no_data == 0)) {

	@Baseline_Refill_Projection_list = sort by_unit_then_by_date @Baseline_Refill_Projection_list;
	@Current_Refill_Projection_list = sort by_unit_then_by_date @Current_Refill_Projection_list;
	
	$local_line_not_match_count = 0;
	   if (($Baseline_Refill_Projection_ep-$Baseline_Refill_Projection_sp) == ($Current_Refill_Projection_ep-$Current_Refill_Projection_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Refill_Projection_lines = $Baseline_Refill_Projection_ep - $Baseline_Refill_Projection_sp;
		for ($j = 0; $j <= $Max_Refill_Projection_lines; $j++){
			if ($Baseline_Refill_Projection_list[$j][1] ne $Current_Refill_Projection_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Projection Unit Name Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Projection_list[$j][0]+1,    "  $Baseline_Refill_Projection_list[$j][1]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Projection_list[$j][0]+1,     "  $Current_Refill_Projection_list[$j][1]\n\n");	
			}
			if ($Baseline_Refill_Projection_list[$j][2] ne $Current_Refill_Projection_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Projection Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Projection_list[$j][0]+1,    "  $Baseline_Refill_Projection_list[$j][2]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Projection_list[$j][0]+1,     "  $Current_Refill_Projection_list[$j][2]\n\n");	
			}
			if ($Baseline_Refill_Projection_list[$j][3] ne $Current_Refill_Projection_list[$j][3]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Projection End Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Projection_list[$j][0]+1,    "  $Baseline_Refill_Projection_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Projection_list[$j][0]+1,     "  $Current_Refill_Projection_list[$j][3]\n\n");	
			}
			$Refill_Projection_difference = abs($Baseline_Refill_Projection_list[$j][4] - $Current_Refill_Projection_list[$j][4]);
			if ($Refill_Projection_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Projection Daily Rate Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Projection_list[$j][0]+1,    "  $Baseline_Refill_Projection_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Projection_list[$j][0]+1,     "  $Current_Refill_Projection_list[$j][4]\n\n");	
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		}
 
	   }else{
#		print ("Files do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }
	
	} elsif (($Baseline_Refill_Projection_has_no_data == 1) || ($Current_Refill_Projection_has_no_data == 1)) {
			if ($Baseline_Refill_Projection_has_no_data == 1) {
				print ($Baseline_file_name, 
				       "\nNo BASELINE Refill Projection Data available for comparison\n");
			}
			if ($Current_Refill_Projection_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Refill Projection Data available for comparison\n\n");
			}
	}

	#Refill Projection Response comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Refill_Projection_Response lines of Baseline file and Current File....\n");
	if (($Baseline_Refill_Projection_Response_has_no_data == 0) && ($Current_Refill_Projection_Response_has_no_data == 0)) {

	@Baseline_Refill_Projection_Response_list = sort by_unit_then_by_date @Baseline_Refill_Projection_Response_list;
	@Current_Refill_Projection_Response_list = sort by_unit_then_by_date @Current_Refill_Projection_Response_list;
	
	$local_line_not_match_count = 0;
	   if (($Baseline_Refill_Projection_Response_ep-$Baseline_Refill_Projection_Response_sp) == ($Current_Refill_Projection_Response_ep-$Current_Refill_Projection_Response_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Refill_Projection_Response_lines = $Baseline_Refill_Projection_Response_ep - $Baseline_Refill_Projection_Response_sp;
		for ($j = 0; $j <= @Baseline_Refill_Projection_Response_Start_Date; $j++){
			if ($Baseline_Refill_Projection_Response_list[$j][1] ne $Current_Refill_Projection_Response_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Projection Response Unit Name Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Projection_Response_list[$j][0]+1,    "  $Baseline_Refill_Projection_Response_list[$j][1]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Projection_Response_list[$j][0]+1,     "  $Current_Refill_Projection_Response_list[$j][1]\n\n");	
			}
			if ($Baseline_Refill_Projection_Response_list[$j][2] ne $Current_Refill_Projection_Response_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Projection Response Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Projection_Response_list[$j][0]+1,    "  $Baseline_Refill_Projection_Response_list[$j][2]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Projection_Response_list[$j][0]+1,     "  $Current_Refill_Projection_Response_list[$j][2]\n\n");	
			}
			if ($Baseline_Refill_Projection_Response_list[$j][3] ne $Current_Refill_Projection_Response_list[$j][3]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Projection Response End Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Projection_Response_list[$j][0]+1,    "  $Baseline_Refill_Projection_Response_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Projection_Response_list[$j][0]+1,     "  $Current_Refill_Projection_Response_list[$j][3]\n\n");	
			}
			$Refill_Projection_Response_difference = abs($Baseline_Refill_Projection_Response_list[$j][4] - $Current_Refill_Projection_Response_list[$j][4]);
			if ($Refill_Projection_Response_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Projection Response Daily Rate Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Projection_Response_list[$j][0]+1,    "  $Baseline_Refill_Projection_Response_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Projection_Response_list[$j][0]+1,     "  $Current_Refill_Projection_Response_list[$j][4]\n\n");	
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		}

 
	   }else{
#		print ("Files do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }
	
	} elsif (($Baseline_Refill_Projection_Response_has_no_data == 1) || ($Current_Refill_Projection_Response_has_no_data == 1)) {
			if ($Baseline_Refill_Projection_Response_has_no_data == 1) {
				print ($Baseline_file_name, 
				       "\nNo BASELINE Refill Projection Response Data available for comparison\n");
			}
			if ($Current_Refill_Projection_Response_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Refill Projection Response Data available for comparison\n\n");
			}
	}	

	#Refill Requisition comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Refill_Requisition lines of Baseline file and Current File....\n");
	if (($Baseline_Refill_Requisition_has_no_data == 0) && ($Current_Refill_Requisition_has_no_data == 0)) {

	@Baseline_Refill_Requisition_list = sort by_unit_then_by_date @Baseline_Refill_Requisition_list;
	@Current_Refill_Requisition_list = sort by_unit_then_by_date @Current_Refill_Requisition_list;
	
	$local_line_not_match_count = 0;
	   if (($Baseline_Refill_Requisition_ep-$Baseline_Refill_Requisition_sp) == ($Current_Refill_Requisition_ep-$Current_Refill_Requisition_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Refill_Requisition_lines = $Baseline_Refill_Requisition_ep - $Baseline_Refill_Requisition_sp;
		for ($j = 0; $j <= $Max_Refill_Requisition_lines; $j++){
			if ($Baseline_Refill_Requisition_list[$j][1] ne $Current_Refill_Requisition_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Requisition Unit Name Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Requisition_list[$j][0]+1,    "  $Baseline_Refill_Requisition_list[$j][1]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Requisition_list[$j][0]+1,     "  $Current_Refill_Requisition_list[$j][1]\n\n");	
			}
			if ($Baseline_Refill_Requisition_list[$j][2] ne $Current_Refill_Requisition_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Requisition Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Requisition_list[$j][0]+1,    "  $Baseline_Refill_Requisition_list[$j][2]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Requisition_list[$j][0]+1,     "  $Current_Refill_Requisition_list[$j][2]\n\n");	
			}
			if ($Baseline_Refill_Requisition_list[$j][3] ne $Current_Refill_Requisition_list[$j][3]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Requisition End Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Requisition_list[$j][0]+1,    "  $Baseline_Refill_Requisition_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Requisition_list[$j][0]+1,     "  $Current_Refill_Requisition_list[$j][3]\n\n");	
			}
			$Refill_Requisition_difference = abs($Baseline_Refill_Requisition_list[$j][4] - $Current_Refill_Requisition_list[$j][4]);
			if ($Refill_Requisition_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Requisition Daily Rate Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Requisition_list[$j][0]+1,    "  $Baseline_Refill_Requisition_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Requisition_list[$j][0]+1,     "  $Current_Refill_Requisition_list[$j][4]\n\n");	
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		} 
	   }else{
#		print ("Files do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }

	} elsif (($Baseline_Refill_Requisition_has_no_data == 1) || ($Current_Refill_Requisition_has_no_data == 1)) {
			if ($Baseline_Refill_Requisition_has_no_data == 1) {
				print ($Baseline_file_name, 
				       "\nNo BASELINE Refill Requisition Data available for comparison\n");
			}
			if ($Current_Refill_Requisition_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Refill Requisition Data available for comparison\n\n");
			}
	}

	#Refill Requisition Response comparison
#	print ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
#	print ("Comparing Refill_Requisition_Response lines of Baseline file and Current File....\n");
	if (($Baseline_Refill_Requisition_Response_has_no_data == 0) && ($Current_Refill_Requisition_Response_has_no_data == 0)) {

	@Baseline_Refill_Requisition_Response_list = sort by_unit_then_by_date @Baseline_Refill_Requisition_Response_list;
	@Current_Refill_Requisition_Response_list = sort by_unit_then_by_date @Current_Refill_Requisition_Response_list;
	
	$local_line_not_match_count = 0;
	   if (($Baseline_Refill_Requisition_Response_ep-$Baseline_Refill_Requisition_Response_sp) == ($Current_Refill_Requisition_Response_ep-$Current_Refill_Requisition_Response_sp)){
#		print ("Files contain same number of lines of data, continuing on with line by line comparison....\n");
		$Max_Refill_Requisition_Response_lines = $Baseline_Refill_Requisition_Response_ep - $Baseline_Refill_Requisition_Response_sp;
		for ($j = 0; $j <= $Max_Refill_Requisition_Response_lines; $j++){
			if ($Baseline_Refill_Requisition_Response_list[$j][1] ne $Current_Refill_Requisition_Response_list[$j][1]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Requisition Response Unit Name Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Requisition_Response_list[$j][0]+1,    "  $Baseline_Refill_Requisition_Response_list[$j][1]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Requisition_Response_list[$j][0]+1,     "  $Current_Refill_Requisition_Response_list[$j][1]\n\n");	
			}
			if ($Baseline_Refill_Requisition_Response_list[$j][2] ne $Current_Refill_Requisition_Response_list[$j][2]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Requisition Response Start Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Requisition_Response_list[$j][0]+1,    "  $Baseline_Refill_Requisition_Response_list[$j][2]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Requisition_Response_list[$j][0]+1,     "  $Current_Refill_Requisition_Response_list[$j][2]\n\n");	
			}
			if ($Baseline_Refill_Requisition_Response_list[$j][3] ne $Current_Refill_Requisition_Response_list[$j][3]){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Requisition Response End Date Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Requisition_Response_list[$j][0]+1,    "  $Baseline_Refill_Requisition_Response_list[$j][3]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Requisition_Response_list[$j][0]+1,     "  $Current_Refill_Requisition_Response_list[$j][3]\n\n");	
			}
			$Refill_Requisition_Response_difference = abs($Baseline_Refill_Requisition_Response_list[$j][4] - $Current_Refill_Requisition_Response_list[$j][4]);
			if ($Refill_Requisition_Response_difference > $tolerance){
				$line_not_match_count++;
				$local_line_not_match_count++;
				print ("Difference # $line_not_match_count\n");
				print ("Refill Requisition Response Daily Rate Fields don't match\n\n");
				print ($Baseline_file_name, ":  Line# ",   $Baseline_Refill_Requisition_Response_list[$j][0]+1,    "  $Baseline_Refill_Requisition_Response_list[$j][4]\n");
				print ($Current_file_name, ":   Line# ",   $Current_Refill_Requisition_Response_list[$j][0]+1,     "  $Current_Refill_Requisition_Response_list[$j][4]\n\n");	
			}
		}
		if ($local_line_not_match_count == 0){
#			print ("All lines match!\n");
		}

 
	   }else{
#		print ("Files do not contain same number of lines of data, COMPARISON IS NOT POSSIBLE!!!\n");
		$data_groups_not_compared++;
	   }
	
	} elsif (($Baseline_Refill_Requisition_Response_has_no_data == 1) || ($Current_Refill_Requisition_Response_has_no_data == 1)) {
			if ($Baseline_Refill_Requisition_Response_has_no_data == 1) {
				print ($Baseline_file_name, 
				       "\nNo BASELINE Refill Requisition Response Data available for comparison\n");
			}
			if ($Current_Refill_Requisition_Response_has_no_data == 1) {
				print ($Current_file_name, 
				       "\nNo CURRENT Refill Requisition Response Data available for comparison\n\n");
			}
	}

     }

}
print ("\nA total of $line_not_match_count data fields do not match\n");
print ("A total of $data_groups_not_compared data group pairs could not be compared due to line number inequalities.\n");
print ("A total of $NUMBER_FILES_NOT_FOUND files were not found and could not be compared\n");

#Sort method definition
sub by_unit_then_by_date {
	
	#Sort by the Unit Name then by the Start Date then by End Date, then by value

	if ( ($a->[1]) ne ($b->[1]) ) {
		$a->[1] cmp $b->[1];
	} elsif ( ($a->[2]) ne ($b->[2]) ) {
		$a->[2] cmp $b->[2];
	} elsif ( ($a->[3]) ne ($b->[3]) ) {
		$a->[3] cmp $b->[3];
	} elsif ( ($a->[4]) ne ($b->[4]) ) {
		$a->[4] cmp $b->[4];
	}




#	if ( (($a->[2]) ne "") and (($b->[2]) ne "") ) {
#		$a->[1] cmp $b->[1]
#		or
#		$a->[2] cmp $b->[2];
#	} elsif ( (($a->[3]) ne "") and (($b->[3]) ne "") ) {
#		$a->[1] cmp $b->[1]
#		or
#		$a->[3] cmp $b->[3];
#	} elsif ( ((($a->[2]) ne "") and (($b->[2]) ne "")) and ((($a->[3]) ne "") and (($b->[3]) ne "")) ) {
#		$a->[1] cmp $b->[1];
#	}
	
}

#Subroutine to gather data fields from the Baseline and Current files
sub Gather_Data {
	my ($Start_Point, $End_Point, @Field_List) = @_;
	my @New_List = ();
	$Field_Count = @Field_List + 1;
 
	for ($i = $Start_Point; $i <= $End_Point; $i++) {

			if ($FILE_type eq "Current"){
				chop (@lines_current_file[$i]);
				split(/,/, @lines_current_file[$i]);
			} elsif ($FILE_type eq "Baseline") {
				chop (@lines_baseline_file[$i]);
				split(/,/, @lines_baseline_file[$i]);
			}

			#For a Field_Count of 4 plus one for line #
			if ($Field_Count == 5){
			@New_Row = ($i, @_[($Field_List[0])], @_[($Field_List[1])], @_[($Field_List[2])], @_[($Field_List[3])]);
			}

			#For a Field_Count of 5 plus one for line #
			if ($Field_Count == 6){
			@New_Row = ($i, @_[($Field_List[0])], @_[($Field_List[1])], @_[($Field_List[2])], @_[($Field_List[3])], @_[($Field_List[4])]);
			}
	
			push (@New_List, [@New_Row]);
	}

	return (@New_List);
}

