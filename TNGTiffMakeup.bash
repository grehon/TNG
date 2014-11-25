#!/bin/bash

#rsync -avz ghonig@192.168.30.8:~/Projects/palm/eek.datatrac.com /Volumes/SeagateBackupPlusDrive/1stChoice/eek.datatrac.com 

NOW=$(date +%Y-%m-%d) 

LOGFILE="logs/log-$NOW.log" 
LOGFILE2="logs/log-MarkIII-$NOW.log"
COPYFILE="logs/cp-$NOW.sh"
#TIFFCURRENT="/u/1stChoice/Share/TNG/tiffCurrent"
TIFFCURRENT="/Volumes/SeagateBackupPlusDrive/1stChoice/TNG/tiffCurrent"
TIFFCURRENTBYDATE="/Volumes/SeagateBackupPlusDrive/1stChoice/TNG/tiffCurrent$NOW"

mkdir $TIFFCURRENTBYDATE
# 
for i in `ls $TIFFCURRENT`
do 
    rm $TIFFCURRENT/$i/*
done 

echo $LOGFILE
echo $LOGFILE2

function vpn-connect {
/usr/bin/env osascript <<-EOF
tell application "System Events"
        tell current location of network preferences
                set VPN to service "1stChoice" -- your VPN name here
                if exists VPN then connect VPN
                repeat while (current configuration of VPN is not connected)
                    delay 1
                end repeat
        end tell
end tell
EOF
}

# rm $TIFFCURRENT/404/*
# rm $TIFFCURRENT/ATL/*
# rm $TIFFCURRENT/SAC/*
#$ITERATOR = 0 
#for i in {7..13}  
#for i in {10..16}  
for i in {20..12}
#for i in {35..20}
#for i in {94..64} 
#for i in {129..44}  
#for i in {169..94} 
do 
    vpn-connect >> $LOGFILE 2>&1
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
    

#rsync -avz ~/Projects/1stChoice/Grails/Source2/tiff ghonig@dtrac01:/u/Source/
#for i in `ls tiffCurrent/`; do ncftpput -f ~/Projects/1stChoice/TNG/TNG/login.cfg images/$itmp tiffCurrent/$i/* ; done
#echo * | xargs ncftpput -f ~/Projects/1stChoice/TNG/TNG/login.cfg images/JAKtmp
#echo * | xargs ncftpput -f ~/Projects/1stChoice/TNG/TNG/login.cfg images/ATLtmp