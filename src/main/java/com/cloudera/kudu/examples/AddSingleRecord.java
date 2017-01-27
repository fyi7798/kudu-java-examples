package com.cloudera.kudu.examples;

import com.google.common.collect.ImmutableList;

import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.CreateTableOptions;
import org.apache.kudu.client.Insert;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduScanner;
import org.apache.kudu.client.KuduSession;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.PartialRow;
import org.apache.kudu.client.RowResult;
import org.apache.kudu.client.RowResultIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This example creates a 'movies' table for movie information.
 */
public class AddSingleRecord {
  private static final Logger LOG = LoggerFactory.getLogger(AddSingleRecord.class);

  public static void main(String[] args) throws KuduException {
    if (args.length < 1) {
      throw new IllegalArgumentException("expected the Kudu master address as the first argument");
    }

    // Retrieve the master address from the first argument. The master address should
    // be 'kudu-training-CLUSTER_ID-1.gce.cloudera.com'.
    String masterAddress = args[0];

    // Create a KuduClient instance, the main entry point to a cluster. Through
    // the KuduClient, new tables can be created and existing tables can be opened.
    KuduClient client = new KuduClient.KuduClientBuilder(masterAddress).build();
    try {
    	
    	AddSingleRecord movieTable = new AddSingleRecord();
    		
    	if (!client.tableExists("movie")) {
    		movieTable.create(client);
    	}
    	
    	movieTable.populateSingleRow(client);
    	
    	movieTable.queryData(client);
    	
    } finally {
    	client.shutdown();
    }
   
  }
  
  private void create(KuduClient client) throws KuduException {

	  	LOG.info("in create");
	    // Create columns for the table.
	    ColumnSchema movieId = new ColumnSchema.ColumnSchemaBuilder("movie_id", Type.INT32).key(true).build();
	    ColumnSchema movieName = new ColumnSchema.ColumnSchemaBuilder("movie_name", Type.STRING).build();
	    ColumnSchema movieYear = new ColumnSchema.ColumnSchemaBuilder("movie_year", Type.STRING).build();

	    // The movie_genre is part of primary key so can do range partition on it.
	    ColumnSchema movieGenre = new ColumnSchema.ColumnSchemaBuilder("movie_genre", Type.STRING).build();

	    List<ColumnSchema> columns = ImmutableList.of(movieId, movieName, movieYear, movieGenre);

	    // Create a schema from the list of columns.
	    Schema schema = new Schema(columns);

	    // Specify hash partitioning over the movie_id column with 4 buckets.
	    CreateTableOptions createOptions =
	        new CreateTableOptions().addHashPartitions(ImmutableList.of("movie_id"), 4);

	    String tableName = "movie";
	    
	    // Create the table.
	    client.createTable(tableName, schema, createOptions);

	    LOG.info("Table '{}' created", tableName);
	    
	    //get schema for Table
	    Schema movieSchema = client.openTable("movie").getSchema();
	    LOG.info("Number of columns in table " + movieSchema.getColumnCount());
	   
	    for (ColumnSchema colSchema : movieSchema.getColumns()) {
	    	LOG.info("Columns in table " + colSchema.getName() + "is primary key " + colSchema.isKey());
	    }
  }
  
  private void populateSingleRow(KuduClient client) throws KuduException {
	  
	  KuduSession session = client.newSession();
	  
	  KuduTable table = client.openTable("movie");
	  
	  Insert insert = table.newInsert();
	  PartialRow row = insert.getRow();
	  row.addInt(0, 1);
	  row.addString(1, "Star Wars Force Awakens ");
	  row.addString(2, "2016");
	  row.addString(3,  "Sci-Fi");
	  
	  session.apply(insert);
	  session.flush();
	  session.close();
	  
	  LOG.info("added one record" );
	  
  }
  
  private void queryData(KuduClient client) throws KuduException {
	  

	  KuduTable table = client.openTable("movie");

	  KuduScanner kuduScanner = client.newScannerBuilder(table).build();
	  while (kuduScanner.hasMoreRows()) {
		  RowResultIterator rows = kuduScanner.nextRows();
		  while (rows.hasNext()) {
			  RowResult row = rows.next();
			  LOG.info("row value " + row.rowToString());
		  }
			  
	  }
	  
  }
    
  
}
