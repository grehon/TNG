#!/bin/bash

# rsync -avz ghonig@192.168.30.8:~/Projects/palm/eek.datatrac.com /Volumes/SeagateBackupPlusDrive/1stChoice/eek.datatrac.com 

NOW=$(date +%Y-%m-%d) 
#THEN=$(date -v -18d +%m%d%y)
THEN=$(date -d -"-19 day" +%m%d%y)

LOGFILE="logs/log-MAN-$NOW.log" 
echo $LOGFILE
echo $THEN
RUNPATH="/mnt/SOURCE/TNG/ManualSignaturesGoHere/WE$THEN"
echo $RUNPATH
PROCESSFILE="bol$NOW.process" 
COPYFILE="logs/cp-$NOW.sh"

rm /u/1stChoice/Share/TNG/tiffCurrentMan/ATL/*.tif
rm /u/1stChoice/Share/TNG/tiffCurrentMan/SAC/*.tif
rm /u/1stChoice/Share/TNG/tiffCurrentMan/404/*.tif 

nohup groovy tngMissingSignatures.groovy -d $RUNPATH 
nohup groovy tif_generation_man_sig_markII.groovy -b $PROCESSFILE > $LOGFILE 2>&1 

grep 'cp tiff' $LOGFILE > $COPYFILE


