//
//  tif_generation
//
//  Created by Greg Honig on 2013-10-03.
//  Copyright (c) 2013 __MyCompanyName__. All rights reserved.
//
//package com.honig.source
@Grab(group='commons-net', module='commons-net', version='2.0')
import org.apache.commons.net.ftp.FTPClient
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
 
    // def src = "/u/1stChoice/Signatures/eek.datatrac.com/palm/"+ monthYear+ '/'+bmp+'.bmp'
    def src = "/Volumes/SeagateBackupPlusDrive/1stChoice/eek.datatrac.com/palm/"+ monthYear+ '/'+bmp+'.bmp'
        
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

def TNGCity = [ "220":"ATL", 
                "223":"SAC",
                "223":"LAX",
                "223":"ON2",
                "125":"RDS",
				"210":"JAK",
				"224":"SLC",
				"226":"PHX",
				"230":"TEX"]

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

db.eachRow('SELECT * FROM cops_reporting.TNGVIEW WHERE actual_service_date IN (\''+date+'\') AND signature_file_name <> \'\'') {
    delivery -> 
    if (bol_number != delivery.bol_number){
        bol_number = delivery.bol_number
        deliveries[delivery.bol_number] = new Stop() 
        deliveries[delivery.bol_number].actualServiceDate = delivery.actual_service_date
        deliveries[delivery.bol_number].customerReference = delivery.customer_reference
        if( delivery.line_item_reference != ''){
            deliveries[delivery.bol_number].lineItemReference = delivery.line_item_reference    
        }
        deliveries[delivery.bol_number].branchId = delivery.branch_id
        deliveries[delivery.bol_number].bolNumber = delivery.bol_number.toString().padLeft(7,'0')
        deliveries[delivery.bol_number].stopName = delivery.stop_name
        deliveries[delivery.bol_number].stopAddress = delivery.stop_address
        deliveries[delivery.bol_number].stopCity = delivery.stop_city
        deliveries[delivery.bol_number].stopState = delivery.stop_state
        deliveries[delivery.bol_number].stopZipPostalCode = delivery.stop_zip_postal_code
        deliveries[delivery.bol_number].stopSignature = delivery.stop_signature 
        deliveries[delivery.bol_number].actualArrivalTime = delivery.actual_arrival_time
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
        
        def filename = delivery.bol_number
        if( filename == null ){
            filename = delivery.branch_id
        }
            
        // println "Here "+ deliveries[delivery.bol_number].signatureFileName
        
        deliveries[delivery.bol_number].pdfFileName = filename
        deliveries[delivery.bol_number].tiffDir = tifDir + deliveries[delivery.bol_number].getDeliveryYear() + '/' + deliveries[delivery.bol_number].getDeliveryMonth()  + '/'
        deliveries[delivery.bol_number].tiffFileName =  filename
        
    }
    if( delivery.item_number == ''){
        deliveries[delivery.bol_number].items.push('blank') 
    }else{
        deliveries[delivery.bol_number].items.push(delivery.item_number)
    }
       
    
}

def delList = deliveries.keySet().toList()
//println delList

def sourceTemplate = this.class.getResource("TNGTemplate.gtpl")


delList.each(){ delkey -> 
    
/*    println deliveries[delkey].stopName
    println deliveries[delkey].stopAddress
    println deliveries[delkey].stopCity
*/    //println "\t"
    //println deliveries[delkey].items 
    
    filename = deliveries[delkey].bolNumber 
    if(deliveries[delkey].signatureFileName.equals("Error")){
        filename = filename + 'bad' 
    }
    println filename
    
    def binding = [tng:deliveries[delkey]]
    def engine = new SimpleTemplateEngine()
    def template = engine.createTemplate(sourceTemplate)
    
    def tmp = File.createTempFile("template",".tmp", new File("tmp"))
    tmp.deleteOnExit()
    
    def fos = new FileOutputStream(tmp)
    fos << template.make(binding).toString().replaceAll("&", "&amp;")
    
    pdfOutFile = new FileOutputStream(new File('pdf/'+filename+'.pdf'))
    
    ITextRenderer renderer = new ITextRenderer() 
    //println tmp.path
    renderer.setDocument(tmp) 
    renderer.layout() 
    renderer.createPDF(pdfOutFile)
     
    pdfOutFile.close() 
    
    //tmp.delete()
   	
    //println template.toString()
}

def pdfFiles = new File('pdf').listFiles()

def numberOfFiles = pdfFiles.size()
def modulus1 =1
if( numberOfFiles > 5 ){
	modulus1 = (Integer)(numberOfFiles/5)
}
def percentDone = 0 
def procCount = 0 
List tifFileNames = []
//print pd
pdfFiles.each{ file ->
    //println file.name.tokenize('.').first() 
    pdfName = 'pdf/'+file.name 
    tifFileNames.push(file.name.tokenize('.').first() + '.tif')
    tifName = tifDir +splitDate[2]+'/'+splitDate[0] + '/' + file.name.tokenize('.').first() + '.tif'
    def tiffDirectoryName = tifDir +splitDate[2]+'/'+splitDate[0]
    def tiffDirectory = new File( tiffDirectoryName ) 
    tiffDirectory.mkdirs()
    
    cmd = "convert -density 288 -compress Group4 "+ pdfName +" " + tifName; 
    def proc = cmd.execute()
    if(! (procCount % 5 )){ 
        proc.waitFor()
    }
    if(! (procCount % modulus1) )
    {
        print "---"+percentDone+"% -----"
        percentDone = percentDone + 20 
    }
    //proc.waitFor()
    // file.delete()
    procCount++ 
}

/**
println("About to connect. to Source ...");

ftp = new FTPClient() 
server = "ftp.sourceinterlink.com"
ftp.connect( server ) 
ftp.login('1CC', 'StLouis1CC') 

println "Connected to $server. $ftp.replyString" 

//ftp.changeWorkingDirectory('/') 

tifFileNames.each{ tiffile -> 
	file = new File( tifDir+ '/' + tiffile )
	file.withInputStream{ fis -> ftp.storeFile( file.name, fis ) }
	println "$file.name: $ftp.replyString" 
}

ftp.disconnect() 
**/

/*tifFileNames.each{ fileName -> 
    println fileName 
    tifDirPath = Paths.get( tifDir+'/'+ fileName )
    if( Files.exists(tifDirPath)){
        tifCurrentPath = Paths.get( tifCurrentDir + '/' + fileName )
/***        try{
        
            Files.copy(tifDirPath,tifCurrentPath)    

        }catch (IOException e){
            println "Unsuccessfully tried to copy file" + fileName
        }

        def copyCommand  = 'cp ' + tifDir + '/' + fileName + ' ' + tifCurrentDir + '/' + 
        def proc = copyCommand.execute()
        proc.waitFor()
        println copyCommand   
    }
    
}**/
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


/*indexFile220 = new File(tiffCurrentDir + '/ATL/Indexes.txt')
indexFile223 = new File(tiffCurrentDir + '/SAC/Indexes.txt')
*/
indexFile404 = new File(tiffCurrentDir + '/404/Indexes.txt')

delList.each{ delkey -> 
	def tifDirPathString = deliveries[delkey].tiffDir + deliveries[delkey].tiffFileName + '.tif'
	println tifDirPathString + ' here'
    tifDirPath = Paths.get( tifDirPathString )
    if( Files.exists(tifDirPath)){
        tifCurrentPath = Paths.get( tiffCurrentDir + '/' + deliveries[delkey].tiffFileName + '.tif' )
// 		try{
//		
//         	Files.copy(tifDirPath,tifCurrentPath, StandardCopyOption.REPLACE_EXISTING)
//        }catch(IOException e){
//#         	println "Unsuccessfully tried to copy file" + deliveries[delkey].tiffFileName
//#         }
		 def copyCommand = 'cp '+ tifDir + splitDate[2]+'/'+splitDate[0] + '/' + deliveries[delkey].tiffFileName + '.tif ' + tiffCurrentDir + '/' + TNGCity[deliveries[delkey].lineItemReference] + '/'
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
             indexFile[deliveries[delkey].lineItemReference] << indexOutput << '\r\n'

		 } else {
		     indexOutput = '\"' + deliveries[delkey].lineItemReference +'_Images\",' + '\"' + deliveries[delkey].lineItemReference +'_Images\",' +
                    '\"DOCNO\",' + '\"' + deliveries[delkey].customerReference + '\",' +
                    '\"SHIPTO\",' + '\"' + deliveries[delkey].bolNumber + '\",' +
                    '\"DATE\",' + '\"' + todayString + '\",' + 
                    '\"ftp2.tng.com \\3PL_images\\'+ deliveries[delkey].lineItemReference +'\\' + deliveries[delkey].bolNumber + '.tif\",' + 
                    '\"D\"'
      	     indexFile404 << indexOutput << '\r\n'
		 }
    
    }else {
    	printTifPathString = deliveries[delkey].tiffDir + '/' + deliveries[delkey].tiffFileName + '.tif' 
    	
    	println deliveries[delkey].tiffFileName
    	println deliveries[delkey].tiffDir
	    println printTifPathString
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
	String tiffDir
	String tiffFileName
	String pdfFileName
    Integer customerNo
	String createdBy 
	String routeId
	String lineItemReference
	List items = []
	
	
	def getDeliveryYear(){
	
		if( actualServiceDate != null ){
			return actualServiceDate.year + 1900
		}else if( routeDate != null ){
			return routeDate.year + 1900 
		}else{
			return '2014'
		}
	}	
	
	def getDeliveryMonth(){
		if( actualServiceDate != null ){
			return (actualServiceDate.month + 1).toString().padLeft(2,'0') 
		}else if( routeDate != null ){
			return (routeDate.month  + 1).toString().padLeft(2,'0')
		}else{
			return '00'
		}
	}
		
	
}







