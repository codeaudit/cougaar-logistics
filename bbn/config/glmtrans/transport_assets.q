Driver = oracle.jdbc.driver.OracleDriver
Database = jdbc:oracle:thin:@${org.cougaar.database}
Username = ${org.cougaar.database.user}
Password = ${org.cougaar.database.password} 

%SQLNonNSNAssetCreator
query = select ASSET_TYPE, QUANTITY, NOMENCLATURE \
        from transport_assets \
	where ORG_ID = :org



