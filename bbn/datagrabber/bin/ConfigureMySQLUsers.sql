use mysql;
grant all privileges on *.* to {yourUserName} identified by '{yourPassword}';
grant all privileges on *.* to {yourUserName}@localhost identified by '{yourPassword}';
grant all privileges on *.* to {yourUserName}@"%" identified by '{yourPassword}';
grant all privileges on *.* to {yourUserName}@{ hostname} identified by '{yourPassword}';
flush privileges;
