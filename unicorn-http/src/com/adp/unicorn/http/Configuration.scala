/******************************************************************************
 *                   Confidential Proprietary                                 *
 *          (c) Copyright ADP 2014, All Rights Reserved                       *
 ******************************************************************************/

package com.adp.unicorn.http

import com.adp.unicorn.store.cassandra.CassandraServer

object Configuration {
  val server = CassandraServer("127.0.0.1", 9160)
  val data = server.dataset("dbpedia")
  data cacheOn
  
  val skeletonTop =
    """<html>
      <head>
        <title>Unicorn Full Text Search</title>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
      </head>
      <body>
        <div id="content" style="margin-top:10px;">
        <form method="get" action="/search">                                        
        <input type="text" name="q" size="60"></input>  
        <input type="submit" value="Search" style="width:100px"></input> 
        </form>                                                                     
        <hr></hr>
    """

  val skeletonBottom = 
    """
        </div>
      </body>
    </html>"""  
}