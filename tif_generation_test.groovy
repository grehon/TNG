//
//  tif_generation_test
//
//  Created by Greg Honig on 2013-12-02.
//  Copyright (c) 2013 __MyCompanyName__. All rights reserved.
//
//
//  tif_generation
//
//  Created by Greg Honig on 2013-10-03.
//  Copyright (c) 2013 __MyCompanyName__. All rights reserved.
//
//package com.honig.source

import groovy.sql.Sql
import groovy.text.Template
import groovy.text.SimpleTemplateEngine
import groovy.util.CliBuilder
import org.xhtmlrenderer.pdf.ITextRenderer
import java.text.SimpleDateFormat 
/*
dbSource = new org.postgresql.ds.PGPoolingDataSource()
dbSource.database = "jdbc:postgresql://localhost:5432/cops_reporting"
dbSource.user = "postgres" 
dbSource.password = "ahab31" 
*/

def convertBmp(bmp,monthYear){
 
    def src = '/Volumes/SpinDrive1/1stChoice/eek.datatrac.com/eek.datatrac.com/palm/'+ monthYear+ '/'+bmp+'.bmp'
    def signatureFile = new File(src)
    println src
 
    if( signatureFile.exists()){
        
        cmd = "convert "+src+" bmp/"+bmp+".jpg"
        def process = cmd.execute()
        process.waitFor() 
        def exitValue = process.exitValue() 
        if( exitValue ){
            println 'error error error error'
            println process.err.text
            return process.err.text
            
        } else { 
            // println src
            return "bmp/"+bmp+".jpg"
        }

    }else{
        return ''
    }
}


def bitmapReturnVal = convertBmp('13284550_Sig', 1113 )
def bitmapReturnVal2 = convertBmp('00000383322017740237', 1113 )


