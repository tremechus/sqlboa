
#Response
In general a response may look like this:

	{
		"status":<status code>",
		"code":<status code>,
		"message":<message>
	}

Each command may add additional properties for consumption as indicated later

###Status Code 
	0	ERROR
	1	OK

###Error Code (Only if in error state)
	0	Unknown
	1	Not connected
	2	Request too large
	3	Internal error
	4	Missing command

###Message (Optional, only if in error state)
This will be additional information about the error, and should
be consumable by the human user

#Handshake
###Request

	{
		"command":"handshake"
	}


###Response

	{
		"status":<status code>
	}


#Status  

###Request

	{
		"command":"status"
	}


###Response

	{
		"status":<status code>
	}


#List
List is a specialize query, it returns a list of items, no other content.  This is generally used to get simple data, like a list of available tables.  

###Request

	{
		"command":"list",
		"query":<sql string>
	}


###Response

	{
		"status":<status code>,
		"list":[...]
	}

#Query
Run a sql query on the database and return a full result set response.  Statements that modify data such as insert, delete, and update will return in the same format with a single 'Updated' column where row1col1val is the number of rows updated   

###Request

	{
		"command":"query",
		"query": <sql string>,
		"params":[bindparam1, ...],
		"page":<pagenum>,
		"perPage":<results per page>
	}


###Response

	{
		"status":<status code>,
		"columns":[colname1, colname2, ...],
		"totalCount": <totalcount>,
		"rows":[
			[row0col1val, row0col2val, ...],
			[row1col1val, row1col2val, ...]
		]
	}

#Exec
Run a sql statement on the database without an expected response, such as table creation  

###Request

	{
		"command":"exec",
		"sql": <sql string>
	}


###Response

	{
		"status":<status code>
	}
