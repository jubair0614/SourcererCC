#!/bin/bash
counter=1
for queryfile in  `ls query*`
do
  foldername="NODE_"$counter"/query/"
  rm -rf $foldername
  mkdir -p $foldername
  mv $queryfile $foldername/
  cp sourcerer-cc.properties "NODE_"$counter/
  counter=$((counter+1))
done
