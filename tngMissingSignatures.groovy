//
//  sourceMissingSignatures
//
//  Created by Greg Honig on 2014-02-10.
//  Copyright (c) 2014 __MyCompanyName__. All rights reserved.
//
//
//package com.honig.source

import groovy.sql.Sql
import groovy.text.Template
import groovy.text.SimpleTemplateEngine
import groovy.util.CliBuilder
import org.xhtmlrenderer.pdf.ITextRenderer
import java.text.SimpleDateFormat 
import java.nio.file.*
import java.nio.file.Files
import java.nio.file.Paths

import java.nio.file.StandardCopyOption.*
/*
dbSource = new org.postgresql.ds.PGPoolingDataSource()
dbSource.database = "jdbc:postgresql://localhost:5432/cops_reporting"
dbSource.user = "postgres" 
dbSource.password = "ahab31" 
*/

def convertBmp(bmp,monthYear){
 
    def src = '/u/1stChoice/Signatures/MissingSignatures/eek.datatrac.com/palm/'+ monthYear+ '/'+bmp+'.bmp'
    def signatureFile = new File(src)
 
    if( signatureFile.exists()){
        cmd = "convert "+src+" bmp/"+bmp+".jpg"
        def process = cmd.execute()
        process.waitFor() 
        def exitValue = process.exitValue() 
        if( process.exitValue()){
            //println 'error error error error' + process.err.text 
            return "error"
            
        } else { 
            // println src
            return "bmp/"+bmp+".jpg"
        }

    }else{
        return ''
    }
}
def missSigBaseDir = "/u/1stChoice/Signatures/MissingSignatures"

                    
def cli = new CliBuilder(usage: 'groovy tngMissingSignatures.groovy [options]',
                         header: 'Options:')
cli.h("Print this message")
cli.d(args:1, argName:'directory','Give a directory where .jpgs are stored. ' )
// cli.i(args:1, argName:'image', 'Name of image file to process.')
cli.l( args:1, argName:'log Name', 'Give a name for the log file. ')
def options = cli.parse(args)
// println options.s

if(!options){
    cli.usage() 
    return
}

if( options.h ) {
    
    cli.usage()
    return
    
}
if(options.d){
    directory = options.d
    println directory 
    def missingSigList = new HashSet()
    imageFiles = new File(directory).listFiles()
    
}

def importReportingLog
def goodRecordsLog
if(options.l){
	logFileName = options.l 
	goodRecordsLog = new File( logFileName + '.process' ) 
	importReportingLog = new File( logFileName + '.report' ) 
}else{
	def today = new Date()
	sdf = new SimpleDateFormat("yyyy-MM-dd")
	goodRecordsLog = new File( 'bol' + sdf.format(today) +'.process' )
	importReportingLog = new File( 'bol' + sdf.format(today) +'.report' )
}
	



//db = Sql.newInstance("jdbc:postgresql://localhost:5432/cops_reporting","postgres","ahab31",
//    "org.postgresql.Driver") 
    
//db = Sql.newInstance("jdbc:postgresql://dtrac02:5432/cops_reporting","postgres","ahab31",
db = Sql.newInstance("jdbc:postgresql://192.168.30.10:5432/cops_reporting","postgres","ahab31",
        "org.postgresql.Driver")
def deliveries = [:] 
def items = new HashSet() 
def bol_number = -1
def totalFiles = imageFiles.size()
def successfulUpdates = 0 
def duplicateUpdates = 0 
def otherProblemUpdates = 0 


imageFiles.each{ file -> 
    bolNum = file.name.tokenize('.').first()    
    db.eachRow('SELECT system_no, company_no, unique_id_no, bol_number, signature_file_name, actual_service_date, route_date FROM cops_reporting.distribution_stop_information WHERE bol_number=(\''+bolNum+'\') AND (customer_no = \'91000\')'){
        delivery ->
        if( delivery.actual_service_date == null && delivery.route_date == null ){
            importReportingLog << file.name + ": No good dates found." <<"\n"
            otherProblemUpdates++
            
        }else if( delivery.signature_file_name == null && (delivery.actual_service_date != null || delivery.route_date != null) ){
            //println file.name + "\n"
            def deliveryYear
            def deliveryMonth 
            if( delivery.actual_service_date != null ){
                deliveryYear = delivery.actual_service_date.year + 1900 
                deliveryMonth = (delivery.actual_service_date.month +1).toString().padLeft(2,'0')               
            }else{
                deliveryYear = delivery.route_date.year + 1900 
                deliveryMonth = (delivery.route_date.month +1).toString().padLeft(2,'0')
            }
            
            def missSigDir = missSigBaseDir +'/'+ deliveryYear +'/'+ deliveryMonth
            def missSigFolder = new File(missSigDir)
            if(!missSigFolder.exists()){ missSigFolder.mkdirs() }
            manSigPath = Paths.get((missSigDir+'/MAN_'+file.name))
            if( Files.notExists(manSigPath)){
                Files.copy(Paths.get(file.toURI()), Paths.get((missSigDir+'/MAN_'+file.name)))
            }
            signatureFileName = 'MAN_'+file.name 
            db.executeUpdate('UPDATE cops_reporting.distribution_stop_information set signature_file_name = ? where system_no = ? and company_no = ? and unique_id_no = ? and bol_number = ?', [signatureFileName,delivery.system_no,delivery.company_no,delivery.unique_id_no,delivery.bol_number] )

            importReportingLog << file.name +":"+delivery.bol_number + "\t" + delivery.system_no + "\t" + delivery.company_no + "\t" + delivery.unique_id_no + "\t" + signatureFileName << "\n"
            goodRecordsLog << delivery.bol_number + "\t" + delivery.system_no + "\t" + delivery.company_no + "\t" + delivery.unique_id_no + "\t" + signatureFileName << "\n"
        	successfulUpdates++
            file.delete()
        } else if( delivery.signature_file_name != null ){
        	importReportingLog << file.name+":"+ delivery.bol_number + ":" + "already has a signature file name of:" + delivery.signature_file_name << "\n"
			duplicateUpdates++
        	file.delete()
        } else if( ( delivery.signature_file_name ) ==  ('MAN_'+ file.name )){
        
        	importReportingLog << file.name + ":"+delivery.bol_number+" already processed... skipping" << "\n"
        	duplicateUpdates++
        	file.delete()
        } else {
        	importReportingLog << file.name + ":"+delivery.bol_number+ " probably doesn't exist" << "\n"
        	otherProblemUpdates++
        
        }
        
        

    }
    
}

importReportingLog << "Total Files to process: $totalFiles"  << "\n"
importReportingLog << "Successful Updates: $successfulUpdates"  << "\n"
importReportingLog << "Duplicate Updates: $duplicateUpdates"  << "\n"
importReportingLog << "Other Issues: $otherProblemUpdates"  << "\n"

