#! /usr/bin/perl.exe

# usage:  process_oplog_ammorate_raw_sql.pl OPLOG_AMMORATE.csv  > oplog_ammorate_p0.sql
#         mysql -u kratkiew -p mycoug_ammo < oplog_ammorate_p0.sql

print <<EOF;
DROP TABLE if exists oplog_ammorate_tmp0;
CREATE TABLE oplog_ammorate_tmp0 (
  echelon varchar(9) default NULL,
  posture varchar(1) default NULL,
  lin varchar(6) default NULL,
  dodic varchar(4) default NULL,
  rounds_per_day decimal(14,10) default NULL
) TYPE=MyISAM;

EOF

$echelon{A} = "COMPANY";
$echelon{B} = "BATTALION";
$echelon{D} = "DIVISION";
$echelon{C} = "BRIGADE";
$echelon{E} = "ABOVE DIV";
$echelon{F} = "THTR-ARMY";
$echelon{G} = "THTR-JNT";

$posture{A} = 1;
$posture{D} = 2;
$posture{P} = 3;
$posture{H} = 4;
$posture{S} = 5;
$posture{R} = 6;
$posture{U} = 7;

open(I, $ARGV[0]);
#print "echelon,posture,lin,dodic,rounds_per_day\n";
while(chop($line=<I>)) {
    next if $.==1; # skip first line
    @fields = split(/,/, $line);
    next if $fields[3] eq "BULK";
    print "INSERT INTO oplog_ammorate_tmp0 VALUES (\'$echelon{$fields[1]}\',\'$posture{$fields[2]}\',\'$fields[3]\',\'$fields[4]',$fields[5]);\n";
}
close(I);

