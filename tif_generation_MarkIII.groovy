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
 
    def src = "/u/1stChoice/Signatures/eek.datatrac.com/palm/"+ monthYear+ '/'+bmp+'.bmp'
    def signatureFile = new File(src)
 
    if( signatureFile.exists()){
        cmd = "convert "+src+" bmp/"+bmp+".jpg"
        def process = cmd.execute()
        process.waitFor() 
        def exitValue = process.exitValue() 
        if( process.exitValue()){
            println 'error error error error' + process.err.text 
            return "error"
            
        } else { 
            // println src
            return "bmp/"+bmp+".jpg"
        }

    }else{
        return ''
    }
}
                
def TNGCity = [ "220":"ATL", 
                "223":"SAC",
                "223":"LAX",
                "223":"ON2",
                "125":"RDS",
				"210":"JAK",
				"224":"SLC",
				"226":"PHX",
				"230":"TEX"]
                    
def cli = new CliBuilder(usage: 'groovy tif_generation.groovy [options]',
                         header: 'Options:')
cli.h("Print this message")
cli.d(args:1, argName:'date','Give date in MM/DD/YYYY format' )
cli.s(args:1, argName:'dateShift', 'Give number of days back to run report for i.e. -7 for 7 days ago')

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
    date = options.d
    println date
}
if(options.s){
    println options.s
    def daysToSubtract = options.s.toInteger()
    def today = new Date()
    sdf = new SimpleDateFormat("MM/dd/yyyy")
    queryDate = today.minus(daysToSubtract)
    date = sdf.format(queryDate)
    todayString = sdf.format(today)
    println date
}

def splitDate = date.split('/')
def shortYear = splitDate[2].getAt(2..3)
// println shortYear

// println splitDate[0] + ' ' + splitDate[2]
//def tiffCurrentDir = '/u/1stChoice/Share/TNG/tiffCurrent' 
def tiffCurrentDir = '/Volumes/SeagateBackupPlusDrive/1stChoice/TNG/tiffCurrent'
def folder1 = new File(tiffCurrentDir)
if( !folder1.exists() ){
    folder1.mkdirs()
}
//def tifDir = '/u/1stChoice/Share/TNG/tiff/'
def tifDir = '/Volumes/SeagateBackupPlusDrive/1stChoice/TNG/tiff/'
def folder = new File(tifDir)
if( !folder.exists()){
    folder.mkdirs()
}

//db = Sql.newInstance("jdbc:postgresql://localhost:5432/cops_reporting","postgres","ahab31",
//    "org.postgresql.Driver") 
    
db = Sql.newInstance("jdbc:postgresql://192.168.30.10:5432/cops_reporting","cops_reporting","1stCh0ic3",
        "org.postgresql.Driver")

def deliveries = [:] 
def items = new HashSet() 
def bol_number = -1
db.eachRow('SELECT * FROM cops_reporting.TNGVIEW WHERE route_date in (\''+date+'\') and actual_service_date is null and signature_file_name <> \'\' and signature_file_name not like \'MAN_%\'' ){ 
//db.eachRow('SELECT * FROM cops_reporting.SOURCEVIEW WHERE actual_service_date IN (\''+date+'\') AND signature_file_name <> \'\'') {
    delivery -> 
    if (bol_number != delivery.bol_number){
        bol_number = delivery.bol_number
        deliveries[delivery.bol_number] = new Stop() 
        deliveries[delivery.bol_number].actualServiceDate = delivery.actual_service_date
        deliveries[delivery.bol_number].routeDate = delivery.route_date
        deliveries[delivery.bol_number].customerReference = delivery.customer_reference
        deliveries[delivery.bol_number].lineItemReference = delivery.line_item_reference
        deliveries[delivery.bol_number].branchId = delivery.branch_id
        deliveries[delivery.bol_number].bolNumber = delivery.bol_number.toString().padLeft(7,'0')
        deliveries[delivery.bol_number].stopName = delivery.stop_name
        deliveries[delivery.bol_number].stopAddress = delivery.stop_address
        deliveries[delivery.bol_number].stopCity = delivery.stop_city
        deliveries[delivery.bol_number].stopState = delivery.stop_state
        deliveries[delivery.bol_number].stopZipPostalCode = delivery.stop_zip_postal_code
        deliveries[delivery.bol_number].stopSignature = delivery.stop_signature 
        deliveries[delivery.bol_number].actualArrivalTime = delivery.actual_arrival_time
		
		def bmpFileDir = new File("/Volumes/SeagateBackupPlusDrive/1stChoice/eek.datatrac.com/")
		
		deliveries[delivery.bol_number].signatureFileName = splitDate[0] + splitDate[2] + '/' + delivery.signature_file_name
/*** 		signatureFileNameMatcher = "${delivery.signature_file_name}.*" 
# 		println signatureFileNameMatcher
# 		bmpFileDir.eachFileMatch( ~/${signatureFileNameMatcher}/ ){
# 			file -> 
# 			
# 			println file.canonicalPath + ' ' + file.name + ' ' + file.directory
# 		}
# 	
**/	
			
/*** 		if( Files.exists( signatureFilePath = Paths.get( splitDate[0] + splitDate[2] + '/' + delivery.signature_file_name ) ))
		{
# 	        deliveries[delivery.bol_number].signatureFileName = splitDate[0] + splitDate[2] + '/' + delivery.signature_file_name
# 	        
# 	    }else if( (splitDate[0] < 12 ) && Paths.get( (splitDate[0].toInteger() + 1).toString() + splitDate[2] + '/' + delivery.signature_file_name )){
# 	    	deliveries[delivery.bol_number].signatureFileName = (splitDate[0].toInteger() + 1).toString() + splitDate[2] + '/' + delivery.signature_file_name
# 	    }else if( Paths.get( '01' + ( splitDate[2].toInteger() + 1 ).toString() + '/' + delivery.signature_file_name ) ){
# 	    	deliveries[delivery.bol_number].signatureFileName = '01' + ( splitDate[2].toInteger() + 1 ).toString() + '/' + delivery.signature_file_name
# 	    }else{ 
# 	    	deliveries[delivery.bol_number].signatureFileName = splitDate[0] + splitDate[2] + '/' + delivery.signature_file_name
# 	    }
***/
	    deliveries[delivery.bol_number].signatureFileName = splitDate[0] + splitDate[2] + '/' + delivery.signature_file_name
        deliveries[delivery.bol_number].customerNo = delivery.customer_no
        deliveries[delivery.bol_number].createdBy = delivery.datetime_created
        deliveries[delivery.bol_number].routeId = delivery.route_code
        //println deliveries[delivery.bol_number].stopName
        bitmapReturnVal = convertBmp(delivery.signature_file_name,splitDate[0] + shortYear )
        if( bitmapReturnVal =~ /bmp/ ){
            deliveries[delivery.bol_number].signatureFileName = bitmapReturnVal
        }else{
            deliveries[delivery.bol_number].signatureFileName = "Error"
            println bitmapReturnVal
            
        }
            
        // println "Here "+ deliveries[delivery.bol_number].signatureFileName
        
    }
    deliveries[delivery.bol_number].items.push(delivery.item_number)    
    
}

def delList = deliveries.keySet().toList()
//println delList

def sourceTemplate = this.class.getResource("SourceTemplate4.gtpl")

delList.each(){ delkey -> 
    
/*    println deliveries[delkey].stopName
    println deliveries[delkey].stopAddress
    println deliveries[delkey].stopCity
*/    //println "\t"
    //println deliveries[delkey].items 
    
    filename = deliveries[delkey].bolNumber
    if(deliveries[delkey].signatureFileName.equals("Error")){
        filename = 'bad/'+filename 
    }
    //println filename
    
    def binding = [source:deliveries[delkey]]
    def engine = new SimpleTemplateEngine()
    def template = engine.createTemplate(sourceTemplate)
    
    def tmp = File.createTempFile("template",".tmp", new File("tmp"))
    
    def fos = new FileOutputStream(tmp)
    fos << template.make(binding).toString().replaceAll("&", "&amp;")
    
    pdfOutFile = new FileOutputStream(new File('pdf/'+filename+'.pdf'))
    
    ITextRenderer renderer = new ITextRenderer() 
    //println tmp.path
    renderer.setDocument(tmp) 
    renderer.layout() 
    renderer.createPDF(pdfOutFile)
     
    pdfOutFile.close() 
    
    tmp.delete()
    
    //println template.toString()
}

def pdfFiles = new File('pdf').listFiles()

def numberOfFiles = pdfFiles.size()
def modulus1 = (Integer)(numberOfFiles/5)
def percentDone = 0 
def procCount = 0 
List tifFileNames = []
//print pd
pdfFiles.each{ file ->
    //println file.name.tokenize('.').first() 
    pdfName = 'pdf/'+file.name 
    tifFileNames.push(file.name.tokenize('.').first() + '.tif')
    tifName = tifDir + '/' + file.name.tokenize('.').first() + '.tif'
    cmd = "convert -density 288 -compress Group4 "+ pdfName +" " + tifName; 
    def proc = cmd.execute()
    if(! (procCount % 5 )){ 
        proc.waitFor()
    }
    if((modulus1 != 0) && ! (procCount % modulus1) )
    {
        print "---"+percentDone+"%---"
        percentDone = percentDone + 20 
    }
    //proc.waitFor()
    // file.delete()
    procCount++ 
}


def indexFile = [:]

TNGCity.each(){ companyKey, companyAlias ->  
	def indexFileCompanyDirectory = tiffCurrentDir + '/' + companyAlias
	def indexFileCompanyDirectoryFile = new File(indexFileCompanyDirectory)
	if( !indexFileCompanyDirectoryFile.exists()){
    	indexFileCompanyDirectoryFile.mkdirs()
	}
	def indexFilename = indexFileCompanyDirectory + '/Indexes.txt' 
	indexFile[companyKey] = new File(indexFilename)
}

//indexFile220 = new File(tiffCurrentDir + '/ATL/Indexes.txt')
//indexFile223 = new File(tiffCurrentDir + '/SAC/Indexes.txt')
indexFile404 = new File(tiffCurrentDir + '/404/Indexes.txt')

delList.each{ delkey -> 

    tifDirPath = Paths.get( deliveries[delkey].tiffDir + '/' + deliveries[delkey].tiffFileName + '.tif' )
    if( Files.exists(tifDirPath)){
        tifCurrentPath = Paths.get( tiffCurrentDir + '/' + deliveries[delkey].tiffFileName + '.tif' )
// 		try{
//		
//         	Files.copy(tifDirPath,tifCurrentPath, StandardCopyOption.REPLACE_EXISTING)
//        }catch(IOException e){
//#         	println "Unsuccessfully tried to copy file" + deliveries[delkey].tiffFileName
//#         }
		 def copyCommand = 'cp '+ deliveries[delkey].tiffDir + deliveries[delkey].tiffFileName + '.tif ' + tiffCurrentDir + TNGCity[deliveries[delkey].lineItemReference] + '/'
		 def proc = copyCommand.execute() 
		 proc.waitFor()
		 
		 println copyCommand 
		 println todayString 

    	 if (TNGCity.containsKey(deliveries[delkey].lineItemReference )){ 
             indexOutput = '\"' + deliveries[delkey].lineItemReference +'_Images\",' + '\"' + deliveries[delkey].lineItemReference +'_Images\",' +
                   '\"DOCNO\",' + '\"' + deliveries[delkey].customerReference + '\",' +
                   '\"SHIPTO\",' + '\"' + deliveries[delkey].bolNumber + '\",' +
                   '\"DATE\",' + '\"' + todayString + '\",' + 
                   '\"ftp2.tng.com \\3PL_images\\'+ deliveries[delkey].lineItemReference+ '\\' + deliveries[delkey].bolNumber + '.tif\",' + 
                   '\"D\"'
             indexFile[deliveries[delkey].lineItemReference] << indexOutput << '\n'

		 } else {
		     indexOutput = '\"' + deliveries[delkey].lineItemReference +'_Images\",' + '\"' + deliveries[delkey].lineItemReference +'_Images\",' +
                    '\"DOCNO\",' + '\"' + deliveries[delkey].customerReference + '\",' +
                    '\"SHIPTO\",' + '\"' + deliveries[delkey].bolNumber + '\",' +
                    '\"DATE\",' + '\"' + todayString + '\",' + 
                    '\"ftp2.tng.com \\3PL_images\\'+ deliveries[delkey].lineItemReference +'\\' + deliveries[delkey].bolNumber + '.tif\",' + 
                    '\"D\"'
      	     indexFile404 << indexOutput << '\n'
		 }

 
    
    }    
       
}



pdfFiles.each{ file ->
    
    file.delete()
}




class Stop {
    Date actualServiceDate
    Date routeDate
    String customerReference    
    String branchId
    String bolNumber
    String stopName
	String stopAddress
	String stopCity
	String stopState
	String stopZipPostalCode
	String stopSignature
	Date actualArrivalTime
	String signatureFileName
    Integer customerNo
	String createdBy 
	String routeId
	String lineItemReference
	List items = []  	
	
}
