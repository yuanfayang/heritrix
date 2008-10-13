#!/bin/bash
OUTPUT_DIR=../../../target/test/output
mkdir -p $OUTPUT_DIR
sh $JBOSS_HOME/bin/run.sh  >> $OUTPUT_DIR/jboss.out 2>&1 &
