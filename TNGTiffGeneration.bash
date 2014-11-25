#!/bin/bash

#rsync -avz ghonig@192.168.30.8:~/Projects/palm/eek.datatrac.com /Volumes/SeagateBackupPlusDrive/1stChoice/eek.datatrac.com 

NOW=$(date +%Y-%m-%d) 

LOGFILE="logs/log-$NOW.log" 
LOGFILE2="logs/log-MarkIII-$NOW.log"
COPYFILE="logs/cp-$NOW.sh"
#TIFFCURRENT="/u/1stChoice/Share/TNG/tiffCurrent"
TIFFCURRENT="/Volumes/SeagateBackupPlusDrive/1stChoice/TNG/tiffCurrent"

for i in `ls $TIFFCURRENT`
do 
   rm $TIFFCURRENT/$i/*
done 

echo $LOGFILE
echo $LOGFILE2

# rm $TIFFCURRENT/404/*
# rm $TIFFCURRENT/ATL/*
# rm $TIFFCURRENT/SAC/*

#for i in {7..13}  
#for i in {10..16}  
for i in {13..7}  
do 
	groovy tif_generation.groovy -s $i >> $LOGFILE 2>&1
	groovy tif_generation_MarkIII.groovy -s $i >> $LOGFILE2 2>&1 
done 

grep 'cp /Volumes' $LOGFILE > $COPYFILE 
grep 'cp /Volumes' $LOGFILE2 >> $COPYFILE

sh $COPYFILE

for i in `ls $TIFFCURRENT`
do 
#    ncftpput -f ~/Projects/1stChoice/TNG/TNG/login.cfg images/"$i"tmp $TIFFCURRENT/$i/*
done
    

#
#for i in `ls tiffCurrent/`; do ncftpput -f ~/Projects/1stChoice/TNG/TNG/login.cfg images/$itmp tiffCurrent/$i/* ; done
