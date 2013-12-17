#!/bin/sh

help(){
  echo "usage: `basename $0` -s <source dir> -j <jar dir> -o <output file for jars> -c <output file for .class>"
  echo ""
  echo "return a list of jar or .class needed by a java project"
  echo "it also tells you want class it wasn't able to match"
  echo ""
  echo "arguments:"
  echo "  -s <source dir>: directory containing the project sources"
  echo "  -j <jar dir>: directory containing the jars or the .class"
  echo "  -o <output file for jars>: file for the list of matching jars"
  echo "  -c <output file for .class>: file for the list of matching .class files"
  exit 1
}

while getopts ":hs:j:o:c:" opt; do
  case $opt in

    h) 
        help
        ;;
    s)
        SRC_DIR="$OPTARG"
        ;;
    j)
        JAR_DIR="$OPTARG"
        ;;
    o)
        OUT_FILE="$OPTARG"
        ;;
    c)
        OUT_CLASS="$OPTARG"
        ;;

    \?)
        echo "Invalid option: -$OPTARG" >&2
        help
        exit 1
        ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
        help
        exit 1
        ;;
  esac
done

#All args a mandotory
if [ -z "$SRC_DIR" ] || [ -z "$JAR_DIR" ] || [ -z "$OUT_FILE" ] || [ -z "$OUT_FILE" ]
then
    help
    exit 1
fi

if [ -f $OUT_FILE ]
then
    printf "file '$OUT_FILE' already exists, change the name or supress it\n"
    exit 1
fi

if [ -f $OUT_CLASS ]
then
    printf "file '$OUT_CLASS' already exists, change the name or supress it\n"
    exit 1
fi

#cleaning function
clean(){
    rm $import_tmp
    rm $jar_tmp
    rm $matched_lib
    rm $matched_lib_sorted
    rm $unmatched_lib
}

#a trap to clean those messy tmp files
trap clean HUP INT TERM

import_tmp=`mktemp`
jar_tmp=`mktemp`
matched_lib=`mktemp`
matched_lib_sorted=`mktemp`
unmatched_lib=`mktemp`

#we get all the imports from the .java and we reformat then to have a path (. -> /)
for f in `find $SRC_DIR -name "*.java"`
do
    grep "^import" $f|sed "s/import[\t\ ]*//" |sed "s/\./\//g"|sed "s/;$//"
done |sort -u >$import_tmp

#a small counter
number_of_jar=`find $JAR_DIR -name "*.jar" |wc -l`
counter=0

#we iterate on all the jar from $JAR_DIR (option -j from the command line)
for j in `find $JAR_DIR -name "*.jar"`
do  
    printf "$counter jar(s) of $number_of_jar jar(s) done (current: $j)\n"
    counter=$(( $counter + 1 ))
    #we try to find every import in each jar)
    while read line
    do
        unzip -l $j |grep -q "$line" 
        ret=$?
        if [ $ret -eq 0 ]
        then
            printf "$j matched for $line\n"
            printf "$j\n" >>$jar_tmp
            printf "$line\n" >>$matched_lib
        fi
    done < $import_tmp
done

cat $jar_tmp |sort -u >$OUT_FILE
cat $matched_lib|sort -u >$matched_lib_sorted
#we get the classes we didn't matched in the jars
diff $matched_lib_sorted $import_tmp |grep "^>"|sed "s/^>\ *//" >$unmatched_lib

#we try to directly find a matching .class
while read line
do
    class_file="`basename $line`.class"
    class=`find $JAR_DIR -iname "$class_file"`
    if [ -z "$class" ]
    then
        printf "$line was not found\n"
    else
        printf "$class\n" >>$OUT_CLASS
    fi
done < $unmatched_lib

clean
