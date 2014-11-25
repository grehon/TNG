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
 
    def src = "/Volumes/SeagateBackupPlusDrive/1stChoice/eek.datatrac.com/eek.datatrac.com/palm/"+ monthYear+ '/'+bmp+'.bmp'
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
def missSigBaseDir = "/Volumes/SeagateBackupPlusDrive/1stChoice/MissingSignatures"
def sourceDC = ["EXO":"CLA", 
                "SDA":"CLA", 
                "SFS":"CLA", 
                "GLA":"CLA", 
                "GSO":"CLA", 
                "BAK":"CLA", 
                "FRE":"CLA", 
                "COU":"CLC", 
                "HBL":"CLC", 
                "ICT":"CLC", 
                "MCI":"CLC", 
                "SGF":"CLC", 
                "SPI":"CLC", 
                "STL":"CLC", 
                "TOP":"CLC", 
                "ATW":"CLC", 
                "DLH":"CLC", 
                "EAU":"CLC", 
                "KTO":"CLC", 
                "LSE":"CLC", 
                "MKE":"CLC", 
                "MSN":"CLC", 
                "MSP":"CLC",                 
                "NIA":"CLC", 
                "SAU":"CLC", 
                "SHO":"CLC", 
                "WAU":"CLC",
                "MN2":"CLC", 
                "FTW":"CLC", 
                "IND":"CLC", 
                "BEA":"CLC",
                "CIN":"CLC", 
                "TOB":"CLC",
                "CID":"CLC", 
                "DAV":"CLC", 
                "DSM":"CLC", 
                "FOD":"CLC",
                "MCW":"CLC",
                "MTP":"CLC",   
                "OMA":"CLC", 
                "SUX":"CLC", 
                "CMH":"CLC", 
                "CVG":"CLC", 
                "DAY":"CLC", 
                "SDF":"CLC", 
                "EVV":"CLC", 
                "ORF":"LUN", 
                "RIC":"LUN", 
                "ROA":"LUN", 
                "CLT":"LUN", 
                "GSP":"LUN", 
                "JFK":"LUN"
                ]
                    
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
    println date
}

/*def splitDate = date.split('/')
def shortYear = splitDate[2].getAt(2..3)*/
// println shortYear

// println splitDate[0] + ' ' + splitDate[2]

def tifDir = 'tiffMan/'+(queryDate.year +1900 )+'/'+ (queryDate.month +1).toString().padLeft(2,'0')
def folder = new File(tifDir)
if( !folder.exists()){
    folder.mkdirs()
}

//db = Sql.newInstance("jdbc:postgresql://localhost:5432/cops_reporting","postgres","ahab31",
//    "org.postgresql.Driver") 
    
db = Sql.newInstance("jdbc:postgresql://192.168.30.10:5432/cops_reporting","postgres","ahab31",
        "org.postgresql.Driver")

def deliveries = [:] 
def items = new HashSet() 
def bol_number = -1

db.eachRow('SELECT * FROM cops_reporting.SOURCEVIEW WHERE actual_service_date IN (\''+date+'\') AND signature_file_name like \'MAN_%\'') {
    delivery -> 
    if (bol_number != delivery.bol_number){
        bol_number = delivery.bol_number
        deliveries[delivery.bol_number] = new Stop() 
        deliveries[delivery.bol_number].actualServiceDate = delivery.actual_service_date
        deliveries[delivery.bol_number].customerReference = delivery.customer_reference
        deliveries[delivery.bol_number].branchId = delivery.branch_id
        deliveries[delivery.bol_number].bolNumber = delivery.bol_number.toString().padLeft(7,'0')
        deliveries[delivery.bol_number].stopName = delivery.stop_name
        deliveries[delivery.bol_number].stopAddress = delivery.stop_address
        deliveries[delivery.bol_number].stopCity = delivery.stop_city
        deliveries[delivery.bol_number].stopState = delivery.stop_state
        deliveries[delivery.bol_number].stopZipPostalCode = delivery.stop_zip_postal_code
        deliveries[delivery.bol_number].stopSignature = delivery.stop_signature 
        deliveries[delivery.bol_number].actualArrivalTime = delivery.actual_arrival_time
        deliveries[delivery.bol_number].signatureFileName = missSigBaseDir+ '/' +(delivery.actual_service_date.year +1900 ) + '/' + (delivery.actual_service_date.month +1).toString().padLeft(2,'0') + '/' + delivery.signature_file_name
        deliveries[delivery.bol_number].customerNo = delivery.customer_no
        deliveries[delivery.bol_number].createdBy = delivery.datetime_created
        deliveries[delivery.bol_number].routeId = delivery.route_code
        //println deliveries[delivery.bol_number].stopName
            
        // println "Here "+ deliveries[delivery.bol_number].signatureFileName
        
    }
    deliveries[delivery.bol_number].items.push(delivery.item_number)    
    
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
    
    def filename = sourceDC[deliveries[delkey].branchId] 
    if( filename == null ){
        filename = deliveries[delkey].branchId
    }
    
    filename = filename+'_IMGMAN_'+deliveries[delkey].customerReference+'_'+deliveries[delkey].bolNumber
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
//print pd
pdfFiles.each{ file ->
    //println file.name.tokenize('.').first() 
    pdfName = 'pdf/'+file.name 
    tifName = tifDir + '/' + file.name.tokenize('.').first() + '.tif'
    
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

pdfFiles.each{ file ->
    file.delete()
}




class Stop {
    Date actualServiceDate
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
	List items = []  	
	
}







