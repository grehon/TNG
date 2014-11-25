<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" 
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
   

<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  <style> 
    @page land {size:landscape; }
    .landscapePage { page:land; width: 29.7cm; }
  
  </style>
  </head>
  <div class="landscapePage">
  <body>


    <table width="1000px">

    	<tr>
        	<td width="500px" valign="top">
             <p>Delivered by:</p>
             <div style="padding-left:50px">
             	1st Choice for TNG<br/>
                ${source?.branchId} Branch <br/>
                Route # ${source?.routeId} <br/>
                <br />
            
                Planned Delivery Date: ${source?.routeDate}<br/>
                Delivery Time: ${source?.actualArrivalTime}<br/>
                Customer #:    ${source?.customerReference}<br/> 
                Invoice #:  ${source?.bolNumber} <br/>
                Signed by: ${source?.stopSignature}      	
         
        
            </div></td>
        
        
            <td width="500px" valign="top">
            	<p>Delivered to:</p>
                <div style="padding-left:50px">
             	${source?.stopName}<br/>
                ${source?.stopAddress} <br/>
                ${source?.stopCity}, ${source?.stopState}, ${source?.stopZipPostalCode} <br/>
            
                <img src="/Users/ghonig/Projects/1stChoice/TNG/TNG/${source?.signatureFileName}"/> 
             </div>
        
            </td>
        </tr>
    
    </table>


    <div style="padding-left:30px; width:1000px">
    	<div>Container #'s</div>

        <div style="padding-left:30px">
        	<table>
        	  <% source?.items.each{ item -> %> <tr> <td> <%= item %> </td></tr> <%} %>

          </table>
        </div>
    </div>
    
  </body>
  </div>

</html>