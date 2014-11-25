#!/bin/bash

#rsync -avz ghonig@192.168.30.8:~/Projects/palm/eek.datatrac.com /Volumes/SeagateBackupPlusDrive/1stChoice/eek.datatrac.com 

for i in {1..7} ; do groovy tif_generation_sig_problems.groovy -s $i >> GregAnalysis3.csv ; done 
