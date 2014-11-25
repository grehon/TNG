//
//  tif_generation_man_sig_markII
//
//  Created by Greg Honig on 2014-02-18.
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
import java.nio.file.StandardCopyOption
import java.nio.file.CopyOption 
import java.nio.file.Path

import java.nio.file.StandardCopyOption.*
/*
dbSource = new org.postgresql.ds.PGPoolingDataSource()
dbSource.database = "jdbc:postgresql://localhost:5432/cops_reporting"
dbSource.user = "postgres" 
dbSource.password = "ahab31" 
*/

def convertBmp(bmp,monthYear){
 
    def src = "/u/1stChoice/Signatures/MissingSignatures/eek.datatrac.com/palm/"+ monthYear+ '/'+bmp+'.bmp'
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
                "125":"RDS",
				"210":"JAK",
				"224":"SLC",
				"226":"PHX",
				"230":"TEX"]
def missSigBaseDir = "/u/1stChoice/Signatures/MissingSignatures"
                    
def cli = new CliBuilder(usage: 'groovy tif_generation.groovy [options]',
                         header: 'Options:')
cli.h("Print this message")
cli.d(args:1, argName:'date','Give date in MM/DD/YYYY format' )
cli.s(args:1, argName:'dateShift', 'Give number of days back to run report for i.e. -7 for 7 days ago')
cli.b(args:1, argName:'bolFile', 'FileName with bol number in the first column')
def options = cli.parse(args)
// println options.s
def todayString
def bolList = new ArrayList()
// filenames with directories. 
def fileList = new HashSet()

if(!options){
    cli.usage() 
    return
}

if( options.h ) {
    
    cli.usage()
    return
    
}
if(options.b){
    bolFileName = options.b 
    bolFile = new File(bolFileName)
    bolFile.splitEachLine('\t'){ entry ->
        bolList.add(entry[0])
        println entry[0]
    }
}


if(options.d){
    date = options.d
    println date
}


def today = new Date()
sdf = new SimpleDateFormat("MM/dd/yyyy")
todayString = sdf.format(today)

/*def splitDate = date.split('/')
def shortYear = splitDate[2].getAt(2..3)*/
// println shortYear

// println splitDate[0] + ' ' + splitDate[2]

def tifCurrentDir = '/u/1stChoice/Share/TNG/tiffCurrentMan' 
def folder1 = new File(tifCurrentDir)
if( !folder1.exists() ){
    folder1.mkdirs()
}

//db = Sql.newInstance("jdbc:postgresql://localhost:5432/cops_reporting","postgres","ahab31",
//    "org.postgresql.Driver") 
    
db = Sql.newInstance("jdbc:postgresql://192.168.30.10:5432/cops_reporting","postgres","ahab31",
        "org.postgresql.Driver")

def deliveries = [:] 
def items = new HashSet() 
def bol_number = -1
bolList.each(){ bolQueryNum ->
    db.eachRow('SELECT * FROM cops_reporting.TNGVIEW2 WHERE bol_number = (\''+bolQueryNum+'\') AND signature_file_name like \'MAN_%\'') {
        delivery -> 
        if (bol_number != delivery.bol_number){
            bol_number = delivery.bol_number
            deliveries[delivery.bol_number] = new Stop() 
            deliveries[delivery.bol_number].actualServiceDate = delivery.actual_service_date
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
            deliveries[delivery.bol_number].routeDate = delivery.route_date
            if( delivery.signature_file_name == null && delivery.signature_file_name != null ) {
            	deliveries[delivery.bol_number].signatureFileName = missSigBaseDir+ '/' + deliveries[delivery.bol_number].getDeliveryYear() + '/' + deliveries[delivery.bol_number].getDeliveryMonth() + '/' + delivery.signature_file_name
			}else if( delivery.signature_file_name != null ){
				deliveries[delivery.bol_number].signatureFileName = missSigBaseDir+ '/' + deliveries[delivery.bol_number].getDeliveryYear() + '/' + deliveries[delivery.bol_number].getDeliveryMonth() + '/' + delivery.signature_file_name
			}else{
				deliveries[delivery.bol_number].signatureFileName = missSigBaseDir+ '/' + deliveries[delivery.bol_number].getDeliveryYear() + '/' + deliveries[delivery.bol_number].getDeliveryMonth() + '/' + delivery.signature_file_name
			}

            deliveries[delivery.bol_number].customerNo = delivery.customer_no
            deliveries[delivery.bol_number].createdBy = delivery.datetime_created
            deliveries[delivery.bol_number].routeId = delivery.route_code
            //println deliveries[delivery.bol_number].stopName
            def filename = delivery.bol_number
            if( filename == null ){
                filename = delivery.branch_id
            }
            
            
            deliveries[delivery.bol_number].pdfFileName = filename
            deliveries[delivery.bol_number].tiffDir = '/u/1stChoice/Share/TNG/tiffMan/' + deliveries[delivery.bol_number].getDeliveryYear() + '/' + deliveries[delivery.bol_number].getDeliveryMonth()  + '/'
            deliveries[delivery.bol_number].tiffFileName =  filename 

            def folder = new File(deliveries[delivery.bol_number].tiffDir)
            if( !folder.exists()){
                folder.mkdirs()
            }

            // println "Here "+ deliveries[delivery.bol_number].signatureFileName

        }
        deliveries[delivery.bol_number].items.push(delivery.item_number)    

    }
    
}
    

def delList = deliveries.keySet().toList()
//println delList

def sourceTemplate = this.class.getResource("SourceTemplate3.gtpl")

delList.each(){ delkey -> 
    
/*    println deliveries[delkey].stopName
    println deliveries[delkey].stopAddress
    println deliveries[delkey].stopCity
*/    //println "\t"
    //println deliveries[delkey].items 
    
/*    filename = filename+'_IMGMAN_'+deliveries[delkey].customerReference+'_'+deliveries[delkey].bolNumber
    if(deliveries[delkey].signatureFileName.equals("Error")){
        filename = 'bad/'+filename 
    }*/
    //println filename
    
    def binding = [source:deliveries[delkey]]
    def engine = new SimpleTemplateEngine()
    def template = engine.createTemplate(sourceTemplate)
    
    def tmp = File.createTempFile("template",".tmp", new File("tmp"))
    
    def fos = new FileOutputStream(tmp)
    fos << template.make(binding).toString().replaceAll("&", "&amp;")
    
    pdfOutFile = new FileOutputStream(new File('pdf/'+deliveries[delkey].pdfFileName+'.pdf'))
    
    ITextRenderer renderer = new ITextRenderer() 
    //println tmp.path
    renderer.setDocument(tmp) 
    renderer.layout() 
    renderer.createPDF(pdfOutFile)
     
    pdfOutFile.close() 
    
    tmp.delete()    
    //println template.toString()
}



/*def pdfFiles = new File('pdf').listFiles()*/

def numberOfFiles = delList.size()
def modulus1 = (Integer)(numberOfFiles/5)
def percentDone = 0 
def procCount = 0 
//print pd
delList.each{ delkey ->
    //println file.name.tokenize('.').first() 
    pdfName = 'pdf/'+deliveries[delkey].pdfFileName+'.pdf' 
    tifName = deliveries[delkey].tiffDir + '/' + deliveries[delkey].tiffFileName + '.tif'
    
    cmd = "convert -density 288 -compress Group4 "+ pdfName +" " + tifName; 
    def proc = cmd.execute()
    if(! (procCount % 5 )){ 
        proc.waitFor()
    }
    if(numberOfFiles < 5 ){
        
    }
    else if(! (procCount % modulus1) )
    {
        print "---"+percentDone+"% -----"
        percentDone = percentDone + 20 
    }
    //proc.waitFor()
    // file.delete()
    procCount++ 
}
indexFile220 = new File('/u/1stChoice/Share/TNG/tiffCurrentMan/ATL/Indexes.txt')
indexFile223 = new File('/u/1stChoice/Share/TNG/tiffCurrentMan/SAC/Indexes.txt')
indexFile404 = new File('/u/1stChoice/Share/TNG/tiffCurrentMan/404/Indexes.txt')


delList.each{ delkey -> 

    tifDirPath = Paths.get( deliveries[delkey].tiffDir + '/' + deliveries[delkey].tiffFileName + '.tif' )
    if( Files.exists(tifDirPath)){
        tifCurrentPath = Paths.get( tifCurrentDir + '/' + deliveries[delkey].tiffFileName + '.tif' )
// 		try{
//		
//         	Files.copy(tifDirPath,tifCurrentPath, StandardCopyOption.REPLACE_EXISTING)
//        }catch(IOException e){
//#         	println "Unsuccessfully tried to copy file" + deliveries[delkey].tiffFileName
//#         }
		 def copyCommand = 'cp '+ deliveries[delkey].tiffDir + deliveries[delkey].tiffFileName + '.tif /u/1stChoice/Share/TNG/tiffCurrentMan/' + TNGCity[deliveries[delkey].lineItemReference] + '/'
		 def proc = copyCommand.execute() 
		 proc.waitFor()
		 
		 println copyCommand 
		 println todayString 
		 
		 if (deliveries[delkey].lineItemReference == '220'){
             indexOutput = '\"' + deliveries[delkey].lineItemReference +'_Images\",' + '\"' + deliveries[delkey].lineItemReference +'_Images\",' +
                   '\"DOCNO\",' + '\"' + deliveries[delkey].customerReference + '\",' +
                   '\"SHIPTO\",' + '\"' + deliveries[delkey].bolNumber + '\",' +
                   '\"DATE\",' + '\"' + todayString + '\",' + 
                   '\"ftp2.tng.com \\3PL_images\\'+ deliveries[delkey].lineItemReference+ '\\' + deliveries[delkey].bolNumber + '.tif\",' + 
                   '\"D\"'
             indexFile220 << indexOutput << '\n'

          } else if( deliveries[delkey].lineItemReference == '223'){
             indexOutput = '\"' + deliveries[delkey].lineItemReference +'_Images\",' + '\"' + deliveries[delkey].lineItemReference +'_Images\",' +
                   '\"DOCNO\",' + '\"' + deliveries[delkey].customerReference + '\",' +
                   '\"SHIPTO\",' + '\"' + deliveries[delkey].bolNumber + '\",' +
                   '\"DATE\",' + '\"' + todayString + '\",' + 
                   '\"ftp2.tng.com \\3PL_images\\'+ deliveries[delkey].lineItemReference +'\\' + deliveries[delkey].bolNumber + '.tif\",' + 
                   '\"D\"'
     	     indexFile223 << indexOutput << '\n'

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



def pdfFiles = new File('pdf').listFiles()
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







