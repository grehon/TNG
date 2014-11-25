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
    //def src = '/Volumes/SpinDrive1/1stChoice/eek.datatrac.com/eek.datatrac.com/palm/'+ monthYear+ '/'+bmp+'.bmp'
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
                "JFK":"LUN",
                "STF":"CLC", 
                "DSH":"CLC", 
                "EFC":"CLC" 
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

def splitDate = date.split('/')
def shortYear = splitDate[2].getAt(2..3)
// println shortYear

// println splitDate[0] + ' ' + splitDate[2]

def tifDir = 'tiff/'+splitDate[2]+'/'+splitDate[0]
def folder = new File(tifDir)
if( !folder.exists()){
    folder.mkdirs()
}

//db = Sql.newInstance("jdbc:postgresql://localhost:5432/cops_reporting","postgres","ahab31",
//    "org.postgresql.Driver") 
    
//db = Sql.newInstance("jdbc:postgresql://dtrac02:5432/cops_reporting","postgres","ahab31",
db = Sql.newInstance("jdbc:postgresql://192.168.30.10:5432/cops_reporting","postgres","ahab31",
        "org.postgresql.Driver")
def deliveries = [:] 
def items = new HashSet() 
def bol_number = -1

db.eachRow('SELECT * FROM cops_reporting.SOURCEVIEW WHERE actual_service_date IN (\''+date+'\')') {
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
        deliveries[delivery.bol_number].signatureFileName = splitDate[0] + splitDate[2] + '/' + delivery.signature_file_name
        deliveries[delivery.bol_number].customerNo = delivery.customer_no
        deliveries[delivery.bol_number].createdBy = delivery.datetime_created
        deliveries[delivery.bol_number].routeId = delivery.route_code
        //println deliveries[delivery.bol_number].stopName
        bitmapReturnVal = convertBmp(delivery.signature_file_name,splitDate[0] + shortYear )
        if( !delivery.signature_file_name){
            deliveries[delivery.bol_number].signatureState = "Blank"
        }else if( bitmapReturnVal =~ /bmp/ ){
            deliveries[delivery.bol_number].signatureState = "Good"
        }else{
            deliveries[delivery.bol_number].signatureState = "Bad"
            
        }
            
        // println "Here "+ deliveries[delivery.bol_number].signatureFileName
        
    }
    deliveries[delivery.bol_number].items.push(delivery.item_number)    
    
}

def delList = deliveries.keySet().toList()
//println delList


delList.each(){ d -> 
    
/*    println deliveries[delkey].stopName
    println deliveries[delkey].stopAddress
    println deliveries[delkey].stopCity
*/    //println "\t"
    //println deliveries[delkey].items 

    println deliveries[d].customerReference + '%' + deliveries[d].bolNumber + '%' + deliveries[d].branchId + '%' + deliveries[d].stopName + '%' + deliveries[d].stopAddress + '%' + 
        deliveries[d].stopCity + '%' + deliveries[d].stopState + '%' + deliveries[d].stopZipPostalCode + '%' + deliveries[d].routeId + '%' + deliveries[d].actualServiceDate + '%' +
        deliveries[d].actualArrivalTime + '%' + deliveries[d].createdBy + '%' + deliveries[d].stopSignature + '%' + deliveries[d].signatureFileName + '%' + deliveries[d].signatureState
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
	String signatureState
	List items = []  	
	
}







