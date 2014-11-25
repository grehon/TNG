#!/bin/bash


TIFFCURRENT="/Volumes/SeagateBackupPlusDrive/1stChoice/TNG/tiffCurrent"

for i in `ls $TIFFCURRENT`
do 
    ncftpput -f ~/Projects/1stChoice/TNG/TNG/login.cfg images/"$i"tmp $TIFFCURRENT/$i/*
done
