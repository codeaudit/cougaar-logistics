#! /usr/bin/perl.exe

# usage:  process_oplog_ammorate_sql.pl oplog_ammorate_p1.csv > oplog_ammorate_tmp.sql
#         mysql -u kratkiew -p mycoug_ammo < oplog_ammorate_tmp.sql

print <<EOF;
DROP TABLE oplog_ammorate_tmp;
CREATE TABLE oplog_ammorate_tmp (
  echelon varchar(9) default NULL,
  lin varchar(6) default NULL,
  dodic varchar(4) default NULL,
  optempo varchar(20) default NULL,
  rounds_per_day decimal(14,10) default NULL
) TYPE=MyISAM;

EOF

open(I, $ARGV[0]);
while(chop($line=<I>)) {
    next if $.==1; # skip first line
    @fields = split(/,/, $line);
    print "INSERT INTO oplog_ammorate_tmp VALUES (\'$fields[0]\',\'$fields[2]\',\'$fields[3]\',\'$fields[1]\',$fields[4]);\n";
}
close(I);

